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
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import osgb.outmodel.AddressReadable._
import play.api.libs.json.{JsArray, Json}
import play.api.libs.ws.WSClient
import play.api.test.Helpers._

// Please keep UprnLookupSuiteV2 and UprnLookupSuiteV2 as similar as appropriate.

class UprnLookupPostSuite()
  extends AnyWordSpec with GuiceOneServerPerSuite with Matchers with AppServerTestApi {

  import FixturesV2._
  override val appEndpoint: String = s"http://localhost:$port"
  override val wsClient: WSClient = app.injector.instanceOf[WSClient]

  "uprn lookup" when {

    "successful" must {

      "give a successful response for a known uprn - uk route" in {
        val response = post("/lookup/uprn", """{"uprn": "11111"}""")
        assert(response.status === OK, dump(response))
        val body = response.body
        val json = Json.parse(body)
        val arr = json.asInstanceOf[JsArray].value
        assert(arr.size === 1, body)
        val address1 = Json.fromJson[AddressRecord](arr.head).get
        assert(address1 === fx9_9py_terse, body)
      }

      "set the content type to application/json" in {
        val response = post("/lookup/uprn", """{"uprn":"9999999999"}""")
        val contentType = response.header("Content-Type").get
        assert(contentType.startsWith("application/json"), dump(response))
      }

      "set the cache-control header and include a positive max-age ignore it" ignore {
        val response = post("/lookup/uprn", """{"uprn":"9999999999"}""")
        val h = response.header("Cache-Control")
        assert(h.nonEmpty && h.get.contains("max-age="), dump(response))
      }

      "set the etag header" ignore {
        val response = post("/lookup/uprn", """{"uprn":"9999999999"}""")
        val h = response.header("ETag")
        assert(h.nonEmpty === true, dump(response))
      }

      "give a successful response with an empty array for an unknown uprn" in {
        val response = post("/lookup/uprn", """{"uprn":"0"}""")
        assert(response.status === OK, dump(response))
        assert(response.body === "[]", dump(response))
      }
    }


    "client error" must {

      "give a bad request when the origin header is absent" in {
        val path = "/lookup/uprn"
        val response = await(wsClient.url(appEndpoint + path).withMethod("POST").withBody("""{"uprn":"9999999999"}""").execute())
        assert(response.status === BAD_REQUEST, dump(response))
      }

      "give a bad request when the uprn parameter is absent" in {
        val response = post("/lookup/uprn", "{}")
        assert(response.status === BAD_REQUEST, dump(response))
      }

      "give a bad request when the payload is missing" in {
        val response = post("/lookup/uprn", "")
        assert(response.status === BAD_REQUEST, dump(response))
      }
    }
  }
}
