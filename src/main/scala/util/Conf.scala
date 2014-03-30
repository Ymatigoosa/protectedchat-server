package util

import com.typesafe.config.{Config, ConfigFactory}
import akka.actor.{ExtendedActorSystem, ExtensionIdProvider, ExtensionId, Extension}
import scala.util.Properties

class ConfExtensionImpl(config: Config) extends Extension {
  config.checkValid(ConfigFactory.defaultReference)

  val appHostName = config.getString("tcp-async.app.hostname")
  val appPort = Properties.envOrElse("PORT", config.getString("tcp-async.app.port")).toInt

  val apiUrl = config.getString("tcp-async.api.url")

  val dbUsername = config.getString("tcp-async.db.username")
  val dbPassword = config.getString("tcp-async.db.password")
  val dbPort = config.getInt("tcp-async.db.port")
  val dbName = config.getString("tcp-async.db.name")
  val dbHost = config.getString("tcp-async.db.host")

  val dbPoolMaxObjects = config.getInt("tcp-async.db.pool.maxObjects")
  val dbPoolMaxIdle = config.getInt("tcp-async.db.pool.maxIdle")
  val dbPoolMaxQueueSize = config.getInt("tcp-async.db.pool.maxQueueSize")
}

object ConfExtension extends ExtensionId[ConfExtensionImpl] with ExtensionIdProvider {
  def lookup() = ConfExtension

  def createExtension(system: ExtendedActorSystem) = new ConfExtensionImpl(system.settings.config)
}