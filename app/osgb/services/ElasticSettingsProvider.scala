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

import config.ConfigHelper
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.address.services.es.ElasticSettings

@Singleton
class ElasticSettingsProvider @Inject() (configuration: Configuration, configHelper: ConfigHelper) {
  lazy val numShards: Map[String, Int] = configuration.getConfig("elastic.shards").map(
    _.entrySet.foldLeft(Map.empty[String, Int])((m, a) => m + (a._1 -> a._2.unwrapped().asInstanceOf[Int]))
  ).getOrElse(Map.empty[String, Int])

  lazy val elasticSettings: ElasticSettings = {
    val localMode = configHelper.getConfigString("elastic.localMode").exists(_.toBoolean)
    val homeDir = configHelper.getConfigString("elastic.homeDir")
    val preDelete = configHelper.getConfigString("elastic.preDelete").exists(_.toBoolean)
    val clusterName = configHelper.mustGetConfigString("elastic.clusterName")
    val connectionString = configHelper.mustGetConfigString("elastic.uri")
    val isCluster = configHelper.getConfigString("elastic.isCluster").exists(_.toBoolean)
    ElasticSettings(localMode, homeDir, preDelete, connectionString, isCluster, clusterName, numShards)
  }
}