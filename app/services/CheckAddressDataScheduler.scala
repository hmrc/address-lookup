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

import akka.actor.ActorSystem
import config.ConfigHelper
import model.address.Postcode
import play.api.Logger
import play.api.inject.ApplicationLifecycle
import repositories.ABPAddressRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

@Singleton
class CheckABPMainlandDataJob @Inject()(addressSearch: ABPAddressRepository) extends ScheduledJob {
  private val logger = Logger(this.getClass.getSimpleName)

  override def name: String = "check-abp-mainland-data"

  override def execute(implicit ec: ExecutionContext): Future[Result] =
    addressSearch.findPostcode(Postcode("W14 9HR"), None).map { results =>
      if (results.nonEmpty) {
        val message = "Mainland address data integrity check PASSED"
        logger.info(message)
        Result(message)
      } else {
        val message = "Mainland address data integrity check FAILED"
        logger.error(message)
        Result(message)
      }
    }

  override def initialDelay: FiniteDuration = 1 minute

  override def interval: FiniteDuration = 15 minutes
}

@Singleton
class CheckABPIslandsDataJob @Inject()(addressSearch: ABPAddressRepository) extends ScheduledJob {
  private val logger = Logger(this.getClass.getSimpleName)

  override def name: String = "check-abp-mainland-data"

  override def execute(implicit ec: ExecutionContext): Future[Result] =
    addressSearch.findPostcode(Postcode("W14 9HR"), None).map { results =>
      if (results.nonEmpty) {
        val message = "Mainland address data integrity check PASSED"
        logger.info(message)
        Result(message)
      } else {
        val message = "Mainland address data integrity check FAILED"
        logger.error(message)
        Result(message)
      }
    }

  override def initialDelay: FiniteDuration = 1 minute

  override def interval: FiniteDuration = 15 minutes
}

@Singleton
class CheckAddressDataScheduler @Inject()(actorSystem: ActorSystem,
                                          lifecycle: ApplicationLifecycle,
                                          configHelper: ConfigHelper,
                                          checkABPMainlandDataJob: CheckABPMainlandDataJob,
                                          checkABPIslandsDataJob: CheckABPIslandsDataJob)(implicit ec: ExecutionContext) {

  private val logger = Logger(this.getClass.getSimpleName)

  lazy val scheduledJobs: Seq[ScheduledJob] = Seq(checkABPMainlandDataJob, checkABPIslandsDataJob)

  lifecycle.addStopHook(() =>
    Future {
      actorSystem.terminate()
    })

  def enable(): Unit = {
    if (configHelper.isCipPaasDbEnabled()) {
      scheduledJobs.foreach(scheduleJob)
      logger.info("Address data integrity check scheduled jobs enabled")
    } else {
      logger.warn("Address data integrity checks not in use because db is not enabled")
    }
  }

  def scheduleJob(job: ScheduledJob)(implicit ec: ExecutionContext): Unit =
    actorSystem.scheduler.scheduleWithFixedDelay(job.initialDelay, job.interval)(() => job.execute)

}
