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
package it.helper

import com.kenshoo.play.metrics.Metrics
import com.sksamuel.elastic4s.ElasticClient
import config.ConfigHelper
import osgb.services.{AddressESSearcher, AddressSearcher, AddressSearcherMetrics}
import play.api.Configuration
import uk.gov.hmrc.address.services.es.{ESAdminImpl, ElasticSettings, ElasticsearchHelper, IndexMetadata}
import uk.gov.hmrc.logging.{LoggerFacade, SimpleLogger}

import scala.concurrent.ExecutionContext
import javax.inject.{Inject, Singleton}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

@Singleton
class ESTestConfig @Inject()(configuration: Configuration,configHelper: ConfigHelper, logger: SimpleLogger, ec: ExecutionContext) {
  private lazy val numShards = configuration.getConfig("elastic.shards").map(
    _.entrySet.foldLeft(Map.empty[String, Int])((m, a) => m + (a._1 -> a._2.unwrapped().asInstanceOf[Int]))
  ).getOrElse(Map.empty[String, Int])

  lazy val settings: ElasticSettings = {
    val localMode = configHelper.getConfigString("elastic.localMode").exists(_.toBoolean)
    val homeDir = configHelper.getConfigString("elastic.homeDir")
    val preDelete = configHelper.getConfigString("elastic.preDelete").exists(_.toBoolean)
    val clusterName = configHelper.mustGetConfigString("elastic.clusterName")
    val connectionString = configHelper.mustGetConfigString("elastic.uri")
    val isCluster = configHelper.getConfigString("elastic.isCluster").exists(_.toBoolean)
    ElasticSettings(localMode, homeDir, preDelete, connectionString, isCluster, clusterName, numShards)
  }

 lazy val indexMetadata: IndexMetadata = {
    val clients = ElasticsearchHelper.buildClients(settings, new LoggerFacade(play.api.Logger.logger))
    val esImpl = new ESAdminImpl(clients, logger, ec, settings)
    new IndexMetadata(esImpl, settings.isCluster, numShards, logger, ec)
  }

  val indexName: String = configHelper.getConfigString("elastic.indexName").getOrElse(IndexMetadata.ariAliasName)
  val esSearcher = new AddressESSearcher(indexMetadata.clients.head, indexName, "GB", defaultContext, settings, logger)
}

class ESTestAddressSearcherMetrics @Inject() (config: ESTestConfig, metrics: Metrics, ec: ExecutionContext)
extends AddressSearcherMetrics(config.esSearcher, metrics.defaultRegistry, ec)
