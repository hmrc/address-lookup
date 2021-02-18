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

import it.helper.AppServerTestApi
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import osgb.outmodel.v2.AddressReadable._
import play.api.libs.json.{JsArray, Json}
import play.api.libs.ws.WSClient
import play.api.test.Helpers._
import address.v2.AddressRecord

// Please keep UprnLookupSuiteV2 and UprnLookupSuiteV2 as similar as appropriate.

class UprnLookupSuiteV2()
  extends WordSpec with GuiceOneServerPerSuite with MustMatchers with AppServerTestApi {

  import FixturesV2._
  override val appEndpoint: String = s"http://localhost:$port"
  override val wsClient: WSClient = app.injector.instanceOf[WSClient]

  "uprn lookup" when {

    "successful" must {

      "give a successful response for a known uprn - uk route" in {
        val response = get("/v2/uk/addresses?uprn=11111")
        assert(response.status === OK, dump(response))
        val body = response.body
        val json = Json.parse(body)
        val arr = json.asInstanceOf[JsArray].value
        assert(arr.size === 1, body)
        val address1 = Json.fromJson[AddressRecord](arr.head).get
        assert(address1 === fx9_9py_terse, body)
      }

      "give a successful response for a known uprn - gb route" in {
        val response = get("/v2/gb/addresses?uprn=9999999999")
        assert(response.status === OK, dump(response))
      }

      "give a successfully truncated response for a long address" in {
        val response = get("/v2/gb/addresses?uprn=44444")
        assert(response.status === OK, dump(response))
        val body = response.body
        assert(body.startsWith("[{"), dump(response))
        assert(body.endsWith("}]"), dump(response))
        val json = Json.parse(body)
        val arr = json.asInstanceOf[JsArray].value
        arr.size mustBe 1
        val address1 = Json.fromJson[AddressRecord](arr.head).get.address
        address1.line1 mustBe "An address with a very long first l"
        address1.line2 mustBe "Second line of address is just as l"
        address1.line3 mustBe "Third line is not the longest but i"
        address1.town.get mustBe fx2_2tb.address.town.get.substring(0, 35)
        address1.county.get mustBe "Cumbria"
      }

      "give an array for at least one item for a known uprn without a county" in {
        val response = get("/v2/uk/addresses?uprn=11111")
        val body = response.body
        assert(body.startsWith("[{"), dump(response))
        assert(body.endsWith("}]"), dump(response))
        val json = Json.parse(body)
        val arr = json.asInstanceOf[JsArray].value
        arr.size mustBe 1
        Json.fromJson[AddressRecord](arr.head).get mustBe fx9_9py_terse
      }

      "set the content type to application/json" in {
        val response = get("/v2/uk/addresses?uprn=9999999999")
        val contentType = response.header("Content-Type").get
        assert(contentType.startsWith("application/json"), dump(response))
      }

      "set the cache-control header and include a positive max-age ignore it" ignore {
        val response = get("/v2/uk/addresses?uprn=9999999999")
        val h = response.header("Cache-Control")
        assert(h.nonEmpty && h.get.contains("max-age="), dump(response))
      }

      "set the etag header" ignore {
        val response = get("/v2/uk/addresses?uprn=9999999999")
        val h = response.header("ETag")
        assert(h.nonEmpty === true, dump(response))
      }

      "give a successful response with an empty array for an unknown uprn" in {
        val response = get("/v2/uk/addresses?uprn=0")
        assert(response.status === OK, dump(response))
        assert(response.body === "[]", dump(response))
      }
    }


    "client error" must {

      "give a bad request when the origin header is absent" in {
        val path = "/v2/uk/addresses?uprn=9999999999"
        val response = await(wsClient.url(appEndpoint + path).withMethod("GET").execute())
        assert(response.status === BAD_REQUEST, dump(response))
      }

      "give a bad request when the uprn parameter is absent" in {
        val response = get("/v2/uk/addresses")
        assert(response.status === BAD_REQUEST, dump(response))
      }
    }
  }
}
