import akka.actor.{ Props, ActorSystem }
import db.UserDBProps
import handler._

import server.Server

// Simple client
import java.net._
import java.io._
import scala.io._

object Main extends App {
  val system = ActorSystem("server")

  val UserDB = system.actorOf(UserDBProps.props(), "UserDBActor")
  val service = system.actorOf(Server.props(SocketHandlerProps), "ServerActor")
}

/*object MainWithEchoHandler extends App {
  val system = ActorSystem("server")
  val service = system.actorOf(TcpServer.props(EchoHandlerProps), "ServerActor")
}

object MainWithApiHandler extends App {
  val system = ActorSystem("server")
  val service = system.actorOf(TcpServer.props(ApiHandlerProps), "ServerActor")
}

object MainWithDbHandler extends App {
  val system = ActorSystem("server")
  val service = system.actorOf(TcpServer.props(DbHandlerProps), "ServerActor")
}*/