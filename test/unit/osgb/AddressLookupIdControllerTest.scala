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

package osgb

import address.osgb.DbAddress
import address.v2._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import osgb.outmodel.Marshall
import osgb.services.{ReferenceData, ResponseProcessor}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.AddressLookupRepository
import uk.gov.hmrc.http.UpstreamErrorResponse
import util.Utils._

import scala.concurrent.Future

class AddressLookupIdControllerTest extends AnyWordSpec with ScalaFutures with MockitoSugar {

  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  private val lc4510 = Some(LocalCustodian(4510, "Custodian1"))
  private val lc9999 = Some(LocalCustodian(9999, "Somewhere"))
  private val lc2935 = Some(LocalCustodian(2935, "Custodian3"))

  private val en = "en"
  private val InUse = Some("In_Use")
  private val Approved = Some("Approved")
  private val AllVehicles = Some("All_Vehicles")

  import Countries._

  val shire = Some("Thereshire")
  val addr1Loc = Location("12.345678", "-12.345678")
  val addr1Db = DbAddress("GB123456", List("10 Test Court", "Test Street", "Tester"), "Testtown upon Tyne", "FX1 1AA",
    Some("GB-ENG"), Some("UK"), Some(4510), Some("en"), None, Some(addr1Loc.toString))
  val addr1Ar = AddressRecord("GB123456", Some(123456L), Address(List("10 Test Court", "A Street", "Tester"), "Testtown upon Tyne", "FX1 1AA", Some(England), UK), en, lc4510, Some(addr1Loc.toSeq))

  val cc = play.api.test.Helpers.stubControllerComponents()

  class ResponseStub(a: List[AddressRecord]) extends ResponseProcessor(ReferenceData.empty) {
    override def convertAddressList(dbAddresses: Seq[DbAddress]): List[AddressRecord] = a
  }

  class Context {
    val searcher = mock[AddressLookupRepository]
  }

  "postcode lookup" must {

    "give bad request" when {

      """when search is called without 'X-Origin' header
       it should give a 'bad request' response via the appropriate exception
       and not log any error
      """ in new Context {
        val addressLookupController = new AddressLookupIdController(searcher, new ResponseStub(Nil), ec, cc)
        val request = FakeRequest("GET", "http://localhost:9000/v2/uk/addresses/123456")

        val e = intercept[UpstreamErrorResponse] {
          addressLookupController.findByIdRequest(request, "123456", Marshall.marshallV2Address)
        }
        assert(e.reportAs === 400)
      }
    }


    "successful findByIdRequest" when {

      """when search is called with correct parameters and a known id
       it should give an 'ok' response
       and log the lookup including the size=1
      """ in new Context {
        when(searcher.findID("GB123456")) thenReturn Future(Some(addr1Db))
        val addressLookupController = new AddressLookupIdController(searcher, new ResponseStub(List(addr1Ar)), ec, cc)
        val request = FakeRequest("GET", "http://localhost:9000/v2/uk/addresses/GB123456").withHeadersOrigin

        val result = await(addressLookupController.findByIdRequest(request, "GB123456", Marshall.marshallV2Address))
        assert(result.header.status === play.api.http.Status.OK)
      }

      """when search is called with correct parameters but an unknown id
       it should give a 'not found' response
       and log the lookup including the size=0
      """ in new Context {
        when(searcher.findID("GB1010101010")) thenReturn Future(None)
        val addressLookupController = new AddressLookupIdController(searcher, new ResponseStub(Nil), ec, cc)
        val request = FakeRequest("GET", "http://localhost:9000/v2/uk/addresses/GB1010101010").withHeadersOrigin

        val result = await(addressLookupController.findByIdRequest(request, "GB1010101010", Marshall.marshallV2Address))
        assert(result.header.status === play.api.http.Status.NOT_FOUND)
      }
    }
  }
}
