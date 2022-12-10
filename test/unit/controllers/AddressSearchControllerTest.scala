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

package controllers

import akka.stream.Materializer
import model.address._
import model.internal.DbAddress
import model.request.{LookupByPostTownRequest, LookupByPostcodeRequest, LookupByUprnRequest}
import model.response.SupportedCountryCodes
import model.{AddressSearchAuditEvent, AddressSearchAuditEventMatchedAddress, AddressSearchAuditEventRequestDetails}
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application, inject}
import repositories.{ABPAddressRepository, NonABPAddressRepository, PostgresABPAddressRepository, PostgresNonABPAddressRepository}
import services.{CheckAddressDataScheduler, ReferenceData, ResponseProcessor}
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import util.Utils._

import scala.concurrent.Future
import scala.concurrent.duration._

class AddressSearchControllerTest extends AnyWordSpec with Matchers with GuiceOneAppPerSuite with MockitoSugar {

  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global
  implicit val timeout: FiniteDuration = 1 second
  val cc = play.api.test.Helpers.stubControllerComponents()

  private val en = "en"

  import model.address.Country._

  val addr1Db = DbAddress("GB100001", 100001L, Some(10000100L), Some(1000010L), Some("gb-oragnisation-1"), List("10 Test Court", "Test Street", "Tester"), "Test upon Tyne", "FX1 5XD", Some("GB-ENG"), Some("GB"), Some(4510), Some("en"), None, Some(Location("12.345678", "-12.345678").toString), None, Some("TestLocalAuthority"))
  val addr1Ar = AddressRecord("GB100001", Some(100001L), Some(10000100L), Some(1000010L), Some("gb-oragnisation-1"), Address(List("10 Test Court", "Test Street", "Tester"), "Test upon Tyne", "FX1 5XD", Some(England), GB), en, Some(LocalCustodian(4510, "Test upon Tyne")), Some(Location("12.345678", "-12.345678").toSeq), Some("TestLocalAuthority"))

  val dx1A = DbAddress("GB100002", 100002L, Some(10000200L), Some(1000020L), Some("gb-oragnisation-2"), List("1 Test Street"), "Testtown", "FZ22 7ZW", Some("GB-XXX"), Some("GB"), Some(9999), Some("en"), None, Some("54.914561,-1.3905597"), None, Some("TestLocalAuthority"))
  val dx1B = DbAddress("GB100003", 100003L, Some(10000300L), Some(1000030L), Some("gb-oragnisation-3"), List("2 Test Street"), "Testtown", "FZ22 7ZW", Some("GB-XXX"), Some("GB"), Some(9999), Some("en"), None, Some("54.914561,-1.3905597"), None, Some("TestLocalAuthority"))
  val dx1C = DbAddress("GB100004", 100004L, Some(10000400L), Some(1000040L), Some("gb-oragnisation-4"), List("3 Test Street"), "Testtown", "FZ22 7ZW", Some("GB-XXX"), Some("GB"), Some(9999), Some("en"), None, Some("54.914561,-1.3905597"), None, Some("TestLocalAuthority"))

  val fx1A = AddressRecord("GB100002", Some(100002L), Some(10000200L), Some(1000020L), Some("gb-oragnisation-2"), Address(List("1 Test Street"), "Testtown", "FZ22 7ZW", Some(England), GB), en, Some(LocalCustodian(9999, "Somewhere")), Some(Location("0,0").toSeq), Some("TestLocalAuthority"))
  val fx1B = AddressRecord("GB100003", Some(100003L), Some(10000300L), Some(1000030L), Some("gb-oragnisation-3"), Address(List("2 Test Street"), "Testtown", "FZ22 7ZW", Some(England), GB), en, Some(LocalCustodian(9999, "Somewhere")), Some(Location("0,0").toSeq), Some("TestLocalAuthority"))
  val fx1C = AddressRecord("GB100004", Some(100004L), Some(10000400L), Some(1000040L), Some("gb-oragnisation-4"), Address(List("3 Test Street"), "Testtown", "FZ22 7ZW", Some(England), GB), en, Some(LocalCustodian(9999, "Somewhere")), Some(Location("0,0").toSeq), Some("TestLocalAuthority"))

