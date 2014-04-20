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
import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * Created by Pahomov Dmitry <topt.iiiii@gmail.com> on 02.03.14.
 */

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
  case class Register(nickname: String, pw: String) extends Message
  case class Authorise(nickname: String, pw: String, ip: String) extends Message
  case class Logout(token: Token) extends TokenedMessage(token)
  case class Getip(token: Token, nickname: String) extends TokenedMessage(token)
  case class AddFriend(owner: Token, friend: String) extends TokenedMessage(owner)
  case class RemoveFriend(owner: Token, friend: String) extends TokenedMessage(owner)
  case class GetFriends(token: Token) extends TokenedMessage(token)
  case class Update(token: Token) extends TokenedMessage(token)
  case class ConnectionClosed() extends Message
  case class OfflineInactive() extends Message

  sealed abstract trait Reply
  case class Registered(nickname: String) extends Reply
  case class Authorised(nickname: String, sesid: Token) extends Reply
  case class Logouted(nickname: String) extends Reply
  case class Userip(nickname: String, ip: String) extends Message
  case class FriendAdded(owner: String, friend: String) extends Reply
  case class FriendList(nickname: String, list: Seq[(String, Boolean)]) extends Reply
  case class FriendDeleted(owner: String, friend: String) extends Reply
  case class Updated(msg: String) extends Reply
  case class Error(msg: String) extends Reply
}

class UserDB extends Actor with ActorLogging with DB with UserQueries {
  import UserDB._

  def system = context.system

  override implicit def dispatcher = context.dispatcher

  val SALT = "bachelorpower"
  val TOKENSALT = "motog"
  val online = 1
  val MAXIDLE: Long = 60000

  context.system.scheduler.schedule(MAXIDLE milliseconds,MAXIDLE milliseconds, self, OfflineInactive())

  case class UserSession(lastActivity: Long,
                         nickname: String,
                         userip: String,
                         status: Int)

  private val _sessions = HashMap[Token, UserSession]()

  def receive: Receive = {
    case Register(nickname, pw) =>
      newUser(nickname, pw)

    case Authorise(nickname, pw, ip) =>
      authoriseUser(nickname, pw, ip)

    case Logout(token) if checkToken(token) =>
      logoutUser(token)

    case Getip(token, nickname) if checkToken(token) =>
      updateLastActivity(token)
      getUserIp(token, nickname)

    case AddFriend(token, friend) if checkToken(token) =>
      updateLastActivity(token)
      addFriendToUser(token, friend)

    case RemoveFriend(token, friend) if checkToken(token) =>
      updateLastActivity(token)
      removeFriendToUser(token, friend)

    case GetFriends(token) if checkToken(token) =>
      updateLastActivity(token)
      getUserFriends(token)

    case Update(token) if checkToken(token) =>
      updateLastActivity(token)
      sender ! Updated(nickname(token).get)

    case OfflineInactive() =>
      offlineInactive()

    case TokenedMessage(token) =>
      sender ! Error("bad token "+token) // TODO - remove token check from functions
  }

  def checkToken(t: Token) = _sessions contains t

  def makeSecureRandomString(us: UserSession) = us match {
    case UserSession(time, nickname, ip, _) => md5(nickname + time.toString + TOKENSALT + ip)
  }


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

  def updateLastActivity(t: Token) {
    _sessions.par.get(t) match {
      case Some(UserSession(_, nickname, userip , status)) => _sessions.update(t, UserSession(System.currentTimeMillis, nickname, userip , status))
      case _ => log.error("there is no user to update last activity")
    }
  }

  def newUser(nickname: String, pw: String) {
    val connection = sender
    val pwhash = md5(nickname+pw+SALT)
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
        log.debug("newUser insertions error: " + e + e.getMessage)
        connection ! Error("nickname already exists")
    }
  }

  def authoriseUser(nickname: String, pw: String, ip: String) {
    val connection = sender
    val pwhash = md5(nickname+pw+SALT)
    Query.getUser(nickname, pwhash).map {
      queryResult =>
        queryResult.rows match {
          case Some(rows) =>
            if (rows.nonEmpty) {
              val us = UserSession(System.currentTimeMillis, nickname, ip, online)
              val token = Token(makeSecureRandomString(us))
              //val userid = rows(0)("id").asInstanceOf[Int]
              _sessions.put(token, us)
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
      case Some( UserSession(_, nickname, _,_)) => sender ! Logouted(nickname)
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
    Query.makeFriends(owner, friend) onComplete {
      case Success(s) => connection ! FriendAdded(owner, friend)
      case Failure(t)  =>
        log.debug("AddFriendToUser error: " + e)
        connection ! Error("cannot add friend")
    }
  }

  def removeFriendToUser(token: Token, friend: String) {
    val connection = sender
    val owner = nickname(token).getOrElse("")
    Query.unfriend(owner, friend) onComplete{
      case Success(s) => connection ! FriendDeleted(owner, friend)
      case Failure(t) => connection ! Error("cannot detele friend"+t.getMessage)
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

  def offlineInactive() {
    val currenttime = System.currentTimeMillis
    log.debug("making offline idling users")
    _sessions.retain{
      case (_, UserSession(time, _, _, _)) => time+MAXIDLE > currenttime
    }
    lazy val d = _sessions map {case (t, UserSession(_, n, _, _)) => s"$t => $n"} mkString("\n")
    log.debug(d)
  }

  def md5( s:String ) : String = {
    // Besides "MD5", "SHA-256", and other hashes are available
    val m = java.security.MessageDigest.getInstance("MD5").digest(s.getBytes("UTF-8"));
    m map {c => (c & 0xff) toHexString} mkString
  }
}