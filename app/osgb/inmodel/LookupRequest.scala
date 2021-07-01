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
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._


case class LookupRequest(postcode: Postcode, filter: Option[String] = None)

object LookupRequest {
  implicit val postcodeReads: Reads[Postcode] = Reads[Postcode] { json =>
    Postcode.cleanupPostcode(json.as[String]) match {
      case pc if pc.isDefined => JsSuccess(pc.get)
      case _                  => JsError("Postcode was not valid")
    }
  }

  implicit val reads: Reads[LookupRequest] = (
      (JsPath \ "postcode").read[Postcode] and
          (JsPath \ "filter").readNullable[String]
      )(
    (pc, fo) => LookupRequest.apply(pc, fo))
}

case class LookupPostcode(postcode: String)

object LookupPostcode {
  implicit val formats: Format[LookupPostcode] = Json.format[LookupPostcode]
}


