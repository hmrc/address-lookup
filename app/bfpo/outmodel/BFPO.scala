/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package bfpo.outmodel

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads, Writes}

case class BFPO(operation: Option[String], lines: List[String], postcode: String, bfpoNo: String) {

  def anyLineContains(filterStr: String): Boolean = {
    val uFilter = filterStr.toUpperCase
    lines.exists(_.toUpperCase.contains(uFilter))
  }
}


object BFPOReadWrite {

  // https://www.playframework.com/documentation/2.3.x/ScalaJsonCombinators
  implicit val AddressReads: Reads[BFPO] = (
    (JsPath \ "operation").readNullable[String] and
      (JsPath \ "lines").read[List[String]] and
      (JsPath \ "postcode").read[String] and
      (JsPath \ "bfpoNo").read[String]) (BFPO.apply _)

  implicit val AddressWrites: Writes[BFPO] = (
    (JsPath \ "operation").writeNullable[String] and
      (JsPath \ "lines").write[List[String]] and
      (JsPath \ "postcode").write[String] and
      (JsPath \ "bfpoNo").write[String]) (unlift(BFPO.unapply))
}
