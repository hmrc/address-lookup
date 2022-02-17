/*
 * Copyright 2022 HM Revenue & Customs
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

package model

import model.address.Postcode
import play.api.libs.json._
import util._

import scala.annotation.tailrec

object internal {

  // non-uk
  case class NonUKAddress(id: String, number: Option[String], street: Option[String], unit: Option[String],
                          city: Option[String], district: Option[String], region: Option[String],
                          postcode: Option[String])
  object NonUKAddress {
    implicit val format: Format[NonUKAddress] = Json.format[NonUKAddress]
  }

  // Do we need these 2 representations of address ?????
  case class SqlDbAddress(uprn: String,
                          parentUprn: Option[String],
                          usrn: Option[String],
                          orgnisationName: Option[String],
                          line1: Option[String],
                          line2: Option[String],
                          line3: Option[String],
                          subdivision: Option[String],
                          countryCode: Option[String],
                          localCustodianCode: Option[String],
                          language: Option[String],
                          location: Option[String],
                          posttown: Option[String],
                          postcode: Option[String],
                          poBoxNumber: Option[String],
                          localAuthority: Option[String])

  /**
    * Address typically represents a postal address.
    * For UK addresses, 'town' will always be present.
    * For non-UK addresses, 'town' may be absent and there may be an extra line instead.
    */
  // id typically consists of some prefix and the uprn
  case class DbAddress(id: String,
                       uprn: Long,
                       parentUprn: Option[Long],
                       usrn: Option[Long],
                       organisationName: Option[String],
                       lines: List[String],
                       town: String,
                       postcode: String,
                       subdivision: Option[String],
                       country: Option[String],
                       localCustodianCode: Option[Int],
                       language: Option[String],
                       blpuClass: Option[String],
                       location: Option[String],
                       poBox: Option[String] = None,
                       administrativeArea: Option[String] = None) {

    def linesContainIgnoreCase(filterStr: String): Boolean = {
      val filter = filterStr.toUpperCase
      lines.map(_.toUpperCase).exists(_.contains(filter))
    }

    def line1: String = if (lines.nonEmpty) lines.head else ""

    def line2: String = if (lines.size > 1) lines(1) else ""

    def line3: String = if (lines.size > 2) lines(2) else ""

    def latLong: Option[LatLong] = LatLong(location)

    // For use as input to MongoDbObject (hence it's not a Map)
    def tupled: List[(String, Any)] = {
      List("lines" -> lines, "postcode" -> postcode) ++
          town.toList.map("town" -> _) ++
          subdivision.toList.map("subdivision" -> _) ++
          country.toList.map("country" -> _) ++
          localCustodianCode.toList.map("localCustodianCode" -> _) ++
          language.toList.map("language" -> _) ++
          blpuClass.toList.map("blpuClass" -> _) ++
          location.toList.map("location" -> _) ++
          poBox.toList.map("poBox" -> _) ++
          administrativeArea.toList.map("administrativeArea" -> _)
    }

    // We're still providing two structures for the lines, pending a decision on how ES will be used.
    def tupledFlat: List[(String, Any)] = {
      def optLine1 = if (lines.nonEmpty) List(lines.head) else Nil

      def optLine2 = if (lines.size > 1) List(lines(1)) else Nil

      def optLine3 = if (lines.size > 2) List(lines(2)) else Nil

      List("postcode" -> postcode) ++
          optLine1.map("line1" -> _) ++
          optLine2.map("line2" -> _) ++
          optLine3.map("line3" -> _) ++
          town.toList.map("town" -> _) ++
          subdivision.toList.map("subdivision" -> _) ++
          country.toList.map("country" -> _) ++
          localCustodianCode.toList.map("localCustodianCode" -> _) ++
          language.toList.map("language" -> _) ++
          blpuClass.toList.map("blpuClass" -> _) ++
          location.toList.map("location" -> _) ++
          poBox.toList.map("poBox" -> _) ++
          administrativeArea.toList.map("administrativeArea" -> _)
    }

    def forMongoDb: List[(String, Any)] = tupled ++ List("_id" -> id)

    def splitPostcode: Postcode = Postcode(postcode)
  }


  object DbAddress {
    final val English = "en"
    final val Cymraeg = "cy"
  }

  case class LatLong(lat: Double, long: Double) {
    def toLocation = lat.toString + "," + long.toString
  }

  object LatLong {
    def apply(location: Option[String]): Option[LatLong] = {
      if (location.isDefined) {
        val a = location.get.split(',')
        Some(LatLong(a(0).toDouble, a(1).toDouble))
      } else None
    }
  }
}
