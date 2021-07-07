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
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.ws.WSClient

class MetricsSuiteV2()
  extends AnyWordSpec with GuiceOneServerPerSuite with Matchers with AppServerTestApi {

  private val className = "InMemoryAddressLookupRepository"
  override val appEndpoint: String = s"http://localhost:$port"
  override val wsClient: WSClient = app.injector.instanceOf[WSClient]

  "metrics" when {
    "successful" must {
      "give a timer for the findId search case" in {
        assert(get("/v2/uk/addresses/GB11111").status === OK)

        val response = get("/admin/metrics")
        assert(response.status === OK, dump(response))
        assert((response.json \ "timers" \ s"$className.findId" \ "count").as[Int] > 0)
      }

      "give a timer for the findUprn search case" in {
        assert(get("/v2/uk/addresses?uprn=9999999999").status === OK)

        val response = get("/admin/metrics")
        assert(response.status === OK, dump(response))
        assert((response.json \ "timers" \ s"$className.findUprn" \ "count").as[Int] > 0)
      }

      "give a timer for the findPostcode search case" in {
        assert(get("/v2/uk/addresses?postcode=FX1+9PY").status === OK)

        val response = get("/admin/metrics")
        assert(response.status === OK, dump(response))
        assert((response.json \ "timers" \ s"$className.findPostcode" \ "count").as[Int] > 0)
      }

      "give a timer for the findPostcodeFilter search case" in {
        assert(get("/v2/uk/addresses?postcode=SE1+9PY&filter=10").status === OK)

        val response = get("/admin/metrics")
        assert(response.status === OK, dump(response))
        assert((response.json \ "timers" \ s"$className.findPostcodeFilter" \ "count").as[Int] > 0)
      }

      "give a timer for the searchFuzzy search case" in {
        assert(get("/v2/uk/addresses?fuzzy=FX1+9PY").status === OK)

        val response = get("/admin/metrics")
        assert(response.status === OK, dump(response))
        assert((response.json \ "timers" \ s"$className.searchFuzzy" \ "count").as[Int] > 0)
      }

      "give a timer for the searchFuzzyFilter search case" in {
        assert(get("/v2/uk/addresses?fuzzy=SE1+9PY&filter=2").status === OK)

        val response = get("/admin/metrics")
        assert(response.status === OK, dump(response))
        assert((response.json \ "timers" \ s"$className.searchFuzzyFilter" \ "count").as[Int] > 0)
      }
    }
  }
}
