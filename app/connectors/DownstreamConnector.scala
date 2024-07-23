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

package connectors

import play.api.Logging
import play.api.http.HeaderNames.{AUTHORIZATION, CONTENT_LENGTH, CONTENT_TYPE, HOST}
import play.api.http.{HeaderNames, HttpEntity, MimeTypes}
import play.api.libs.json.JsValue
import play.api.mvc.Results.{BadGateway, InternalServerError}
import play.api.mvc.{Request, ResponseHeader, Result}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DownstreamConnector @Inject()(httpClient: HttpClient) extends Logging {
  def forward(request: Request[JsValue], url: String, authToken: String)(implicit ec: ExecutionContext): Future[Result] = {
    import uk.gov.hmrc.http.HttpReads.Implicits.readRaw

    implicit val hc: HeaderCarrier = DownstreamConnector.overrideHeaderCarrier(authToken)
    val onwardHeaders = request.headers.remove(CONTENT_LENGTH, HOST, AUTHORIZATION).headers

    logger.info(s"Forwarding to downstream url: $url")

    // TODO: Check if the context path is present - if so remove before forwarding
    val called = request.method match {
      case "POST" =>
        httpClient.POST[Option[JsValue], HttpResponse](url = url, body = Some(request.body), onwardHeaders)
      case "GET"  =>
        httpClient.GET[HttpResponse](url = url, onwardHeaders)
    }

    try {
      called.map { response: HttpResponse =>
        val returnHeaders = response.headers
          .filterNot { case (n, _) => n == CONTENT_TYPE || n == CONTENT_LENGTH }
          .view.mapValues(x => x.mkString).toMap
        Result(
          ResponseHeader(response.status, returnHeaders),
          HttpEntity.Streamed(response.bodyAsSource, None, response.header(CONTENT_TYPE)))
      }.recoverWith { case t: Throwable =>
        logger.error(s"Downstream call failed with ${t.getMessage}")
        Future.successful(BadGateway("{\"code\": \"REQUEST_DOWNSTREAM\", \"desc\": \"An issue occurred when the downstream service tried to handle the request\"}").as(MimeTypes.JSON))
      }
    } catch {
      case t: Throwable =>
        logger.error(s"Call to search service failed with ${t.getMessage}")
        Future.successful(InternalServerError("{\"code\": \"REQUEST_FORWARDING\", \"desc\": \"An issue occurred when forwarding the request to the downstream service\"}").as(MimeTypes.JSON))
    }
  }

  def checkConnectivity(url: String, authToken: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
    implicit val hc: HeaderCarrier = DownstreamConnector.overrideHeaderCarrier(authToken)

    try {
      httpClient.GET[HttpResponse](url = url).map {
        case response if response.status > 400      => false
        case response if response.status / 100 == 5 => false
        case _                                      => true
      }.recoverWith { case t: Throwable =>
        Future.successful(false)
      }
    }
    catch {
      case t: Throwable => Future.successful(false)
    }
  }
}

object DownstreamConnector {
  def overrideHeaderCarrier(authToken: String): HeaderCarrier = {
    HeaderCarrier(extraHeaders = Seq(HeaderNames.AUTHORIZATION -> authToken))
  }
}
