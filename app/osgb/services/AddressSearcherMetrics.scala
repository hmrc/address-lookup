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

import com.codahale.metrics.MetricRegistry.name
import com.codahale.metrics.Timer.Context
import com.codahale.metrics.{MetricRegistry, Timer}
import osgb.SearchParameters
import uk.gov.hmrc.address.osgb.DbAddress
import uk.gov.hmrc.address.uk.{Outcode, Postcode}

import scala.concurrent.{ExecutionContext, Future}

class AddressSearcherMetrics(peer: AddressSearcher, registry: MetricRegistry, ec: ExecutionContext) extends AddressSearcher {

  private implicit val xec = ec

  private val prefix = peer.getClass.getSimpleName
  private val findIdTimer: Timer = registry.timer(name(prefix, "findId"))
  private val findUprnTimer: Timer = registry.timer(name(prefix, "findUprn"))
  private val findPostcodeTimer: Timer = registry.timer(name(prefix, "findPostcode"))
  private val findPostcodeFilterTimer: Timer = registry.timer(name(prefix, "findPostcodeFilter"))
  private val findOutcodeTimer: Timer = registry.timer(name(prefix, "findOutcode"))
  private val searchFuzzyTimer: Timer = registry.timer(name(prefix, "searchFuzzy"))
  private val searchFuzzyFilterTimer: Timer = registry.timer(name(prefix, "searchFuzzyFilter"))

  private def timerStop(t: Context, r: List[DbAddress]) = {
    t.stop()
    r
  }

  override def findID(id: String): Future[Option[DbAddress]] = {
    val context = findIdTimer.time()
    peer.findID(id) map {
      r =>
        context.stop()
        r
    }
  }

  override def findUprn(uprn: String): Future[List[DbAddress]] = {
    val context = findUprnTimer.time()
    peer.findUprn(uprn) map (timerStop(context, _))
  }

  override def findPostcode(postcode: Postcode, filterStr: Option[String]): Future[List[DbAddress]] = {
    val context = if (filterStr.isDefined) findPostcodeFilterTimer.time() else findPostcodeTimer.time()
    peer.findPostcode(postcode, filterStr) map (timerStop(context, _))
  }

  override def findOutcode(outcode: Outcode, filterStr: String): Future[List[DbAddress]] = {
    val context = findOutcodeTimer.time()
    peer.findOutcode(outcode, filterStr) map (timerStop(context, _))
  }

  override def searchFuzzy(sp: SearchParameters): Future[List[DbAddress]] = {
    val context = if (sp.filter.isDefined) searchFuzzyFilterTimer.time() else searchFuzzyTimer.time()
    peer.searchFuzzy(sp) map (timerStop(context, _))
  }
}
