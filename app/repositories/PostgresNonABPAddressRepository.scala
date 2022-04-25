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

package repositories

import cats.effect.IO
import doobie.Transactor
import doobie.implicits._
import doobie.util.fragment.Fragment
import model.internal.NonUKAddress
import model.response.SupportedCountryCodes

import javax.inject.Inject
import scala.concurrent.Future

class PostgresNonABPAddressRepository @Inject()(transactor: Transactor[IO], queryConfig: RdsQueryConfig) extends NonABPAddressRepository {

  override def findInCountry(countryCode: String, filter: String): Future[List[NonUKAddress]] = {
    val timeLimit = Fragment(s"SET statement_timeout=${queryConfig.queryTimeoutMillis};", List())
    val limitSql = Fragment(s" LIMIT ${queryConfig.queryResultsLimit};", List())

    (timeLimit ++
    Fragment.const(
      s"""
         |SELECT id, number, street, unit, city, district, region, postcode
         |FROM $countryCode
         |WHERE address_lookup_ft_col @@ plainto_tsquery('english', '$filter');
         |""".stripMargin)
      ++ limitSql)
      .query[NonUKAddress]
        .to[List]
        .transact(transactor)
        .unsafeToFuture()
  }
}
