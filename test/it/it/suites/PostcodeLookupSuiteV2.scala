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
import model.address.{AddressRecord, Postcode}
import org.mockito.ArgumentMatchers.{eq => meq}
import org.mockito.Mockito.when
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsArray, Json}
import play.api.libs.ws.WSClient
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import play.inject.Bindings
import repositories.InMemoryAddressLookupRepository.{dbAddresses, doFilter}
import services.AddressLookupService

import scala.concurrent.Future

class PostcodeLookupSuiteV2 ()
  extends AnyWordSpec with GuiceOneServerPerSuite with AppServerTestApi {

  import FixturesV2._

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

  "postcode lookup" when {
    import AddressRecord.formats._

    "successful" should {

      "give a successful response for a known postcode - uk route" in {
        when(repository.findPostcode(meq(Postcode("fx1 9py")), meq(None))).thenReturn(
          Future.successful(dbAddresses.filter(_.postcode == "FX1 9PY").toList))

        val response = post("/lookup", """{"postcode":"fx1 9py"}""")
        response.status shouldBe OK
      }

      "give a successful response for a known postcode - old style 'X-Origin'" in {
        when(repository.findPostcode(meq(Postcode("fx1 9py")), meq(None))).thenReturn(
          Future.successful(dbAddresses.filter(_.postcode == "FX1 9PY").toList))

        val response = request("POST", "/lookup", """{"postcode":"fx1 9py"}""", headerOrigin -> "xxx")
        response.status shouldBe OK
      }

      "give a successful response for a known v.large postcode - uk route" in {
        when(repository.findPostcode(meq(Postcode("fx4 7al")), meq(None))).thenReturn(
          Future.successful(dbAddresses.filter(_.postcode == "FX4 7AL").toList))

        val response = post("/lookup", """{"postcode":"fx47al"}""")
        response.status shouldBe OK
        val json = Json.parse(response.body)
        val arr = json.asInstanceOf[JsArray].value
        arr.size shouldBe largePostcodeExampleSize
        val address1 = Json.fromJson[AddressRecord](arr.head).get.address
        address1.line1 shouldBe "FLAT 1"
        address1.line2 shouldBe "A APARTMENTS"
        address1.line3 shouldBe "AROAD"
        address1.town shouldBe "ATOWN"
        address1.postcode shouldBe "FX4 7AL"
      }

      "give a successful response for a po box postcode" in {
        when(repository.findPostcode(meq(Postcode("PO1 1PO")), meq(None))).thenReturn(
          Future.successful(dbAddresses.filter(_.postcode == "PO1 1PO").toList))

        val response = post("/lookup", """{"postcode":"PO11PO"}""")
        response.status shouldBe OK
        val json = Json.parse(response.body)
        val arr = json.asInstanceOf[JsArray].value
        val address1 = Json.fromJson[AddressRecord](arr.head).get
        address1.poBox shouldBe Some("1234")
      }

      "set the content type to application/json" in {
        when(repository.findPostcode(meq(Postcode("FX1 9PY")), meq(None))).thenReturn(
          Future.successful(dbAddresses.filter(_.postcode == "FX1 9PY").toList))

        val response = post("/lookup", """{"postcode":"FX1 9PY"}""")
        val contentType = response.header("Content-Type").get
        contentType should startWith ("application/json")
      }

      "set the cache-control header and include a positive max-age in it" ignore {
        when(repository.findPostcode(meq(Postcode("FX1 9PY")), meq(None))).thenReturn(
          Future.successful(dbAddresses.filter(_.postcode == "FX1 9PY").toList))

        val response = post("/lookup", """{"postcode":"FX1 9PY"}""")
        val h = response.header("Cache-Control")
        h should not be empty
        h.get should include ("max-age=")
      }

      "set the etag header" ignore {
        when(repository.findPostcode(meq(Postcode("FX1 9PY")), meq(None))).thenReturn(
          Future.successful(dbAddresses.filter(_.postcode == "FX1 9PY").toList))

        val response = post("/lookup", """{"postcode":"FX1 9PY"}""")
        val h = response.header("ETag")
        h.nonEmpty shouldBe true
      }

      "give a successful response for an unknown postcode" in {
        when(repository.findPostcode(meq(Postcode("zz10 9zz")), meq(None))).thenReturn(
          Future.successful(dbAddresses.filter(_.postcode == "ZZ10 9ZZ").toList))

        val response = post("/lookup", """{"postcode":"zz10 9zz"}""")
        response.status shouldBe OK
      }

      "give an empty array for an unknown postcode" in {
        when(repository.findPostcode(meq(Postcode("ZZ10 9ZZ")), meq(None))).thenReturn(
          Future.successful(dbAddresses.filter(_.postcode == "ZZ10 9ZZ").toList))

        val response = post("/lookup", """{"postcode":"ZZ10 9ZZ"}""")
        response.body shouldBe "[]"
      }

      "give sorted results when two addresses are returned" in {
        when(repository.findPostcode(meq(Postcode("FX1 6JN")), meq(None))).thenReturn(
          Future.successful(dbAddresses.filter(_.postcode == "FX1 6JN").toList))

        val body = post("/lookup", """{"postcode":"FX1 6JN"}""").body
        body should startWith("[{")
        body should endWith("}]")
        val json = Json.parse(body)
        val seq = Json.fromJson[Seq[AddressRecord]](json).get
        seq.size shouldBe 2
        seq shouldBe Seq(fx1_6jn_a_terse, fx1_6jn_b_terse)
      }

      "give single result when a filter is used" in {
        when(repository.findPostcode(meq(Postcode("FX1 6JN")), meq(None))).thenReturn(
          Future.successful(doFilter(dbAddresses.filter(_.postcode == "FX1 6JN"), Some("House")).toList))

        val body = post("/lookup", """{"postcode":"FX1 6JN", "filter":"House"}""").body
        body should startWith("[{")
        body should endWith("}]")
        val json = Json.parse(body)
        val seq = Json.fromJson[Seq[AddressRecord]](json).get
        seq.size shouldBe 1
        seq shouldBe Seq(fx1_6jn_b_terse)
      }

      "give sorted results when many addresses are returned" in {
        when(repository.findPostcode(meq(Postcode("FX1 1PG")), meq(None))).thenReturn(
          Future.successful(dbAddresses.filter(_.postcode == "FX1 1PG").toList))

        val body = post("/lookup", """{"postcode":"FX1 1PG"}""").body
        body should startWith("[{")
        body should endWith("}]")
        val json = Json.parse(body)
        val seq = Json.fromJson[Seq[AddressRecord]](json).get.map(_.address)
        seq.size shouldBe 46
        val expected = Seq(
          Seq("10 Astreet"),
          Seq("12-16 Astreet"),
          Seq("12-20 Astreet"),
          Seq("18-20 Astreet"),
          Seq("30-34 Astreet"),
          Seq("40 Astreet"),
          Seq("42 Astreet"),
          Seq("46 Astreet"),
          Seq("54 Astreet"),
          Seq("6 Astreet"),
          Seq("A1test, Acourt", "22-28 Astreet"),
          Seq("A2test Floor 1, Abuilding", "31 Astreet"),
          Seq("B1test Floor 4, Abuilding", "31 Astreet"),
          Seq("B2test, Acourt", "22-28 Astreet"),
          Seq("C1test, Abuilding", "31 Astreet"),
          Seq("Flat 1", "44 Astreet"),
          Seq("Flat 1", "48 Astreet"),
          Seq("Flat 2", "44 Astreet"),
          Seq("Flat 2", "48 Astreet"),
          Seq("Flat 3", "44 Astreet"),
          Seq("Flat 4", "44 Astreet"),
          Seq("Flat 5", "44 Astreet"),
          Seq("Floor 1", "48 Astreet"),
          Seq("Floor 1", "52 Astreet"),
          Seq("Floor 1, Abuilding", "31 Astreet"),
          Seq("Floor 1, Acourt", "22-28 Astreet"),
          Seq("Floor 2, Acourt", "22-28 Astreet"),
          Seq("Floor 3, Abuilding", "31 Astreet"),
          Seq("Floor 3, Acourt", "22-28 Astreet"),
          Seq("Floor 4, Abuilding", "31 Astreet"),
          Seq("Floor 4, Acourt", "22-28 Astreet"),
          Seq("Floor 5, Abuilding", "31 Astreet"),
          Seq("Fx1test", "38 Astreet"),
          Seq("Fx2test", "52 Astreet"),
          Seq("Fx3test Floor 3, Abuilding", "31 Astreet"),
          Seq("Fx4test Floor 5, Abuilding", "31 Astreet"),
          Seq("Fx5test Floor 4, Abuilding", "31 Astreet"),
          Seq("Ground Floor", "52 Astreet"),
          Seq("Ground Floor", "52 Astreet", "Floor 1"),
          Seq("H1test Floor 1, Abuilding", "31 Astreet"),
          Seq("H2test", "38 Astreet"),
          Seq("Key Training, Acourt", "22-28 Astreet"),
          Seq("M1test, Abuilding", "31 Astreet"),
          Seq("R1test, Abuilding", "31 Astreet"),
          Seq("R2test, Floor 5, Abuilding", "31 Astreet"),
          Seq("T1test", "44 Astreet")
        )
        for (i <- expected.indices) {
          seq(i).lines shouldBe expected(i)
          seq(i).town shouldBe "Acity"
          seq(i).postcode shouldBe "FX1 1PG"
        }
      }
    }


    "client error" should {
      "give a bad request when the origin header is absent" in {
        val path = "/lookup"
        val response = await(wsClient.url(appEndpoint + path).withMethod("POST").withBody("""{"postcode":"FX1 4AB"}""").execute())
        response.status shouldBe BAD_REQUEST
      }

      "give a bad request when the postcode parameter is absent" in {
        val response = post("/lookup", "{}")
        response.status shouldBe BAD_REQUEST
      }

      "give a bad request when the postcode parameter is rubbish text" in {
        val response = post("/lookup", """{"postcode":"ThisIsNotAPostcode"}""")
        response.status shouldBe BAD_REQUEST
      }

      "give a bad request when an unexpected parameter is sent on its own" in {
        val response = post("/lookup", """{"foo":"FX1 4AC"}""")
        response.status shouldBe BAD_REQUEST
      }

      "give a not found when an unknown path is requested" in {
        val response = post("/somethingElse", """{"foo":"FX1 4AD"}""")
        response.status shouldBe NOT_FOUND
      }

      // PlayFramework doesn't provide a hook for correctly handling bad method errors.
      // It was removed from earlier versions.
      "give a bad method when using GET to the address URL" ignore {
        val response = request("GET", "/lookup?postcode=FX1+9PY", headerOrigin -> "xxx")
        response.status shouldBe METHOD_NOT_ALLOWED
      }
    }
  }
}
