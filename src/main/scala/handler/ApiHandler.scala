package handler

import akka.actor.{ Props, ActorRef, Actor, ActorLogging }
import akka.pattern.ask
import akka.util.ByteString
import scala.concurrent.duration._
import java.net.InetSocketAddress
import scala.language.postfixOps
import db.UserDB
import util.UniqueIdGenerator
import spray.can.Http
import spray.can.server.Stats
import spray.util._
import spray.http._
import HttpMethods._
import MediaTypes._
import akka.io.IO
import util._

class ApiHandler() extends Actor with ActorLogging { // TODO - rename
  import context.system

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

  //context.setReceiveTimeout(180000 milliseconds)
  IO(Http) ! Http.Bind(self, ConfExtension(system).appHostName, ConfExtension(system).appPort)

  def receive: Receive = {
    case Http.CommandFailed(_: Http.Bind) => context stop self

    case Http.Connected(remote, local) =>
      sender ! Http.Register(self)

    case HttpRequest(GET, Uri.Path("/"), _, _, _) =>
      send("hello", sender)

    case HttpRequest(_, _, _, _, _) =>
      error("Bad request", sender)

    /*case UserDB.Registered(nickname) =>
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
      send("friendlist "+nickname+" "+seq.mkString(","))*/

    case Timedout(HttpRequest(method, uri, _, _, _)) =>
      sender ! HttpResponse(
        status = 500,
        entity = "The " + method + " request to '" + uri + "' has timed out..."
      )
    case ev: Http.ConnectionClosed => stop()
  }

  def received(data: ByteString) {
    val client = sender
    data.utf8String.trim match {
      /*case abort() => connection ! Abort
      case confirmedClose() => connection ! ConfirmedClose
      case close() => connection ! Close*/
      case registration(nickname, pwhash) => UsersDBRef ! UserDB.Register(nickname, pwhash)
      case authorisation(nickname, pwhash) => UsersDBRef ! UserDB.Authorise(nickname, pwhash, new InetSocketAddress("0.0.0.0", 5555)) // TODO - fix addr
      case logout(token) => UsersDBRef ! UserDB.Logout(UserDB.Token(token))
      case update() => send("conupdated", client)
      case getip(token, nickname) => UsersDBRef ! UserDB.Getip(UserDB.Token(token), nickname)
      case addfriend(token, nickname) => UsersDBRef ! UserDB.AddFriend(UserDB.Token(token), nickname)
      case getfriends(token) => UsersDBRef ! UserDB.GetFriends(UserDB.Token(token))
      case _ => error("command syntax error", client)
    }
  }

  def send(msg: String, client: ActorRef) {
    client ! HttpResponse(entity = msg)
  }

  def error(msg: String, client: ActorRef) {
    client ! HttpResponse(status = 500, entity = msg)
  }

  def stop() {
    log.debug("Stopping")
    //UsersDBRef ! UserDB.ConnectionClosed
    //context stop self
  }
}