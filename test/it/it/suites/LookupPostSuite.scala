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

import address.model.AddressRecord
import it.helper.AppServerTestApi
import it.tools.Utils.headerOrigin
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import osgb.outmodel.AddressReadable._
import play.api.libs.json.{JsArray, Json}
import play.api.libs.ws.WSClient
import play.api.test.Helpers._

class LookupPostSuite()
  extends WordSpec with GuiceOneServerPerSuite with MustMatchers with AppServerTestApi {

  import FixturesV2._

  private val largePostcodeExampleSize = 2517
  override val appEndpoint: String = s"http://localhost:$port"
  override val wsClient: WSClient = app.injector.instanceOf[WSClient]

  "lookup POST" when {

    "successful" must {

      "give a successful response for a known postcode - uk route" in {
        val payload =
          """{"postcode": "fx1 9py"}""".stripMargin

        val response = post("/lookup", payload)
        assert(response.status === OK, dump(response))
      }

      "give a successful response for a known postcode - old style 'X-Origin'" in {
        val payload =
          """{
            |  "postcode": "fx1  9py"
            |}""".stripMargin

        val response = request("POST", "/lookup", payload, headerOrigin -> "xxx")
        assert(response.status === OK, dump(response))
      }

      "give a successful response for a known v.large postcode - uk route" in {
        val payload =
          """{
            |  "postcode": "fx47al"
            |}""".stripMargin

        val response = post("/lookup", payload)
        assert(response.status === OK, dump(response))
        val json = Json.parse(response.body)
        val arr = json.asInstanceOf[JsArray].value
        arr.size mustBe largePostcodeExampleSize
        val address1 = Json.fromJson[AddressRecord](arr.head).get.address
        address1.line1 mustBe "Flat 1"
        address1.line2 mustBe "A Apartments"
        address1.line3 mustBe "ARoad"
        address1.town mustBe "ATown"
        address1.postcode mustBe "FX4 7AL"
      }

      "set the content type to application/json" in {
        val payload =
        """{
          |  "postcode": "FX1 9PY"
          |}""".stripMargin

        val response = post("/lookup", payload)
        val contentType = response.header("Content-Type").get
        assert(contentType.startsWith("application/json"), dump(response))
      }

      "set the cache-control header and include a positive max-age in it" ignore {
        val payload =
          """{
            |  "postcode": "FX1 9PY"
            |}""".stripMargin

        val response = post("/lookup", payload)
        val h = response.header("Cache-Control")
        assert(h.nonEmpty && h.get.contains("max-age="), dump(response))
      }

      "set the etag header" ignore {val payload =
        """{
          |  "postcode": "FX1 9PY"
          |}""".stripMargin

        val response = post("/lookup", payload)
        val h = response.header("ETag")
        assert(h.nonEmpty === true, dump(response))
      }

      "give a successful response for an unknown postcode" in {
        val payload =
          """{
            |  "postcode": "zz10 9zz"
            |}""".stripMargin

        val response = post("/lookup", payload)
        assert(response.status === OK, dump(response))
      }

      "give an empty array for an unknown postcode" in {
        val payload =
          """{
            |  "postcode": "ZZ10 9ZZ"
            |}""".stripMargin

        val response = post("/lookup", payload)
        assert(response.body === "[]", dump(response))
      }
    }

    "client error" must {

      "give a bad request when the origin header is absent" in {
        val payload =
          """{
            |  "postcode": "FX1 4AB"
            |}""".stripMargin

        val path = "/lookup"
        val response = await(wsClient.url(appEndpoint + path)
          .withMethod("POST")
          .withHttpHeaders("content-type" -> "application/json")
          .withBody(payload).execute())

        assert(response.status === BAD_REQUEST, dump(response))
      }

      "give a bad request when the postcode parameter is absent" in {
        val response = post("/lookup", "{}")
        assert(response.status === BAD_REQUEST, dump(response))
        assert(response.body == """{"obj.postcode":[{"msg":["error.path.missing"],"args":[]}]}""")
      }

      "give a bad request when the postcode parameter is rubbish text" in {
        val payload =
          """{
            |  "postcode": "ThisIsNotAPostcode"
            |}""".stripMargin

        val response = post("/lookup", payload)
        assert(response.status === BAD_REQUEST, dump(response))
        assert(response.body == """{"obj.postcode":[{"msg":["error.invalid"],"args":[]}]}""")
      }

      "give a bad request when the postcode parameter is of the wrong type" in {
        val payload =
          """{
            |  "postcode": 1234
            |}""".stripMargin

        val response = post("/lookup", payload)
        assert(response.status === BAD_REQUEST, dump(response))
        assert(response.body == """{"obj.postcode":[{"msg":["error.expected.jsstring"],"args":[]}]}""")
      }

      "give a bad request when an unexpected parameter is sent on its own" in {
        val payload =
          """{
            |  "foo": "FX1 4AC"
            |}""".stripMargin

        val response = post("/lookup", payload)
        assert(response.status === BAD_REQUEST, dump(response))
        assert(response.body == """{"obj.postcode":[{"msg":["error.path.missing"],"args":[]}]}""")
      }

      "not give a bad request when an unexpected parameter is sent" in {
        val payload =
          """{
            |  "postcode": "FX1 4AC",
            |  "foo": "bar"
            |}""".stripMargin

        val response = post("/lookup", payload)
        assert(response.status === OK, dump(response))
      }

      "give a not found when an unknown path is requested" in {
        val response = post("/somethingElse", "{}")
        assert(response.status === NOT_FOUND, dump(response))
      }
    }
  }
}
