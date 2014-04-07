package util

import java.util.UUID
import scala.collection.mutable.Queue

/**
 * Created by Pahomov Dmitry on 04.03.14.
 */
object UniqueIdGenerator{
  def apply() = {
    UUID.randomUUID.toString
  }
}
