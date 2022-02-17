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
import it.suites.Fixtures.{fx1_6jn_a_terse, fx1_6jn_b_terse, nukdb_fx1, nukdb_fx2}
import model.address.AddressRecord
import model.internal.NonUKAddress
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
import repositories.InMemoryAddressLookupRepository.{dbAddresses, nonUKAddress}
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
    import NonUKAddress._

    "successful" should {

      "give a successful response for bermuda" in {
        when(repository.findInCountry(meq("BM"), meq("HM02"))).thenReturn(
          Future.successful(nonUKAddress.getOrElse("bm", Seq()).filter(_.postcode.contains("HM02")).toList))

        val response = post("/country/BM/lookup", """{"filter":"HM02"}""")

        val contentType = response.header("Content-Type").get
        contentType should startWith("application/json")

        val c = response.header("Cache-Control")
        c should not be empty
        c.get should include("max-age=")

        response.status shouldBe OK
      }

      "give a successful response for an unknown postcode" in {
        when(repository.findInCountry(meq("BM"), meq("HM99"))).thenReturn(
          Future.successful(nonUKAddress.getOrElse("bm", Seq()).filter(_.postcode.contains("HM99")).toList))

        val response = post("/country/BM/lookup", """{"filter":"HM99"}""")
        response.status shouldBe OK
        response.body shouldBe "[]"
      }

      "give sorted results when two addresses are returned" in {
        when(repository.findInCountry(meq("BM"), meq("WK04"))).thenReturn(
          Future.successful(nonUKAddress.getOrElse("bm", Seq()).filter(_.postcode.contains("WK04")).toList))

        val response = post("/country/BM/lookup", """{"filter":"WK04"}""")

        val body = response.body
        body should startWith("[{")
        body should endWith("}]")

        val json = Json.parse(body)
        val seq = Json.fromJson[Seq[NonUKAddress]](json).get
        seq.size shouldBe 2
        seq shouldBe Seq(nukdb_fx1, nukdb_fx2)
      }
    }

    "client error" should {
      "give a 404 response for an unknown country" in {
        val response = post("/country/QQ/lookup", """{"filter":"ZZ10 9ZZ"}""")
        response.status shouldBe BAD_REQUEST
      }

      "give a bad request for an country code that is too long" in {
        val response = post("/country/QQQ/lookup", """{"filter":"ZZ10 9ZZ"}""")
        response.status shouldBe BAD_REQUEST
      }

      "give a bad request for an country code that is too short" in {
        val response = post("/country/Q/lookup", """{"filter":"ZZ10 9ZZ"}""")
        response.status shouldBe BAD_REQUEST
      }

      "give a bad request for an badly formed country code" in {
        val response = post("/country/22/lookup", """{"filter":"ZZ10 9ZZ"}""")
        response.status shouldBe BAD_REQUEST
      }

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
