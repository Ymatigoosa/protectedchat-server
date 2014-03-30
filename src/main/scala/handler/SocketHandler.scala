package handler

import akka.actor.{ Props, ActorRef, Actor, ActorLogging }
import akka.io.Tcp._
import akka.io.Tcp.Received
import akka.util.ByteString
import scala.util.parsing.json._
import akka.actor.ReceiveTimeout
import scala.concurrent.duration._
import java.net.InetSocketAddress
import scala.language.postfixOps
import db.UserDB
import util.UniqueIdGenerator

object SocketHandlerProps extends HandlerProps{
  def props(connection: ActorRef, remote: InetSocketAddress) = Props(classOf[SocketHandler], connection, remote)
}

class SocketHandler(val connection: ActorRef, val remote: InetSocketAddress) extends Actor with ActorLogging {

  val abort = "abort".r
  val confirmedClose = "confirmedclose".r
  val close = "close".r
  val registration = "registration\\s+(\\w+)\\s+(\\w+)".r
  val authorisation = "authorisation\\s+(\\w+)\\s+(\\w+)".r
  val logout = "logout\\s+([-0-9a-zA-Z]+)".r
  val update = "update".r
  val getip = "getip\\s+([-0-9a-zA-Z]+)\\s+(\\w+)".r
  val addfriend = "addfriend\\s+([-0-9a-zA-Z]+)\\s+(\\w+)".r
  val getfriends = "getfriends\\s+([-0-9a-zA-Z]+)".r

  val UsersDBRef = context.actorSelection("akka://server/user/UserDBActor")
  val myname = self.path.name

  context.setReceiveTimeout(180000 milliseconds)
  println("SocketHandler started for "+remote.toString)

  def receive: Receive = {
    case Received(data) =>
      received(data)
    case UserDB.Registered(nickname) =>
      send("registered "+nickname)
    case UserDB.Authorised(nickname, token) =>
      send("authorised "+nickname+" "+token)
    case UserDB.Error(msg) =>
      error(msg)
    case UserDB.Logouted(nickname) =>
      send("logouted "+nickname)
    case UserDB.Userip(nickname, ip) =>
      send("ip "+nickname+" "+ip)
    case UserDB.FriendAdded(owner, friend) =>
      send("friendadded "+owner+" "+friend)
    case UserDB.FriendList(nickname, seq) =>
      send("friendlist "+nickname+" "+seq.mkString(","))
    case PeerClosed =>
      log.debug("PeerClosed")
      stop()
    case ErrorClosed =>
      log.debug("ErrorClosed")
      stop()
    case Closed =>
      log.debug("Closed")
      stop()
    case ConfirmedClosed =>
      log.debug("ConfirmedClosed")
      stop()
    case Aborted =>
      log.debug("Aborted")
      stop()
    case ReceiveTimeout =>
      log.debug("TimeoutReceived")
      stop()
  }

  def received(data: ByteString) {
    data.utf8String.trim match {
      case abort() => connection ! Abort
      case confirmedClose() => connection ! ConfirmedClose
      case close() => connection ! Close
      case registration(nickname, pwhash) => UsersDBRef ! UserDB.Register(nickname, pwhash)
      case authorisation(nickname, pwhash) => UsersDBRef ! UserDB.Authorise(nickname, pwhash, remote)
      case logout(token) => UsersDBRef ! UserDB.Logout(UserDB.Token(token))
      case update() => send("conupdated")
      case getip(token, nickname) => UsersDBRef ! UserDB.Getip(UserDB.Token(token), nickname)
      case addfriend(token, nickname) => UsersDBRef ! UserDB.AddFriend(UserDB.Token(token), nickname)
      case getfriends(token) => UsersDBRef ! UserDB.GetFriends(UserDB.Token(token))
      case _ => error("command syntax error")
    }
  }

  def send(msg: String) {
    log.debug("sended "+msg+" to "+connection.toString())
    connection ! Write(ByteString(msg+"\n"))
  }

  def error(msg: String) = send("error "+"\""+msg+"\"")

  def stop() {
    log.debug("Stopping")
    UsersDBRef ! UserDB.ConnectionClosed
    context stop self
  }

  def RemoteIp = remote
}