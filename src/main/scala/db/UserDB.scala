package db

import akka.actor._
import java.net.InetSocketAddress
import user.User
import scala.collection.mutable.HashMap
import user.User.UserInfo
import com.github.mauricio.async.db.{QueryResult, RowData}
import scala.Some
import util.UniqueIdGenerator
import scala.concurrent.Future
import scala.util.{Success, Failure}
import java.util.UUID

/**
 * Created by Pahomov Dmitry <topt.iiiii@gmail.com> on 02.03.14.
 */

trait UserQueries {
  this: DB =>
  object Query {
    def addUser(nickname: String, pwhash: String) = execute("INSERT INTO users(nickname, pwhash, userinfo) VALUES (?, ?, ?)",nickname, pwhash, "")
    def getUser(nickname: String, pwhash: String) = execute("SELECT id FROM users WHERE nickname=? AND pwhash=?", nickname, pwhash)
    def makeFriends(owner: String, friend: String) = execute("INSERT INTO friends(owner, friend) SELECT u1.id, u2.id FROM users AS u1, users AS u2 WHERE u1.nickname=? AND u2.nickname=? AND NOT EXISTS (SELECT owner, friend FROM friends WHERE owner = u1.id AND friend=u2.id)", owner, friend)
    def getFriendList(nickname: String) = fetch("SELECT u2.nickname FROM users AS u1, users AS u2, friends WHERE u1.nickname=? AND u1.id=friends.owner AND u2.id=friends.friend", nickname)
  }
}

object UserDBProps {
  def props() = Props(classOf[UserDB])
}

object UserDB {
  case class Token(val s: String) {
    override def equals(arg0: Any) = {
      if (arg0.isInstanceOf[String]) s==arg0
      else if (arg0.isInstanceOf[Token]) s== (arg0.asInstanceOf[Token].s)
      else false
    }
    override def hashCode() = s.hashCode()
    override def toString() = s.toString
  }

  object TokenedMessage {
    def unapply(t: TokenedMessage) = Some(t.t)
  }

  sealed abstract trait Message
  class TokenedMessage(val t: Token) extends Message
  case class Register(nickname: String, pwhash: String) extends Message
  case class Authorise(nickname: String, pwhash: String, ip: InetSocketAddress) extends Message
  case class Logout(token: Token) extends TokenedMessage(token)
  case class Getip(token: Token, nickname: String) extends TokenedMessage(token)
  case class AddFriend(owner: Token, friend: String) extends TokenedMessage(owner)
  case class GetFriends(token: Token) extends TokenedMessage(token)
  case class ConnectionClosed() extends Message

  sealed abstract trait Reply
  case class Registered(nickname: String) extends Reply
  case class Authorised(nickname: String, sesid: Token) extends Reply
  case class Logouted(nickname: String) extends Reply
  case class Userip(nickname: String, ip: InetSocketAddress) extends Message
  case class FriendAdded(owner: String, friend: String) extends Reply
  case class FriendList(nickname: String, list: Seq[(String, Boolean)]) extends Reply
  case class Error(msg: String) extends Reply
}

class UserDB extends Actor with ActorLogging with DB with UserQueries {
  import UserDB._

  def system = context.system

  override implicit def dispatcher = context.dispatcher

  val offline = 0
  val online = 1

  case class UserSession(socketactor: ActorRef,
                         nickname: String,
                         userip: InetSocketAddress,
                         status: Int)
  private val _sessions = HashMap[Token, UserSession]()

  def receive: Receive = {
    case Register(nickname, pwhash) => newUser(nickname, pwhash)
    case Authorise(nickname, pwhash, ip) => authoriseUser(nickname, pwhash, ip)
    case Logout(token) if checkToken(token) => logoutUser(token)
    case Getip(token, nickname) if checkToken(token) => getUserIp(token, nickname)
    case AddFriend(token, friend) if checkToken(token) => addFriendToUser(token, friend)
    case GetFriends(token) if checkToken(token) => getUserFriends(token)
    case ConnectionClosed() => makeOffline(sender)
    case TokenedMessage(token) => sender ! Error("bad token "+token) // TODO - remove token check from functions
  }

