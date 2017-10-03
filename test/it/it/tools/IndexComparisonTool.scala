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

package it.tools

import java.io._
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

import osgb.services.AddressESSearcher
import play.api.libs.concurrent.Execution.Implicits._
import uk.co.bigbeeconsultants.util.DiagnosticTimer
import uk.gov.hmrc.address.osgb.DbAddress
import uk.gov.hmrc.address.services.es._
import uk.gov.hmrc.logging.Stdout

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{Await, Future}
import scala.io.{BufferedSource, Source}

object IndexComparisonTool {

  val defaultUri = "elasticsearch://localhost:9300"
  val clusterName = "address-reputation"
  val indexName = "address-reputation-data"

  def main(args: Array[String]) {
    var a = args

    var uri = defaultUri
    var skip = "0"
    var n = Int.MaxValue.toString

    while (a.length > 2 && a(0).head == '-') {
      a(0) match {
        case "-uri" => uri = a(1)
        case "-skip" => skip = a(1)
        case "-n" => n = a(1)
        case _ => println(a(0) + ": unrecognised argument.")
      }
      a = a.drop(2)
    }

    if (a.length < 3) {
      println(
        s"""Usage: IndexComparisonTool [opts] <index1> <index2> <file-of-uprns>
            |
            |where:
            |  -uri <uri>         specifies the Elasticsearch connecction string as a URI
            |  -skip <n>          skips the first n uprns
            |  -n <n>             processes n uprns only, skipping all that remain
            |  <index1> <index2>  the names of the two indexes to be compared
            |  <file-of-uprns>    the name of a text file listing the UPRNs of interest; use '-' for stdin.
            |
            |Numbers can end in k (x1,000) or M (x1,000,000).
            |The default URI is $defaultUri
        """.stripMargin)

    } else {
      val isCluster = false // actually we don't care in this case
      val netSettings = ElasticNetClientSettings(uri, isCluster, clusterName, Map())
      val esClients = ElasticsearchHelper.buildNetClients(netSettings, Stdout)
      val settings = ElasticSettings(netClient = Some(netSettings))
      val admin = new ESAdminImpl(esClients, Stdout, scala.concurrent.ExecutionContext.Implicits.global, settings)
      new IndexComparisonTool(admin, a(0), a(1), a(2), skip, n, settings).go()
    }
  }
}


class IndexComparisonTool(esAdmin: ESAdmin, index1: String, index2: String, origin: String, skipStr: String, nStr: String, settings: ElasticSettings) {

  private val skip = parseNumber(skipStr)
  private val n = parseNumber(nStr)

  private final val separator = "|"

  private var num = 0
  private var nSame = new AtomicInteger(0)
  private var nSimilarExceptAddressLines = new AtomicInteger(0)
  private var nDifferentTown = new AtomicInteger(0)
  private var nLeftOnly = new AtomicInteger(0)
  private var nRightOnly = new AtomicInteger(0)
  private var nNotFound = new AtomicInteger(0)
  private var nUnknown = new AtomicInteger(0)

  //-------------------------------------------------------

  private val esClient = esAdmin.clients.head
  private val searcher1 = new AddressESSearcher(esClient, index1, "GB", defaultContext, settings, Stdout)
  private val searcher2 = new AddressESSearcher(esClient, index2, "GB", defaultContext, settings, Stdout)

  //-------------------------------------------------------

  private val source = selectSource(origin)
  private val dest = selectDest(origin)
  private val dt = new DiagnosticTimer

  //-------------------------------------------------------

  // method is not strictly necessary but simplifies some constructor ordering issues
  def go() {
    dest.println("Left:  " + index1)
    dest.println(esAdmin.getIndexSettings(index1).toString.substring(3))
    dest.println()
    dest.println("Right: " + index2)
    dest.println(esAdmin.getIndexSettings(index2).toString.substring(3))
    dest.println()
    dest.println(s"Skip: $skipStr  N: $nStr")
    dest.println("--------------------------------------------------------------------------------")

    var i = 0
    val it = source.getLines
    while (num < n && it.hasNext) {
      val uprn = it.next
      i += 1
      if (i > skip) {
        num += 1
        if (num % 100000 == 0) {
          report(System.out)
        }

        val r1 = searcher1.findUprn(uprn).map(_.headOption)
        val r2 = searcher2.findUprn(uprn).map(_.headOption)

        val xs = await(Future.sequence(Seq(r1, r2)))
        xs match {
          case Seq(Some(a1), Some(a2)) => compare(wipeStreetClass(a1), wipeStreetClass(a2))
          case Seq(None, Some(a2)) => missingFromFirst(a2)
          case Seq(Some(a1), None) => missingFromSecond(a1)
          case Seq(None, None) => notFound(uprn)
          case _ => nUnknown.incrementAndGet()
        }
      }
    }

    report(dest)

    if (origin != "-") {
      dest.close()
    }
  }

  // Street classification is always absent from DPA so treat it as irrelevant
  private def wipeStreetClass(a: DbAddress) = a.copy(streetClass = None)

