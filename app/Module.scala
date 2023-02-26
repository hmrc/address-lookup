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

import com.google.inject.{AbstractModule, Provides}
import com.kenshoo.play.metrics.Metrics
import config.ConfigHelper
import model.response.SupportedCountryCodes
import play.api.inject.ApplicationLifecycle
import play.api.{Configuration, Environment}
import repositories._
import services.{ABPAddressRepositoryMetrics, NonABPAddressRepositoryMetrics, ReferenceData}

import javax.inject.Singleton
import scala.concurrent.ExecutionContext

class Module(environment: Environment, configuration: Configuration) extends AbstractModule {

  override def configure(): Unit = {}

  @Provides
  @Singleton
  def provideSupportedCountryCodes(configuration: Configuration): SupportedCountryCodes =
    configuration.get[SupportedCountryCodes]("supported-country-codes")

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
  def provideAbpAddressRepository(metrics: Metrics, configHelper: ConfigHelper,
                                  rdsQueryConfig: RdsQueryConfig,
                                  applicationLifecycle: ApplicationLifecycle,
                                  inMemoryABPAddressRepository: InMemoryABPAddressRepository)
                                 (implicit executionContext: ExecutionContext) : ABPAddressRepository = {

    val dbEnabled = configHelper.isCipPaasDbEnabled()

    val repository: ABPAddressRepository = if (dbEnabled) {
      val transactor = new TransactorProvider(configuration, applicationLifecycle, "cip-address-lookup-rds").get(executionContext)
      new PostgresABPAddressRepository(transactor, rdsQueryConfig)
    } else {
      inMemoryABPAddressRepository
    }

    new ABPAddressRepositoryMetrics(repository, metrics.defaultRegistry, executionContext)
  }

  @Provides
  @Singleton
  def provideNonAbpAddressRepository(metrics: Metrics, configuration: Configuration, configHelper: ConfigHelper,
                                     rdsQueryConfig: RdsQueryConfig, executionContext: ExecutionContext,
                                     applicationLifecycle: ApplicationLifecycle,
                                     inMemoryNonABPAddressRepository: InMemoryNonABPAddressRepository): NonABPAddressRepository = {

    val dbEnabled = configHelper.isCipPaasDbEnabled()

    val repository: NonABPAddressRepository = if (dbEnabled) {
      val transactor = new TransactorProvider(configuration, applicationLifecycle, "cip-address-lookup-rds").get(executionContext)
      new PostgresNonABPAddressRepository(transactor, rdsQueryConfig)
    } else {
      inMemoryNonABPAddressRepository
    }

    new NonABPAddressRepositoryMetrics(repository, metrics.defaultRegistry, executionContext)
  }

  @Provides
  @Singleton
  def providesReferenceData(configHelper: ConfigHelper): ReferenceData =
    ReferenceData.load(configHelper.mustGetConfigString("lcc.table"))
}