  def checkToken(t: Token) = {
    session(t) match {
      case Some((_, UserSession(ar, _, _, _) )) => ar.equals(sender)
      case None => false
    }
  }

  def printAll() {
    log.debug("values in db are:")
    for {
      queryResult <- fetch("SELECT * FROM pro")
      resultSet <- queryResult
      rowData <- resultSet
      result = getData(rowData)
    } log.debug(result)
  }

  def getData(rowData: RowData) = {
    rowData("data").asInstanceOf[String]
  }

  def makeSecureRandomString() = UniqueIdGenerator()

  def session(token: Token) = _sessions.par.get(token) match {
    case Some(u: UserSession) => Some( (token,u) )
    case None => None
  }
  def session(nickname: String) = _sessions.par.find{ case (_,UserSession(_, elnickname, _, _)) => elnickname==nickname }

  def nickname(token: Token) = {
    _sessions.par.get(token) match {
      case Some(UserSession(_, nickname,_ , _)) => Some(nickname)
      case None => None
    }
  }

  def isOnline(nick: String) = session(nick).nonEmpty
  def isOnline(token: Token) = _sessions.par.contains(token)

  def newUser(nickname: String, pwhash: String) {
    val connection = sender
    if (!"\\A\\w+\\z".r.pattern.matcher(nickname).matches) {
      connection ! Error("bad nickname")
      return
    }
    Query.addUser(nickname, pwhash).map {
      queryResult  =>
        log.debug("newUser insertions complite " + queryResult)
        connection ! Registered(nickname)
    } onFailure  {
      case e  =>
        log.debug("newUser insertions error: " + e)
        connection ! Error("nickname already exists")
    }
  }

  def authoriseUser(nickname: String, pwhash: String, ip: InetSocketAddress) {
    val connection = sender
    Query.getUser(nickname, pwhash).map {
      queryResult =>
        queryResult.rows match {
          case Some(rows) =>
            if (rows.nonEmpty) {
              val token = Token(makeSecureRandomString())
              //val userid = rows(0)("id").asInstanceOf[Int]
              _sessions.put(token, UserSession(connection, nickname, ip, online))
              connection ! Authorised(nickname, token)
            } else {
              connection ! Error("bad nickname or password")
            }
          case None => connection ! Error("cannot authorise this user (query error?)")
        }
    }
  }

  def logoutUser(token: Token) {
    _sessions.par.remove(token) match {
      case Some( UserSession(socketactor, nickname, _,_)) => socketactor ! Logouted(nickname)
      case None => sender ! Error("nothing to logout for "+token)
    }
  }

  def getUserIp(token: Token, nick: String) {
    session(nick) match {
      case Some((_, UserSession(_, _, ip, _) )) => sender ! Userip(nick, ip)
      case None => sender ! Error("cannot get ip for "+nick)
    }
  }



  def addFriendToUser(token: Token, friend: String) {
    val connection = sender
    val owner = nickname(token).getOrElse("")
    Query.makeFriends(owner, friend).map {
      _ => connection ! FriendAdded(owner, friend)
    } onFailure  {
      case e  =>
        log.debug("AddFriendToUser error: " + e)
        connection ! Error("cannot add friend")
    }
  }

  def getUserFriends(token: Token) {
    val connection = sender
    val nick = nickname(token).getOrElse("")
    Query.getFriendList(nick) map {
      q => q match {
        case Some(rows) =>
          for {
            row <- rows
            friendnick = row("nickname").asInstanceOf[String]
            isonline = isOnline(friendnick)
          } yield (friendnick, isonline)
        case _ => Seq()
      }
    } onComplete {
      case Success(s) => connection ! FriendList(nick, s)
      case Failure(t) => connection ! Error("friendlisterror "+t.getMessage)
    }
  }

  def makeOffline(sender: ActorRef) {
    for {
      (token, _) <- _sessions.par.find{ case (_, UserSession(a, _, _, _)) => a.equals(sender) }
    } _sessions -= token
  }
}

//SELECT users.nickname FROM friends,users WHERE users.id=friends.friend AND friends.owner=1
