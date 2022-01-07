/*
 * Copyright 2022 HM Revenue & Customs
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

package model

import model.address.Postcode
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json._

class RequestTest extends AnyWordSpec with Matchers {

  import model.request.LookupByPostTownRequest

  "LookupByPostTownRequest" should {
    "de-serialise filter correctly" when {
      "filter is specified and is non-empty" in {
        val postTownJson = Json.parse("""{"posttown":"WINDSOR", "filter":"some-filter"}""")
        val postTownRequest = Json.fromJson[LookupByPostTownRequest](postTownJson)
        postTownRequest shouldBe a[JsSuccess[_]]
        postTownRequest.get.posttown shouldBe "WINDSOR"
        postTownRequest.get.filter shouldBe Some("some-filter")
      }

      "filter is not specified" in {
        val postTownJson = Json.parse("""{"posttown":"WINDSOR"}""")
        val postTownRequest = Json.fromJson[LookupByPostTownRequest](postTownJson)
        postTownRequest shouldBe a[JsSuccess[_]]
        postTownRequest.get.posttown shouldBe "WINDSOR"
        postTownRequest.get.filter shouldBe None
      }

      "filter is specified but is empty" in {
        val postTownJson = Json.parse("""{"posttown":"WINDSOR", "filter":""}""")
        val postTownRequest = Json.fromJson[LookupByPostTownRequest](postTownJson)
        postTownRequest shouldBe a[JsSuccess[_]]
        postTownRequest.get.posttown shouldBe "WINDSOR"
        postTownRequest.get.filter shouldBe None
      }
    }
  }

  "LookupByPostcodeRequest" should {
    import model.request.LookupByPostcodeRequest

    "de-serialise correctly" when {
      "filter is specified and is non-empty" in {
        val postCodeJson = Json.parse("""{"postcode":"SW6 6SA", "filter":"some-filter"}""")
        val postCodeRequest = Json.fromJson[LookupByPostcodeRequest](postCodeJson)
        postCodeRequest shouldBe a[JsSuccess[_]]
        postCodeRequest.get.postcode shouldBe Postcode("SW6 6SA")
        postCodeRequest.get.filter shouldBe Some("some-filter")
      }

      "filter is not specified" in {
        val postCodeJson = Json.parse("""{"postcode":"SW6 6SA"}""")
        val postCodeRequest = Json.fromJson[LookupByPostcodeRequest](postCodeJson)
        postCodeRequest shouldBe a[JsSuccess[_]]
        postCodeRequest.get.postcode shouldBe Postcode("SW6 6SA")
        postCodeRequest.get.filter shouldBe None
      }

      "filter is specified but is empty" in {
        val postCodeJson = Json.parse("""{"postcode":"SW6 6SA", "filter":""}""")
        val postCodeRequest = Json.fromJson[LookupByPostcodeRequest](postCodeJson)
        postCodeRequest shouldBe a[JsSuccess[_]]
        postCodeRequest.get.postcode shouldBe Postcode("SW6 6SA")
        postCodeRequest.get.filter shouldBe None
      }
    }
  }
}
