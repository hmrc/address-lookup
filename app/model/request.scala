/*
 * Copyright 2023 HM Revenue & Customs
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

package model

import model.address.Postcode
import play.api.http.HeaderNames
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.mvc.Request


object request {
  case class LookupByPostcodeRequest(postcode: Postcode, filter: Option[String] = None)

  object LookupByPostcodeRequest {
    implicit val postcodeReads: Reads[Postcode] = Reads[Postcode] { json =>
      json.validate[String] match {
        case e: JsError           => e
        case s: JsSuccess[String] =>
          Postcode.cleanupPostcode(s.get) match {
            case pc if pc.isDefined => JsSuccess(pc.get)
            case _                  => JsError("error.invalid")
          }
      }
    }

    implicit val postcodeWrites: Writes[Postcode] = new Writes[Postcode]{
      override def writes(o: Postcode): JsValue =
        JsString(o.toString)
    }

    implicit val reads: Reads[LookupByPostcodeRequest] = (
        (JsPath \ "postcode").read[Postcode] and
            (JsPath \ "filter").readNullable[String].map(fo =>
              fo.flatMap(f => if(f.trim.isEmpty) None else Some(f))
            )
        ) (
      (pc, fo) => LookupByPostcodeRequest.apply(pc, fo))

    implicit val writes: Writes[LookupByPostcodeRequest] = Json.writes[LookupByPostcodeRequest]
  }

  case class LookupByUprnRequest(uprn: String)

  object LookupByUprnRequest {
    implicit val reads: Reads[LookupByUprnRequest] = Json.reads[LookupByUprnRequest]
    implicit val writes: Writes[LookupByUprnRequest] = Json.writes[LookupByUprnRequest]
  }

  case class LookupByPostTownRequest(posttown: String, filter: Option[String])

  object LookupByPostTownRequest {
    implicit val reads: Reads[LookupByPostTownRequest] = (
        (JsPath \ "posttown").read[String] and
            (JsPath \ "filter").readNullable[String].map(fo =>
              fo.flatMap(f => if(f.trim.isEmpty) None else Some(f))
            )
        ) (
      (pt, fo) => LookupByPostTownRequest.apply(pt, fo))

    implicit val writes: Writes[LookupByPostTownRequest] = Json.writes[LookupByPostTownRequest]
  }

  case class LookupByCountryFilterRequest(filter: String)

  object LookupByCountryFilterRequest {
    implicit val reads: Reads[LookupByCountryFilterRequest] = Json.reads[LookupByCountryFilterRequest]
    implicit val writes: Writes[LookupByCountryFilterRequest] = Json.writes[LookupByCountryFilterRequest]
  }

  case class LookupByCountryRequest(country: String, filter: String)

  object LookupByCountryRequest {
    implicit val reads: Reads[LookupByCountryRequest] = Json.reads[LookupByCountryRequest]
    implicit val writes: Writes[LookupByCountryRequest] = Json.writes[LookupByCountryRequest]
  }

  final case class UserAgent(unwrap: String)

  object UserAgent {
    def apply(request: Request[_]): Option[UserAgent] =
      request.headers.get(HeaderNames.USER_AGENT).map(ua => UserAgent(ua))
  }
}
