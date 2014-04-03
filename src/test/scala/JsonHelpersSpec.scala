import util._
import util.JsonPattern._
import org.scalatest.matchers.MustMatchers
import org.scalatest._
import play.api.libs.json._
import play.api.libs.json.extensions._
import play.api.libs.json.monad._
import play.api.libs.json.monad.syntax._

/**
 * Created by 7 on 03.04.2014.
 */
class JsonHelpersSpec extends FlatSpec {

  val pattern1 = json"""{
        "mode" : "registration",
        "nickname" : "a",
        "pw": "b"
      }""".pattern("_?_")

  val pattern2 = json"""{
        "mode" : "registration",
        "nickname" : "_?_",
        "pw": "_?_"
      }""".pattern("_?_")

  val pattern3 = json"""{
        "mode" : "registration",
        "pw": "???",
        "nickname" : "???"
      }""".pattern

  val pattern4 = json"""["a", true, "_?_", null, {"a":"b"}]""".pattern("_?_")
  val pattern5 = json"""["a", true, "_?_", null, {"a":"_?_"}]""".pattern("_?_")
  val pattern6 = json"""["a", true, "_?_", null, {"b":"_?_"}]""".pattern("_?_")

  val js1 = json"""{
        "mode" : "registration",
        "nickname" : "a",
        "pw": "b"
      }"""

  val js2 = json"""["a", true, 75, null, {"a":"b"}]"""

  val js3 = json"""{
        "mode" : "registration",
        "pw": "b",
        "nickname" : "a"
      }"""

  val js4 = json"""{
        "mode" : "registration",
        "nickname" : "a",
        "pw": "b",
        "foo": "bar"
      }"""

  val js5 = json"""{
        "mode" : "registration",
        "nickname" : "a"
      }"""

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
