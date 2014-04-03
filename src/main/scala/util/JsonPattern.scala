package util
import spray.json._
import DefaultJsonProtocol._ // !!! IMPORTANT, else `convertTo` and `toJson` won't work correctly
/**
 * Created by 7 on 02.04.2014.
 *
 * unapply stolen from https://github.com/mandubian/play-json-zipper
 */
package object JsonPattern {

  implicit class JsValueHelper(val jv: JsValue) {
    def pattern(placeholder: JsValue) = new JsonPattern(jv, placeholder)
  }

  class JsonPattern(val pattern: JsValue, val placeholder: JsValue) {
    private def compareStep(ax: Option[Seq[JsValue]], pattern: JsValue, source: JsValue): Option[Seq[JsValue]] = {
      if (ax.nonEmpty) {
        (pattern, source) match {
          case (`placeholder`, s) => Some(ax.get :+ s)
          case (p: JsObject, s: JsObject) if p.fields.size==s.fields.size =>
            val zipped = p.fields.toSeq.zip(s.fields.toSeq)
            zipped.foldLeft(ax) {
              case (acc, ((kp, vp), (ks, vs))) =>
                if (kp == ks && acc.nonEmpty) {
                  compareStep(acc, vp, vs)
                } else {
                  None
                }
            }
          case (p: JsArray, s: JsArray) if p.elements.size==s.elements.size =>
            val zipped = p.elements.zip(s.elements)
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