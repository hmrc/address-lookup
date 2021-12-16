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
import model.address.Postcode
import org.mockito.ArgumentMatchers.{eq => meq}
import org.mockito.Mockito.when
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.inject.Bindings
import repositories.InMemoryAddressLookupRepository.{dbAddresses, doFilter}
import services.AddressLookupService

import scala.concurrent.Future

class MetricsSuiteV2()
  extends AnyWordSpec with GuiceOneServerPerSuite with AppServerTestApi {

  private val className = "AddressLookupService"

  val repository: AddressLookupService = mock[AddressLookupService]
  override def fakeApplication(): Application = {
    SharedMetricRegistries.clear()
    new GuiceApplicationBuilder()
        .overrides(Bindings.bind(classOf[AddressLookupService]).toInstance(repository))
        .build()
  }

  override val appEndpoint: String = s"http://localhost:$port"
  override val wsClient: WSClient = app.injector.instanceOf[WSClient]

  "metrics" when {
    "successful" should {
      "give a timer for the findUprn search case" in {
        when(repository.findUprn(meq("9999999999"))).thenReturn(
          Future.successful(dbAddresses.find(_.id == "9999999999").toList))

        post("/lookup/by-uprn", """{"uprn":"9999999999"}""").status shouldBe OK

        val response = get("/admin/metrics")
        response.status shouldBe OK
        (response.json \ "timers" \ s"$className.findUprn" \ "count").as[Int] should be > 0
      }

      "give a timer for the findPostcode search case" in {
        when(repository.findPostcode(meq(Postcode("FX1 9PY")), meq(None))).thenReturn(
          Future.successful(dbAddresses.filter(_.postcode == "FX1 9PY").toList))

        post("/lookup", """{"postcode":"FX1 9PY"}""").status shouldBe OK

        val response = get("/admin/metrics")
        response.status shouldBe OK
        (response.json \ "timers" \ s"$className.findPostcode" \ "count").as[Int] should be > 0
      }

      "give a timer for the findPostcodeFilter search case" in {
        when(repository.findPostcode(meq(Postcode("SE1 9PY")), meq(Option("10")))).thenReturn(
          Future.successful(doFilter(dbAddresses.filter(_.postcode == "SE1 9PY"), Option("10")).toList))

        post("/lookup", """{"postcode":"SE1 9PY", "filter":"10"}""").status shouldBe OK

        val response = get("/admin/metrics")
        response.status shouldBe OK
        (response.json \ "timers" \ s"$className.findPostcodeFilter" \ "count").as[Int] should be > 0
      }
    }
  }
}
