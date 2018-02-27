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

import java.io.{BufferedReader, InputStreamReader}
import java.nio.charset.StandardCharsets
import java.util.NoSuchElementException
import java.util.zip.GZIPInputStream

import config._
import uk.gov.hmrc.address.services.Capitalisation
import uk.gov.hmrc.address.v2.ReferenceItem

import scala.collection.mutable.ListBuffer


case class ReferenceData(mappings: Map[Int, ReferenceItem]) {
  def get(k: Int) = mappings.get(k)

  def get(k: Option[Int]) = if (k.isEmpty) None else mappings.get(k.get)
}


object ReferenceData {
  val empty = new ReferenceData(Map())

  def load(localCustodians: String, lccCounties: String): ReferenceData = {
    try {
      val lccFileList = localCustodians.qsplit(',').map(_.trim)
      val countiesFileList = lccCounties.qsplit(',').map(_.trim)

      val custodians = lccFileList.flatMap(loadResource(_, 0, 1)).toMap
      val counties = countiesFileList.flatMap(loadResource(_, 0, 2)).toMap

      val joined = for ((k, v) <- custodians) yield {
        val ri = ReferenceItem(k, v, counties.get(k))
        k -> ri
      }

      new ReferenceData(joined)

    } catch {
      case e: NoSuchElementException => throw new RuntimeException("Error in reference data files. Check the codes match!", e)
    }
  }

  def loadResource(resource: String, keyIndex: Int, valueIndex: Int): Map[Int, String] = {
    val start = System.currentTimeMillis()
    val is = getClass.getClassLoader.getResourceAsStream(resource)
    if (is == null) {
      throw new IllegalArgumentException(resource + ": no such resource")
    }
    val giz = if (resource.endsWith(".gz")) new GZIPInputStream(is) else is
    val reader = new BufferedReader(new InputStreamReader(giz, StandardCharsets.UTF_8))
    val b = new ListBuffer[(Int, String)]
    try {
      var line = reader.readLine()
      while (line != null) {
        val cells = line.qsplit(',')
        if (cells.length > keyIndex && cells.length > valueIndex) {
          val k = cells(keyIndex)
          val v = cells(valueIndex)
          if (Character.isDigit(k(0)) && v.nonEmpty) {
            val ki = k.toInt
            b += ki -> capitalise(v)
          } // else ignore non-numeric codes
        }
        line = reader.readLine()
      }
    } finally {
      reader.close()
    }
    val time = System.currentTimeMillis() - start
    println(s"Loading $resource took ${time}ms")
    b.toMap
  }

  private def capitalise(v: String) = {
    val c = Capitalisation.normaliseAddressLine(v)
    // the place-name normaliser doesn't know about Unitary Authorities
    if (v.endsWith(" UA")) c.substring(0, c.length - 2) + "UA"
    else c
  }

}
