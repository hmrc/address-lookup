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
import it.helper.AppServerTestApi
import model.address.AddressRecord
import org.mockito.ArgumentMatchers.{eq => meq}
import org.mockito.Mockito.when
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsArray, Json}
import play.api.libs.ws.WSClient
import play.api.test.Helpers._
import play.inject.Bindings
import repositories.ABPAddressRepository
import repositories.InMemoryAddressTestData.dbAddresses

import scala.concurrent.Future

// Please keep UprnLookupSuiteV2 and UprnLookupSuiteV2 as similar as appropriate.

class UprnLookupPostSuite()
  extends AnyWordSpec with GuiceOneServerPerSuite with AppServerTestApi {

  import Fixtures._

  val repository: ABPAddressRepository = mock[ABPAddressRepository]
  override def fakeApplication(): Application = {
    SharedMetricRegistries.clear()
    new GuiceApplicationBuilder()
        .overrides(Bindings.bind(classOf[ABPAddressRepository]).toInstance(repository))
        .build()
  }

  override val appEndpoint: String = s"http://localhost:$port"
  override val wsClient: WSClient = app.injector.instanceOf[WSClient]

  "uprn lookup" when {
    import AddressRecord.formats._

    "successful" should {

      "give a successful response for a known uprn - uk route" in {
        when(repository.findUprn(meq("11111"))).thenReturn(
          Future.successful(dbAddresses.filter(_.uprn == 11111L).toList))

        val response = post("/lookup/by-uprn", """{"uprn": "11111"}""")
        response.status shouldBe OK
        val body = response.body
        val json = Json.parse(body)
        val arr = json.asInstanceOf[JsArray].value
        arr.size shouldBe 1
        val address1 = Json.fromJson[AddressRecord](arr.head).get
        address1 shouldBe fx9_9py_terse
      }

      "set the content type to application/json" in {
        when(repository.findUprn(meq("9999999999"))).thenReturn(
          Future.successful(dbAddresses.filter(_.uprn == 9999999999L).toList))

        val response = post("/lookup/by-uprn", """{"uprn":"9999999999"}""")
        val contentType = response.header("Content-Type").get
        contentType should startWith ("application/json")
      }

      "set the cache-control header and include a positive max-age ignore it" ignore {
        when(repository.findUprn(meq("9999999999"))).thenReturn(
          Future.successful(dbAddresses.filter(_.uprn == 9999999999L).toList))

        val response = post("/lookup/by-uprn", """{"uprn":"9999999999"}""")
        val h = response.header("Cache-Control")
        h should not be empty
        h.get should include ("max-age=")
      }

      "set the etag header" ignore {
        when(repository.findUprn(meq("9999999999"))).thenReturn(
          Future.successful(dbAddresses.filter(_.uprn == 9999999999L).toList))

        val response = post("/lookup/by-uprn", """{"uprn":"9999999999"}""")
        val h = response.header("ETag")
        h.nonEmpty shouldBe true
      }

      "give a successful response with an empty array for an unknown uprn" in {
        when(repository.findUprn(meq("0"))).thenReturn(
          Future.successful(dbAddresses.filter(_.uprn == 0L).toList))

        val response = post("/lookup/by-uprn", """{"uprn":"0"}""")
        response.status shouldBe OK
        response.body shouldBe "[]"
      }
    }


    "client error" should {

      "give a bad request when the origin header is absent" in {
        val path = "/lookup/by-uprn"
        val response = await(wsClient.url(appEndpoint + path).withMethod("POST").withBody("""{"uprn":"9999999999"}""").execute())
        response.status shouldBe BAD_REQUEST
      }

      "give a bad request when the uprn parameter is absent" in {
        val response = post("/lookup/by-uprn", "{}")
        response.status shouldBe BAD_REQUEST
      }

      "give a bad request when the payload is missing" in {
        val response = post("/lookup/by-uprn", "")
        response.status shouldBe BAD_REQUEST
      }
    }
  }
}
