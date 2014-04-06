import akka.actor.{ Props, ActorSystem }
import db.UserDB
import handler._

object Main extends App {
  val system = ActorSystem("server")

  val UserDB = system.actorOf(Props(classOf[UserDB]), "UserDBActor")
  val service = system.actorOf(Props(classOf[ApiHandler]), "ApiHandler")
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