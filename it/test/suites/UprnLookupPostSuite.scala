/*
 * Copyright 2024 HM Revenue & Customs
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

package suites

import com.codahale.metrics.SharedMetricRegistries
import it.helper.AppServerTestApi
import model.address.{Address, AddressRecord, Country, LocalCustodian}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.MimeTypes
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsArray, Json}
import play.api.libs.ws.WSClient
import play.api.test.Helpers._

// Please keep UprnLookupSuiteV2 and UprnLookupSuiteV2 as similar as appropriate.

class UprnLookupPostSuite()
  extends AnyWordSpec with GuiceOneServerPerSuite with AppServerTestApi {

  override def fakeApplication(): Application = {
    SharedMetricRegistries.clear()
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.access-control.enabled" -> true,
        "microservice.services.access-control.allow-list.1" -> "xyz")
      .build()
  }

  override val appEndpoint: String = s"http://localhost:$port"
  override val wsClient: WSClient = app.injector.instanceOf[WSClient]

  "uprn lookup" when {
    import AddressRecord.formats._

    "successful" should {

      "give a successful response for a known uprn - uk route" in {
        val expectedAddressRecord = AddressRecord(
          "GB690091234501",Some(690091234501L),None,None,None,
          Address(List("1 Test Street"),"Testtown","AA00 0AA",Some(Country("GB-ENG","England")),Country("GB","United Kingdom"))
          ,"en",Some(LocalCustodian(121,"NORTH SOMERSET")),None,None,None)

        val response = post("/lookup/by-uprn", """{"uprn": "690091234501"}""")
        response.status shouldBe OK
        val body = response.body
        val json = Json.parse(body)
        val arr = json.asInstanceOf[JsArray].value
        arr.size shouldBe 1
        val address1 = Json.fromJson[AddressRecord](arr.head).get
        address1 shouldBe expectedAddressRecord
      }

      "give a successful response for a known uprn with text content-type - uk route" in {
        val expectedAddressRecord = AddressRecord(
          "GB690091234501",Some(690091234501L),None,None,None,
          Address(List("1 Test Street"),"Testtown","AA00 0AA",Some(Country("GB-ENG","England")),Country("GB","United Kingdom"))
          ,"en",Some(LocalCustodian(121,"NORTH SOMERSET")),None,None,None)

        val response = post("/lookup/by-uprn", """{"uprn": "690091234501"}""", MimeTypes.TEXT)
        response.status shouldBe OK
      }

      "set the content type to application/json" in {
        val response = post("/lookup/by-uprn", """{"uprn":"9999999999"}""")
        val contentType = response.header("Content-Type").get
        contentType should startWith("application/json")
      }

      "set the cache-control header and include a positive max-age ignore it" ignore {
        val response = post("/lookup/by-uprn", """{"uprn":"9999999999"}""")
        val h = response.header("Cache-Control")
        h should not be empty
        h.get should include("max-age=")
      }

      "set the etag header" ignore {
        val response = post("/lookup/by-uprn", """{"uprn":"9999999999"}""")
        val h = response.header("ETag")
        h.nonEmpty shouldBe true
      }

      "give a successful response with an empty array for an unknown uprn" in {
        val response = post("/lookup/by-uprn", """{"uprn":"0"}""")
        response.status shouldBe OK
        response.body shouldBe "[]"
      }
    }


    "client error" should {

      "return forbidden when the user-agent is absent" in {
        val path = "/lookup/by-uprn"
        val response = await(wsClient.url(appEndpoint + path).withMethod("POST").withBody("""{"uprn":"9999999999"}""").execute())
        response.status shouldBe FORBIDDEN
      }

      "give a bad request when the uprn parameter is absent" in {
        val response = post("/lookup/by-uprn", "{}")
        response.status shouldBe BAD_REQUEST
      }

      "give a bad request when the payload is missing" in {
        val response = post("/lookup/by-uprn", "")
        response.status shouldBe BAD_REQUEST
      }
    }
  }
}
