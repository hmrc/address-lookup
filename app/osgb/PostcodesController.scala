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

package osgb

import address.uk.Postcode
import osgb.services.{AddressSearcher, ResponseProcessor}
import play.api.libs.json._
import play.api.mvc._

import java.net.URLDecoder
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class PostcodeResponse(isPOBox: Boolean)

object PostcodeResponseReadWrite {
  implicit val reads: Reads[PostcodeResponse] = Json.reads[PostcodeResponse]
  implicit val writes: OWrites[PostcodeResponse] = Json.writes[PostcodeResponse]
}

class PostcodesController @Inject()(addressSearch: AddressSearcher, responseProcessor: ResponseProcessor,
                                    ec: ExecutionContext, cc: ControllerComponents)
  extends AddressController(cc) {

  import PostcodeResponseReadWrite._

  implicit private val xec = ec

  @deprecated("Please use Post endpoint", "4.87.0")
  def lookupWithGet(postcode: String): Action[AnyContent] = Action.async {
    (request: Request[AnyContent]) =>
      val origin = getOriginHeaderIfSatisfactory(request.headers)
      if (request.rawQueryString.nonEmpty) {
        Future.successful {
          badRequest(request.uri, "Query String supplied but not supported")
        }
      } else {
        processPostcode(origin, request.uri, postcode)
      }
  }

  private def processPostcode(origin: String, requestUri: String, postcode: String): Future[Result] = {
    val cleanPostcode = Postcode.cleanupPostcode(URLDecoder.decode(postcode, "UTF-8"))
    if (cleanPostcode.isDefined) {
      findPostcode(origin, requestUri, cleanPostcode)
    } else {
      Future.successful {
        badRequest(requestUri, "Invalid postcode")
      }
    }
  }

  private def findPostcode(origin: String, requestUri: String, cleanPostcode: Option[Postcode]) = {
    addressSearch.findPostcode(cleanPostcode.get, None).map {
      addressList =>
        logEvent("LOOKUP", "origin" -> origin, "postcode" -> cleanPostcode.get.toString, "matches" -> addressList.size.toString)

        if (addressList.isEmpty) {
          notFound(requestUri, "Unknown postcode")
        } else {
          val poBox = addressList.exists(_.poBox.isDefined)
          Ok(Json.toJson(PostcodeResponse(poBox)))
        }
    }
  }

  // Do we need this???
  def missing: Action[AnyContent] = Action {
    BadRequest("Missing postcode")
  }

  private def badRequest(requestUri: String, msg: String): Result = {
    BadRequest(Json.obj("statusCode" -> BAD_REQUEST, "message" -> msg, "requested" -> requestUri))
  }

  private def notFound(requestUri: String, msg: String): Result = {
    NotFound(Json.obj("statusCode" -> NOT_FOUND, "message" -> msg, "requested" -> requestUri))
  }
}