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
import repositories.InMemoryAddressLookupRepository.{dbAddresses, doFilter}
import services.AddressLookupService

import scala.concurrent.Future

class TownLookupPostSuite()
  extends AnyWordSpec with GuiceOneServerPerSuite with AppServerTestApi {

  val repository: AddressLookupService = mock[AddressLookupService]
  override def fakeApplication(): Application = {
    SharedMetricRegistries.clear()
    new GuiceApplicationBuilder()
        .overrides(Bindings.bind(classOf[AddressLookupService]).toInstance(repository))
        .build()
  }

  override val appEndpoint: String = s"http://localhost:$port"
  override val wsClient: WSClient = app.injector.instanceOf[WSClient]

  "lookup town POST" when {
    import model.address.AddressRecord.formats._

    "successful" should {

      "give a successful response for a known town - uk route" in {
        when(repository.findTown(meq("some-town"), meq(None))).thenReturn(
          Future.successful(dbAddresses.filter(_.town == "some-town").toList))

        val payload =
          """{"posttown": "some-town"}""".stripMargin

        val response = post("/lookup/by-post-town", payload)
        response.status shouldBe OK
      }

      "give a successful response for a known town - old style 'X-Origin'" in {
        when(repository.findTown(meq("some-town"), meq(None))).thenReturn(
          Future.successful(dbAddresses.filter(_.town == "some-town").toList))

        val payload =
          """{
            |  "posttown": "some-town"
            |}""".stripMargin

        val response = request("POST", "/lookup/by-post-town", payload, headerOrigin -> "xxx")
        response.status shouldBe OK
      }

      "give a successful response for a known v.large town - uk route" in {
        when(repository.findTown(meq("ATown"), meq(None))).thenReturn(
          Future.successful(dbAddresses.filter(_.town == "ATown").toList))

        val payload =
          """{
            |  "posttown": "ATown"
            |}""".stripMargin

        val response = post("/lookup/by-post-town", payload)
        response.status shouldBe OK
        val json = Json.parse(response.body)
        val arr = json.asInstanceOf[JsArray].value
        arr.size shouldBe 3000
        val address1 = Json.fromJson[AddressRecord](arr.head).get.address
        address1.line1 shouldBe "1 Bankside"
        address1.line2 shouldBe ""
        address1.line3 shouldBe ""
        address1.town shouldBe "ATown"
        address1.postcode shouldBe "FX4 7AJ"
      }

      "give a successful filtered response for a known v.large town - uk route" in {
        when(repository.findTown(meq("ATown"), meq(None))).thenReturn(
          Future.successful(doFilter(dbAddresses.filter(_.town == "ATown"), Option("10")).toList))

        val payload =
          """{
            |  "posttown": "ATown",
            |  "filter": "10"
            |}""".stripMargin

        val response = post("/lookup/by-post-town", payload)
        response.status shouldBe OK
        val json = Json.parse(response.body)
        val arr = json.asInstanceOf[JsArray].value
        arr.size shouldBe 2
        val address1 = Json.fromJson[AddressRecord](arr.head).get.address
        address1.line1 shouldBe "10 Bankside"
        address1.line2 shouldBe ""
        address1.line3 shouldBe ""
        address1.town shouldBe "ATown"
        address1.postcode shouldBe "FX4 7AJ"
        val address2 = Json.fromJson[AddressRecord](arr.tail.head).get.address
        address2.line1 shouldBe "Flat 10"
        address2.line2 shouldBe "A Apartments"
        address2.line3 shouldBe "ARoad"
        address2.town shouldBe "ATown"
        address2.postcode shouldBe "FX4 7AL"
      }

      "set the content type to application/json" in {
        when(repository.findTown(meq("some-town"), meq(None))).thenReturn(
          Future.successful(dbAddresses.filter(_.town == "some-town").toList))

        val payload =
        """{
          |  "posttown": "some-town"
          |}""".stripMargin

        val response = post("/lookup/by-post-town", payload)
        val contentType = response.header("Content-Type").get
        contentType should startWith ("application/json")
      }

      "set the cache-control header and include a positive max-age in it" ignore {
        when(repository.findTown(meq("some-town"), meq(None))).thenReturn(
          Future.successful(dbAddresses.filter(_.town == "some-town").toList))

        val payload =
          """{
            |  "posttown": "some-town"
            |}""".stripMargin

        val response = post("/lookup/by-post-town", payload)
        val h = response.header("Cache-Control")
        h should not be empty
        h.get should include ("max-age=")
      }

      "set the etag header" ignore {
        when(repository.findTown(meq("some-town"), meq(None))).thenReturn(
          Future.successful(dbAddresses.filter(_.town == "some-town").toList))

        val payload = """{
          |  "posttown": "some-town"
          |}""".stripMargin

        val response = post("/lookup/by-post-town", payload)
        val h = response.header("ETag")
        h.nonEmpty shouldBe true
      }

      "give a successful response for an unknown town" in {
        when(repository.findTown(meq("unknown-town"), meq(None))).thenReturn(
          Future.successful(dbAddresses.filter(_.town == "unknown-town").toList))

        val payload =
          """{
            |  "posttown": "unknown-town"
            |}""".stripMargin

        val response = post("/lookup/by-post-town", payload)
        response.status shouldBe OK
      }

      "give an empty array for an unknown town" in {
        when(repository.findTown(meq("unknown-town"), meq(None))).thenReturn(
          Future.successful(dbAddresses.filter(_.town == "unknown-town").toList))

        val payload =
          """{
            |  "posttown": "unknown-town"
            |}""".stripMargin

        val response = post("/lookup/by-post-town", payload)
        response.body shouldBe "[]"
      }
    }

    "client error" should {

      "give a bad request when the origin header is absent" in {
        when(repository.findTown(meq("some-town"), meq(None))).thenReturn(
          Future.successful(dbAddresses.filter(_.town == "some-town").toList))

        val payload =
          """{
            |  "posttown": "some-town"
            |}""".stripMargin

        val path = "/lookup/by-post-town"
        val response = await(wsClient.url(s"$appEndpoint$path")
          .withMethod("POST")
          .withHttpHeaders("content-type" -> "application/json")
          .withBody(payload).execute())

        response.status shouldBe BAD_REQUEST
      }

      "give a bad request when the town parameter is absent" in {
        val response = post("/lookup/by-post-town", "{}")
        response.status shouldBe BAD_REQUEST
        response.body shouldBe """{"obj.posttown":[{"msg":["error.path.missing"],"args":[]}]}"""
      }

      "give a bad request when the town parameter is of the wrong type" in {
        val payload =
          """{
            |  "posttown": 1234
            |}""".stripMargin

        val response = post("/lookup/by-post-town", payload)
        response.status shouldBe BAD_REQUEST
        response.body shouldBe """{"obj.posttown":[{"msg":["error.expected.jsstring"],"args":[]}]}"""
      }

      "give a bad request when an unexpected parameter is sent on its own" in {
        val payload =
          """{
            |  "foo": "some-town"
            |}""".stripMargin

        val response = post("/lookup/by-post-town", payload)
        response.status shouldBe BAD_REQUEST
        response.body shouldBe """{"obj.posttown":[{"msg":["error.path.missing"],"args":[]}]}"""
      }

      "not give a bad request when an unexpected parameter is sent" in {
        val payload =
          """{
            |  "posttown": "some-town",
            |  "foo": "bar"
            |}""".stripMargin

        val response = post("/lookup/by-post-town", payload)
        response.status shouldBe OK
      }

      "give a not found when an unknown path is requested" in {
        val response = post("/somethingElse", "{}")
        response.status shouldBe NOT_FOUND
      }
    }
  }
}
