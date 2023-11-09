package model

import model.address.Postcode
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class PostcodeTest extends AnyFunSuite with Matchers {
  test(
    """Given valid postcode string should cleanup successfully""") {
    val pc = "FX11XX"
    val maybePostcode = Postcode.cleanupPostcode(pc)
    maybePostcode shouldBe defined
    maybePostcode.get.toString shouldBe "FX1 1XX"
  }

  test(
    """Given valid short postcode string should cleanup successfully""") {
    val pc = "W12DN"
    val maybePostcode = Postcode.cleanupPostcode(pc)
    maybePostcode shouldBe defined
    maybePostcode.get.toString shouldBe "W1 2DN"
  }

  test(
    """Given valid long postcode string should cleanup successfully""") {
    val pc = "DE128HJ"
    val maybePostcode = Postcode.cleanupPostcode(pc)
    maybePostcode shouldBe defined
    maybePostcode.get.toString shouldBe "DE12 8HJ"
  }

  test(
    """Given valid edge case postcode string should cleanup successfully""") {
    val pc = "GIR0AA"
    val maybePostcode = Postcode.cleanupPostcode(pc)
    maybePostcode shouldBe defined
    maybePostcode.get.toString shouldBe "GIR 0AA"
  }
}
