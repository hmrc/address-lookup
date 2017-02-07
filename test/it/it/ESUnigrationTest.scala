/*
 * Copyright 2017 HM Revenue & Customs
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

package it

import bfpo.outmodel.BFPOReadWrite
import com.sksamuel.elastic4s.ElasticDsl._
import it.helper.{AppServerWithES, PSuites}
import it.suites._
import org.scalatest._
import osgb.services.CSV
import play.api.test.WsTestClient
import uk.gov.hmrc.address.osgb.DbAddress
import uk.gov.hmrc.address.services.CsvParser

//***** For the time being, either this test must be marked DoNotDiscover, or MongoUnigrationTest must be.
//@DoNotDiscover
class ESUnigrationTest extends WordSpec with AppServerWithES with SequentialNestedSuiteExecution {
  implicit val bfpoReadWrite = BFPOReadWrite.AddressReads

  import suites.FixturesV1._

  val largePostcodeExample1 = 2517
  val largePostcodeExample2 = 3001

  override def beforeAppServerStarts() {
    super.beforeAppServerStarts()

    // these three samples are simple
    esOutput(db_fx9_9py)
    esOutput(db_fx1_6jn_a)
    esOutput(db_fx1_6jn_b)
    esOutput(db_fx1_2tb)

    // this sample set is larger and comes from a file
    for (strings <- CsvParser.split(fx11pgText)) {
      val jsonLine = CSV.convertCsvLine(strings)
      esOutput(jsonLine)
    }

    // For interest, the largest number of UPRNs is 2517 UPRNs for a single post code.
    val largeInsert1 = for (i <- 1 to largePostcodeExample1) yield {
      val dba = DbAddress(s"GB100$i", List(s"Flat $i", "A Apartments", "ARoad"), Some("ATown"), "FX4 7AL",
        Some("GB-ENG"), Some("UK"), Some(3725), Some("en"), Some(2), Some(1), None, None, None)
      esOutput(dba)
      index into idx -> doc fields dba.forElasticsearch id dba.id routing dba.postcode
    }

    esClient execute {
      bulk(largeInsert1)
    }

    // A fictitious set that is larger than the max limit
    val largeInsert2 = for (i <- 1 to largePostcodeExample2) yield {
      val dba = DbAddress(s"GB200$i", List(s"$i Bankside"), Some("ATown"), "FX4 7AJ",
        Some("GB-ENG"), Some("UK"), Some(3725), Some("en"), Some(2), Some(1), None, None, None)
      esOutput(dba)
      index into idx -> doc fields dba.forElasticsearch id dba.id routing dba.postcode
    }

    esClient execute {
      bulk(largeInsert2)
    }

    Thread.sleep(800)
    finaliseInserts(idx)
    waitForEsIndex(idx)
  }

  override def runNestedSuites(args: Args): Status = {
    WsTestClient.withClient { wsClient =>
      val s = new PSuites(
        new PostcodeLookupSuiteV1(wsClient, appEndpoint, largePostcodeExample1)(app),
        new PostcodeLookupSuiteV2(wsClient, appEndpoint, largePostcodeExample1)(app),
        new OutcodeLookupSuiteV1(wsClient, appEndpoint)(app),
        new OutcodeLookupSuiteV2(wsClient, appEndpoint)(app),
        new UprnLookupSuiteV1(wsClient, appEndpoint)(app),
        new UprnLookupSuiteV2(wsClient, appEndpoint)(app),
        new IdLookupSuiteV1(wsClient, appEndpoint)(app),
        new IdLookupSuiteV2(wsClient, appEndpoint)(app),
        new BfpoLookupSuite(wsClient, appEndpoint)(app),
        new FuzzySearchSuiteV2(wsClient, appEndpoint)(app),
        new MetricsSuiteV1(wsClient, appEndpoint, "AddressESSearcher")(app),
        new MetricsSuiteV2(wsClient, appEndpoint, "AddressESSearcher")(app),
        new PingSuite(wsClient, appEndpoint)(app)
      )
      s.runNestedSuites(args)
    }
  }
}
