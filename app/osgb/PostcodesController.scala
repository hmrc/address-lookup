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

import java.net.URLDecoder

import javax.inject.Inject
import osgb.services.{AddressSearcher, ResponseProcessor}
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, ControllerComponents, Request, Result}
import uk.gov.hmrc.address.uk.Postcode
import uk.gov.hmrc.logging.SimpleLogger

import scala.concurrent.{ExecutionContext, Future}

case class PostcodeResponse(isPOBox: Boolean)

object PostcodeResponseReadWrite {
  implicit val reads: Reads[PostcodeResponse] = Json.reads[PostcodeResponse]
  implicit val writes: OWrites[PostcodeResponse] = Json.writes[PostcodeResponse]
}

class PostcodesController @Inject()(addressSearch: AddressSearcher, responseProcessor: ResponseProcessor,
                                    logger: SimpleLogger, ec: ExecutionContext, cc: ControllerComponents)
  extends AddressController(logger, cc) {

  import PostcodeResponseReadWrite._

  implicit private val xec = ec

  def lookup(postcode: String): Action[AnyContent] = Action.async {
    (request: Request[AnyContent]) =>
      val origin = getOriginHeaderIfSatisfactory(request.headers)
      if (request.rawQueryString.nonEmpty) {
        Future.successful {
          badRequest(request, "Query String supplied but not supported")
        }
      } else {
        processPostcode(origin, request, postcode)
      }
  }

  private def processPostcode(origin: String, request: Request[AnyContent], postcode: String): Future[Result] = {
    val cleanPostcode = Postcode.cleanupPostcode(URLDecoder.decode(postcode, "UTF-8"))
    if (cleanPostcode.isDefined) {
      findPostcode(origin, request, cleanPostcode)
    } else {
      Future.successful {
        badRequest(request, "Invalid postcode")
      }
    }
  }

  private def findPostcode(origin: String, request: Request[AnyContent], cleanPostcode: Option[Postcode]) = {
    addressSearch.findPostcode(cleanPostcode.get, None).map {
      addressList =>
        logEvent("LOOKUP", "origin" -> origin, "postcode" -> cleanPostcode.get.toString, "matches" -> addressList.size.toString)

        if (addressList.isEmpty) {
          notFound(request, "Unknown postcode")
        } else {
          val poBox = addressList.exists(_.poBox.isDefined)
          Ok(Json.toJson(PostcodeResponse(poBox)))
        }
    }
  }

  def missing: Action[AnyContent] = Action {
    BadRequest("Missing postcode")
  }

  private def badRequest(request: Request[AnyContent], msg: String): Result = {
    BadRequest(Json.obj("statusCode" -> BAD_REQUEST, "message" -> msg, "requested" -> request.uri))
  }

  private def notFound(request: Request[AnyContent], msg: String): Result = {
    NotFound(Json.obj("statusCode" -> NOT_FOUND, "message" -> msg, "requested" -> request.uri))
  }
}
