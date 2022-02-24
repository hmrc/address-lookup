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

import com.codahale.metrics.MetricRegistry.name
import com.codahale.metrics.Timer.Context
import com.codahale.metrics.{MetricRegistry, Timer}
import model.internal.NonUKAddress
import model.response
import repositories.NonABPAddressRepository

import scala.concurrent.{ExecutionContext, Future}

class NonABPAddressRepositoryMetrics(peer: NonABPAddressRepository, registry: MetricRegistry, ec: ExecutionContext) extends NonABPAddressRepository {
  private implicit val xec = ec

  private val prefix = peer.getClass.getSimpleName
  private val findInCountryTimer: Timer = registry.timer(name(prefix, "findInCountry"))

  private def timerStop[T](t: Context, r: T) = {
    t.stop()
    r
  }

  override def findInCountry(countryCode: String, filter: String): Future[List[NonUKAddress]] = {
    val context = findInCountryTimer.time()
    peer.findInCountry(countryCode, filter) map (timerStop(context, _))
  }
}
