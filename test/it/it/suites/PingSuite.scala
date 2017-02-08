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
import play.api.http.Status
import play.api.libs.ws.WSClient

class PingSuite @Inject() (val wsClient: WSClient, val appEndpoint: String)(implicit val app: Application)
  extends WordSpec with MustMatchers with AppServerTestApi {

  "ping resource" must {
    "give a successful response" in {
      get("/ping").status mustBe Status.OK
    }

//    "give version information in the response body" in {
//      (get("/ping").json \ "version").as[String] must not be empty
//    }

    "give headers that disable caching of the response" in {
      // TODO should include 'must-revalidate'
      get("/ping").header("Cache-Control").get mustBe "no-cache,no-store,max-age=0"
    }
  }

}
