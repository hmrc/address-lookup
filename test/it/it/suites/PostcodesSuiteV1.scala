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
import play.api.libs.json.Json
import play.api.libs.ws.WSClient

class PostcodesSuiteV1()
  extends WordSpec with GuiceOneServerPerSuite with MustMatchers with AppServerTestApi {

  override val appEndpoint: String = s"http://localhost:$port"
  override val wsClient: WSClient = app.injector.instanceOf[WSClient]

  "postcode lookup" when {

    "successful" must {

      "give a successful response for a known postcode that is a PO Box" in {
        val response = get("/v1/gb/postcodes/fx171tb")
        assert(response.status === OK, dump(response))
        println(response.body)
        val json = Json.parse(response.body)
        assert( (json \ "isPOBox").as[Boolean] ===  true)
      }

      "give a successful response for a known postcode that is not a PO Box" in {
        val response = get("/v1/gb/postcodes/fx9+9py")
        assert(response.status === OK, dump(response))
        println(response.body)
        val json = Json.parse(response.body)

        assert( (json \ "isPOBox").as[Boolean] ===  false)
      }

      "give a not found response for a valid but unknown postcode" in {
        val response = get("/v1/gb/postcodes/fx1+1aa")
        assert(response.status === NOT_FOUND, dump(response))
      }
    }

    "client error" must {

      "give a bad request when the postcode is absent" in {
        val response = get("/v1/gb/postcodes")
        assert(response.status === BAD_REQUEST, dump(response))
      }

      "give a bad request when the postcode is rubbish text" in {
        val response = get("/v1/gb/postcodes/ThisIsNotAPostcode")
        assert(response.status === BAD_REQUEST, dump(response))
      }

      "give a bad request if a query string is given" in {
        val response = get("/v1/gb/postcodes/FX9+9PY?foo=bar")
        assert(response.status === BAD_REQUEST, dump(response))
      }

    }

  }
}