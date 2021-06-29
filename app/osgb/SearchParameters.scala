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

import address.uk.{Outcode, Postcode}
import osgb.inmodel.LookupRequest

case class SearchParameters(
                               uprn: Option[String] = None,
                               outcode: Option[Outcode] = None,
                               postcode: Option[Postcode] = None,
                               fuzzy: Option[String] = None,
                               filter: Option[String] = None,
                               town: Option[String] = None,
                               lines: List[String] = Nil
                           ) {

  private def nonBlank(s: Option[String]) = if (s.map(_.trim).contains("")) None else s

  def clean = new SearchParameters(
    uprn = nonBlank(uprn),
    outcode = outcode,
    postcode = postcode,
    fuzzy = nonBlank(fuzzy),
    filter = nonBlank(filter),
    town = nonBlank(town),
    lines.filterNot(_.trim.isEmpty))

  def isFuzzy: Boolean = fuzzy.isDefined || lines.nonEmpty // don't care about town

  // For use in logging/auditing
  def tupled: List[(String, String)] = {
    uprn.toList.map("uprn" -> _) ++
        outcode.toList.map("outcode" -> _.toString) ++
        postcode.toList.map("postcode" -> _.urlSafe) ++
        town.toList.map("town" -> _) ++
        fuzzy.toList.map("fuzzy" -> _) ++
        filter.toList.map("filter" -> _) ++ linesTupled
  }

  private def linesTupled = lines.size match {
    case 0 => Nil
    case 1 => List("line1" -> lines.head)
    case 2 => List("line1" -> lines.head, "line2" -> lines(1))
    case 3 => List("line1" -> lines.head, "line2" -> lines(1), "line3" -> lines(2))
    case _ => List("line1" -> lines.head, "line2" -> lines(1), "line3" -> lines(2), "line4" -> lines(3))
  }
}


object SearchParameters {

  private[osgb] val UPRN = "uprn"
  private[osgb] val OUTCODE = "outcode"
  private[osgb] val POSTCODE = "postcode"
  private[osgb] val FUZZY = "fuzzy"
  private[osgb] val FILTER = "filter"
  private[osgb] val TOWN = "town"

  private[osgb] val LINE1 = "line1"
  private[osgb] val LINE2 = "line2"
  private[osgb] val LINE3 = "line3"
  private[osgb] val LINE4 = "line4"
  private[osgb] val LIMIT = "limit"

  def fromQueryParameters(queryString: Map[String, Seq[String]]): SearchParameters = {
    apply(queryString.map(kv => kv._1 -> kv._2.head))
  }

  def fromLookupRequest(lookupRequest: LookupRequest): SearchParameters = {
    val lookupRequestMap: Map[String, String] = Seq[(String, Option[String])](
      POSTCODE -> Some(lookupRequest.postcode),
      FILTER -> lookupRequest.filter,
    ).collect { case (k, Some(v)) => k -> v }.toMap

    apply(lookupRequestMap)
  }

  def apply(queryString: Map[String, String]): SearchParameters = {
    val line1 = queryString.get("line1").toList
    val line2 = queryString.get("line2").toList
    val line3 = queryString.get("line3").toList
    val line4 = queryString.get("line4").toList
    new SearchParameters(
      queryString.get(UPRN),
      queryString.get(OUTCODE).flatMap(Outcode.cleanupOutcode),
      queryString.get(POSTCODE).flatMap(Postcode.cleanupPostcode),
      queryString.get(FUZZY),
      queryString.get(FILTER) match {
        case Some(f) if f.trim.isEmpty => None
        case o                         => o
      },
      queryString.get(TOWN),
      line1 ++ line2 ++ line3 ++ line4)
  }
}
