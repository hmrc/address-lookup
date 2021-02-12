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

package it.helper

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ElasticClient, RichGetResponse, RichSearchResponse}
import org.elasticsearch.common.unit.TimeValue
import org.scalatest._
import org.scalatestplus.play.ServerProvider
import osgb.services.AddressSearcher
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.TestServer
import uk.gov.hmrc.address.osgb.DbAddress
import uk.gov.hmrc.address.services.es.{ESSchema, ElasticDiskClientSettings, ElasticsearchHelper}
import uk.gov.hmrc.address.uk.Postcode

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait AppServerWithES extends SuiteMixin with ServerProvider {
  this: Suite =>

  val esDataPath: String = System.getProperty("java.io.tmpdir") + "/es"

  lazy val esClient: ElasticClient = ElasticsearchHelper.buildDiskClient(ElasticDiskClientSettings(esDataPath, preDelete=true))

  def beforeAppServerStarts() {
    esClient execute {
      ESSchema.createIndexDefinition(idx, doc,
        ESSchema.Settings(1, 0, "1s"))
    } await()

    waitForEsIndex(idx)
  }

  def afterAppServerStops() {
    //    FileUtils.deleteDir(new File(esDataPath))
  }

  //-----------------------------------------------------------------------------------------------

  val idx = "es_integration"
  val doc = "address"

  def waitForEsIndex(idx: String, timeout: TimeValue = TimeValue.timeValueSeconds(2)) {
    esClient.java.admin.cluster.prepareHealth(idx).setWaitForGreenStatus().setTimeout(timeout).get
  }

  def finaliseInserts(idx: String) {
    esClient execute {
      update settings idx set Map(
        "index.refresh_interval" -> "1s"
      )
    } await()
  }

  def esOutput(a: DbAddress) {
    val tuples = a.forElasticsearch
    esClient execute {
      index into idx -> doc fields tuples id a.id routing a.postcode
    }
  }

  private def convertGetResponse(response: RichGetResponse): List[DbAddress] = {
    List(DbAddress(response.fields))
  }

  private def convertSearchResponse(response: RichSearchResponse): List[DbAddress] = {
    response.hits.map(hit => DbAddress(hit.sourceAsMap)).toList
  }

  //  private def findID(idStr: String): Future[List[DbAddress]] = {
  //    val getResponse = esClient.execute {
  //      get id idStr from idx -> doc routing "postcode"
  //    }
  //    getResponse map convertGetResponse
  //  }

  private def findID(id: String): Future[List[DbAddress]] = {
    val searchResponse = esClient.execute {
      search in idx -> doc query matchQuery("id", id)
    }
    searchResponse map convertSearchResponse
  }

  def findPostcode(postcode: Postcode): Future[List[DbAddress]] = {
    val searchResponse = esClient.execute {
      search in idx -> doc query matchQuery("postcode.raw", postcode.toString) routing postcode.toString size 100
    }
    searchResponse map convertSearchResponse
  }

  //-----------------------------------------------------------------------------------------------

  private lazy val appConfiguration = Map(
    "app.store" -> "elasticsearch",
    "elastic.localMode" -> "true",
    "elastic.indexName" -> idx,
    "mongodb.cannedData" -> "false")

//  implicit override final lazy val app: Application = GuiceApplicationBuilder().configure(appConfiguration).build()

  implicit override final lazy val app: Application = {
    import play.api.inject.bind

    GuiceApplicationBuilder()
      .overrides(bind[ESTestConfig].toSelf)
      //.overrides(bind[ESTestAddressSearcher].toSelf)
      .overrides(bind[AddressSearcher].to[ESTestAddressSearcherMetrics])
      .configure(appConfiguration).build()
  }
  /**
    * The port used by the `TestServer`.  By default this will be set to the result returned from
    * `Helpers.testServerPort`. You can override this to provide a different port number.
    */
  lazy val port: Int = 19001 // Helpers.testServerPort

  lazy val appEndpoint = s"http://localhost:$port"

  abstract override def run(testName: Option[String], args: Args): Status = {
    println("********** AppServerWithES ********** " + getClass.getSimpleName)
    Thread.sleep(10)
    beforeAppServerStarts()
    val testServer = TestServer(port, app)
    testServer.start()
    try {
      val newConfigMap = args.configMap + ("org.scalatestplus.play.app" -> app) + ("org.scalatestplus.play.port" -> port)
      val newArgs = args.copy(configMap = newConfigMap)
      val status = super.run(testName, newArgs)
      status.waitUntilCompleted()
      status
    }
    finally {
      testServer.stop()
      afterAppServerStops()
    }
  }
}

