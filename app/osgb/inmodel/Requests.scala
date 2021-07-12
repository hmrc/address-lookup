/*
 * Copyright 2021 HM Revenue & Customs
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

package osgb.inmodel

import address.uk.Postcode
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._


case class LookupByPostcodeRequest(postcode: Postcode, filter: Option[String] = None)

object LookupByPostcodeRequest {
  implicit val postcodeReads: Reads[Postcode] = Reads[Postcode] { json =>
    json.validate[String] match {
      case e: JsError => e
      case s: JsSuccess[String] =>
        Postcode.cleanupPostcode(s.get) match {
          case pc if pc.isDefined => JsSuccess(pc.get)
          case _ => JsError("error.invalid")
        }
    }
  }

  implicit val reads: Reads[LookupByPostcodeRequest] = (
    (JsPath \ "postcode").read[Postcode] and
      (JsPath \ "filter").readNullable[String]
    ) (
    (pc, fo) => LookupByPostcodeRequest.apply(pc, fo))
}

case class LookupByUprnRequest(uprn: String)
object LookupByUprnRequest {
  implicit val reads: Reads[LookupByUprnRequest] = Json.reads[LookupByUprnRequest]
}

case class LookupByTownRequest(town: String)
object LookupByTownRequest {
  implicit val reads: Reads[LookupByTownRequest] = Json.reads[LookupByTownRequest]
}