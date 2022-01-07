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

package config

import scala.collection.immutable.HashSet

object Capitalisation {

  def normaliseAddressLine(phrase: String*): String = normalise(phrase.map(_.trim.toLowerCase))

  private def normalise(phrase: Seq[String]): String = {
    val words: Seq[String] = phrase.flatMap(_.split(' ').filterNot(_ == ""))

    if (words.isEmpty) ""
    else if (words.length == 1) asFirstWord(words.head)
    else asFirstWord(words.head) + words.tail.map(asOtherWord).mkString(" ", " ", "")
  }

  private def joinDashedWords(first: String, rest: Seq[String]): String =
    if (rest.isEmpty) first
    else first + rest.map(capitaliseRestOfSubwords).mkString("-", "-", "")

  private def splitOnDash(phrase: String): Seq[String] = phrase.split('-')

  private def asFirstWord(word: String): String = {
    val dashedPhrase = splitOnDash(word)
    if (dashedPhrase.nonEmpty) joinDashedWords(capitaliseFirstSubword(dashedPhrase.head), dashedPhrase.tail) else "-"
  }

  private def asOtherWord(word: String): String = {
    val dashedPhrase = splitOnDash(word)
    if (dashedPhrase.nonEmpty) joinDashedWords(capitaliseRestOfSubwords(dashedPhrase.head), dashedPhrase.tail) else "-"
  }

  private def capitaliseFirstSubword(word: String): String =
    acronymSpecialCases.get(word) match {
      case Some(specialCase) => specialCase
      case None              => word.capitalize
    }

  private def capitaliseRestOfSubwords(word: String): String =
    if (stopWords.contains(word)) word else capitaliseSpecialCases(word)

  private def capitaliseSpecialCases(lcWord: String): String =
    subwordSpecialCases.get(lcWord) match {
      case Some(specialCase) => specialCase
      case None              => capitaliseWithContractedPrefix(lcWord)
    }

  private def capitaliseWithContractedPrefix(word: String): String =
    if (word.length < 2) word.capitalize
    else {
      val two = word.substring(0, 2)
      if (contractedPrefixes.contains(two)) two.capitalize + word.substring(2).capitalize
      else word.capitalize
    }

  //-----------------------------------------------------------------------------------------------

  private val stopWords = HashSet(
    // English stop words
    "and", "at", "by", "cum", "in", "next", "of", "on", "the", "to", "under", "upon", "with",
    // "but" isn't included because it's often a proper name too
    // French loan words
    "de", "en", "la", "le",
    // Welsh stop words
    "y", "yr",
    // Gaelic and Cornish stop words
    "an", "na", "nam"
  )

  private val contractedPrefixes = HashSet("a'", "d'", "o'")

  private val subwordSpecialCases = Map(
    "i'anson" -> "I'Anson") // DL3 0RL

  private val acronymSpecialCases = Map(
    "bfpo" -> "BFPO",
    "po" -> "PO")

}