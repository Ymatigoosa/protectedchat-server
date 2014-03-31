package server

import akka.io.{ IO, Tcp }
import java.net.InetSocketAddress
import util._
import handler._
import akka.actor.{Actor, Props}
import spray.can.Http

object Server {
  def props(handlerProps: HandlerProps): Props =
    Props(classOf[Server], handlerProps)
}

class Server(handlerProps: HandlerProps) extends Actor {

  import context.system

  IO(Http) ! Http.Bind(self, ConfExtension(system).appHostName, ConfExtension(system).appPort)

  override def receive = {
    case Http.CommandFailed(_: Tcp.Bind) => context stop self

    case Http.Connected(remote, local) =>
      val handler = context.actorOf(SocketHandlerProps.props(sender, remote))
      sender ! Http.Register(handler)
  }
}