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
import osgb.outmodel.v1.AddressReadable._
import play.api.Application
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.test.Helpers._
import uk.gov.hmrc.address.v1.AddressRecord
import it.tools.Utils._

class OutcodeLookupSuiteV1 @Inject()(val wsClient: WSClient, val appEndpoint: String)(implicit val app: Application)
  extends WordSpec with MustMatchers with AppServerTestApi {

  import FixturesV1._

  "outcode lookup" when {

    "successful" must {

      "give a successful response for a known outcode - uk route" in {
        val response = get("/uk/addresses?outcode=fx1&filter=House")
        assert(response.status === OK, dump(response))
      }

      "give a successful response for a known outcode - gb route" in {
        val response = get("/gb/addresses?outcode=fx1&filter=House")
        assert(response.status === OK, dump(response))
      }

      "give a successful response for a known outcode - old style 'X-Origin'" in {
        val response = request("GET", "/uk/addresses?outcode=fx1&filter=House", headerOrigin -> "xxx")
        assert(response.status === OK, dump(response))
      }

      "give an array for at least one item for a known outcode without a county" in {
        val response = get("/uk/addresses?outcode=fx9&filter=House")
        val body = response.body
        assert(body.startsWith("[{"), dump(response))
        assert(body.endsWith("}]"), dump(response))
        val json = Json.parse(body)
        val seq = Json.fromJson[Seq[AddressRecord]](json).get
        seq.size mustBe 1
        seq.head mustBe fx9_9py
      }

      "set the content type to application/json" in {
        val response = get("/uk/addresses?outcode=FX9&filter=House")
        val contentType = response.header("Content-Type").get
        assert(contentType.startsWith("application/json"), dump(response))
      }

      "set the cache-control header and include a positive max-age in it" ignore {
        val response = get("/uk/addresses?outcode=FX9&filter=House")
        val h = response.header("Cache-Control")
        assert(h.nonEmpty && h.get.contains("max-age="), dump(response))
      }

      "set the etag header" ignore {
        val response = get("/uk/addresses?outcode=FX9&filter=House")
        val h = response.header("ETag")
        assert(h.nonEmpty === true, dump(response))
      }

      "give a successful response for an unknown outcode" in {
        val response = get("/uk/addresses?outcode=zz10&filter=House")
        assert(response.status === OK, dump(response))
      }

      "give an empty array for an unknown outcode" in {
        val response = get("/uk/addresses?outcode=ZZ10&filter=House")
        assert(response.body === "[]", dump(response))
      }

      "give sorted results when two addresses are returned" in {
        val body = get("/uk/addresses?outcode=FX1&filter=Boulevard").body
        body must startWith("[{")
        body must endWith("}]")
        val json = Json.parse(body)
        val seq = Json.fromJson[Seq[AddressRecord]](json).get
        seq.size mustBe 2
        seq mustBe Seq(fx1_6jn_a, fx1_6jn_b)
      }
    }


    "client error" must {

      "give a bad request when the origin header is absent" in {
        val path = "/uk/addresses?outcode=FX1&filter=FOO"
        val response = await(wsClient.url(appEndpoint + path).withMethod("GET").execute())
        assert(response.status === BAD_REQUEST, dump(response))
      }

      "give a bad request when the outcode parameter is absent" in {
        val response = get("/uk/addresses")
        assert(response.status === BAD_REQUEST, dump(response))
      }

      "give a bad request when the outcode parameter is rubbish text" in {
        val response = get("/uk/addresses?outcode=ThisIsNotAnOutcode&filter=FOO")
        assert(response.status === BAD_REQUEST, dump(response))
      }

      "give a bad request when an unexpected parameter is sent" in {
        val response = get("/uk/addresses?outcode=FX1+4AC&foo=bar")
        assert(response.status === BAD_REQUEST, dump(response))
      }
    }
  }
}
