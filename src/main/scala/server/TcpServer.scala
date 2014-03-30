package server

import akka.io.{ IO, Tcp }
import java.net.InetSocketAddress
import util._
import handler._
import akka.actor.Props

object TcpServer {
  def props(handlerProps: HandlerProps): Props =
    Props(classOf[TcpServer], handlerProps)
}

class TcpServer(handlerProps: HandlerProps) extends Server {

  import context.system

  IO(Tcp) ! Tcp.Bind(self, new InetSocketAddress(ConfExtension(system).appHostName, ConfExtension(system).appPort))

  override def receive = {
    case Tcp.CommandFailed(_: Tcp.Bind) => context stop self

    case Tcp.Connected(remote, local) =>
      val handler = context.actorOf(SocketHandlerProps.props(sender, remote))
      sender ! Tcp.Register(handler)
  }
}