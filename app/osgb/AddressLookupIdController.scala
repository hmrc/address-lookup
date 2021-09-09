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

import address.model.AddressRecord
import osgb.outmodel.Marshall
import osgb.services.{AddressSearcher, ResponseProcessor}
import play.api.libs.json.JsValue
import play.api.mvc._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class AddressLookupIdController @Inject()(addressSearch: AddressSearcher, responseProcessor: ResponseProcessor,
                                          ec: ExecutionContext, cc: ControllerComponents)
  extends AddressController(cc) {

  implicit private val xec: ExecutionContext = ec

  def findById(id: String): Action[AnyContent] = Action.async {
    request =>
      findByIdRequest(request, id, Marshall.marshallV2Address)
  }

  private[osgb] def findByIdRequest[A](request: Request[A], id: String, marshall: AddressRecord => JsValue): Future[Result] = {
    val origin = getOriginHeaderIfSatisfactory(request.headers)
    Try(addressSearch.findID(id).map {
      a =>
        val list = a.toList
        logEvent("LOOKUP", "origin" -> origin, "id" -> id, "matches" -> list.size.toString)
        if (a.isDefined) {
          val a2 = responseProcessor.convertAddressList(list)
          Ok(marshall(a2.head))
        }
        else {
          NotFound(s"id matched nothing")
        }
    }).getOrElse(Future.successful(BadRequest(s"Check the id supplied: $id")))
  }
}