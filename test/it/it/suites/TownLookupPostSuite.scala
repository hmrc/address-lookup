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
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import osgb.outmodel.AddressReadable._
import play.api.libs.json.{JsArray, Json}
import play.api.libs.ws.WSClient
import play.api.test.Helpers._

class TownLookupPostSuite()
  extends AnyWordSpec with GuiceOneServerPerSuite with Matchers with AppServerTestApi {

  import FixturesV2._

  override val appEndpoint: String = s"http://localhost:$port"
  override val wsClient: WSClient = app.injector.instanceOf[WSClient]

  "lookup town POST" when {

    "successful" must {

      "give a successful response for a known town - uk route" in {
        val payload =
          """{"posttown": "some-town"}""".stripMargin

        val response = post("/lookup/by-post-town", payload)
        assert(response.status === OK, dump(response))
      }

      "give a successful response for a known town - old style 'X-Origin'" in {
        val payload =
          """{
            |  "posttown": "some-town"
            |}""".stripMargin

        val response = request("POST", "/lookup/by-post-town", payload, headerOrigin -> "xxx")
        assert(response.status === OK, dump(response))
      }

      "give a successful response for a known v.large town - uk route" in {
        val payload =
          """{
            |  "posttown": "ATown"
            |}""".stripMargin

        val response = post("/lookup/by-post-town", payload)
        assert(response.status === OK, dump(response))
        val json = Json.parse(response.body)
        val arr = json.asInstanceOf[JsArray].value
        arr.size mustBe 3000
        val address1 = Json.fromJson[AddressRecord](arr.head).get.address
        address1.line1 mustBe "1 Bankside"
        address1.line2 mustBe ""
        address1.line3 mustBe ""
        address1.town mustBe "ATown"
        address1.postcode mustBe "FX4 7AJ"
      }

      "give a successful filtered response for a known v.large town - uk route" in {
        val payload =
          """{
            |  "posttown": "ATown",
            |  "filter": "10"
            |}""".stripMargin

        val response = post("/lookup/by-post-town", payload)
        assert(response.status === OK, dump(response))
        val json = Json.parse(response.body)
        val arr = json.asInstanceOf[JsArray].value
        println(s""">>> arr.toString: ${arr.toString}""")
        arr.size mustBe 2
        val address1 = Json.fromJson[AddressRecord](arr.head).get.address
        address1.line1 mustBe "10 Bankside"
        address1.line2 mustBe ""
        address1.line3 mustBe ""
        address1.town mustBe "ATown"
        address1.postcode mustBe "FX4 7AJ"
        val address2 = Json.fromJson[AddressRecord](arr.tail.head).get.address
        address2.line1 mustBe "Flat 10"
        address2.line2 mustBe "A Apartments"
        address2.line3 mustBe "ARoad"
        address2.town mustBe "ATown"
        address2.postcode mustBe "FX4 7AL"
      }

      "set the content type to application/json" in {
        val payload =
        """{
          |  "posttown": "some-town"
          |}""".stripMargin

        val response = post("/lookup/by-post-town", payload)
        val contentType = response.header("Content-Type").get
        assert(contentType.startsWith("application/json"), dump(response))
      }

      "set the cache-control header and include a positive max-age in it" ignore {
        val payload =
          """{
            |  "posttown": "some-town"
            |}""".stripMargin

        val response = post("/lookup/by-post-town", payload)
        val h = response.header("Cache-Control")
        assert(h.nonEmpty && h.get.contains("max-age="), dump(response))
      }

      "set the etag header" ignore {val payload =
        """{
          |  "posttown": "some-town"
          |}""".stripMargin

        val response = post("/lookup/by-post-town", payload)
        val h = response.header("ETag")
        assert(h.nonEmpty === true, dump(response))
      }

      "give a successful response for an unknown town" in {
        val payload =
          """{
            |  "posttown": "unknown-town"
            |}""".stripMargin

        val response = post("/lookup/by-post-town", payload)
        assert(response.status === OK, dump(response))
      }

      "give an empty array for an unknown town" in {
        val payload =
          """{
            |  "posttown": "unknown-town"
            |}""".stripMargin

        val response = post("/lookup/by-post-town", payload)
        assert(response.body === "[]", dump(response))
      }
    }

    "client error" must {

      "give a bad request when the origin header is absent" in {
        val payload =
          """{
            |  "posttown": "some-town"
            |}""".stripMargin

        val path = "/lookup/by-post-town"
        val response = await(wsClient.url(appEndpoint + path)
          .withMethod("POST")
          .withHttpHeaders("content-type" -> "application/json")
          .withBody(payload).execute())

        assert(response.status === BAD_REQUEST, dump(response))
      }

      "give a bad request when the town parameter is absent" in {
        val response = post("/lookup/by-post-town", "{}")
        assert(response.status === BAD_REQUEST, dump(response))
        assert(response.body == """{"obj.posttown":[{"msg":["error.path.missing"],"args":[]}]}""")
      }

      "give a bad request when the town parameter is of the wrong type" in {
        val payload =
          """{
            |  "posttown": 1234
            |}""".stripMargin

        val response = post("/lookup/by-post-town", payload)
        assert(response.status === BAD_REQUEST, dump(response))
        assert(response.body == """{"obj.posttown":[{"msg":["error.expected.jsstring"],"args":[]}]}""")
      }

      "give a bad request when an unexpected parameter is sent on its own" in {
        val payload =
          """{
            |  "foo": "some-town"
            |}""".stripMargin

        val response = post("/lookup/by-post-town", payload)
        assert(response.status === BAD_REQUEST, dump(response))
        assert(response.body == """{"obj.posttown":[{"msg":["error.path.missing"],"args":[]}]}""")
      }

      "not give a bad request when an unexpected parameter is sent" in {
        val payload =
          """{
            |  "posttown": "some-town",
            |  "foo": "bar"
            |}""".stripMargin

        val response = post("/lookup/by-post-town", payload)
        assert(response.status === OK, dump(response))
      }

      "give a not found when an unknown path is requested" in {
        val response = post("/somethingElse", "{}")
        assert(response.status === NOT_FOUND, dump(response))
      }
    }
  }
}