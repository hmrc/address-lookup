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
import it.suites.Fixtures.{fx1_6jn_a_terse, fx1_6jn_b_terse}
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
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import play.inject.Bindings
import repositories.InMemoryAddressLookupRepository.dbAddresses
import services.AddressLookupService

import scala.concurrent.Future

class CountryLookupSuiteV2()
  extends AnyWordSpec with GuiceOneServerPerSuite with AppServerTestApi {

  private val largePostcodeExampleSize = 2517

  val repository: AddressLookupService = mock[AddressLookupService]

  override def fakeApplication(): Application = {
    SharedMetricRegistries.clear()
    new GuiceApplicationBuilder()
      .overrides(Bindings.bind(classOf[AddressLookupService]).toInstance(repository))
      .build()
  }

  override val appEndpoint: String = s"http://localhost:$port"
  override val wsClient: WSClient = app.injector.instanceOf[WSClient]

  "country specific lookup" when {
    import AddressRecord.formats._

    "successful" should {

      "give a successful response for great britain" in {
        when(repository.findInCountry(meq("GB"), meq("FX1 9PY"))).thenReturn(
          Future.successful(dbAddresses.filter(_.postcode == "FX1 9PY").toList))

        val response = post("/country/GB/lookup", """{"filter":"FX1 9PY"}""")

        val contentType = response.header("Content-Type").get
        contentType should startWith("application/json")

        val c = response.header("Cache-Control")
        c should not be empty
        c.get should include("max-age=")

        response.status shouldBe OK
      }

      "give a successful response for an unknown postcode" in {
        when(repository.findInCountry(meq("GB"), meq("ZZ10 9ZZ"))).thenReturn(
          Future.successful(dbAddresses.filter(_.postcode == "ZZ10 9ZZ").toList))

        val response = post("/country/GB/lookup", """{"filter":"ZZ10 9ZZ"}""")
        response.status shouldBe OK
        response.body shouldBe "[]"
      }

      "give sorted results when two addresses are returned" in {
        when(repository.findInCountry(meq("GB"), meq("FX1 6JN"))).thenReturn(
          Future.successful(dbAddresses.filter(_.postcode == "FX1 6JN").toList))

        val response = post("/country/GB/lookup", """{"filter":"FX1 6JN"}""")

        val body = response.body
        body should startWith("[{")
        body should endWith("}]")

        val json = Json.parse(body)
        val seq = Json.fromJson[Seq[AddressRecord]](json).get
        seq.size shouldBe 2
        seq shouldBe Seq(fx1_6jn_a_terse, fx1_6jn_b_terse)
      }
    }

    "client error" should {
      "give a bad request when the origin header is absent" in {
        val path = "/country/GB/lookup"
        val response = await(wsClient.url(appEndpoint + path).withMethod("POST").withBody("""{"filter":"FX1 4AB"}""").execute())
        response.status shouldBe BAD_REQUEST
      }

      "give a bad request when the filter parameter is absent" in {
        val response = post("/country/GB/lookup", "{}")
        response.status shouldBe BAD_REQUEST
      }

      "give a bad request when an unexpected parameter is sent on its own" in {
        val response = post("/country/GB/lookup", """{"foo":"FX1 4AC"}""")
        response.status shouldBe BAD_REQUEST
      }

      // PlayFramework doesn't provide a hook for correctly handling bad method errors.
      // It was removed from earlier versions.
      "give a bad method when using GET to the address URL" ignore {
        val response = request("GET", "/country/GB/lookup?postcode=FX1+9PY", headerOrigin -> "xxx")
        response.status shouldBe METHOD_NOT_ALLOWED
      }
    }
  }
}