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

import osgb.SearchParameters
import play.api.Environment
import uk.gov.hmrc.address.osgb.DbAddress
import uk.gov.hmrc.address.services.CsvLineSplitter
import uk.gov.hmrc.address.uk.{Outcode, Postcode}

import javax.inject.Inject
import scala.collection.JavaConverters.asScalaIteratorConverter
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source

class InMemoryAddressLookupRepository @Inject()(env: Environment, ec: ExecutionContext) extends AddressSearcher {
  val cannedData = "conf/data/testaddresses.csv"

  lazy val (dbAddresses, dbAddressesStr) = {
    val file = env.getExistingFile(cannedData).getOrElse {
      throw new Exception("Missing " + cannedData)
    }

    val splitter = new CsvLineSplitter(Source.fromFile(file).bufferedReader()).asScala.toSeq
    val dbas = splitter.map(CSV.convertCsvLine)
    val dbass = dbas.map { dba => (dba.lines.mkString(" ") + " " + dba.town.getOrElse("")).replaceAll("\\p{Space}+", " ") -> dba }.toMap
    (dbas, dbass)
  }

  override def findID(id: String): Future[Option[DbAddress]] =
    Future.successful(dbAddresses.find(_.id == id))

  override def findUprn(uprn: String): Future[List[DbAddress]] =
    Future.successful(dbAddresses.filter(_.uprn == uprn.toLong).toList)

  override def findPostcode(postcode: Postcode, filter: Option[String]): Future[List[DbAddress]] =
    Future.successful(dbAddresses.filter(_.postcode == postcode.toString).toList)

  override def findOutcode(outcode: Outcode, filter: String): Future[List[DbAddress]] =
    Future.successful(dbAddresses.filter(_.postcode.startsWith(outcode.toString)).toList)

  override def searchFuzzy(sp: SearchParameters): Future[List[DbAddress]] = {
    val filter = for {
      f <- Option(sp.lines).map(_.mkString(" ")).orElse(Some(" "))
      t <- sp.town.orElse(Some(" "))
    } yield (f + " " + t).trim.replaceAll("\\p{Space}+", " ")
    if (sp.postcode.isDefined) findPostcode(sp.postcode.get, filter)
    else {
      Future.successful {
        val filterTokens = filter.map(_.split(" ")).map(_.toSet)
        filterTokens.map(tokens =>
          dbAddressesStr.filterKeys(addrStr => tokens.forall(t => addrStr.contains(t))).values.toList
        ).getOrElse(List())
      }
    }
  }
}
