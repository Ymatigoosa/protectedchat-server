package handler

import akka.actor.{ActorLogging, Props, ActorRef, Actor}
import akka.io.Tcp._
import akka.io.Tcp.Received

trait HandlerProps {
}

abstract class Handler(val connection: ActorRef) extends Actor with ActorLogging {

  val abort = "(?i)abort".r
  val confirmedClose = "(?i)confirmedclose".r
  val close = "(?i)close".r

  def receive: Receive = {
    case Received(data) =>
      data.utf8String.trim match {
        case abort() => connection ! Abort
        case confirmedClose() => connection ! ConfirmedClose
        case close() => connection ! Close
        case str => received(str)
      }
    case PeerClosed =>
      peerClosed()
      stop()
    case ErrorClosed =>
      errorClosed()
      stop()
    case Closed =>
      closed()
      stop()
    case ConfirmedClosed =>
      confirmedClosed()
      stop()
    case Aborted =>
      aborted()
      stop()
  }

  def received(str: String): Unit

  def peerClosed() {
    log.info("PeerClosed")
  }

  def errorClosed() {
    log.info("ErrorClosed")
  }

  def closed() {
    log.info("Closed")
  }

  def confirmedClosed() {
    log.info("ConfirmedClosed")
  }

  def aborted() {
    log.info("Aborted")
  }

  def stop() {
    log.info("Stopping")
    context stop self
  }
}