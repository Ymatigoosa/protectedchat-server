package util

import play.api.libs.json._

/**
 * Created by 7 on 02.04.2014.
 *
 * unapply stolen from https://github.com/mandubian/play-json-zipper
 */
package object JsonHelpers {

  implicit class JsValueHelper(val jv: JsValue) {
    def pattern = new JsonPattern(jv)
  }

  class JsonPattern(val jv: JsValue) { // TODO - keys dont match!
    def compare(matcher: JsZipper, zipper: JsZipper): Option[Seq[JsValue]] = {
      def step(acc: Seq[Option[JsValue]], curMatcher: JsZipper, curZipper: JsZipper): Seq[Option[JsValue]] = {
        (curMatcher.value, curZipper.value) match {
          case (JsString("_?_:String"), a: JsString) => curZipper.right match {
            case JsZipper.Empty => acc :+ Some(a)
            case right => step(acc :+ Some(a), curMatcher.right, right)
          }

          case (JsString("_?_"), a) => curZipper.right match {
            case JsZipper.Empty => acc :+ Some(a)
            case right => step(acc :+ Some(a), curMatcher.right, right)
          }

          case (a, b) if a == b => curZipper.right match {
            case JsZipper.Empty => acc
            case right => step(acc, curMatcher.right, right)
          }

          case (a, b) => curZipper.right match {
            case JsZipper.Empty => if (curZipper.isLeaf) acc :+ None
            else step(acc, curMatcher.down.first, curZipper.down.first)
            case right => if (curZipper.isLeaf) step(acc :+ None, curMatcher.right, right)
            else step(acc, curMatcher.down.first, curZipper.down.first)
          }
        }
      }

      val matching = step(Seq(), matcher, zipper)

      if (matching.exists(_.isEmpty)) None
      else Some(matching.map(_.get))
    }

    def unapplySeq(js: JsValue): Option[Seq[JsValue]] = {
      compare(JsZipper(jv), JsZipper(js))
    }
  }
}