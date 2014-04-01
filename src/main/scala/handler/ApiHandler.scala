package handler

import akka.actor.{ Props, ActorRef, Actor, ActorLogging }
import akka.pattern.ask
import akka.util.ByteString
import scala.concurrent.duration._
import java.net.InetSocketAddress
import scala.language.postfixOps
import db.UserDB
import spray.can.Http
import spray.can.server.Stats
import spray.util._
import spray.http._
import HttpMethods._
import MediaTypes._
import StatusCodes._
import akka.io.IO
import util._
import scala.util.parsing.json.{JSONObject}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.json.extensions._
import play.api.libs.json.monad._
import play.api.libs.json.monad.syntax._

class ApiHandler() extends Actor with ActorLogging { // TODO - rename
  import context.system

  val UsersDBRef = context.actorSelection("akka://server/user/UserDBActor")

  IO(Http) ! Http.Bind(self, ConfExtension(system).appHostName, ConfExtension(system).appPort)

  def receive: Receive = {
    case Http.CommandFailed(_: Http.Bind) => context stop self

    case Http.Connected(remote, local) =>
      sender ! Http.Register(self)

    case HttpRequest(POST, Uri.Path("/api/json/"), _, entity, _) =>
      processJson(entity.asString, sender)

    case HttpRequest(_, _, _, _, _) =>
      error(BadRequest, "Bad request", sender)

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
      error(InternalServerError, "The " + method + " request to '" + uri + "' has timed out...", sender)

    case ev: Http.ConnectionClosed =>
      stop()
  }

  def processJson(data: String, client: ActorRef) {
    Json.parse(data) match {
      case json"""{
        "mode" : "registration",
        "nickname" : $nickname,
        "pw": $pw
      }""" if isJsString(nickname,pw) => error(NotImplemented, s"Not implemented yet $nickname $pw", client)

      case json"""{
        "mode" : "authorisation",
        "nickname" : $nickname,
        "pw": $pw,
        "p2pip": $p2pip
      }""" if isJsString(nickname, pw, p2pip) => error(NotImplemented, s"Not implemented yet $nickname $pw $p2pip", client)

      case json"""{
        "mode" : "logout",
        "token" : $token
      }""" if isJsString(token) => error(NotImplemented, s"Not implemented yet $token", client)

      case json"""{
        "mode" : "getip",
        "token" : $token,
        "nickname" : $nickname
      }""" if isJsString(token, nickname) => error(NotImplemented, s"Not implemented yet $token $nickname", client)

      case json"""{
        "mode" : "addfriend",
        "token" : $token,
        "nickname" : $nickname
      }""" if isJsString(token, nickname) => error(NotImplemented, s"Not implemented yet $token $nickname", client)

      case json"""{
        "mode" : "removefriend",
        "token" : $token,
        "nickname" : $nickname
      }""" if isJsString(token, nickname) => error(NotImplemented, s"Not implemented yet $token $nickname", client)

      case json"""{
        "mode" : "getfriends",
        "token" : $token
      }""" if isJsString(token) => error(NotImplemented, s"Not implemented yet $token", client)

      case json"""{
        "mode" : "update",
        "token" : $token
      }""" if isJsString(token) => error(NotImplemented, s"Not implemented yet $token", client)

      case _ =>
        error(BadRequest, "bad json", client)
      /*case registration(nickname, pwhash) => UsersDBRef ! UserDB.Register(nickname, pwhash)
      case authorisation(nickname, pwhash) => UsersDBRef ! UserDB.Authorise(nickname, pwhash, new InetSocketAddress("0.0.0.0", 5555)) // TODO - fix addr
      case logout(token) => UsersDBRef ! UserDB.Logout(UserDB.Token(token))
      case update() => send("conupdated", client)
      case getip(token, nickname) => UsersDBRef ! UserDB.Getip(UserDB.Token(token), nickname)
      case addfriend(token, nickname) => UsersDBRef ! UserDB.AddFriend(UserDB.Token(token), nickname)
      case getfriends(token) => UsersDBRef ! UserDB.GetFriends(UserDB.Token(token))
      case _ => error("command syntax error", client)*/
    }
  }

  def isJsString(a: JsValue*) = a.forall( b => b.isInstanceOf[JsString])

  def send(json: JsValue, client: ActorRef) {
    client ! HttpResponse(
      status = OK,
      entity = HttpEntity(`application/json`, Json.stringify(json))
    )
  }

  def error(code: StatusCode, msg: String, client: ActorRef) {
    client ! HttpResponse(
      status = code,
      entity = HttpEntity(`application/json`, JSONObject(Map("error" -> msg)).toString())
    )
  }

  def stop() {
    log.debug("Stopping")
    //UsersDBRef ! UserDB.ConnectionClosed
    //context stop self
  }
}