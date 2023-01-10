/*
 * Copyright 2023 HM Revenue & Customs
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

package services

import com.github.tototoshi.csv.CSVReader
import model.address.ReferenceItem

import java.util.zip.GZIPInputStream
import scala.collection.mutable.ListBuffer
import scala.io.{Codec, Source}

case class ReferenceData(mappings: Map[Int, ReferenceItem]) {
  def get(k: Int): Option[ReferenceItem] = mappings.get(k)

  def get(k: Option[Int]): Option[ReferenceItem] =
    if (k.isEmpty) None else mappings.get(k.get)
}

object ReferenceData {
  val empty = new ReferenceData(Map())

  def load(localCustodians: String): ReferenceData = {
    try {
      val lccFileList = ','.qsplit(localCustodians).map(_.trim)

      val custodians = lccFileList.flatMap(loadResource(_, 0, 1)).toMap

      val joined = for ((k, v) <- custodians) yield {
        val ri = ReferenceItem(k, v)
        k -> ri
      }

      new ReferenceData(joined)

    } catch {
      case e: NoSuchElementException =>
        throw new RuntimeException(
          "Error in reference data files. Check the codes match!",
          e)
    }
  }

  def loadResource(resource: String,
                   keyIndex: Int,
                   valueIndex: Int): Map[Int, String] = {
    val start = System.currentTimeMillis()

    val is = getClass.getClassLoader.getResourceAsStream(resource)
    if (is == null) {
      throw new IllegalArgumentException(resource + ": no such resource")
    }

    val gis = if (resource.endsWith(".gz")) new GZIPInputStream(is) else is

    val outputBuffer = new ListBuffer[(Int, String)]

    implicit val codec = Codec.UTF8
    val reader = CSVReader.open(Source.fromInputStream(gis))

    try {
      for (cells <- reader) {
        if (cells.length > keyIndex && cells.length > valueIndex) {
          val k = cells(keyIndex)
          val v = cells(valueIndex)
          if (Character.isDigit(k(0)) && v.nonEmpty) {
            val ki = k.toInt
            outputBuffer += ki -> v
          } // else ignore non-numeric codes
        }
      }
    } finally {
      reader.close()
    }

    val time = System.currentTimeMillis() - start
    // println(s"Loading $resource took ${time}ms")

    outputBuffer.toMap
  }
}
