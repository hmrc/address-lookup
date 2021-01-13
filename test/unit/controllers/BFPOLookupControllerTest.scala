/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers

import bfpo.{BFPOFileParser, BFPOLookupController}
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.junit.JUnitRunner
import play.api.http.Status._
import play.api.test.FakeRequest
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.logging.StubLogger

@RunWith(classOf[JUnitRunner])
class BFPOLookupControllerTest extends FunSuite with ScalaFutures {

  val bfpoList = BFPOFileParser.loadResource("bfpo-test-sample.txt")

  test(
    """when postcodeRequest is called without 'X-Origin' header
       it should give a 'bad request' response via the appropriate exception
       and not log any error
    """) {

    val cc = play.api.test.Helpers.stubControllerComponents()
    val logger = new StubLogger
    val bfpoLookupController = new BFPOLookupController(bfpoList, logger, cc)
    val request = FakeRequest("GET", "http://localhost:9000/bfpo/addresses?SOMETHING=FX304HG")

    val e = intercept[UpstreamErrorResponse] {
      bfpoLookupController.postcodeRequest(request)
    }
    assert(e.reportAs === BAD_REQUEST)
    assert(logger.isEmpty)
  }

  test(
    """when postcodeRequest is called with unwanted parameters
       it should give a 'bad request' response
       and log the error
    """) {
    val logger = new StubLogger
    val cc = play.api.test.Helpers.stubControllerComponents()
    val bfpoLookupController = new BFPOLookupController(bfpoList, logger, cc)
    val request = FakeRequest("GET", "http://localhost:9000/bfpo/addresses?SOMETHING=FX304HG").withHeaders("User-Agent" -> "xyz")

    val result = bfpoLookupController.postcodeRequest(request)
    assert(result.header.status === BAD_REQUEST)
    assert(logger.size === 1)
    assert(logger.infos.head.message === "BAD-PARAMETER origin=xyz postcode=None error=unexpected query parameter(s): SOMETHING")
  }

  test(
    """when postcodeRequest is called without the postcode parameter
       it should give a 'bad request' response
       and log the error
    """) {
    val logger = new StubLogger
    val cc = play.api.test.Helpers.stubControllerComponents()
    val bfpoLookupController = new BFPOLookupController(bfpoList, logger, cc)
    val request = FakeRequest("GET", "http://localhost:9000/bfpo/addresses?filter=FX304HG").withHeaders("User-Agent" -> "xyz")

    val result = bfpoLookupController.postcodeRequest(request)
    assert(result.header.status === BAD_REQUEST)
    assert(logger.size === 1)
    assert(logger.infos.head.message === "BAD-POSTCODE origin=xyz postcode=None error=missing or badly-formed postcode parameter")
  }

  test(
    """when postcodeRequest is called with correct parameters
       it should clean up the postcode parameter
       and give an 'ok' response
       and log the lookup including the size of the list
    """) {
    val logger = new StubLogger
    val cc = play.api.test.Helpers.stubControllerComponents()
    val bfpoLookupController = new BFPOLookupController(bfpoList, logger, cc)
    val request = FakeRequest("GET", "http://localhost:9000/bfpo/addresses?postcode=bf19ZZ").withHeaders("User-Agent" -> "xyz")

    val result = bfpoLookupController.postcodeRequest(request)
    assert(result.header.status === OK)
    assert(logger.size === 1)
    assert(logger.infos.head.message === "LOOKUP origin=xyz postcode=BF1 9ZZ matches=2")
  }

  test(
    """when postcodeRequest is called with a postcode that will give several results
       it should give an 'ok' response containing the same list
       and log the lookup including the size of the list
    """) {
    val logger = new StubLogger
    val cc = play.api.test.Helpers.stubControllerComponents()
    val bfpoLookupController = new BFPOLookupController(bfpoList, logger, cc)
    val request = FakeRequest("GET", "http://localhost:9000/bfpo/addresses?postcode=BF19ZZ&filter=999").withHeaders("User-Agent" -> "xyz")

    val result = bfpoLookupController.postcodeRequest(request)
    assert(result.header.status === OK)
    assert(logger.size === 1)
    assert(logger.infos.head.message === "LOOKUP origin=xyz postcode=BF1 9ZZ matches=0")
  }
}
