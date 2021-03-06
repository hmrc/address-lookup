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

package osgb.services

import address.osgb.DbAddress
import address.uk.{Outcode, Postcode}
import com.codahale.metrics.Timer.Context
import com.codahale.metrics.{MetricRegistry, Timer}
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.{verify, when}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import osgb.SearchParameters
import play.api.test.Helpers._

import scala.concurrent.Future

class AddressSearcherMetricsTest extends AnyFunSuite with Matchers with MockitoSugar {

  val dummyGBDbAddr1 = DbAddress("GB123456", List("Line1", "Line2", "Line3"), "ATOWN", "FX30 4HG",
    Some("GB-ENG"), Some("UK"), Some(4510), Some("en"), None, Some("12.34567,-12.34567"))
  implicit val ec = scala.concurrent.ExecutionContext.global

  class TestContext {
    val peer = mock[AddressSearcher]

    val context = mock[Context]

    val timer = mock[Timer]
    when(timer.time()) thenReturn context

    val registry = mock[MetricRegistry]
    when(registry.timer(anyString())) thenReturn timer

    val asm = new AddressSearcherMetrics(peer, registry, ec)
  }

  test("testFindID") {
    new TestContext {
      when(peer.findID("GB1234567890")) thenReturn Future.successful(Some(dummyGBDbAddr1))

      assert(await(asm.findID("GB1234567890")) === Some(dummyGBDbAddr1))

      verify(context).stop()
    }
  }

  test("testFindPostcode") {
    new TestContext {
      when(peer.findPostcode(Postcode("FX30 4HG"), None)) thenReturn Future.successful(List(dummyGBDbAddr1))

      assert(await(asm.findPostcode(Postcode("FX30 4HG"), None)) === List(dummyGBDbAddr1))

      verify(context).stop()
    }
  }

  test("testFindOutcode") {
    new TestContext {
      when(peer.findOutcode(Outcode("FX30"), "Foo")) thenReturn Future.successful(List(dummyGBDbAddr1))

      assert(await(asm.findOutcode(Outcode("FX30"), "Foo")) === List(dummyGBDbAddr1))

      verify(context).stop()
    }
  }

  test("testFindUprn") {
    new TestContext {
      when(peer.findUprn("1234567890")) thenReturn Future.successful(List(dummyGBDbAddr1))

      assert(await(asm.findUprn("1234567890")) === List(dummyGBDbAddr1))

      verify(context).stop()
    }
  }

  test("testSearchFuzzy") {
    new TestContext {
      val sp = SearchParameters(postcode = Postcode.cleanupPostcode("FX30 4HG"))
      when(peer.searchFuzzy(sp)) thenReturn Future(List(dummyGBDbAddr1))

      assert(await(asm.searchFuzzy(sp)) === List(dummyGBDbAddr1))

      verify(context).stop()
    }
  }
}
