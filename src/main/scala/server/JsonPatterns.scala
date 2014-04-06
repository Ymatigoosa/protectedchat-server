package server

import util._
import util.JsonPattern._
import spray.json._
import DefaultJsonProtocol._ // !!! IMPORTANT, else `convertTo` and `toJson` won't work correctly

/**
 * Created by 7 on 03.04.2014.
 */
trait JsonPatterns {
  val __ = JsString("_?_")

  val registration = JsObject(
    "mode" -> JsString("registration"),
    "nickname" -> __,
    "pw" -> __
  ).pattern(__)

  val authorization = JsObject(
    "mode" -> JsString("authorisation"),
    "nickname" -> __,
    "pw" -> __,
    "p2pip" -> __
  ).pattern(__)

  val logout = JsObject(
    "mode" -> JsString("logout"),
    "token" -> __
  ).pattern(__)

  val getip = JsObject(
    "mode" -> JsString("getip"),
    "token" -> __,
    "nickname" -> __
  ).pattern(__)

  val addfriend = JsObject(
        "mode" -> JsString("addfriend"),
        "token" -> __,
        "nickname" -> __
  ).pattern(__)

  val removefriend = JsObject(
    "mode" -> JsString("removefriend"),
    "token" -> __,
    "nickname" -> __
  ).pattern(__)

  val getfriends = JsObject(
    "mode" -> JsString("getfriends"),
    "token" -> __
  ).pattern(__)

  val update = JsObject(
    "mode" -> JsString("update"),
    "token" -> __
  ).pattern(__)
}
