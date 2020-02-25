/*
 * Copyright 2020 HM Revenue & Customs
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
import com.google.inject.{AbstractModule, Provides}
import javax.inject.Singleton
import com.kenshoo.play.metrics.Metrics
import config.ConfigHelper
import osgb.services._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.{Configuration, Environment}
import uk.gov.hmrc.address.services.es.{ESAdminImpl, ElasticSettings, ElasticsearchHelper, IndexMetadata}
import uk.gov.hmrc.logging.{LoggerFacade, SimpleLogger}

import scala.concurrent.ExecutionContext

class Module(environment: Environment,
             configuration: Configuration) extends AbstractModule {

  private lazy val numShards = configuration.getConfig("elastic.shards").map(
    _.entrySet.foldLeft(Map.empty[String, Int])((m, a) => m + (a._1 -> a._2.unwrapped().asInstanceOf[Int]))
  ).getOrElse(Map.empty[String, Int])

  def configure(): Unit = {
    bind(classOf[CannedData]).to(classOf[CannedDataImpl]).asEagerSingleton()
  }

  @Provides @Singleton
  def provideLogger: SimpleLogger = new LoggerFacade(play.api.Logger.logger)

  @Provides @Singleton
  def provideElasticSettings(configHelper: ConfigHelper): ElasticSettings = {
    val localMode = configHelper.getConfigString("elastic.localMode").exists(_.toBoolean)
    val homeDir = configHelper.getConfigString("elastic.homeDir")
    val preDelete = configHelper.getConfigString("elastic.preDelete").exists(_.toBoolean)
    val clusterName = configHelper.mustGetConfigString("elastic.clusterName")
    val connectionString = configHelper.mustGetConfigString("elastic.uri")
    val isCluster = configHelper.getConfigString("elastic.isCluster").exists(_.toBoolean)
    ElasticSettings(localMode, homeDir, preDelete, connectionString, isCluster, clusterName, numShards)
  }

  @Provides @Singleton
  def provideIndexMetadata(configHelper: ConfigHelper, logger: SimpleLogger, ec: ExecutionContext, settings: ElasticSettings): IndexMetadata = {
    val clients = ElasticsearchHelper.buildClients(settings, new LoggerFacade(play.api.Logger.logger))
    val esImpl = new ESAdminImpl(clients, logger, ec, settings)
    new IndexMetadata(esImpl, settings.isCluster, numShards, logger, ec)
  }

  @Provides @Singleton
  def provideAddressSearcher(indexMetadata: IndexMetadata, metrics: Metrics, configHelper: ConfigHelper, settings: ElasticSettings, logger: SimpleLogger): AddressSearcher = {
    val indexName: String = configHelper.getConfigString("elastic.indexName").getOrElse(IndexMetadata.ariAliasName)

    def elasticSearcher: AddressSearcher = new AddressESSearcher(indexMetadata.clients.head, indexName, "GB", defaultContext, settings, logger)

    new AddressSearcherMetrics(elasticSearcher, metrics.defaultRegistry, defaultContext)
  }

  @Provides @Singleton
  def provideBFPOList(configHelper: ConfigHelper): List[BFPO] = BFPOFileParser.loadResource(configHelper.mustGetConfigString("bfpo.data"))

  @Provides @Singleton
  def providesReferenceData(configHelper: ConfigHelper): ReferenceData =
    ReferenceData.load(configHelper.mustGetConfigString("lcc.table"), configHelper.mustGetConfigString("lcc.counties"))
}
