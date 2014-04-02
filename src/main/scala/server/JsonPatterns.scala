package server

import util._
import util.JsonPattern._
import play.api.libs.json._
import play.api.libs.json.extensions._
import play.api.libs.json.monad._
import play.api.libs.json.monad.syntax._

/**
 * Created by 7 on 03.04.2014.
 */
trait JsonPatterns {
  val placeholder = "???"

  val registration = json"""{
        "mode" : "registration",
        "nickname" : "$placeholder",
        "pw": "$placeholder"
      }""".pattern(`placeholder`)

  val authorization = json"""{
        "mode" : "authorisation",
        "nickname" : "$placeholder",
        "pw": "$placeholder",
        "p2pip": "$placeholder"
      }""".pattern(placeholder)

  val logout = json"""{
        "mode" : "logout",
        "token" : "$placeholder"
      }""".pattern(placeholder)

  val getip = json"""{
        "mode" : "getip",
        "token" : "$placeholder",
        "nickname" : "$placeholder"
      }""".pattern(placeholder)

  val addfriend = json"""{
        "mode" : "addfriend",
        "token" : "$placeholder",
        "nickname" : "$placeholder"
      }""".pattern(placeholder)

  val removefriend = json"""{
        "mode" : "removefriend",
        "token" : "$placeholder",
        "nickname" : "$placeholder"
      }""".pattern(placeholder)

  val getfriends = json"""{
        "mode" : "getfriends",
        "token" : "$placeholder"
      }""".pattern(placeholder)

  val update = json"""{
        "mode" : "update",
        "token" : "$placeholder"
      }""".pattern(placeholder)
}
