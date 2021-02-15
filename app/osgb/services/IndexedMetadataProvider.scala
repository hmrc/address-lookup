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

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.address.services.es.{ESAdminImpl, ElasticsearchHelper, IndexMetadata}
import uk.gov.hmrc.logging.{LoggerFacade, SimpleLogger}

import scala.concurrent.ExecutionContext

@Singleton
class IndexedMetadataProvider @Inject()(settingsProvider: ElasticSettingsProvider, logger: SimpleLogger,
                                        ec: ExecutionContext) {

  lazy val indexedMetaData: IndexMetadata = {
    val numShards = settingsProvider.numShards
    val settings = settingsProvider.elasticSettings

    val clients = ElasticsearchHelper.buildClients(settings, new LoggerFacade(play.api.Logger.logger))
    val esImpl = new ESAdminImpl(clients, logger, ec, settings)
    new IndexMetadata(esImpl, settings.isCluster, numShards, logger, ec)
  }
}