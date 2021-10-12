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

import com.codahale.metrics.SharedMetricRegistries
import controllers.services.AddressSearcher
import it.helper.AppServerTestApi
import model.address.AddressRecord
import org.mockito.ArgumentMatchers.{eq => meq}
import org.mockito.Mockito.when
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.test.Helpers._
import play.inject.Bindings
import repositories.InMemoryAddressLookupRepository.singleAddresses
import services.AddressLookupService

import scala.concurrent.Future

// Please keep IdLookupSuiteV1 and IdLookupSuiteV2 as similar as appropriate.

class IdLookupSuiteV2()
  extends AnyWordSpec with GuiceOneServerPerSuite with AppServerTestApi {

  import FixturesV2._

  val repository: AddressLookupService = mock[AddressLookupService]
  override def fakeApplication(): Application = {
    SharedMetricRegistries.clear()
    new GuiceApplicationBuilder()
        .overrides(Bindings.bind(classOf[AddressSearcher]).toInstance(repository))
        .build()
  }

  override val appEndpoint: String = s"http://localhost:$port"
  override val wsClient: WSClient = app.injector.instanceOf[WSClient]

  "id lookup" when {
    import AddressRecord.formats._

    "successful" should {

      "give a successful response for a known id - uk route" in {
        when(repository.findID(meq("GB22222"))).thenReturn(
          Future.successful(singleAddresses.find(_.id == "GB22222")))
        val response = get("/v2/uk/addresses/GB22222")
        response.status shouldBe OK
        val body = response.body
        val json = Json.parse(body)
        val address1 = json.as[AddressRecord]
        address1 shouldBe fx1_6jn_a_terse
      }

      "set the content type to application/json" in {
        when(repository.findID(meq("GB11111"))).thenReturn(
          Future.successful(singleAddresses.find(_.id == "GB11111")))
        val response = get("/v2/uk/addresses/GB11111")
        val contentType = response.header("Content-Type").get
        contentType should startWith ("application/json")
      }

      "set the cache-control header and include a positive max-age ignore it" ignore {
        when(repository.findID(meq("GB0001"))).thenReturn(
          Future.successful(singleAddresses.find(_.id == "GB0001")))
        val response = get("/v2/uk/addresses/GB0001")
        val h = response.header("Cache-Control")
        h should not be empty
        h.get should include ("max-age=")
      }

      "set the etag header" ignore {
        when(repository.findID(meq("GB0001"))).thenReturn(
          Future.successful(singleAddresses.find(_.id == "GB0001")))
        val response = get("/v2/uk/addresses/GB0001")
        val h = response.header("ETag")
        h.nonEmpty shouldBe true
      }

      "give a 404 response for an unknown id" in {
        when(repository.findID(meq("X0"))).thenReturn(
          Future.successful(singleAddresses.find(_.id == "X0")))
        val response = get("/v2/uk/addresses/X0")
        response.status shouldBe NOT_FOUND
      }
    }


    "client error" should {
      "give a bad request when the origin header is absent" in {
        when(repository.findID(meq("GB0001"))).thenReturn(
          Future.successful(singleAddresses.find(_.id == "GB0001")))
        val path = "/v2/uk/addresses/GB0001"
        val response = await(wsClient.url(appEndpoint + path).withMethod("GET").execute())
        response.status shouldBe BAD_REQUEST
      }
    }
  }
}