  private def report(out: PrintStream) {
    val samePC = percentage(nSame.get, num)
    val similarExceptAddressLinesPC = percentage(nSimilarExceptAddressLines.get, num)
    val differentTownPC = percentage(nDifferentTown.get, num)
    out.println()
    out.println("--------------------------------------------------------------------------------")
    out.println(f"Number of identical addresses:    ${nSame.get}%10d  $samePC%4.1f%%")
    out.println(f"Number with different lines only: ${nSimilarExceptAddressLines.get}%10d  $similarExceptAddressLinesPC%4.1f%%")
    out.println(f"Number with different town:       ${nDifferentTown.get}%10d  $differentTownPC%4.1f%%")
    out.println(f"Number found left only:           ${nLeftOnly.get}%10d")
    out.println(f"Number found right only:          ${nRightOnly.get}%10d")
    out.println(f"UPRNs not found:                  ${nNotFound.get}%10d")
    out.println(f"Unknown:                          ${nUnknown.get}%10d")
    out.println(f"Total:                            $num%10d  ($dt)")
  }

  private def percentage(num: Int, denom: Int): Double = (100.0 * num) / denom

  // n.b. to avoid output interleaving, ensure there is only one println call per compare call

  private def compare(a1: DbAddress, a2: DbAddress) {
    if (a1 == a2) {
      nSame.incrementAndGet()
      dest.println("EQ " + toCsv(a1) + "\n")

    } else {
      val x = a2.copy(lines = a1.lines)
      if (a1 == x) nSimilarExceptAddressLines.incrementAndGet()
      if (a1.town != a2.town) nDifferentTown.incrementAndGet()
      val result =
        "LX " + toCsv(a1) + "\n" +
          "RX " + toCsv(a2) + "\n"

      val s1 = (a1.lines ++ a1.town.toList).toSet
      val s2 = (a2.lines ++ a2.town.toList).toSet
      val diffInfo = compareAddressDetails(s1, s2)
      dest.println(result + diffInfo)
    }
  }

  private def compareAddressDetails(s1: Set[String], s2: Set[String]) = {
    if (s2.size < s1.size && (s2 subsetOf s1)) {
      // Left is proper subset of right
      val diff = s1 -- s2
      f"L${diff.size}%d " + diff.toList.sorted.mkString("|") + "\n"

    } else if (s1.size < s2.size && (s1 subsetOf s2)) {
      // Right is proper subset of left
      val diff = s2 -- s1
      f"R${diff.size}%d " + diff.toList.sorted.mkString("|") + "\n"

    } else {
      // Left and right partially overlap
      val diff = (s1 union s2) -- (s1 intersect s2)
      if (diff.isEmpty) ""
      else {
        f"X${diff.size}%d " + diff.toList.sorted.mkString("|") + "\n"
      }
    }
  }

  private def missingFromFirst(a2: DbAddress) {
    nRightOnly.incrementAndGet()
    dest.println("RO " + toCsv(a2) + "\n")
  }

  private def missingFromSecond(a1: DbAddress) {
    nLeftOnly.incrementAndGet()
    dest.println("LO " + toCsv(a1) + "\n")
  }

  private def notFound(uprn: String) {
    nNotFound.incrementAndGet()
    dest.println("NF " + uprn + "\n")
  }

  private def toCsv(a: DbAddress) = {
    val n = a.productArity
    var csv = a.uprn + separator + a.line1 + separator + a.line2 + separator + a.line3
    var i = 2
    while (i < n) {
      csv = csv + field(i, a)
      i += 1
    }
    csv
  }

  private def field(i: Int, a: DbAddress): String = {
    a.productElement(i) match {
      case s: String => separator + s
      case Some(s: Any) => separator + s.toString
      case None => separator
    }
  }

  private def basename(file: String) = {
    val dot = file.lastIndexOf('.')
    if (dot > 0) file.substring(0, dot)
    else file
  }

  private def await[T](future: Future[T], timeout: Duration = FiniteDuration(10, "s")): T = Await.result(future, timeout)

  private def selectSource(origin: String): BufferedSource = {
    if (origin == "-")
      Source.stdin
    else if (origin.endsWith(".gz"))
      Source.fromInputStream(new GZIPInputStream(new BufferedInputStream(new FileInputStream(origin))), "UTF-8")
    else
      Source.fromFile(origin)
  }

  private def selectDest(origin: String): PrintStream = {
    if (origin == "-") System.out
    else {
      val ss = if (skip > 0) s"-$skipStr" else ""
      val ns = if (n < Int.MaxValue) s"-$nStr" else ""
      val core = s"$ss$ns.out"
      val out = if (origin.endsWith(".gz")) basename(basename(origin)) + core + ".gz" else basename(origin) + core
      println("Writing to " + out)
      val fos = new FileOutputStream(out)
      if (origin.endsWith(".gz"))
        new PrintStream(new GZIPOutputStream(new BufferedOutputStream(fos)))
      else
        new PrintStream(fos)
    }
  }

  private def parseNumber(a: String): Int = {
    if (a.endsWith("M")) a.substring(0, a.length - 1).toInt * 1000000
    else if (a.endsWith("k")) a.substring(0, a.length - 1).toInt * 1000
    else a.toInt
  }
}

// BLPU States
// 1 under construction
// 2 In use
// 3 Unoccupied / vacant / derelict
// 4 Demolished
// 6 Planning permission granted

// Logical States
// 1 Approved
// 3 Alternative
// 6 Provisional
// 8 Historical

// Street Classifications
// 4  Pedestrian way or footpath
// 6  Cycletrack or cycleway
// 8  All vehicles
// 9  Restricted byway
// 10 Bridleway
