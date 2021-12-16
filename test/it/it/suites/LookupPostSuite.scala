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
import model.address.{AddressRecord, Postcode}
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
import repositories.InMemoryAddressLookupRepository.{dbAddresses, singleAddresses}
import services.AddressLookupService

import scala.concurrent.Future

class LookupPostSuite()
  extends AnyWordSpec with GuiceOneServerPerSuite with AppServerTestApi {

  private val largePostcodeExampleSize = 2517

  val repository: AddressLookupService = mock[AddressLookupService]
  override def fakeApplication(): Application = {
    SharedMetricRegistries.clear()
    new GuiceApplicationBuilder()
        .overrides(Bindings.bind(classOf[AddressSearcher]).toInstance(repository))
        .build()
  }

  override val appEndpoint: String = s"http://localhost:$port"
  override val wsClient: WSClient = app.injector.instanceOf[WSClient]

  "lookup POST" when {
    import model.address.AddressRecord.formats._

    "successful" should {

      "give a successful response for a known postcode - uk route" in {
        when(repository.findPostcode(meq(Postcode("FX1 9PY")), meq(None))).thenReturn(
          Future.successful(singleAddresses.filter(_.postcode == "FX1 9PY").toList))

        val payload =
          """{"postcode": "fx1 9py"}""".stripMargin

        val response = post("/lookup", payload)
        response.status shouldBe OK
      }

      "give a successful response for a known postcode - old style 'X-Origin'" in {
        when(repository.findPostcode(meq(Postcode("FX1 9PY")), meq(None))).thenReturn(
          Future.successful(singleAddresses.filter(_.postcode == "FX1 9PY").toList))

        val payload =
          """{
            |  "postcode": "fx1  9py"
            |}""".stripMargin

        val response = request("POST", "/lookup", payload, headerOrigin -> "xxx")
        response.status shouldBe OK
      }

      "give a successful response for a known v.large postcode - uk route" in {
        when(repository.findPostcode(meq(Postcode("FX4 7AL")), meq(None))).thenReturn(
          Future.successful(dbAddresses.filter(_.postcode == "FX4 7AL").toList))

        val payload =
          """{
            |  "postcode": "fx47al"
            |}""".stripMargin

        val response = post("/lookup", payload)
        response.status shouldBe OK
        val json = Json.parse(response.body)
        val arr = json.as[List[AddressRecord]]
        arr.size shouldBe largePostcodeExampleSize
        val address1 = arr.head
        address1.address.line1 shouldBe "Flat 1"
        address1.address.line2 shouldBe "A Apartments"
        address1.address.line3 shouldBe "ARoad"
        address1.address.town shouldBe "ATown"
        address1.address.postcode shouldBe "FX4 7AL"
      }

      "set the content type to application/json" in {
        when(repository.findPostcode(meq(Postcode("FX4 9PY")), meq(None))).thenReturn(
          Future.successful(dbAddresses.filter(_.postcode == "FX4 9PY").toList))
        val payload =
        """{
          |  "postcode": "FX1 9PY"
          |}""".stripMargin

        val response = post("/lookup", payload)
        val contentType = response.header("Content-Type").get
        contentType should startWith ("application/json")
      }

      "set the cache-control header and include a positive max-age in it" ignore {
        when(repository.findPostcode(meq(Postcode("FX4 9PY")), meq(None))).thenReturn(
          Future.successful(dbAddresses.filter(_.postcode == "FX4 9PY").toList))
        val payload =
          """{
            |  "postcode": "FX1 9PY"
            |}""".stripMargin

        val response = post("/lookup", payload)
        val h = response.header("Cache-Control")
        h should not be empty
        h.get should include ("max-age=")
      }

      "set the etag header" ignore {
        when(repository.findPostcode(meq(Postcode("FX4 9PY")), meq(None))).thenReturn(
          Future.successful(dbAddresses.filter(_.postcode == "FX4 9PY").toList))

        val payload =
        """{
          |  "postcode": "FX1 9PY"
          |}""".stripMargin

        val response = post("/lookup", payload)
        val h = response.header("ETag")
        h.nonEmpty shouldBe true
      }

      "give a successful response for an unknown postcode" in {
        when(repository.findPostcode(meq(Postcode("ZZ10 9ZZ")), meq(None))).thenReturn(
          Future.successful(dbAddresses.filter(_.postcode == "ZZ10 9ZZ").toList))

        val payload =
          """{
            |  "postcode": "zz10 9zz"
            |}""".stripMargin

        val response = post("/lookup", payload)
        response.status shouldBe OK
      }

      "give an empty array for an unknown postcode" in {
        when(repository.findPostcode(meq(Postcode("ZZ10 9ZZ")), meq(None))).thenReturn(
          Future.successful(dbAddresses.filter(_.postcode == "ZZ10 9ZZ").toList))

        val payload =
          """{
            |  "postcode": "ZZ10 9ZZ"
            |}""".stripMargin

        val response = post("/lookup", payload)
        response.body shouldBe "[]"
      }
    }

    "client error" should {

      "give a bad request when the origin header is absent" in {
        when(repository.findPostcode(meq(Postcode("FX1 4AB")), meq(None))).thenReturn(
          Future.successful(dbAddresses.filter(_.postcode == "FX1 4AB").toList))

        val payload =
          """{
            |  "postcode": "FX1 4AB"
            |}""".stripMargin

        val path = "/lookup"
        val response = await(wsClient.url(s"$appEndpoint$path")
          .withMethod("POST")
          .withHttpHeaders("content-type" -> "application/json")
          .withBody(payload).execute())

        response.status shouldBe BAD_REQUEST
      }

      "give a bad request when the postcode parameter is absent" in {
        val response = post("/lookup", "{}")
        response.status shouldBe BAD_REQUEST
        response.body shouldBe """{"obj.postcode":[{"msg":["error.path.missing"],"args":[]}]}"""
      }

      "give a bad request when the postcode parameter is rubbish text" in {
        val payload =
          """{
            |  "postcode": "ThisIsNotAPostcode"
            |}""".stripMargin

        val response = post("/lookup", payload)
        response.status shouldBe BAD_REQUEST
        response.body shouldBe """{"obj.postcode":[{"msg":["error.invalid"],"args":[]}]}"""
      }

      "give a bad request when the postcode parameter is of the wrong type" in {
        val payload =
          """{
            |  "postcode": 1234
            |}""".stripMargin

        val response = post("/lookup", payload)
        response.status shouldBe BAD_REQUEST
        response.body shouldBe """{"obj.postcode":[{"msg":["error.expected.jsstring"],"args":[]}]}"""
      }

      "give a bad request when an unexpected parameter is sent on its own" in {
        when(repository.findPostcode(meq(Postcode("FX1 4AC")), meq(None))).thenReturn(
          Future.successful(dbAddresses.filter(_.postcode == "FX1 4AC").toList))

        val payload =
          """{
            |  "foo": "FX1 4AC"
            |}""".stripMargin

        val response = post("/lookup", payload)
        response.status shouldBe BAD_REQUEST
        response.body shouldBe """{"obj.postcode":[{"msg":["error.path.missing"],"args":[]}]}"""
      }

      "not give a bad request when an unexpected parameter is sent" in {
        when(repository.findPostcode(meq(Postcode("FX1 4AC")), meq(None))).thenReturn(
          Future.successful(dbAddresses.filter(_.postcode == "FX1 4AC").toList))

        val payload =
          """{
            |  "postcode": "FX1 4AC",
            |  "foo": "bar"
            |}""".stripMargin

        val response = post("/lookup", payload)
        response.status shouldBe OK
      }

      "give a not found when an unknown path is requested" in {
        val response = post("/somethingElse", "{}")
        response.status shouldBe NOT_FOUND
      }
    }
  }
}
