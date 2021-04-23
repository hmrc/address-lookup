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

import org.mockito.Mockito._
import org.scalatest.WordSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import osgb.outmodel.Marshall
import osgb.services.{ReferenceData, ResponseProcessor}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.AddressLookupRepository
import address.osgb.DbAddress
import address.uk.{Outcode, Postcode}
import address.v2._
import play.api.Logger
import uk.gov.hmrc.http.UpstreamErrorResponse
import util.Utils._

import scala.concurrent.Future

class AddressSearchControllerTest extends WordSpec with ScalaFutures with MockitoSugar {

  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global
  val cc = play.api.test.Helpers.stubControllerComponents()

  private val en = "en"
  private val InUse = Some("In_Use")
  private val Approved = Some("Approved")
  private val AllVehicles = Some("All_Vehicles")

  import Countries._

  val addr1Db = DbAddress("GB100001", List("10 Test Court", "Test Street", "Tester"), "Test upon Tyne", "FX1 5XD", Some("GB-ENG"), Some("UK"), Some(4510), Some("en"), None, Some(Location("12.345678", "-12.345678").toString), None, Some("TestLocalAuthority"))
  val addr1Ar = AddressRecord("GB100001", Some(100001L), Address(List("10 Test Court", "Test Street", "Tester"), "Test upon Tyne", "FX1 5XD", Some(England), UK), en, Some(LocalCustodian(4510, "Test upon Tyne")), Some(Location("12.345678", "-12.345678").toSeq), Some("TestLocalAuthority"))

  val dx1A = DbAddress("GB100002", List("1 Test Street"), "Testtown", "FZ22 7ZW", Some("GB-XXX"), Some("UK"), Some(9999), Some("en"), None, Some("54.914561,-1.3905597"), None, Some("TestLocalAuthority"))
  val dx1B = DbAddress("GB100003", List("2 Test Street"), "Testtown", "FZ22 7ZW", Some("GB-XXX"), Some("UK"), Some(9999), Some("en"), None, Some("54.914561,-1.3905597"), None, Some("TestLocalAuthority"))
  val dx1C = DbAddress("GB100004", List("3 Test Street"), "Testtown", "FZ22 7ZW", Some("GB-XXX"), Some("UK"), Some(9999), Some("en"), None, Some("54.914561,-1.3905597"), None, Some("TestLocalAuthority"))

  val fx1A = AddressRecord("GB100002", Some(100002L), Address(List("1 Test Street"), "Testtown", "FZ22 7ZW", Some(England), UK), en, Some(LocalCustodian(9999, "Somewhere")), Some(Location("0,0").toSeq), Some("TestLocalAuthority"))
  val fx1B = AddressRecord("GB100003", Some(100003L), Address(List("2 Test Street"), "Testtown", "FZ22 7ZW", Some(England), UK), en, Some(LocalCustodian(9999, "Somewhere")), Some(Location("0,0").toSeq), Some("TestLocalAuthority"))
  val fx1C = AddressRecord("GB100004", Some(100004L), Address(List("3 Test Street"), "Testtown", "FZ22 7ZW", Some(England), UK), en, Some(LocalCustodian(9999, "Somewhere")), Some(Location("0,0").toSeq), Some("TestLocalAuthority"))

  val addressDb1 = DbAddress("GB100005", List("Test Road"), "ATown", "FX11 7LX", Some("GB-ENG"), Some("UK"), Some(2935), Some("en"), None, Some(Location("12.345678", "-12.345678").toString), None, Some("TestLocalAuthority"))
  val addressDb2 = DbAddress("GB100006", List("ARoad", "ARoad"), "Atown", "FX11 7LA", Some("GB-ENG"), Some("UK"), Some(2935), Some("en"), None, Some(Location("12.345678", "-12.345678").toString), None, Some("TestLocalAuthority"))

  val addressAr1 = AddressRecord("GB100005", Some(100005L), Address(List("Test Road"), "ATown", "FX11 7LX", Some(England), UK), en, Some(LocalCustodian(2935, "Testland")), Some(Location("12.345678", "-12.345678").toSeq), Some("TestLocalAuthority"))
  val addressAr2 = AddressRecord("GB100006", Some(100006L), Address(List("Test Station", "Test Road"), "ATown", "FX11 7LA", Some(England), UK), en, Some(LocalCustodian(2935, "Testland")), Some(Location("12.345678", "-12.345678").toSeq), Some("TestLocalAuthority"))

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
        val controller = new AddressSearchController(searcher, new ResponseStub(Nil), ec, cc)
        val request = FakeRequest("GET", "http://localhost:9000/v2/uk/addresses?SOMETHING=FX114HG")

