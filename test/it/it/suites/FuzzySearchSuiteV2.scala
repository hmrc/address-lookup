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
import controllers.SearchParameters
import controllers.services.AddressSearcher
import it.helper.AppServerTestApi
import model.address.{AddressRecord, Location}
import model.internal.DbAddress
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
import repositories.InMemoryAddressLookupRepository.{boulevardAddresses, doFilter, singleAddresses}
import services.AddressLookupService

import scala.concurrent.Future

class FuzzySearchSuiteV2()
  extends AnyWordSpec with GuiceOneServerPerSuite with AppServerTestApi {

  val repository: AddressLookupService = mock[AddressLookupService]
  override def fakeApplication(): Application = {
    SharedMetricRegistries.clear()
    new GuiceApplicationBuilder()
        .overrides(Bindings.bind(classOf[AddressSearcher]).toInstance(repository))
        .build()
  }

  override val appEndpoint: String = s"http://localhost:$port"
  override val wsClient: WSClient = app.injector.instanceOf[WSClient]

  "fuzzy search lookup" when {
    import AddressRecord.formats._

    "successful" should {

      "give a successful response for a known postcode  - uk route" in {
        val sp = SearchParameters(fuzzy = Some("A Street"))
        when(repository.searchFuzzy(meq(sp))).thenReturn(Future.successful(List(DbAddress("GB11111", List("A House 27-45", "A Street"), "London", "FX9 9PY", Some("GB-ENG"), Some("GB"),
          Some(5840), Some("en"), None, Some(Location("12.345678", "-12.345678").toString)))))
        val response = get("/v2/uk/addresses?fuzzy=A+Street")
        response.status shouldBe OK
      }

      "give a successful response for a known v.large postcode- uk route" in {
        val sp = SearchParameters(fuzzy = Some("bankside"))
        when(repository.searchFuzzy(meq(sp))).thenReturn(Future.successful(boulevardAddresses.toList))
        val response = get("/v2/uk/addresses?fuzzy=bankside")
        response.status shouldBe OK
      }

      "give sorted results when multiple addresses are returned" in {
        val sp = SearchParameters(fuzzy = Some("bankside"))
        when(repository.searchFuzzy(meq(sp))).thenReturn(Future.successful(boulevardAddresses.toList))
        val body = get("/v2/uk/addresses?fuzzy=bankside").body
        val json = Json.parse(body)
        val arr = json.as[List[AddressRecord]]
        arr.size shouldBe 3000
        // TODO this sort order indicates a numbering problem with result sorting (see TXMNT-64)
        arr.head.address.line1 shouldBe "1 Bankside"
        arr(1).address.line1 shouldBe "10 Bankside"
        arr(2).address.line1 shouldBe "100 Bankside"
        arr(3).address.line1 shouldBe "1000 Bankside"
      }

      "filter results" in {
        val sp = SearchParameters(fuzzy = Some("bankside"), filter = Some("100"))
        when(repository.searchFuzzy(meq(sp))).thenReturn(Future.successful(
          doFilter(boulevardAddresses.filter(_.line1 startsWith("100 ")), Some("100")).toList
        ))
        val body = get("/v2/uk/addresses?fuzzy=bankside&filter=100").body
        val json = Json.parse(body)
        val arr = json.as[List[AddressRecord]]
        arr.size shouldBe 1
        // TODO this sort order indicates a numbering problem with result sorting (see TXMNT-64)
        arr.head.address.line1 shouldBe "100 Bankside"
      }


      "set the content type to application/json" in {
        val sp = SearchParameters(fuzzy = Some("Stamford Street"))
        when(repository.searchFuzzy(meq(sp))).thenReturn(Future.successful(List()))
        val response = get("/v2/uk/addresses?fuzzy=Stamford+Street")
        val contentType = response.header("Content-Type").get
        contentType should startWith ("application/json")
      }

      "set the cache-control header and include a positive max-age in it" in {
        val sp = SearchParameters(fuzzy = Some("A Street"))
        when(repository.searchFuzzy(meq(sp))).thenReturn(
          Future.successful(singleAddresses.filter(_.line2 == "A Street").toList))
        val response = get("/v2/uk/addresses?fuzzy=A+Street")
        val h = response.header("Cache-Control")
        h should not be empty
        h.get should include ("max-age=")
      }

      "set the etag header" ignore {
        val sp = SearchParameters(fuzzy = Some("A Street"))
        when(repository.searchFuzzy(meq(sp))).thenReturn(
          Future.successful(singleAddresses.filter(_.line2 == "A Street").toList))
        val response = get("/v2/uk/addresses?fuzzy=A+Street")
        val h = response.header("ETag")
        h should not be empty
      }

      "give a successful response for an unmatched result" in {
        val sp = SearchParameters(fuzzy = Some("zzz zzz zzz"))
        when(repository.searchFuzzy(meq(sp))).thenReturn(Future.successful(List()))
        val response = get("/v2/uk/addresses?fuzzy=zzz+zzz+zzz")
        response.status shouldBe OK
      }

      "give an empty array for an unmatched result" in {
        val sp = SearchParameters(fuzzy = Some("zzz zzz zzz"))
        when(repository.searchFuzzy(meq(sp))).thenReturn(Future.successful(List()))
        val response = get("/v2/uk/addresses?fuzzy=zzz+zzz+zzz")
        response.body shouldBe "[]"
      }

    }


    "client error" should {

      "give a bad request when the origin header is absent" in {
        val path = "/v2/uk/addresses?fuzzy=FX1+4AB"
        val response = await(wsClient.url(appEndpoint + path).withMethod("GET").execute())
        response.status shouldBe BAD_REQUEST
      }

      "give a bad request when the fuzzy parameter is absent" in {
        val response = get("/v2/uk/addresses")
        response.status shouldBe BAD_REQUEST
      }
    }
  }
}