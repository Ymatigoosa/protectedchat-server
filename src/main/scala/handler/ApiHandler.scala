package handler

import akka.actor.{ Props, ActorRef, Actor, ActorLogging }
import akka.pattern.ask
import akka.util.{Timeout, ByteString}
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
import util.JsonPattern._
import spray.json._
import DefaultJsonProtocol._ // !!! IMPORTANT, else `convertTo` and `toJson` won't work correctly
import org.parboiled.errors.ParsingException
import server.JsonPatterns

class ApiHandler() extends Actor with ActorLogging with JsonPatterns { // TODO - rename
  implicit val system = context.system
  implicit val timeout = Timeout(5 seconds)
  implicit val dispatcher = context.dispatcher

  val UsersDBRef = context.actorSelection("akka://server/user/UserDBActor")

  IO(Http) ! Http.Bind(self, ConfExtension(system).appHostName, ConfExtension(system).appPort)

  def receive: Receive = {
    case Http.CommandFailed(_: Http.Bind) => context stop self

    case Http.Connected(remote, local) =>
      sender ! Http.Register(self)

    case HttpRequest(GET, Uri.Path("/"), _, _, _) =>
      sender ! HttpResponse(status = OK,entity = "This is json api server. You can see full information at https://github.com/pahomovda/protectedchat-server")

    case HttpRequest(POST, Uri.Path("/api/json/"), _, entity: HttpEntity.NonEmpty, _) =>
      processJson(entity.asString, sender)

    case HttpRequest(_, _, _, _, _) =>
      error(BadRequest, "Bad request", sender)

    case Timedout(HttpRequest(method, uri, _, _, _)) =>
      error(InternalServerError, "The " + method + " request to '" + uri + "' has timed out...", sender)

    case ev: Http.ConnectionClosed =>
      stop()
  }

  def processJson(data: String, client: ActorRef) {
    try {
      JsonParser(data) match {

        case registration(nickname: JsString, pw: JsString) =>
          (UsersDBRef ? UserDB.Register(nickname.value, pw.value)) map (processDBReply(_, client))

        case authorization(nickname: JsString, pw: JsString, p2pip: JsString) =>
          (UsersDBRef ? UserDB.Authorise(nickname.value, pw.value, p2pip.value)) map (processDBReply(_, client))

        case logout(token: JsString) =>
          (UsersDBRef ? UserDB.Logout(UserDB.Token(token.value))) map (processDBReply(_, client))

        case getip(token: JsString, nickname: JsString) =>
          (UsersDBRef ? UserDB.Getip(UserDB.Token(token.value), nickname.value)) map (processDBReply(_, client))

        case addfriend(token: JsString, nickname: JsString) =>
          (UsersDBRef ? UserDB.AddFriend(UserDB.Token(token.value), nickname.value)) map (processDBReply(_, client))

        case removefriend(token: JsString, nickname: JsString) =>
          (UsersDBRef ? UserDB.RemoveFriend(UserDB.Token(token.value), nickname.value)) map (processDBReply(_, client))

        case getfriends(token: JsString) =>
          (UsersDBRef ? UserDB.GetFriends(UserDB.Token(token.value))) map (processDBReply(_, client))

        case update(token: JsString) =>
          (UsersDBRef ? UserDB.Update(UserDB.Token(token.value))) map (processDBReply(_, client))

        case _ =>
          error(BadRequest, "bad json content", client)
      }
    } catch {
      case e: ParsingException => error(BadRequest, "bad json format", client)
    }
  }

  def processDBReply(msg: Any, client: ActorRef) {
    msg match {
      case UserDB.Registered(nickname) =>
        send(JsObject(
          "status" -> JsString("registered"),
          "nickname" -> JsString(nickname)
        ), client)

      case UserDB.Authorised(nickname, token) =>
        send(JsObject(
          "status" -> JsString("authorised"),
          "nickname" -> JsString(nickname),
          "token" -> JsString(token.s)
        ), client)

      case UserDB.Error(msg) =>
        error(400, msg, client)

      case UserDB.Logouted(nickname) =>
        send(JsObject(
          "status" -> JsString("logouted"),
          "nickname" -> JsString(nickname)
        ), client)

      case UserDB.Userip(nickname, ip) =>
        send(JsObject(
          "status" -> JsString("ip"),
          "nickname" -> JsString(nickname),
          "ip" -> JsString(ip)
        ), client)

      case UserDB.FriendAdded(owner, friend) =>
        send(JsObject(
          "status" -> JsString("friendadded"),
          "owner" -> JsString(owner),
          "friend" -> JsString(friend)
        ), client)

      case UserDB.FriendList(nickname, seq) =>
        send(JsObject(
          "status" -> JsString("friendadded"),
          "nickname" -> JsString(nickname),
          "list" -> JsString(seq.mkString(","))
        ), client)

      case UserDB.Updated(nickname) =>
        send(JsObject(
          "status" -> JsString("updated"),
          "nickname" -> JsString(nickname)
        ), client)

      case _ => log.error("wrong msg in processDBReply")
    }
  }

  def isJsString(a: JsValue*) = a.forall( b => b.isInstanceOf[JsString])

  def send(json: JsValue, client: ActorRef) {
    client ! HttpResponse(
      status = OK,
      entity = HttpEntity(`application/json`, json.compactPrint)
    )
  }

  def error(code: StatusCode, msg: String, client: ActorRef) {
    client ! HttpResponse(
      status = code,
      entity = HttpEntity(`application/json`, JsObject(
        "status" -> JsString("error"),
        "errormsg" ->  JsString(msg)
      ).compactPrint)
    )
  }

  def stop() {
    log.debug("Stopping")
    //UsersDBRef ! UserDB.ConnectionClosed
    //context stop self
  }
}