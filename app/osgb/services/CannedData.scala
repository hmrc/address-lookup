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

package osgb.services

import java.io.File
import javax.inject.Inject

import config.ConfigHelper
import play.api.{Environment, Logger}
import uk.gov.hmrc.BuildProvenance
import uk.gov.hmrc.address.services.CsvLineSplitter
import uk.gov.hmrc.address.services.es._
import uk.gov.hmrc.address.services.writers.{OutputESWriter, WriterSettings}
import uk.gov.hmrc.logging.SimpleLogger

import scala.concurrent.ExecutionContext
import scala.io.Source

trait CannedData

class CannedDataImpl @Inject()(indexMetadata: IndexMetadata, logger: SimpleLogger, env: Environment, configHelper: ConfigHelper, ec: ExecutionContext) extends CannedData {
  val cannedData = "conf/data/testaddresses.csv"

  def upload(): String = {
    val file = env.getExistingFile(cannedData).getOrElse {
      throw new Exception("Missing " + cannedData)
    }
    uploadToElasticSearch(file)
  }

  def uploadToElasticSearch(file: File): String = {
    val indexName = IndexName("test", Some(1), Some(IndexName.newTimestamp))
    val noProvenance = BuildProvenance(None, None)
    val out = new OutputESWriter(indexName, logger, indexMetadata, WriterSettings.default, ec, noProvenance)

    out.begin()

    var count = 0
    var duplicates = 0
    val splitter = new CsvLineSplitter(Source.fromFile(file).bufferedReader())
    while (splitter.hasNext) {
      val strings = splitter.next()
      val address = CSV.convertCsvLine(strings)
      out.output(address)
      count += 1
    }
    out.end(true)

    indexMetadata.setIndexInUse(indexName)

    val status =
      s"""
         |Bulk inserted ${file.getName} containing $count lines.
         |$indexName contains $count documents.""".stripMargin
    Logger.info(status)
    status
  }

  if (configHelper.getConfigString("elastic.cannedData").exists(_.toBoolean)) upload()

}
