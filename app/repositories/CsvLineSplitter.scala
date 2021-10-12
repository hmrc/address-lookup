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

package repositories

import com.univocity.parsers.csv.{CsvParser => UnivocityParser, CsvParserSettings => UnivocityParserSettings}

import java.io._
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream
import scala.collection.JavaConverters._

/**
  * Raw-Java style fast line splitter for CSV files. This uses the Univocity API for high performance.
  */
class CsvLineSplitter(reader: Reader) extends java.util.Iterator[Array[String]] {

  def this(is: InputStream) = this(new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)))

  def this(string: String) = this(new StringReader(string))

  private val settings = new UnivocityParserSettings()
  settings.getFormat.setLineSeparator("\n")
  settings.setEmptyValue("")
  settings.setNullValue("") // slightly faster if blank instead of null
  settings.setHeaderExtractionEnabled(false)
  settings.setColumnReorderingEnabled(false)
  settings.setLineSeparatorDetectionEnabled(true)
  settings.setReadInputOnSeparateThread(true) // shaves 20% off file i/o & parsing time
  settings.setMaxColumns(50)
  // limits the consequence of any insane lines that crop up

  private val parser = new UnivocityParser(settings)
  parser.beginParsing(reader)

  private var row: Array[String] = parser.parseNext()

  override def hasNext: Boolean = row != null

  override def next(): Array[String] = {
    val r = row
    if (row != null)
      row = parser.parseNext()
    r
  }

  def stopParsing() {
    if (row != null) {
      parser.stopParsing()
      row = null
    }
  }
}


/**
  * Pimped-Scala style iterator for CSV file splitting, based on CsvLineSplitter.
  */
object CsvParser {

  def splitResource(resource: String): Iterator[Array[String]] = {
    val is = getClass.getClassLoader.getResourceAsStream(resource)
    if (is == null) {
      throw new IllegalArgumentException(resource + ": no such resource")
    }
    val giz = if (resource.endsWith(".gz")) new GZIPInputStream(is) else is
    val list = split(new InputStreamReader(giz, StandardCharsets.UTF_8))
    giz.close()
    list
  }

  def split(multilineCsvString: String): Iterator[Array[String]] = {
    split(new StringReader(multilineCsvString))
  }

  def split(reader: Reader): Iterator[Array[String]] = {
    new CsvLineSplitter(reader).asScala
  }

  def split(is: InputStream): Iterator[Array[String]] = {
    new CsvLineSplitter(is).asScala
  }
}