  val addressDb1 = DbAddress("GB100005", 100005L, Some(10000500L), Some(1000050L), Some("gb-oragnisation-5"), List("Test Road"), "ATown", "FX11 7LX", Some("GB-ENG"), Some("GB"), Some(2935), Some("en"), None, Some(Location("12.345678", "-12.345678").toString), None, Some("TestLocalAuthority"))
  val addressDb2 = DbAddress("GB100006", 100006L, Some(10000600L), Some(1000060L), Some("gb-oragnisation-6"), List("ARoad", "ARoad"), "Atown", "FX11 7LA", Some("GB-ENG"), Some("GB"), Some(2935), Some("en"), None, Some(Location("12.345678", "-12.345678").toString), None, Some("TestLocalAuthority"))

  val addressAr1 = AddressRecord("GB100005", Some(100005L), Some(10000500L), Some(1000050L), Some("gb-oragnisation-5"), Address(List("Test Road"), "ATown", "FX11 7LX", Some(England), GB), en, Some(LocalCustodian(2935, "Testland")), Some(Location("12.345678", "-12.345678").toSeq), Some("TestLocalAuthority"))
  val addressAr2 = AddressRecord("GB100006", Some(100006L), Some(10000600L), Some(1000060L), Some("gb-oragnisation-6"), Address(List("Test Station", "Test Road"), "ATown", "FX11 7LA", Some(England), GB), en, Some(LocalCustodian(2935, "Testland")), Some(Location("12.345678", "-12.345678").toSeq), Some("TestLocalAuthority"))

  val abpSearcher: PostgresABPAddressRepository = mock[PostgresABPAddressRepository]
  val nonAbpSearcher: PostgresNonABPAddressRepository = mock[PostgresNonABPAddressRepository]
  val mockAuditConnector = mock[AuditConnector]
  val mockCheckAddressDataScheduler = mock[CheckAddressDataScheduler]

  //when(mockCheckAddressDataScheduler.enable()).thenReturn(Unit)

  override implicit lazy val app: Application = {
    new GuiceApplicationBuilder()
        .overrides(inject.bind[ABPAddressRepository].toInstance(abpSearcher))
        .overrides(inject.bind[NonABPAddressRepository].toInstance(nonAbpSearcher))
        .overrides(inject.bind[AuditConnector].toInstance(mockAuditConnector))
        .overrides(inject.bind[CheckAddressDataScheduler].toInstance(mockCheckAddressDataScheduler))
        .build()
  }

  val controller: AddressSearchController = app.injector.instanceOf[AddressSearchController]
  implicit val mat: Materializer = app.injector.instanceOf[Materializer]

  class ResponseStub(a: List[AddressRecord]) extends ResponseProcessor(ReferenceData.empty) {
    override def convertAddressList(dbAddresses: Seq[DbAddress]): List[AddressRecord] = a
  }

