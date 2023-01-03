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

package services

import com.codahale.metrics.MetricRegistry.name
import com.codahale.metrics.Timer.Context
import com.codahale.metrics.{Meter, MetricRegistry, Timer}
import model.address.{Outcode, Postcode}
import model.internal.DbAddress
import repositories.ABPAddressRepository

import scala.concurrent.{ExecutionContext, Future}

class ABPAddressRepositoryMetrics(peer: ABPAddressRepository, registry: MetricRegistry, ec: ExecutionContext) extends ABPAddressRepository {

  private implicit val xec = ec

  private val prefix = "AddressLookupService"
  private val findIdTimer: Timer = registry.timer(name(prefix, "findId"))
  private val findUprnTimer: Timer = registry.timer(name(prefix, "findUprn"))
  private val findPostcodeTimer: Timer = registry.timer(name(prefix, "findPostcode"))
  private val findTownTimer: Timer = registry.timer(name(prefix, "findTown"))
  private val findPostcodeFilterTimer: Timer = registry.timer(name(prefix, "findPostcodeFilter"))
  private val findTownFilterTimer: Timer = registry.timer(name(prefix, "findTownFilter"))
  private val findOutcodeTimer: Timer = registry.timer(name(prefix, "findOutcode"))

  private val addressSearchHits: Meter = registry.meter(name(prefix, "searchResults"))

  private def timerStop[T](t: Context, r: T) = {
    t.stop()
    r
  }

  override def findID(id: String): Future[List[DbAddress]] = {
    val context = findIdTimer.time()
    peer.findID(id) map {
      r =>
        context.stop()
        addressSearchHits.mark(r.length)
        r
    }
  }

  override def findUprn(uprn: String): Future[List[DbAddress]] = {
    val context = findUprnTimer.time()
    peer.findUprn(uprn) map { r =>
      timerStop(context, r)
      addressSearchHits.mark(r.length)
      r
    }
  }

  override def findPostcode(postcode: Postcode, filterStr: Option[String]): Future[List[DbAddress]] = {
    val context = if (filterStr.isDefined) findPostcodeFilterTimer.time() else findPostcodeTimer.time()
    peer.findPostcode(postcode, filterStr) map { r =>
      timerStop(context, r)
      addressSearchHits.mark(r.length)
      r
    }
  }

  override def findTown(town: String, filterStr: Option[String]): Future[List[DbAddress]] = {
    val context = if (filterStr.isDefined) findTownFilterTimer.time() else findTownTimer.time()
    peer.findTown(town, filterStr) map { r =>
      timerStop(context, r)
      addressSearchHits.mark(r.length)
      r
    }
  }

  override def findOutcode(outcode: Outcode, filterStr: String): Future[List[DbAddress]] = {
    val context = findOutcodeTimer.time()
    peer.findOutcode(outcode, filterStr) map { r =>
      timerStop(context, r)
      addressSearchHits.mark(r.length)
      r
    }
  }
}


