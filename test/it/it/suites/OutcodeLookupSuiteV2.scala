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
import model.address.{AddressRecord, Outcode}
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
import repositories.InMemoryAddressLookupRepository.{dbAddresses, doFilter}
import services.AddressLookupService

import scala.concurrent.Future

class OutcodeLookupSuiteV2()
  extends AnyWordSpec with GuiceOneServerPerSuite with AppServerTestApi {

  SharedMetricRegistries.clear()

  import FixturesV2._

  val repository: AddressLookupService = mock[AddressLookupService]
  override def fakeApplication(): Application = {
    SharedMetricRegistries.clear()
    new GuiceApplicationBuilder()
        .overrides(Bindings.bind(classOf[AddressLookupService]).toInstance(repository))
        .build()
  }

  override val appEndpoint: String = s"http://localhost:$port"
  override val wsClient: WSClient = app.injector.instanceOf[WSClient]

  "outcode lookup" when {
    import AddressRecord.formats._

    "successful" should {

      "give a successful response for a known outcode - uk route" in {
        when(repository.findOutcode(meq(Outcode("fx1")), meq("House"))).thenReturn(
          Future.successful(doFilter(dbAddresses.filter(_.postcode.startsWith("FX1")).toList, Some("House")).toList))

        val response = get("/v2/uk/addresses?outcode=fx1&filter=House")
        response.status shouldBe OK
      }

      "give a successful response for a known outcode - old style 'X-Origin'" in {
        when(repository.findOutcode(meq(Outcode("fx1")), meq("House"))).thenReturn(
          Future.successful(doFilter(dbAddresses.filter(_.postcode.startsWith("FX1")), Some("House")).toList))

        val response = request("GET", "/v2/uk/addresses?outcode=fx1&filter=House", headerOrigin -> "xxx")
        response.status shouldBe OK
      }

      "set the content type to application/json" in {
        when(repository.findOutcode(meq(Outcode("FX1")), meq("Dorset"))).thenReturn(
          Future.successful(doFilter(dbAddresses.filter(_.postcode.startsWith("FX1")), Some("Dorset")).toList))

        val response = get("/v2/uk/addresses?outcode=FX1&filter=Dorset")
        val contentType = response.header("Content-Type").get
        contentType should startWith ("application/json")
      }

      "set the cache-control header and include a positive max-age in it" ignore {
        when(repository.findOutcode(meq(Outcode("FX1")), meq("Dorset"))).thenReturn(
          Future.successful(doFilter(dbAddresses.filter(_.postcode.startsWith("FX1")), Some("Dorset")).toList))

        val response = get("/v2/uk/addresses?outcode=FX1&filter=Dorset")
        val h = response.header("Cache-Control")
        h should not be empty
        h.get should include ("max-age=")
      }

      "set the etag header" ignore {
        when(repository.findOutcode(meq(Outcode("FX1")), meq("Dorset"))).thenReturn(
          Future.successful(doFilter(dbAddresses.filter(_.postcode.startsWith("FX1")), Some("Dorset")).toList))

        val response = get("/v2/uk/addresses?outcode=FX1&filter=Dorset")
        val h = response.header("ETag")
        h.nonEmpty shouldBe true
      }

      "give a successful response for an unknown outcode" in {
        when(repository.findOutcode(meq(Outcode("zz10")), meq("Dorset"))).thenReturn(
          Future.successful(doFilter(dbAddresses.filter(_.postcode.startsWith("ZZ01")), Some("Dorset")).toList))

        val response = get("/v2/uk/addresses?outcode=zz10&filter=Dorset")
        response.status shouldBe OK
      }

      "give an empty array for an unknown outcode" in {
        when(repository.findOutcode(meq(Outcode("ZZ091")), meq("Dorset"))).thenReturn(
          Future.successful(doFilter(dbAddresses.filter(_.postcode.startsWith("ZZ01")), Some("Dorset")).toList))

        val response = get("/v2/uk/addresses?outcode=ZZ10&filter=Dorset")
        response.body shouldBe "[]"
      }

      "give sorted results when two addresses are returned" in {
        when(repository.findOutcode(meq(Outcode("FX1")), meq("Boulevard"))).thenReturn(
          Future.successful(doFilter(dbAddresses.filter(_.postcode.startsWith("FX1")), Some("Boulevard")).toList))

        val body = get("/v2/uk/addresses?outcode=FX1&filter=Boulevard").body
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
        when(repository.findOutcode(meq(Outcode("FX1")), meq("FOO"))).thenReturn(
          Future.successful(doFilter(dbAddresses.filter(_.postcode.startsWith("FX1")), Some("FOO")).toList))

        val path = "/v2/uk/addresses?outcode=FX1&filter=FOO"
        val response = await(wsClient.url(appEndpoint + path).withMethod("GET").execute())
        response.status shouldBe BAD_REQUEST
      }

      "give a bad request when the outcode parameter is absent" in {
        val response = get("/v2/uk/addresses")
        response.status shouldBe BAD_REQUEST
      }

      "give a bad request when the outcode parameter is rubbish text" in {
        val response = get("/v2/uk/addresses?outcode=ThisIsNotAnOutcode&filter=FOO")
        response.status shouldBe BAD_REQUEST
      }

      "give a bad request when an unexpected parameter is sent" in {
        val response = get("/v2/uk/addresses?outcode=FX1+4AC&foo=bar")
        response.status shouldBe BAD_REQUEST
      }
    }
  }
}