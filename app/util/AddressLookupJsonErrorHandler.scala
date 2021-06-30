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

package util

import play.api.Configuration
import play.api.http.Status.BAD_REQUEST
import play.api.mvc.Results.BadRequest
import play.api.mvc.{RequestHeader, Result}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.backend.http.JsonErrorHandler
import uk.gov.hmrc.play.bootstrap.config.HttpAuditEvent

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddressLookupJsonErrorHandler @Inject()(auditConnector: AuditConnector,
       httpAuditEvent: HttpAuditEvent, configuration: Configuration)(
    implicit ec: ExecutionContext) extends JsonErrorHandler(auditConnector, httpAuditEvent, configuration) {

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    (statusCode, message) match {
      case (BAD_REQUEST, msg) if msg.startsWith("Json validation error") =>
        Future.successful(BadRequest(s"""{"statusCode": $BAD_REQUEST, "message": "missing or badly-formed postcode parameter"}"""))
      case _ => super.onClientError(request, statusCode, message)
    }
  }
}
