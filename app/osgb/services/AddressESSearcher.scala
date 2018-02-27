/*
 * Copyright 2018 HM Revenue & Customs
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

import com.sksamuel.elastic4s._
import org.elasticsearch.index.query.MatchQueryBuilder
import osgb.SearchParameters
import uk.gov.hmrc.address.osgb.DbAddress
import uk.gov.hmrc.address.services.es.{ElasticClientWrapper, ElasticSettings}
import uk.gov.hmrc.address.uk.{Outcode, Postcode}
import uk.gov.hmrc.logging.SimpleLogger

import scala.concurrent.{ExecutionContext, Future}

class AddressESSearcher(client: ElasticClient,
                        index: String,
                        idPrefix: String,
                        ec: ExecutionContext,
                        settings: ElasticSettings,
                        logger: SimpleLogger)
  extends AddressSearcher with ElasticDsl {

  import AddressESSearcher._

  private implicit val xec = ec
  private val target = index -> ariDocumentName
  private val wrapped = new ElasticClientWrapper(List(client), settings, logger)

  def findUprn(uprn: String): Future[List[DbAddress]] = doFindID(idPrefix + uprn)

  def findID(id: String): Future[Option[DbAddress]] =
    doFindID(id).map {
      list =>
        require(list.size <= 1, s"Expected single item but got list of ${list.size} for '$id'")
        list.headOption
    }

  private def doFindID(id: String): Future[List[DbAddress]] = {
    wrapped.withReinitialization(0, 3) {
      val c = wrapped.clients.head
      val searchResponse = c.execute {
        search in target query matchQuery("id", id)
      }
      searchResponse map convertSearchResponse
    }
  }

  //-----------------------------------------------------------------------------------------------

  def findPostcode(postcode: Postcode, filter: Option[String]): Future[List[DbAddress]] = {
    val builder = search in target
    val postcodeString = postcode.toString
    val postcodeMatchQuery = matchQuery("postcode.raw", postcodeString)

    def queryFilter(fs: String) = matchPhraseQuery("lines", fs) operator MatchQueryBuilder.Operator.AND

    val searchQuery =
      if (filter.isDefined) builder query bool {
        must(postcodeMatchQuery) filter queryFilter(filter.get.trim)
      } routing postcodeString
      else builder query postcodeMatchQuery routing postcodeString

    doSearchWithRetry(searchQuery)
  }

  //-----------------------------------------------------------------------------------------------
  // Although intended for searching by outcode, this also happens to work for searching by incode.
  // The filter must not be blank.

  def findOutcode(outcode: Outcode, filter: String): Future[List[DbAddress]] = {
    val builder = search in target
    val postcodeMatchQuery = matchQuery("postcode", outcode.toString)

    def queryFilter(fs: String) = matchPhraseQuery("lines", fs) operator MatchQueryBuilder.Operator.AND

    val searchQuery =
      builder query bool {
        must(postcodeMatchQuery) filter queryFilter(filter.trim)
      }

    doSearchWithRetry(searchQuery)
  }

  //-----------------------------------------------------------------------------------------------

  def searchFuzzy(sp: SearchParameters): Future[List[DbAddress]] = {
    if (sp.fuzzy.isDefined) searchFuzzy(sp.fuzzy.get, sp.postcode, sp.filter)
    else searchArbitrary(sp.lines, sp.postcode, sp.town)
  }

  private def searchFuzzy(searchCriteria: String, postcode: Option[Postcode], filterStr: Option[String]): Future[List[DbAddress]] = {
    val builder = search in target

    def fuzzy = must(matchQuery("lines", searchCriteria) fuzziness "AUTO" operator MatchQueryBuilder.Operator.AND)

    def postcodeMatch(pc: Postcode) = matchQuery("postcode.raw", pc.toString)

    def queryFilter(fs: String) = matchPhraseQuery("lines", fs) operator MatchQueryBuilder.Operator.AND

    val searchQuery =
      (postcode, filterStr) match {
        case (Some(pc), None)     => builder query bool { fuzzy must postcodeMatch(pc) } routing pc.toString
        case (Some(pc), Some(fs)) => builder query bool { fuzzy must postcodeMatch(pc) filter queryFilter(fs.trim) } routing pc.toString
        case (None,     Some(fs)) => builder query bool { fuzzy filter queryFilter(fs.trim) }
        case (None,     None)     => builder query fuzzy
      }

    doSearchWithRetry(searchQuery)
  }

  private def searchArbitrary(lines: Seq[String], postcode: Option[Postcode], town: Option[String]): Future[List[DbAddress]] = {

    //TODO this is largely incomplete

    val builder = search in target

    def fuzzy = must(matchQuery("lines", lines.head) fuzziness "AUTO" operator MatchQueryBuilder.Operator.AND)

    def postcodeMatch(pc: Postcode) = matchQuery("postcode.raw", pc.toString)

    def townFilter(fs: String) = matchQuery("town", fs)

    val searchQuery =
      (postcode, town) match {
        case (Some(pc), None)     => builder query bool { fuzzy must postcodeMatch(pc) } routing pc.toString
        case (Some(pc), Some(fs)) => builder query bool { fuzzy must postcodeMatch(pc) filter townFilter(fs.trim) } routing pc.toString
        case (None,     Some(fs)) => builder query bool { fuzzy filter townFilter(fs.trim) }
        case (None,     None)     => builder query fuzzy
      }

    doSearchWithRetry(searchQuery)
  }

  //-----------------------------------------------------------------------------------------------

  // doSearchWithRetry
  // -----------------
  // It is more performant to search with a small size limit. Most result sets fall into this
  // category so that's what we do in the first instance.
  //
  // However, some result sets are (much) larger, so we re-run the same search in their case,
  // using the actual number of hits to allocate the size limit precisely.
  //
  // (An alternative strategy, not used here, might be to apply DB pagination to the results.)

  private def doSearchWithRetry(searchQuery: SearchDefinition): Future[List[DbAddress]] = {
    val fuOutcome1 = doSearch(searchQuery, resultSetSizeHint)

    fuOutcome1 flatMap {
      outcome1 =>
        if (outcome1.actualHits <= resultSetSizeHint) {
          Future.successful(outcome1.converted)
        } else {
          // make the same query but using much larger size allocation
          val limit = if (outcome1.actualHits < resultSetSizeMax) outcome1.actualHits else resultSetSizeMax
          val fuOutcome2 = doSearch(searchQuery, limit)
          fuOutcome2 map (_.converted)
        }
    }
  }

  private def doSearch(searchQuery: SearchDefinition, max: Int): Future[SearchOutcome] = {
    wrapped.withReinitialization(0, 3) {
      val c = wrapped.clients.head
      val searchResponse = c.execute {
        searchQuery size max
      }
      searchResponse map (r => SearchOutcome(r.totalHits.toInt, r.hits))
    }
  }

  //-----------------------------------------------------------------------------------------------

  private def convertGetResponse(response: RichGetResponse): List[DbAddress] = {
    List(DbAddress(response.fields))
  }

  private def convertSearchResponse(response: RichSearchResponse): List[DbAddress] = {
    response.hits.map(hit => DbAddress.apply(hit.sourceAsMap)).toList
  }

  private case class SearchOutcome(actualHits: Int, hits: Seq[RichSearchHit]) {
    def converted: List[DbAddress] = {
      hits.map(hit => DbAddress.apply(hit.sourceAsMap)).toList
    }
  }

}


object AddressESSearcher {
  val ariDocumentName = "address"
  val resultSetSizeHint = 100
  val resultSetSizeMax = 3000
}