        val e = intercept[UpstreamErrorResponse] {
          controller.searchRequest(request, Marshall.marshallV2List)
        }
        assert(e.reportAs === 400)
      }

      """when search is called with unwanted parameters
       it should give a 'bad request' response
       and log the error
      """ in new Context {
        val controller = new AddressSearchController(searcher, new ResponseStub(Nil), ec, cc)
        val request = FakeRequest("GET", "http://localhost:9000/v2/uk/addresses?SOMETHING=FX114HG").withHeadersOrigin

        val result = await(controller.searchRequest(request, Marshall.marshallV2List))
        assert(result.header.status === play.api.http.Status.BAD_REQUEST)
      }

      """when search is called without the postcode parameter
       it should give a 'bad request' response
       and log the error
      """ in new Context {
        val controller = new AddressSearchController(searcher, new ResponseStub(Nil), ec, cc)
        val request = FakeRequest("GET", "http://localhost:9000/v2/uk/addresses?filter=FX114HG").withHeadersOrigin

        val result = await(controller.searchRequest(request, Marshall.marshallV2List))
        assert(result.header.status === play.api.http.Status.BAD_REQUEST)
      }
    }


    "successful findPostcode" when {

      """when search is called with correct parameters
       it should clean up the postcode parameter
       and give an 'ok' response
       and log the lookup including the size of the result list
      """ in new Context {
        when(searcher.findPostcode(Postcode("FX11 4HG"), Some("FOO"))) thenReturn Future(List(addr1Db))
        val controller = new AddressSearchController(searcher, new ResponseStub(List(addr1Ar)), ec, cc)
        val request = FakeRequest("GET", "http://localhost:9000/v2/uk/addresses?postcode=FX114HG&filter=FOO").withHeadersOrigin

        val result = await(controller.searchRequest(request, Marshall.marshallV2List))
        assert(result.header.status === play.api.http.Status.OK)
      }

      """when search is called with blank filter
       it should clean up the postcode parameter
       and give an 'ok' response as if the filter parameter wass absent
       and log the lookup including the size of the result list
      """ in new Context {
        when(searcher.findPostcode(Postcode("FX11 4HG"), None)) thenReturn Future(List(addr1Db))
        val controller = new AddressSearchController(searcher, new ResponseStub(List(addr1Ar)), ec, cc)
        val request = FakeRequest("GET", "http://localhost:9000/v2/uk/addresses?postcode=FX114HG&filter=").withHeadersOrigin

        val result = await(controller.searchRequest(request, Marshall.marshallV2List))
        assert(result.header.status === play.api.http.Status.OK)
      }

      """when search is called with a postcode that will give several results
       it should give an 'ok' response containing the result list
       and log the lookup including the size of the result list
      """ in new Context {
        when(searcher.findPostcode(Postcode("FX11 4HG"), None)) thenReturn Future(List(dx1A, dx1B, dx1C))
        val controller = new AddressSearchController(searcher, new ResponseStub(List(fx1A, fx1B, fx1C)), ec, cc)
        val request = FakeRequest("GET", "http://localhost:9000/v2/uk/addresses?postcode=fx114hg").withHeadersOrigin

        val result = await(controller.searchRequest(request, Marshall.marshallV2List))
        assert(result.header.status === play.api.http.Status.OK)
      }
    }


    "findOutcode" when {

      """when search is called with correct parameters
       it should clean up the outcode parameter
       and give an 'ok' response
       and log the lookup including the size of the result list
      """ in new Context {
        when(searcher.findOutcode(Outcode("FX11"), "FOO")) thenReturn Future(List(addr1Db))
        val controller = new AddressSearchController(searcher, new ResponseStub(List(addr1Ar)), ec, cc)
        val request = FakeRequest("GET", "http://localhost:9000/v2/uk/addresses?outcode=FX11&filter=FOO").withHeadersOrigin

        val result = await(controller.searchRequest(request, Marshall.marshallV2List))
        assert(result.header.status === play.api.http.Status.OK)
      }

      """when search is called with a outcode that will give several results
       it should give an 'ok' response containing the result list
       and log the lookup including the size of the result list
      """ in new Context {
        when(searcher.findOutcode(Outcode("FX11"), "FOO")) thenReturn Future(List(dx1A, dx1B, dx1C))
        val controller = new AddressSearchController(searcher, new ResponseStub(List(fx1A, fx1B, fx1C)), ec, cc)
        val request = FakeRequest("GET", "http://localhost:9000/v2/uk/addresses?outcode=fx11&filter=FOO").withHeadersOrigin

        val result = await(controller.searchRequest(request, Marshall.marshallV2List))
        assert(result.header.status === play.api.http.Status.OK)
      }

      """when search is called with the outcode parameter but no filter
       it should give a 'bad request' response
       and log the error
      """ in new Context {
        val controller = new AddressSearchController(searcher, new ResponseStub(Nil), ec, cc)
        val request = FakeRequest("GET", "http://localhost:9000/v2/uk/addresses?outcode=FX11").withHeadersOrigin

        val result = await(controller.searchRequest(request, Marshall.marshallV2List))
        assert(result.header.status === play.api.http.Status.BAD_REQUEST)
      }
    }


    "successful findUprn" when {

      """when search is called with a uprn
       it should give an 'ok' response containing a list of one address
       and log the lookup including the size of the list
      """ in new Context {
        when(searcher.findUprn("100001")) thenReturn Future(List(addr1Db))
        val controller = new AddressSearchController(searcher, new ResponseStub(List(addr1Ar)), ec, cc)
        val request = FakeRequest("GET", "http://localhost:9000/v2/uk/addresses?uprn=100001").withHeadersOrigin

        val result = await(controller.searchRequest(request, Marshall.marshallV2List))
        assert(result.header.status === play.api.http.Status.OK)
      }
    }


    "successful searchFuzzy" when {

      """when search is called with a fuzzy term but without postcode/filter
       it should give an 'ok' response containing a list of two addresses
       and log the lookup including the size of the list
      """ in new Context {
        val sp = SearchParameters(fuzzy = Some("ATown"))
        when(searcher.searchFuzzy(sp)) thenReturn Future(List(addressDb1, addressDb2))
        val addressLookupController = new AddressSearchController(searcher, new ResponseStub(List(addressAr1, addressAr2)), ec, cc)
        val request = FakeRequest("GET", "http://localhost:9000/v2/uk/addresses?fuzzy=ATown").withHeadersOrigin

        val result = await(addressLookupController.searchRequest(request, Marshall.marshallV2List))
        assert(result.header.status === play.api.http.Status.OK)
      }

      """when search is called with a fuzzy term, postcode and filter
       it should give an 'ok' response containing a list of one address
       and log the lookup including the size of the list
      """ in new Context {
        val sp = SearchParameters(fuzzy = Some("ATown"), postcode = Postcode.cleanupPostcode("FX11 7LA"), filter = Some("AStreet"))
        when(searcher.searchFuzzy(sp)) thenReturn Future(List(addressDb2))
        val controller = new AddressSearchController(searcher, new ResponseStub(List(addressAr2)), ec, cc)
        val request = FakeRequest("GET", "http://localhost:9000/v2/uk/addresses?fuzzy=ATown&postcode=FX11+7LA&filter=AStreet").withHeadersOrigin

        val result = await(controller.searchRequest(request, Marshall.marshallV2List))
        assert(result.header.status === play.api.http.Status.OK)
      }

      """when search is called with line1 and postcode
       it should give an 'ok' response containing a list of one address
       and log the lookup including the size of the list
      """ in new Context {
        val sp = SearchParameters(postcode = Postcode.cleanupPostcode("FX11 7LA"), lines = List("AStreet", "ATown"))
        when(searcher.searchFuzzy(sp)) thenReturn Future(List(addressDb2))
        val controller = new AddressSearchController(searcher, new ResponseStub(List(addressAr2)), ec, cc)
        val request = FakeRequest("GET", "http://localhost:9000/v2/uk/addresses?line1=AStreet&line2=ATown&postcode=FX11+7LA").withHeadersOrigin

        val result = await(controller.searchRequest(request, Marshall.marshallV2List))
        assert(result.header.status === play.api.http.Status.OK)
      }
    }
  }
}