  "postcode lookup with POST requests" should {

    "give bad request" when {

      """when search is called without 'X-Origin' header
       it should give a 'bad request' response via the appropriate exception
       and not log any error
      """ in {
        import LookupByPostcodeRequest._
        val jsonPayload = Json.toJson(LookupByPostcodeRequest(Postcode("FX11 4HG")))
        val request: Request[String] = FakeRequest("POST", "/lookup")
            .withBody(jsonPayload.toString())

        intercept[UpstreamErrorResponse] {
          val result = controller.search().apply(request)
          status(result) shouldBe Status.BAD_REQUEST
        }
      }
    }

    "successful findPostTown" when {
      """when search is called with a postcode that will give several results
       it should give an 'ok' response containing the result list
       and log the lookup including the size of the result list
       and audit the results
      """ in {
        clearInvocations(mockAuditConnector)

        when(abpSearcher.findTown(meq("TESTTOWN"), meq(Some("Test Street")))).thenReturn(Future(List(dx1A, dx1B, dx1C)))

        val jsonPayload = Json.toJson(LookupByPostTownRequest("Testtown", Some("Test Street")))
        val request = FakeRequest("POST", "/lookup/by-post-town")
          .withBody(jsonPayload.toString)
          .withHeaders("User-Agent" -> "test-user-agent")
          .withHeadersOrigin

        val expectedAuditRequestDetails = AddressSearchAuditEventRequestDetails(postTown = Some("TESTTOWN"), filter = Some("Test Street"))

        val expectedAuditAddressMatches = Seq(
          AddressSearchAuditEventMatchedAddress("100002", Some(10000200),Some(1000020),Some("gb-oragnisation-2"), List("1 Test Street"), "Testtown", None, Some(List(54.914561, -1.3905597)), Some("TestLocalAuthority"), None, "FZ22 7ZW", None, Country("GB", "United Kingdom")),
          AddressSearchAuditEventMatchedAddress("100003", Some(10000300),Some(1000030),Some("gb-oragnisation-3"), List("2 Test Street"), "Testtown", None, Some(List(54.914561, -1.3905597)), Some("TestLocalAuthority"), None, "FZ22 7ZW", None, Country("GB", "United Kingdom")),
          AddressSearchAuditEventMatchedAddress("100004", Some(10000400),Some(1000040),Some("gb-oragnisation-4"), List("3 Test Street"), "Testtown", None, Some(List(54.914561, -1.3905597)), Some("TestLocalAuthority"), None, "FZ22 7ZW", None, Country("GB", "United Kingdom")))

        val expectedAuditEvent = AddressSearchAuditEvent(Some("test-user-agent"), expectedAuditRequestDetails, 3, expectedAuditAddressMatches)

        val result = controller.searchByPostTown().apply(request)
        status(result) shouldBe Status.OK

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(meq("AddressSearch"), meq(expectedAuditEvent))(any(), any(), any())
      }

      """when search is called with a posttown that gives no results
       it should give an 'ok' response and not send an explicit audit event
      """ in {
        clearInvocations(mockAuditConnector)

        when(abpSearcher.findTown(meq("TESTTOWN"), meq(None))) thenReturn Future(List())

        val jsonPayload = Json.toJson(LookupByPostTownRequest("Testtown", None))
        val request = FakeRequest("POST", "/lookup/by-post-town")
          .withBody(jsonPayload.toString)
          .withHeaders("User-Agent" -> "test-user-agent")
          .withHeadersOrigin

        val result = controller.searchByPostTown().apply(request)
        status(result) shouldBe Status.OK

        verify(mockAuditConnector, never()).sendExplicitAudit(any(), any[AddressSearchAuditEvent]())(any(), any(), any())
      }
    }

    "successful findPostcode" when {

      """when search is called with correct parameters
       it should clean up the postcode parameter
       and give an 'ok' response
       and log the lookup including the size of the result list
      """ in {
        clearInvocations(mockAuditConnector)
        when(abpSearcher.findPostcode(Postcode("FX11 4HG"), Some("FOO"))) thenReturn Future(List(addr1Db))
        val jsonPayload = Json.toJson(LookupByPostcodeRequest(Postcode("FX11 4HG"), Some("FOO")))
        val request = FakeRequest("POST", "/lookup")
          .withBody(jsonPayload.toString)
          .withHeadersOrigin

        val result = controller.search().apply(request)
        status(result) shouldBe Status.OK
      }

      """when search is called with blank filter
       it should clean up the postcode parameter
       and give an 'ok' response as if the filter parameter wass absent
       and log the lookup including the size of the result list
      """ in {
        clearInvocations(mockAuditConnector)
        when(abpSearcher.findPostcode(Postcode("FX11 4HG"), None)) thenReturn Future(List(addr1Db))
        val jsonPayload = Json.toJson(LookupByPostcodeRequest(Postcode("FX11 4HG"), None))
        val request = FakeRequest("POST", "/lookup")
          .withBody(jsonPayload.toString)
          .withHeadersOrigin

        val result = controller.search().apply(request)
        status(result) shouldBe Status.OK
      }

      """when search is called with a postcode that will give several results
       it should give an 'ok' response containing the result list
       and log the lookup including the size of the result list
       and audit the results
      """ in {
        clearInvocations(mockAuditConnector)

        when(abpSearcher.findPostcode(meq(Postcode("FX11 4HG")), meq(Some("Test Street")))) thenReturn Future(List(dx1A, dx1B, dx1C))
        val jsonPayload = Json.toJson(LookupByPostcodeRequest(Postcode("FX11 4HG"), Some("Test Street")))
        val request = FakeRequest("POST", "/lookup")
          .withBody(jsonPayload.toString)
          .withHeaders("User-Agent" -> "test-user-agent")
          .withHeadersOrigin

        val expectedAuditRequestDetails = AddressSearchAuditEventRequestDetails(postcode = Some("FX11 4HG"), filter = Some("Test Street"))

        val expectedAuditAddressMatches = Seq(
          AddressSearchAuditEventMatchedAddress("100002", Some(10000200),Some(1000020),Some("gb-oragnisation-2"), List("1 Test Street"), "Testtown", None, Some(List(54.914561, -1.3905597)), Some("TestLocalAuthority"), None, "FZ22 7ZW", None, Country("GB", "United Kingdom")),
          AddressSearchAuditEventMatchedAddress("100003", Some(10000300),Some(1000030),Some("gb-oragnisation-3"), List("2 Test Street"), "Testtown", None, Some(List(54.914561, -1.3905597)), Some("TestLocalAuthority"), None, "FZ22 7ZW", None, Country("GB", "United Kingdom")),
          AddressSearchAuditEventMatchedAddress("100004", Some(10000400),Some(1000040),Some("gb-oragnisation-4"), List("3 Test Street"), "Testtown", None, Some(List(54.914561, -1.3905597)), Some("TestLocalAuthority"), None, "FZ22 7ZW", None, Country("GB", "United Kingdom")))

        val expectedAuditEvent = AddressSearchAuditEvent(Some("test-user-agent"), expectedAuditRequestDetails, 3, expectedAuditAddressMatches)

        val result = controller.search().apply(request)
        status(result) shouldBe Status.OK

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(meq("AddressSearch"), meq(expectedAuditEvent))(any(), any(), any())
      }

      """when search is called with a postcode thatgives no results
       it should give an 'ok' response and not send an explicit audit event
      """ in {
        clearInvocations(mockAuditConnector)

        when(abpSearcher.findPostcode(meq(Postcode("FX11 4HX")), meq(None))) thenReturn Future(List())
        val jsonPayload = Json.toJson(LookupByPostcodeRequest(Postcode("FX11 4HX")))
        val request = FakeRequest("POST", "/lookup")
          .withBody(jsonPayload.toString)
          .withHeaders("User-Agent" -> "test-user-agent")
          .withHeadersOrigin

        val result = controller.search().apply(request)
        status(result) shouldBe Status.OK

        verify(mockAuditConnector, never()).sendExplicitAudit(any(), any[AddressSearchAuditEvent]())(any(), any(), any())
      }
    }
  }

  "uprn lookup with POST request" should {
    "give success" when {
      """search is called with a valid uprn""" in {
        import LookupByUprnRequest._
        when(abpSearcher.findUprn(meq("0123456789"))).thenReturn(Future.successful(List()))
        val jsonPayload = Json.toJson(LookupByUprnRequest("0123456789"))
        val request = FakeRequest("POST", "/lookup/by-uprn")
        .withBody(jsonPayload.toString)
        .withHeadersOrigin

        val response = controller.searchByUprn().apply(request)
        status(response) shouldBe 200
      }
    }

    "give bad request" when {
      """search is called with an invalid uprn""" in {

        val scheduler = app.injector.instanceOf[CheckAddressDataScheduler]
        val controller = new AddressSearchController(abpSearcher, nonAbpSearcher, new ResponseStub(Nil), mockAuditConnector, ec, cc, SupportedCountryCodes(List(), List()), scheduler)
        val jsonPayload = Json.toJson(LookupByUprnRequest("GB0123456789"))
        val request = FakeRequest("POST", "/lookup/by-uprn")
        .withBody(jsonPayload.toString)
        .withHeadersOrigin

        val response = controller.searchByUprn().apply(request)
        status(response) shouldBe 400
      }
    }

  }
}
