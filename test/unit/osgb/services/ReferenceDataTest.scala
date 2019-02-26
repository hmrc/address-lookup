/*
 * Copyright 2019 HM Revenue & Customs
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

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import uk.gov.hmrc.address.v2.ReferenceItem

@RunWith(classOf[JUnitRunner])
class ReferenceDataTest extends FunSuite {

  val refData = ReferenceData.load("sample_local_custodian_table.csv", "sample_local_custodian_ceremonial_counties.csv")

  test("should load counties data file and api should find county by outcode or postcode") {
    assert(refData.get(114) === Some(ReferenceItem(114, "Bath and North East Somerset", Some("Somerset"))))
    assert(refData.get(240) === Some(ReferenceItem(240, "Central Bedfordshire", Some("Bedfordshire"))))
    assert(refData.get(340) === Some(ReferenceItem(340, "West Berkshire", Some("Berkshire"))))
    assert(refData.get(6925) === Some(ReferenceItem(6925, "Merthyr Tydfil UA", Some("Mid Glamorgan"))))
    assert(refData.get(7655) === Some(ReferenceItem(7655, "Ordnance Survey", None)))
  }
}
