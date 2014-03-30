package handler

import akka.actor._
import akka.util.ByteString
import akka.io.Tcp.Write

object EchoHandlerProps extends HandlerProps {
  def props(connection: ActorRef) = Props(classOf[EchoHandler], connection)
}

class EchoHandler(connection: ActorRef) extends Handler(connection) {

  /**
   * Echoes incoming message.
   */
  def received(data: String) = connection ! Write(ByteString(data + "\n"))
}