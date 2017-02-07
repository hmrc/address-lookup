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
import play.api.Application
import play.api.libs.ws.WSClient

class MetricsSuiteV1 @Inject() (val wsClient: WSClient, val appEndpoint: String, className: String)(implicit val app: Application)
  extends WordSpec with MustMatchers with AppServerTestApi {

  "metrics" when {
    "successful" must {
      "give a timer for the findId search case" in {
        assert(get("/gb/addresses/GB11111").status === OK)

        val response = get("/admin/metrics")
        assert(response.status === OK, dump(response))
        assert((response.json \ "timers" \ s"$className.findId" \ "count").as[Int] > 0)
      }

      "give a timer for the findUprn search case" in {
        assert(get("/gb/addresses?uprn=9999999999").status === OK)

        val response = get("/admin/metrics")
        assert(response.status === OK, dump(response))
        assert((response.json \ "timers" \ s"$className.findUprn" \ "count").as[Int] > 0)
      }

      "give a timer for the findPostcode search case" in {
        assert(get("/gb/addresses?postcode=FX1+9PY").status === OK)

        val response = get("/admin/metrics")
        assert(response.status === OK, dump(response))
        assert((response.json \ "timers" \ s"$className.findPostcode" \ "count").as[Int] > 0)
      }

      "give a timer for the searchFuzzy search case" in {
        assert(get("/gb/addresses?fuzzy=FX1+9PY").status === OK)

        val response = get("/admin/metrics")
        assert(response.status === OK, dump(response))
        assert((response.json \ "timers" \ s"$className.searchFuzzy" \ "count").as[Int] > 0)
      }
    }
  }
}
