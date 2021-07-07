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

class FuzzySearchSuiteV2()
  extends AnyWordSpec with GuiceOneServerPerSuite with Matchers with AppServerTestApi {

  override val appEndpoint: String = s"http://localhost:$port"
  override val wsClient: WSClient = app.injector.instanceOf[WSClient]

  "fuzzy search lookup" when {

    "successful" must {

      "give a successful response for a known postcode  - uk route" in {
        val response = get("/v2/uk/addresses?fuzzy=A+Street")
        assert(response.status === OK, dump(response))
      }

      "give a successful response for a known v.large postcode- uk route" in {
        val response = get("/v2/uk/addresses?fuzzy=bankside")
        assert(response.status === OK, dump(response))
      }

      "give sorted results when multiple addresses are returned" in {
        val body = get("/v2/uk/addresses?fuzzy=bankside").body
        val json = Json.parse(body)
        val seq = Json.fromJson[Seq[AddressRecord]](json).get
        val arr = json.asInstanceOf[JsArray].value
        arr.size mustBe 3000
        // TODO this sort order indicates a numbering problem with result sorting (see TXMNT-64)
        Json.fromJson[AddressRecord](arr.head).get.address.line1 mustBe "1 Bankside"
        Json.fromJson[AddressRecord](arr(1)).get.address.line1 mustBe "10 Bankside"
        Json.fromJson[AddressRecord](arr(2)).get.address.line1 mustBe "100 Bankside"
        Json.fromJson[AddressRecord](arr(3)).get.address.line1 mustBe "1000 Bankside"
      }

      "filter results" in {
        val body = get("/v2/uk/addresses?fuzzy=bankside&filter=100").body
        val json = Json.parse(body)
        val seq = Json.fromJson[Seq[AddressRecord]](json).get
        val arr = json.asInstanceOf[JsArray].value
        arr.size mustBe 1
        // TODO this sort order indicates a numbering problem with result sorting (see TXMNT-64)
        Json.fromJson[AddressRecord](arr.head).get.address.line1 mustBe "100 Bankside"
      }


      "set the content type to application/json" in {
        val response = get("/v2/uk/addresses?fuzzy=Stamford+Street")
        val contentType = response.header("Content-Type").get
        assert(contentType.startsWith("application/json"), dump(response))
      }

      "set the cache-control header and include a positive max-age in it" ignore {
        val response = get("/v2/uk/addresses?fuzzy=A+Street")
        val h = response.header("Cache-Control")
        assert(h.nonEmpty && h.get.contains("max-age="), dump(response))
      }

      "set the etag header" ignore {
        val response = get("/v2/uk/addresses?fuzzy=A+Street")
        val h = response.header("ETag")
        assert(h.nonEmpty === true, dump(response))
      }

      "give a successful response for an unmatched result" in {
        val response = get("/v2/uk/addresses?fuzzy=zzz+zzz+zzz")
        assert(response.status === OK, dump(response))
      }

      "give an empty array for an unmatched result" in {
        val response = get("/v2/uk/addresses?fuzzy=zzz+zzz+zzz")
        assert(response.body === "[]", dump(response))
      }

    }


    "client error" must {

      "give a bad request when the origin header is absent" in {
        val path = "/v2/uk/addresses?fuzzy=FX1+4AB"
        val response = await(wsClient.url(appEndpoint + path).withMethod("GET").execute())
        assert(response.status === BAD_REQUEST, dump(response))
      }

      "give a bad request when the fuzzy parameter is absent" in {
        val response = get("/v2/uk/addresses")
        assert(response.status === BAD_REQUEST, dump(response))
      }
    }
  }
}
