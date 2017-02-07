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

import javax.inject.Inject

import it.helper.AppServerTestApi
import it.tools.Utils.headerOrigin
import org.scalatest.{MustMatchers, WordSpec}
import osgb.outmodel.v1.AddressReadable._
import play.api.Application
import play.api.libs.json.{JsArray, Json}
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.test.Helpers._
import uk.gov.hmrc.address.v1.AddressRecord

class PostcodeLookupSuiteV1 @Inject() (val wsClient: WSClient, val appEndpoint: String, largePostcodeExampleSize: Int)(implicit val app: Application)
  extends WordSpec with MustMatchers with AppServerTestApi {

  import FixturesV1._

  "postcode lookup" when {

    "successful" must {

      "give a successful response for a known postcode - uk route" in {
        val response = get("/uk/addresses?postcode=fx1++9py")
        assert(response.status === OK, dump(response))
      }

      "give a successful response for a known postcode - gb route" in {
        val response = get("/gb/addresses?postcode=fx1++9py")
        assert(response.status === OK, dump(response))
      }

      "give a successful response for a known postcode  - old style url with .json" in {
        val response: WSResponse = get("/v1/uk/addresses.json?postcode=fx1++9py")
        assert(response.status === OK, dump(response))
      }

      "give a successful response for a known postcode - v1 route url" in {
        val response: WSResponse = get("/v1/uk/addresses?postcode=fx1++9py")
        assert(response.status === OK, dump(response))
      }

      "give a successful response for a known postcode - old style 'X-Origin'" in {
        val response = request("GET", "/v1/uk/addresses.json?postcode=fx1++9py", headerOrigin -> "xxx")
        assert(response.status === OK, dump(response))
      }

      "give a successfully truncated response for a long address" in {
        val response = get("/gb/addresses?postcode=fx22tb")
        assert(response.status === OK, dump(response))
        val body = response.body
        assert(body.startsWith("[{"), dump(response))
        assert(body.endsWith("}]"), dump(response))
        val json = Json.parse(body)
        val seq = Json.fromJson[Seq[AddressRecord]](json).get
        seq.size mustBe 1
        val address1 = seq.head.address
        address1.line1 mustBe "An address with a very long first l"
        address1.line2 mustBe "Second line of address is just as l"
        address1.line3 mustBe "Third line is not the longest but i"
        address1.town.get mustBe fx1_2tb.address.town.get.substring(0, 35)
      }

      "give an array for at least one item for a known postcode without a county" in {
        val response = get("/uk/addresses?postcode=fx9+9py")
        val body = response.body
        assert(body.startsWith("[{"), dump(response))
        assert(body.endsWith("}]"), dump(response))
        val json = Json.parse(body)
        val seq = Json.fromJson[Seq[AddressRecord]](json).get
        seq.size mustBe 1
        seq.head mustBe fx9_9py
      }

      "give a successful response for a known v.large postcode  - uk route" in {
        val response = get("/uk/addresses?postcode=fx47al")
        assert(response.status === OK, dump(response))
        val json = Json.parse(response.body)
        val arr = json.asInstanceOf[JsArray].value
        arr.size mustBe largePostcodeExampleSize
        val address1 = Json.fromJson[AddressRecord](arr.head).get.address
        address1.line1 mustBe "Flat 1"
        address1.line2 mustBe "A Apartments"
        address1.line3 mustBe "ARoad"
        address1.town.get mustBe "ATown"
        address1.postcode mustBe "FX4 7AL"
      }

      "set the content type to application/json" in {
        val response = get("/uk/addresses?postcode=FX1+9PY")
        val contentType = response.header("Content-Type").get
        assert(contentType.startsWith("application/json"), dump(response))
      }

      "set the cache-control header and include a positive max-age in it" ignore {
        val response = get("/uk/addresses?postcode=FX1+9PY")
        val h = response.header("Cache-Control")
        assert(h.nonEmpty && h.get.contains("max-age="), dump(response))
      }

      "set the etag header" ignore {
        val response = get("/uk/addresses?postcode=FX1+9PY")
        val h = response.header("ETag")
        assert(h.nonEmpty === true, dump(response))
      }

      "give a successful response for an unknown postcode" in {
        val response = get("/uk/addresses?postcode=zz10+9zz")
        assert(response.status === OK, dump(response))
      }

      "give an empty array for an unknown postcode" in {
        val response = get("/uk/addresses?postcode=ZZ10+9ZZ")
        assert(response.body === "[]", dump(response))
      }

      "give sorted results when two addresses are returned" in {
        val body = get("/uk/addresses?postcode=FX1+6JN").body
        body must startWith("[{")
        body must endWith("}]")
        val json = Json.parse(body)
        val seq = Json.fromJson[Seq[AddressRecord]](json).get
        seq.size mustBe 2
        seq mustBe Seq(fx1_6jn_a, fx1_6jn_b)
      }

      "give sorted results when many addresses are returned" in {
        val body = get("/uk/addresses?postcode=FX1+1PG").body
        body must startWith("[{")
        body must endWith("}]")
        val json = Json.parse(body)
        val seq = Json.fromJson[Seq[AddressRecord]](json).get.map(_.address)
        seq.size mustBe 46
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
          seq(i).lines mustBe expected(i)
          seq(i).town.get mustBe "Acity"
          seq(i).postcode mustBe "FX1 1PG"
        }
      }

      "filter results" in {
        // Deliberate space after Floor 1 in order to ensure filter is trimmed
        val body = get("/uk/addresses?postcode=FX1+1PG&filter=Floor 1 ").body
        body must startWith("[{")
        body must endWith("}]")
        val json = Json.parse(body)
        val seq = Json.fromJson[Seq[AddressRecord]](json).get.map(_.address)
        seq.size mustBe 7
        val expected = Seq(
          Seq("A2test Floor 1, Abuilding", "31 Astreet"),
          Seq("Floor 1", "48 Astreet"),
          Seq("Floor 1", "52 Astreet"),
          Seq("Floor 1, Abuilding", "31 Astreet"),
          Seq("Floor 1, Acourt", "22-28 Astreet"),
          Seq("Ground Floor", "52 Astreet", "Floor 1"),
          Seq("H1test Floor 1, Abuilding", "31 Astreet")
        )
        for (i <- expected.indices) {
          seq(i).lines mustBe expected(i)
          seq(i).town.get mustBe "Acity"
          seq(i).postcode mustBe "FX1 1PG"
        }
      }
    }


    "client error" must {

      "give a bad request when the origin header is absent" in {
        val path = "/uk/addresses?postcode=FX1+4AB"
        val response = await(wsClient.url(appEndpoint + path).withMethod("GET").execute())
        assert(response.status === BAD_REQUEST, dump(response))
      }

      "give a bad request when the postcode parameter is absent" in {
        val response = get("/uk/addresses")
        assert(response.status === BAD_REQUEST, dump(response))
      }

      "give a bad request when the postcode parameter is rubbish text" in {
        val response = get("/uk/addresses?postcode=ThisIsNotAPostcode")
        assert(response.status === BAD_REQUEST, dump(response))
      }

      "give a bad request when an unexpected parameter is sent" in {
        val response = get("/uk/addresses?foo=FX1+4AC")
        assert(response.status === BAD_REQUEST, dump(response))
      }

      "give a not found when an unknown path is requested" in {
        val response = get("/uk/somethingElse?foo=FX1+4AD")
        assert(response.status === NOT_FOUND, dump(response))
      }

      // PlayFramework doesn't provide a hook for correctly handling bad method errors.
      // It was removed from earlier versions.
      "give a bad method when posting to the address URL" ignore {
        val response = request("POST", "/uk/addresses?postcode=FX1+9PY", headerOrigin -> "xxx")
        assert(response.status === METHOD_NOT_ALLOWED, dump(response))
      }

    }
  }
}
