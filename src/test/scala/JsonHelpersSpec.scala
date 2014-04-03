import util._
import util.JsonPattern._
import org.scalatest.matchers.MustMatchers
import org.scalatest._
import spray.json._
import DefaultJsonProtocol._ // !!! IMPORTANT, else `convertTo` and `toJson` won't work correctly

/**
 * Created by 7 on 03.04.2014.
 */
class JsonHelpersSpec extends FlatSpec {

  val __ = JsString("???")

  val pattern1 = JsObject(
    "mode" -> JsString("registration"),
    "nickname" -> JsString("a"),
    "pw" -> JsString("b")
  ).pattern(__)

  val pattern2 = JsObject(
    "mode" -> JsString("registration"),
    "nickname" -> __,
    "pw" -> __
  ).pattern(__)

  val pattern3 = JsObject(
    "mode" -> JsString("registration"),
    "pw" -> __,
    "nickname" -> __
  ).pattern(__)

  val pattern4 = JsArray(
    JsString("a"),
    JsBoolean(true),
    __,
    JsNull,
    JsObject(
      "a" -> JsString("b")
    )
  ).pattern(__)

  val pattern5 = JsArray(
    JsString("a"),
    JsBoolean(true),
    __,
    JsNull,
    JsObject(
      "a" -> __
    )
  ).pattern(__)

  val pattern6 = JsArray(
    JsString("a"),
    JsBoolean(true),
    __,
    JsNull,
    JsObject(
      "b" -> __
    )
  ).pattern(__)

  val js1 = JsObject(
    "mode" -> JsString("registration"),
    "nickname" -> JsString("a"),
    "pw" -> JsString("b")
  )

  val js2 = JsArray(
    JsString("a"),
    JsBoolean(true),
    JsNumber(75),
    JsNull,
    JsObject(
      "a" -> JsString("b")
    )
  )

  val js3 = JsObject(
    "mode" -> JsString("registration"),
    "pw" -> JsString("b"),
    "nickname" -> JsString("b")
  )

  val js4 = JsObject(
    "mode" -> JsString("registration"),
    "nickname" -> JsString("a"),
    "pw" -> JsString("b"),
    "foo" -> JsString("bar")
  )

  val js5 = JsObject(
    "mode" -> JsString("registration"),
    "pw" -> JsString("b")
  )

  "A JsonHelper" should "compare jsons" in {
    js1 match {
      case pattern1() => {}
      case _ => fail
    }
  }

  it should "keep field order" in {
    js3 match {
      case pattern2(nickname, pw) =>
        fail
      case _ => {}
    }
  }

  it should "extract values from object" in {
    js1 match {
      case pattern2(nickname, pw) =>
        assert(nickname equals JsString("a"))
        assert(pw equals JsString("b"))
      case _ => fail
    }
  }

  it should "extract values from array" in {
    val tt = pattern4.unapplySeq(js2)
    js2 match {
      case pattern4(n) =>
        assert(n equals JsNumber(75))
      case _ => fail
    }
  }

  it should "extract values from nested object" in {
    js2 match {
      case pattern5(n,s) =>
        assert(n equals JsNumber(75))
        assert(s equals JsString("b"))
      case _ => fail
    }
  }

  it should "match keys" in {
    js2 match {
      case pattern6(n,s) =>
        fail
      case _ => {}
    }
  }

  it should "work correctly on different keysets" in {
    js4 match {
      case pattern2(n,s) =>
        fail
      case _ => {}
    }

    js5 match {
      case pattern2(n,s) =>
        fail
      case _ => {}
    }
  }
}
