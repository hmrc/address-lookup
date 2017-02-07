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
import bfpo.outmodel.{BFPO, BFPOReadWrite}
import it.helper.AppServerTestApi
import org.scalatest.{MustMatchers, WordSpec}
import play.api.Application
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.test.Helpers._

class BfpoLookupSuite @Inject() (val wsClient: WSClient, val appEndpoint: String)(implicit val app: Application)
  extends WordSpec with MustMatchers with AppServerTestApi {

  implicit val bfpoReadWrite = BFPOReadWrite.AddressReads

  "bfpo lookup" when {

    "successful" must {

      "give a successful response for a known postcode (HMS Albion)" in {
        val response = get("/bfpo/addresses?postcode=BF1+4AF")
        assert(response.status === OK, dump(response))
        val json = Json.parse(response.body)
        val addresses = Json.fromJson[List[BFPO]](json).get
        addresses mustBe List(BFPO(None, List("HMS Albion"), "BF1 4AF", "204"))
      }

      "give a successful filtered response for a known postcode with a filter" in {
        val response = get("/bfpo/addresses?postcode=BF1+0AX&filter=RAF")
        assert(response.status === OK, dump(response))
        val json = Json.parse(response.body)
        val addresses = Json.fromJson[List[BFPO]](json).get
        for (a <- addresses) {
          a.postcode mustBe "BF1 0AX"
          a.anyLineContains("RAF") mustBe true
        }
      }

      "give a successful response for a known BFPO number (HMS Albion)" in {
        val response = get("/bfpo/addresses?bfpo=204")
        assert(response.status === OK, dump(response))
        val json = Json.parse(response.body)
        val addresses = Json.fromJson[List[BFPO]](json).get
        addresses mustBe List(BFPO(None, List("HMS Albion"), "BF1 4AF", "204"))
      }

      "give a successful filtered response for a known BFPO number with a filter" in {
        val response = get("/bfpo/addresses?bfpo=105&filter=RAF")
        assert(response.status === OK, dump(response))
        val json = Json.parse(response.body)
        val addresses = Json.fromJson[List[BFPO]](json).get
        for (a <- addresses) {
          a.postcode mustBe "BF1 0AX"
          a.anyLineContains("RAF") mustBe true
        }
      }

      "set the content type to application/json" in {
        val response = get("/bfpo/addresses?postcode=BF1+4AF")
        val contentType = response.header("Content-Type").get
        assert(contentType.startsWith("application/json"), dump(response))
      }

      "set the cache-control header and include a positive max-age in it" ignore {
        val response = get("/bfpo/addresses?postcode=BF1+4AF")
        val h = response.header("Cache-Control")
        assert(h.nonEmpty && h.get.contains("max-age="), dump(response))
      }

      "set the etag header" ignore {
        val response = get("/bfpo/addresses?postcode=BF1+4AF")
        val h = response.header("ETag")
        assert(h.nonEmpty === true, dump(response))
      }

      "give a successful response for an unknown postcode" in {
        val response = get("/bfpo/addresses?postcode=zz10+9zz")
        assert(response.status === OK, dump(response))
      }

      "give an empty array for an unknown postcode" in {
        val response = get("/bfpo/addresses?postcode=ZZ10+9ZZ")
        assert(response.body === "[]", dump(response))
      }
    }


    "client error" must {
      "give a bad request when the User-Agent header is absent" in {
        val path = "/bfpo/addresses?postcode=BF1+4AF"
        val response = await(wsClient.url(appEndpoint + path).withMethod("GET").execute())
        assert(response.status === BAD_REQUEST, dump(response))
      }

      "give a bad request when the postcode parameter is absent" in {
        val response = get("/bfpo/addresses")
        assert(response.status === BAD_REQUEST, dump(response))
      }

      "give a bad request when the postcode parameter is rubbish text" in {
        val response = get("/bfpo/addresses?postcode=ThisIsNotAPostcode")
        assert(response.status === BAD_REQUEST, dump(response))
      }

      "give a bad request when an unexpected parameter is sent" in {
        val response = get("/bfpo/addresses?foo=BF1+4AF")
        assert(response.status === BAD_REQUEST, dump(response))
      }

      "give a not found when an unknown path is requested" in {
        val response = get("/bfpo/somethingElse?foo=BF1+4AF")
        assert(response.status === NOT_FOUND, dump(response))
      }

      // PlayFramework doesn't provide a hook for correctly handling bad method errors.
      // It was removed from earlier versions.
      "give a bad method when posting to the address URL" ignore {
        val response = request("POST", "/bfpo/addresses?postcode=ZZ1+9PY")
        assert(response.status === METHOD_NOT_ALLOWED, dump(response))
      }
    }
  }
}
