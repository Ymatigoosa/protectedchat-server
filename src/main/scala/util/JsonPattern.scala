package util

import play.api.libs.json._

/**
 * Created by 7 on 02.04.2014.
 *
 * unapply stolen from https://github.com/mandubian/play-json-zipper
 */
package object JsonPattern {

  implicit class JsValueHelper(val jv: JsValue) {
    def pattern(placeholder: String) = new JsonPattern(jv, placeholder)
    def pattern: JsonPattern = pattern("???")
  }

  class JsonPattern(val pattern: JsValue, val placeholder: String) {
    private def compareStep(ax: Option[Seq[JsValue]], pattern: JsValue, source: JsValue): Option[Seq[JsValue]] = {
      if (ax.nonEmpty) {
        (pattern, source) match {
          case (JsString(`placeholder`), s) => Some(ax.get :+ s)
          case (p: JsObject, s: JsObject) if p.fieldSet.size==s.fieldSet.size  =>
            val zipped = p.fieldSet.zip(s.fieldSet)
            zipped.foldLeft(ax) {
              case (acc, ((kp, vp), (ks, vs))) =>
                if (kp == ks && acc.nonEmpty) {
                  compareStep(acc, vp, vs)
                } else {
                  None
                }
            }
          case (p: JsArray, s: JsArray) if p.value.size==s.value.size =>
            val zipped = p.value.zip(s.value)
            zipped.foldLeft(ax) {
              case (acc, (vp, vs)) => compareStep(acc, vp, vs)
            }
          case (p , s) if p==s => ax
          case (_, _) => None
        }
      }
      else {
        None
      }
    }

    def unapplySeq(source: JsValue): Option[Seq[JsValue]] = {
      compareStep(Some(Seq()), pattern, source)
    }
  }
}