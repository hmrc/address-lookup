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

import bfpo.BFPOFileParser
import bfpo.outmodel.BFPO
import cats.effect.IO
import com.google.inject.{AbstractModule, Provides}
import com.kenshoo.play.metrics.Metrics
import config.ConfigHelper
import doobie.Transactor

import javax.inject.Singleton
import osgb.services._
import play.api.inject.ApplicationLifecycle
import play.api.{Configuration, Environment, Logger}
import repositories.{AddressLookupRepository, InMemoryAddressLookupRepository, RdsQueryConfig, TransactorProvider}

import scala.concurrent.ExecutionContext

class Module(environment: Environment, configuration: Configuration) extends AbstractModule {

  def configure(): Unit = {}

  @Provides
  @Singleton
  def provideRdsQueryConfig(configHelper: ConfigHelper): RdsQueryConfig = {
    val queryTimeoutMillis =
      configHelper.getConfigString("address-lookup-rds.query-timeout-ms").map(_.toInt).getOrElse(10000)
    val queryResultsLimit =
      configHelper.getConfigString("address-lookup-rds.query-results-limit").map(_.toInt).getOrElse(300)

    RdsQueryConfig(queryTimeoutMillis, queryResultsLimit)
  }

  @Provides
  @Singleton
  def provideTransactorOptional(configHelper: ConfigHelper, configuration: Configuration,
                                applicationLifecycle: ApplicationLifecycle,
                                executionContext: ExecutionContext): Option[Transactor[IO]] = {
    if (isDbEnabled(configHelper))
      Some(new TransactorProvider(configuration, applicationLifecycle).get(executionContext))
    else None
  }

  @Provides
  @Singleton
  def provideAddressSearcher(metrics: Metrics, configuration: Configuration,
                             configHelper: ConfigHelper, rdsQueryConfig: RdsQueryConfig, executionContext: ExecutionContext, applicationLifecycle: ApplicationLifecycle): AddressSearcher = {
    val dbEnabled = isDbEnabled(configHelper)

    val searcher = if (dbEnabled) {
      val transactor = new TransactorProvider(configuration, applicationLifecycle).get(executionContext)
      new AddressLookupRepository(transactor, rdsQueryConfig)
    } else {
      new InMemoryAddressLookupRepository(environment, executionContext)
    }

    new AddressSearcherMetrics(searcher, metrics.defaultRegistry, executionContext)
  }

  private def isDbEnabled(configHelper: ConfigHelper): Boolean =
    configHelper.getConfigString("address-lookup-rds.enabled").getOrElse("false").toBoolean

  @Provides
  @Singleton
  def provideBFPOList(configHelper: ConfigHelper): List[BFPO] = BFPOFileParser.loadResource(configHelper
    .mustGetConfigString("bfpo.data"))

  @Provides
  @Singleton
  def providesReferenceData(configHelper: ConfigHelper): ReferenceData =
    ReferenceData.load(configHelper.mustGetConfigString("lcc.table"))
}
