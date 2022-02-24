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

package services

import com.codahale.metrics.Timer.Context
import com.codahale.metrics.{MetricRegistry, Timer}
import model.address.{Outcode, Postcode}
import model.internal.DbAddress
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.{verify, when}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import repositories.ABPAddressRepository

import scala.concurrent.Future

class ABPAddressRepositoryMetricsTest extends AnyWordSpec with Matchers with MockitoSugar {

  val dummyGBDbAddr1 = DbAddress("GB123456", 123456L, None, None, None, List("Line1", "Line2", "Line3"), "ATOWN", "FX30 4HG", Some("GB-ENG"), Some("GB"), Some(4510), Some("en"), None, Some("12.34567,-12.34567"))
  implicit val ec = scala.concurrent.ExecutionContext.global

  class TestContext {
    val peer = mock[ABPAddressRepository]

    val context = mock[Context]

    val timer = mock[Timer]
    when(timer.time()) thenReturn context

    val registry = mock[MetricRegistry]
    when(registry.timer(anyString())) thenReturn timer

    val asm = new ABPAddressRepositoryMetrics(peer, registry, ec)
  }

  "AddressSearchMetrics" when {

    "testFindID is called" should {
      "return address with the requested id" in new TestContext {
        when(peer.findID("GB1234567890")) thenReturn Future.successful(List(dummyGBDbAddr1))

        await(asm.findID("GB1234567890")) shouldBe List(dummyGBDbAddr1)

        verify(context).stop()
      }
    }

    "testFindPostcode is called" should {
      "return address with the requested postcode" in new TestContext {
        when(peer.findPostcode(Postcode("FX30 4HG"), None)) thenReturn Future.successful(List(dummyGBDbAddr1))

        await(asm.findPostcode(Postcode("FX30 4HG"), None)) shouldBe List(dummyGBDbAddr1)

        verify(context).stop()
      }
    }

    "testFindOutcode is called" should {
      "return address with a postcode having the requested outcode" in new TestContext {
        when(peer.findOutcode(Outcode("FX30"), "Foo")) thenReturn Future.successful(List(dummyGBDbAddr1))

        await(asm.findOutcode(Outcode("FX30"), "Foo")) shouldBe List(dummyGBDbAddr1)

        verify(context).stop()
      }
    }

    "testFindUprn is called" should {
      "return address with the requested uprn" in new TestContext {
        when(peer.findUprn("1234567890")) thenReturn Future.successful(List(dummyGBDbAddr1))

        await(asm.findUprn("1234567890")) shouldBe List(dummyGBDbAddr1)

        verify(context).stop()
      }
    }
  }
}
