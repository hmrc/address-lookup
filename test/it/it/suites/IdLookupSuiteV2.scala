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
import org.scalatest.{MustMatchers, WordSpec}
import osgb.outmodel.v2.AddressReadable._
import play.api.Application
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.test.Helpers._
import uk.gov.hmrc.address.v2.AddressRecord

// Please keep IdLookupSuiteV1 and IdLookupSuiteV2 as similar as appropriate.

class IdLookupSuiteV2 @Inject() (val wsClient: WSClient, val appEndpoint: String)(implicit val app: Application)
  extends WordSpec with MustMatchers with AppServerTestApi {

  import FixturesV2._

  "id lookup" when {

    "successful" must {

      "give a successful response for a known id - uk route" in {
        val response = get("/v2/uk/addresses/GB22222")
        assert(response.status === OK, dump(response))
        val body = response.body
        val json = Json.parse(body)
        val address1 = Json.fromJson[AddressRecord](json).get
        assert(address1 === fx1_6jn_a_terse, body)
      }

      "give a successful response for a known id - gb route" in {
        val response = get("/v2/gb/addresses/GB22222")
        assert(response.status === OK, dump(response))
      }

      "give a successfully truncated response for a long address" in {
        val response = get("/v2/gb/addresses/GB44444")
        assert(response.status === OK, dump(response))
        val body = response.body
        val json = Json.parse(body)
        val address1 = Json.fromJson[AddressRecord](json).get.address
        address1.line1 mustBe "An address with a very long first l"
        address1.line2 mustBe "Second line of address is just as l"
        address1.line3 mustBe "Third line is not the longest but i"
        address1.town.get mustBe fx2_2tb.address.town.get.substring(0, 35)
        address1.county.get mustBe "Cumbria"
      }

      "give an address record for a known id without a county" in {
        val response = get("/v2/uk/addresses/GB11111")
        val body = response.body
        val json = Json.parse(body)
        Json.fromJson[AddressRecord](json).get mustBe fx9_9py_terse
      }

      "set the content type to application/json" in {
        val response = get("/v2/uk/addresses/GB11111")
        val contentType = response.header("Content-Type").get
        assert(contentType.startsWith("application/json"), dump(response))
      }

      "set the cache-control header and include a positive max-age ignore it" ignore {
        val response = get("/v2/uk/addresses/GB0001")
        val h = response.header("Cache-Control")
        assert(h.nonEmpty && h.get.contains("max-age="), dump(response))
      }

      "set the etag header" ignore {
        val response = get("/v2/uk/addresses/GB0001")
        val h = response.header("ETag")
        assert(h.nonEmpty === true, dump(response))
      }

      "give a 404 response for an unknown id" in {
        val response = get("/v2/uk/addresses/X0")
        assert(response.status === NOT_FOUND, dump(response))
      }
    }


    "client error" must {

      "give a bad request when the origin header is absent" in {
        val path = "/v2/uk/addresses/GB0001"
        val response = await(wsClient.url(appEndpoint + path).withMethod("GET").execute())
        assert(response.status === BAD_REQUEST, dump(response))
      }
    }
  }
}
