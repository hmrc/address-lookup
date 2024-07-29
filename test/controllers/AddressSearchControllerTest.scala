/*
 * Copyright 2023 HM Revenue & Customs
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

import config.AppConfig
import connectors.DownstreamConnector
import model.address._
import model.request.{LookupByPostTownRequest, LookupByPostcodeRequest, LookupByUprnRequest}
import model.{AddressSearchAuditEvent, AddressSearchAuditEventMatchedAddress, AddressSearchAuditEventRequestDetails}
import org.apache.pekko.stream.Materializer
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.{HeaderNames, MimeTypes, Status}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsNumber, JsObject, JsString, Json}
import play.api.mvc.{ControllerComponents, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application, inject}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import util.Utils._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class AddressSearchControllerTest extends AnyWordSpec with Matchers with GuiceOneAppPerSuite with MockitoSugar {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit val timeout: FiniteDuration = 1 second
  private val cc: ControllerComponents = play.api.test.Helpers.stubControllerComponents()

  private val en = "en"

  import model.address.Country._

  val fx1A = AddressRecord("GB100002", Some(100002L), Some(10000200L), Some(1000020L), Some("gb-oragnisation-2"), Address(List("1 Test Street"), "Testtown", "FZ22 7ZW", Some(England), GB), en, Some(LocalCustodian(9999, "Somewhere")), Some(Location("0,0").toSeq), Some("TestLocalAuthority"))
  val fx1B = AddressRecord("GB100003", Some(100003L), Some(10000300L), Some(1000030L), Some("gb-oragnisation-3"), Address(List("2 Test Street"), "Testtown", "FZ22 7ZW", Some(England), GB), en, Some(LocalCustodian(9999, "Somewhere")), Some(Location("0,0").toSeq), Some("TestLocalAuthority"))
  val fx1C = AddressRecord("GB100004", Some(100004L), Some(10000400L), Some(1000040L), Some("gb-oragnisation-4"), Address(List("3 Test Street"), "Testtown", "FZ22 7ZW", Some(England), GB), en, Some(LocalCustodian(9999, "Somewhere")), Some(Location("0,0").toSeq), Some("TestLocalAuthority"))

  val addressAr1 = AddressRecord("GB100005", Some(100005L), Some(10000500L), Some(1000050L), Some("gb-oragnisation-5"), Address(List("Test Road"), "ATown", "FX11 7LX", Some(England), GB), en, Some(LocalCustodian(2935, "Testland")), Some(Location("12.345678", "-12.345678").toSeq), Some("TestLocalAuthority"))
  val addressAr2 = AddressRecord("GB100006", Some(100006L), Some(10000600L), Some(1000060L), Some("gb-oragnisation-6"), Address(List("Test Station", "Test Road"), "ATown", "FX11 7LA", Some(England), GB), en, Some(LocalCustodian(2935, "Testland")), Some(Location("12.345678", "-12.345678").toSeq), Some("TestLocalAuthority"))

  val mockAuditConnector = mock[AuditConnector]


  override implicit lazy val app: Application = {
    new GuiceApplicationBuilder()
      .overrides(inject.bind[AuditConnector].toInstance(mockAuditConnector))
      .configure(
        "microservice.services.access-control.enabled" -> true,
        "microservice.services.access-control.allow-list.1" -> "test-user-agent",
        "microservice.services.access-control.allow-list.2" -> "another-user-agent")
      .build()
  }

  val controller: AddressSearchController = app.injector.instanceOf[AddressSearchController]
  implicit val mat: Materializer = app.injector.instanceOf[Materializer]

  "findPostTown" should {

    """when search is called without valid 'user-agent' header
       it should give a forbidden response and not log any error
      """ in {
      clearInvocations(mockAuditConnector)


      val payload = LookupByPostTownRequest("Testtown", Some("Test Street"))
      val request = FakeRequest("POST", "/lookup/by-post-town")
        .withBody(payload)
        .withHeaders(HeaderNames.USER_AGENT -> "forbidden-user-agent", HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)

      val result = controller.searchByPostTown().apply(request)
      contentType(result) shouldBe Some(MimeTypes.JSON)
      contentAsJson(result) shouldBe JsObject(Map(
        "code" -> JsNumber(FORBIDDEN),
        "description" -> JsString("One or more user agents in 'forbidden-user-agent' are not authorized to use this service. Please complete 'https://forms.office.com/Pages/ResponsePage.aspx?id=PPdSrBr9mkqOekokjzE54cRTj_GCzpRJqsT4amG0JK1UMkpBS1NUVDhWR041NjJWU0lCMVZUNk5NTi4u' to request access.")
      ))
      status(result) shouldBe Status.FORBIDDEN
    }

    """when search is called with a postcode that will give several results
       it should give an 'ok' response containing the result list
       and log the lookup including the size of the result list
       and audit the results
      """ in {
      clearInvocations(mockAuditConnector)


      val payload = LookupByPostTownRequest("town", Some("address lines"))
      val request = FakeRequest("POST", "/lookup/by-post-town")
        .withBody(payload)
        .withHeaders(HeaderNames.USER_AGENT -> "test-user-agent", HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
        .withHeadersOrigin

      val expectedAuditRequestDetails = AddressSearchAuditEventRequestDetails(postTown = Some("TOWN"), filter = Some("address lines"))

      val expectedAuditAddressMatches = Seq(
        AddressSearchAuditEventMatchedAddress("990091234568",None,None,None,List("Address with 2 Address Lines", "Second Address Line"),"Town",Some(LocalCustodian(425,"WYCOMBE")),None,None,None,"ZZ1Z 6AB",Some(Country("GB-ENG","England")),Country("GB","United Kingdom")),
        AddressSearchAuditEventMatchedAddress("990091234637",None,None,None,List("Address with 2 Address Lines", "Second Address Line"),"Town",Some(LocalCustodian(6810,"GWYNEDD")),None,None,None,"FX52 9SJ",Some(Country("GB-ENG","England")),Country("GB","United Kingdom")),
        AddressSearchAuditEventMatchedAddress("990091234569",None,None,None,List("Address with 3 Address Lines", "Second Address Line", "Third Address Line"),"Town",Some(LocalCustodian(425,"WYCOMBE")),None,None,None,"ZZ1Z 7AB",Some(Country("GB-ENG","England")),Country("GB","United Kingdom")),
        AddressSearchAuditEventMatchedAddress("990091234638",None,None,None,List("Address with 3 Address Lines", "Second Address Line", "Third Address Line"),"Town",Some(LocalCustodian(6810,"GWYNEDD")),None,None,None,"FX0 2GJ",Some(Country("GB-ENG","England")),Country("GB","United Kingdom")))
      val expectedAuditEvent = AddressSearchAuditEvent(Some("test-user-agent"), expectedAuditRequestDetails, 4, expectedAuditAddressMatches)

      val result = controller.searchByPostTown().apply(request)
      status(result) shouldBe Status.OK

      verify(mockAuditConnector, times(1))
        .sendExplicitAudit(meq("AddressSearch"), meq(expectedAuditEvent))(any(), any(), any())
    }

    """when search is called with a posttown that gives no results
       it should give an 'ok' response and not send an explicit audit event
      """ in {
      clearInvocations(mockAuditConnector)


      val payload = LookupByPostTownRequest("non-existent-town", None)
      val request = FakeRequest("POST", "/lookup/by-post-town")
        .withBody(payload)
        .withHeaders(HeaderNames.USER_AGENT -> "test-user-agent", HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
        .withHeadersOrigin

      val result = controller.searchByPostTown().apply(request)
      status(result) shouldBe Status.OK

      verify(mockAuditConnector, never()).sendExplicitAudit(any(), any[AddressSearchAuditEvent]())(any(), any(), any())
    }
  }

  "findPostcode" should {

    """when search is called without valid 'user-agent' header
       it should give a forbidden response and not log any error
      """ in {
      import LookupByPostcodeRequest._

      val payload = LookupByPostcodeRequest(Postcode("FX11 4HG"))
      val request = FakeRequest("POST", "/lookup")
        .withBody(payload)
        .withHeaders(HeaderNames.USER_AGENT -> "forbidden-user-agent", HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)

      val result = controller.searchByPostcode().apply(request)
      status(result) shouldBe Status.FORBIDDEN

    }

    """when search is called with correct parameters
       it should clean up the postcode parameter
       and give an 'ok' response
       and log the lookup including the size of the result list
      """ in {
      clearInvocations(mockAuditConnector)
      val payload = LookupByPostcodeRequest(Postcode("FX11 4HG"), Some("FOO"))
      val request = FakeRequest("POST", "/lookup")
        .withBody(payload)
        .withHeaders(HeaderNames.USER_AGENT -> "test-user-agent", HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
        .withHeadersOrigin

      val result = controller.searchByPostcode().apply(request)
      status(result) shouldBe Status.OK
    }

    """when search is called with blank filter
       it should clean up the postcode parameter
       and give an 'ok' response as if the filter parameter wass absent
       and log the lookup including the size of the result list
      """ in {
      clearInvocations(mockAuditConnector)
      val payload = LookupByPostcodeRequest(Postcode("FX11 4HG"), None)
      val request = FakeRequest("POST", "/lookup")
        .withBody(payload)
        .withHeaders(HeaderNames.USER_AGENT -> "test-user-agent", HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
        .withHeadersOrigin

      val result = controller.searchByPostcode().apply(request)
      status(result) shouldBe Status.OK
    }

    """when search is called with a postcode that will give several results
       it should give an 'ok' response containing the result list
       and log the lookup including the size of the result list
       and audit the results
      """ in {
      clearInvocations(mockAuditConnector)

      val payload = LookupByPostcodeRequest(Postcode("ZZ11 1ZZ"), Some("Test Street"))
      val request = FakeRequest("POST", "/lookup")
        .withBody(payload)
        .withHeaders(HeaderNames.USER_AGENT -> "test-user-agent", HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
        .withHeadersOrigin

      val expectedAuditRequestDetails = AddressSearchAuditEventRequestDetails(postcode = Some("ZZ11 1ZZ"), uprn = None, filter = Some("Test Street"))

      val expectedAuditAddressMatches = Seq(
        AddressSearchAuditEventMatchedAddress("990091234512", None, None, None, List("10 Test Street"), "Testtown", Some(LocalCustodian(121, "NORTH SOMERSET")), None, None, None, "ZZ11 1ZZ", Some(Country("GB-ENG", "England")), Country("GB", "United Kingdom")),
        AddressSearchAuditEventMatchedAddress("990091234513", None, None, None, List("11 Test Street"), "Testtown", Some(LocalCustodian(121, "NORTH SOMERSET")), None, None, None, "ZZ11 1ZZ", Some(Country("GB-ENG", "England")), Country("GB", "United Kingdom")),
        AddressSearchAuditEventMatchedAddress("990091234504", None, None, None, List("4 Test Street"), "Testtown", Some(LocalCustodian(121, "NORTH SOMERSET")), None, None, None, "ZZ11 1ZZ", Some(Country("GB-ENG", "England")), Country("GB", "United Kingdom")),
        AddressSearchAuditEventMatchedAddress("990091234505", None, None, None, List("5 Test Street"), "Testtown", Some(LocalCustodian(121, "NORTH SOMERSET")), None, None, None, "ZZ11 1ZZ", Some(Country("GB-ENG", "England")), Country("GB", "United Kingdom")),
        AddressSearchAuditEventMatchedAddress("990091234506", None, None, None, List("6 Test Street"), "Testtown", Some(LocalCustodian(121, "NORTH SOMERSET")), None, None, None, "ZZ11 1ZZ", Some(Country("GB-ENG", "England")), Country("GB", "United Kingdom")),
        AddressSearchAuditEventMatchedAddress("990091234510", None, None, None, List("8 Test Street"), "Testtown", Some(LocalCustodian(121, "NORTH SOMERSET")), None, None, None, "ZZ11 1ZZ", Some(Country("GB-ENG", "England")), Country("GB", "United Kingdom")),
        AddressSearchAuditEventMatchedAddress("990091234511", None, None, None, List("9 Test Street"), "Testtown", Some(LocalCustodian(121, "NORTH SOMERSET")), None, None, None, "ZZ11 1ZZ", Some(Country("GB-ENG", "England")), Country("GB", "United Kingdom")),
        AddressSearchAuditEventMatchedAddress("990091234507", None, None, None, List("Flat 1a", "7 Test Street"), "Testtown", Some(LocalCustodian(121, "NORTH SOMERSET")), None, None, None, "ZZ11 1ZZ", Some(Country("GB-ENG", "England")), Country("GB", "United Kingdom")),
        AddressSearchAuditEventMatchedAddress("990091234508", None, None, None, List("Flat 1b", "7 Test Street"), "Testtown", Some(LocalCustodian(121, "NORTH SOMERSET")), None, None, None, "ZZ11 1ZZ", Some(Country("GB-ENG", "England")), Country("GB", "United Kingdom")),
        AddressSearchAuditEventMatchedAddress("990091234509", None, None, None, List("Flat 2a", "7 Test Street"), "Testtown", Some(LocalCustodian(121, "NORTH SOMERSET")), None, None, None, "ZZ11 1ZZ", Some(Country("GB-ENG", "England")), Country("GB", "United Kingdom"))
      )

      val expectedAuditEvent = AddressSearchAuditEvent(Some("test-user-agent"), expectedAuditRequestDetails, 10, expectedAuditAddressMatches)

      val result = controller.searchByPostcode().apply(request)
      status(result) shouldBe Status.OK

      verify(mockAuditConnector, times(1))
        .sendExplicitAudit(meq("AddressSearch"), meq(expectedAuditEvent))(any(), any(), any())
    }

    """when search is called with a postcode that gives no results
       it should give an 'ok' response and not send an explicit audit event
      """ in {
      clearInvocations(mockAuditConnector)

      val payload = LookupByPostcodeRequest(Postcode("ZZ11 1YY"))
      val request = FakeRequest("POST", "/lookup")
        .withBody(payload)
        .withHeaders(HeaderNames.USER_AGENT -> "test-user-agent", HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
        .withHeadersOrigin

      val result = controller.searchByPostcode().apply(request)
      status(result) shouldBe Status.OK

      verify(mockAuditConnector, never()).sendExplicitAudit(any(), any[AddressSearchAuditEvent]())(any(), any(), any())
    }
  }


  "uprn lookup with POST request" should {
    "give forbidden" when {
      """search is called without a valid user agent""" in {
        import LookupByUprnRequest._
        val payload = LookupByUprnRequest("0123456789")
        val request = FakeRequest("POST", "/lookup/by-uprn")
          .withBody(payload)
          .withHeaders(HeaderNames.USER_AGENT -> "forbidden-user-agent", HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
          .withHeadersOrigin

        val response = controller.searchByUprn().apply(request)
        status(response) shouldBe 403
      }
    }

    "give success" when {
      """search is called with a valid uprn""" in {
        import LookupByUprnRequest._

        clearInvocations(mockAuditConnector)

        val expectedAuditRequestDetails = AddressSearchAuditEventRequestDetails(uprn = Some("790091234501"))

        val expectedAuditAddressMatches = Seq(
          AddressSearchAuditEventMatchedAddress("790091234501",None,None,None,List("1 Test Street"),"Testtown",Some(LocalCustodian(9010,"SHETLAND ISLANDS")),None,None,None,"BB00 0BB",Some(Country("GB-SCT","Scotland")),Country("GB","United Kingdom"))
        )

        val expectedAuditEvent = AddressSearchAuditEvent(Some("test-user-agent"), expectedAuditRequestDetails, 10, expectedAuditAddressMatches)

        val payload = LookupByUprnRequest("790091234501")
        val request = FakeRequest("POST", "/lookup/by-uprn")
          .withBody(payload)
          .withHeaders(HeaderNames.USER_AGENT -> "test-user-agent", HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
          .withHeadersOrigin

        val response = controller.searchByUprn().apply(request)
        status(response) shouldBe 200

        verify(mockAuditConnector, never())
          .sendExplicitAudit(any(), meq(expectedAuditEvent))(any(), any(), any())
      }
    }

    "give bad request" when {
      """search is called with an invalid uprn""" in {
        val connector = app.injector.instanceOf[DownstreamConnector]
        val configHelper = app.injector.instanceOf[AppConfig]
        val controller = new AddressSearchController(connector, mockAuditConnector, cc, configHelper)(ec)
        val payload = LookupByUprnRequest("GB0123456789")
        val request = FakeRequest("POST", "/lookup/by-uprn")
          .withBody(payload)
          .withHeaders(HeaderNames.USER_AGENT -> "test-user-agent", HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
          .withHeadersOrigin

        val response = controller.searchByUprn().apply(request)
        status(response) shouldBe 400
      }
    }

  }
}
