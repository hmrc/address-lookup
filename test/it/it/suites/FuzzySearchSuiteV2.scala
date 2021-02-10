/*
 * Copyright 2017 HM Revenue & Customs
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

package it.suites

import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import osgb.outmodel.v2.AddressReadable._
import play.api.Application
import play.api.http.Status
import play.api.libs.json.{JsArray, Json}
import play.api.libs.ws.WSClient
import play.api.test.Helpers._
import play.test.Helpers
import uk.gov.hmrc.address.v2.AddressRecord

import scala.concurrent.Await
import scala.concurrent.duration._

class FuzzySearchSuiteV2 extends WordSpec with Matchers with Status with GuiceOneServerPerSuite {
  //(val wsClient: WSClient, val appEndpoint: String)(implicit val app: Application)
  val defaultDuration = 5 seconds

  override def fakeApplication(): Application = super.fakeApplication()
  val wsClient = app.injector.instanceOf[WSClient]
  def url(uri: String) = s"http://localhost:${port}${uri}"

  "fuzzy search lookup" when {

    "successful" should {

      "give a successful response for a known postcode  - uk route" in {
        val response = Await.result(wsClient.url(url("/v2/uk/addresses?fuzzy=A+Street")).get, defaultDuration)
        response.status shouldBe OK
      }

      "give a successful response for a known v.large postcode- uk route" in {
        val response = Await.result(wsClient.url(url("/v2/uk/addresses?fuzzy=bankside")).get, defaultDuration)
        response.status shouldBe OK
      }

      "give sorted results when multiple addresses are returned" in {
        val body = Await.result(wsClient.url(url("/v2/uk/addresses?fuzzy=bankside")).get, defaultDuration).body
        val json = Json.parse(body)
        val seq = Json.fromJson[Seq[AddressRecord]](json).get
        val arr = json.asInstanceOf[JsArray].value
        arr.size shouldBe 3000
        // TODO this sort order indicates a numbering problem with result sorting (see TXMNT-64)
        Json.fromJson[AddressRecord](arr.head).get.address.line1 shouldBe "1 Bankside"
        Json.fromJson[AddressRecord](arr(1)).get.address.line1 shouldBe "10 Bankside"
        Json.fromJson[AddressRecord](arr(2)).get.address.line1 shouldBe "100 Bankside"
        Json.fromJson[AddressRecord](arr(3)).get.address.line1 shouldBe "1000 Bankside"
      }

      "filter results" in {
        val body = Await.result(wsClient.url(url("/v2/uk/addresses?fuzzy=bankside&filter=100")).get, defaultDuration)
          .body
        val json = Json.parse(body)
        val seq = Json.fromJson[Seq[AddressRecord]](json).get
        val arr = json.asInstanceOf[JsArray].value
        arr.size shouldBe 1
        // TODO this sort order indicates a numbering problem with result sorting (see TXMNT-64)
        Json.fromJson[AddressRecord](arr.head).get.address.line1 shouldBe "100 Bankside"
      }


      "set the content type to application/json" in {
        val response = Await.result(wsClient.url(url("/v2/uk/addresses?fuzzy=Stamford+Street")).get, defaultDuration)
        val contentType = response.header("Content-Type").get
        contentType should startWith("application/json")
      }

      "set the cache-control header and include a positive max-age in it" ignore {
        val response = Await.result(wsClient.url(url("/v2/uk/addresses?fuzzy=A+Street")).get, defaultDuration)
        val h = response.header("Cache-Control")
        h.size should be  > 0
        h.get should contain("max-age=")
      }

      "set the etag header" ignore {
        val response = Await.result(wsClient.url(url("/v2/uk/addresses?fuzzy=A+Street")).get, defaultDuration)
        val h = response.header("ETag")
        h.size should be  > 0
      }

      "give a successful response for an unmatched result" in {
        val response = Await.result(wsClient.url(url("/v2/uk/addresses?fuzzy=zzz+zzz+zzz")).get, defaultDuration)
        response.status shouldBe OK
      }

      "give an empty array for an unmatched result" in {
        val response = Await.result(wsClient.url(url("/v2/uk/addresses?fuzzy=zzz+zzz+zzz"))
          .withHttpHeaders("User-Agent" -> "test").get, 100 seconds)
        response.body shouldBe "[]"
      }

    }


    "client error" must {

      "give a bad request when the origin header is absent" in {
        val path = "/v2/uk/addresses?fuzzy=FX1+4AB"
        val response = Await.result(wsClient.url(url(path)).get, defaultDuration)
        response.status shouldBe BAD_REQUEST
      }

      "give a bad request when the fuzzy parameter is absent" in {
        val response = Await.result(wsClient.url(url("/v2/uk/addresses")).get, defaultDuration)
        response.status shouldBe BAD_REQUEST
      }
    }
  }
}
