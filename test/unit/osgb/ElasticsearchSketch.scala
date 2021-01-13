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

package osgb

import osgb.services.{AddressESSearcher, AddressSearcher}
import play.api.Logger
import uk.gov.hmrc.address.osgb.DbAddress
import uk.gov.hmrc.address.services.es._
import uk.gov.hmrc.address.uk.{Outcode, Postcode}
import uk.gov.hmrc.logging.{LoggerFacade, Stdout}
import uk.gov.hmrc.util.JacksonMapper

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.{Duration, FiniteDuration}

// for manual test/development
object ElasticsearchSketch {
  private implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  def main(args: Array[String]) {

    val indexName = IndexMetadata.ariAliasName
    val logger = Stdout
    val clusterName = "address-reputation"
    val connectionString = "elasticsearch://localhost:9300"
    val isCluster = false
    val numShards = Map.empty[String, Int]

    val settings = ElasticSettings(false, None, false, connectionString, isCluster, clusterName, numShards)
    val clients = ElasticsearchHelper.buildClients(settings, logger)
    val esImpl = new ESAdminImpl(clients, logger, ec, settings)
    val indexMetadata: IndexMetadata = new IndexMetadata(esImpl, isCluster, numShards, logger, ec)

    val searcher = new AddressESSearcher(indexMetadata.clients.head, indexName, "GB", ec, settings, logger)

    idSample("GB123456", searcher)
    uprnSample("123456", searcher)
    postcodeSample("FX1 9PY", searcher)
    outcodeSample("FX1", "Dorset", searcher)
    outcodeSample("FX2", "2", searcher)
    fuzzySample(SearchParameters(fuzzy = Some("Dorset"), postcode = Postcode.cleanupPostcode("FX1 9PY")), searcher)
    fuzzySample(SearchParameters(fuzzy = Some("Elm"), postcode = Postcode.cleanupPostcode("FX51 7UR")), searcher)
  }

  private def idSample(id: String, searcher: AddressSearcher) {
    report(s"ID $id", searcher.findID(id).map(_.toList), 3)
  }

  private def uprnSample(uprn: String, searcher: AddressSearcher) {
    report(s"UPRN $uprn", searcher.findUprn(uprn), 3)
  }

  private def postcodeSample(postcode: String, searcher: AddressSearcher) {
    report(s"Postcode $postcode", searcher.findPostcode(Postcode(postcode), None), 3)
  }

  private def outcodeSample(outcode: String, filter: String, searcher: AddressSearcher) {
    report(s"Outcode $outcode & $filter", searcher.findOutcode(Outcode(outcode), filter), 3)
  }

  private def fuzzySample(sp: SearchParameters, searcher: AddressSearcher) {
    val info = JacksonMapper.writeValueAsString(sp)
    report(s"Fuzzy $info", searcher.searchFuzzy(sp), 3)
  }

  private def report(title: String, output: Future[List[DbAddress]], max: Int) {
    println(title)
    println(PrettyMapper.writeValueAsString(await(output).take(max)))
    println()
  }

  private def await[T](future: Future[T], timeout: Duration = FiniteDuration(10, "s")): T = Await.result(future, timeout)
}
