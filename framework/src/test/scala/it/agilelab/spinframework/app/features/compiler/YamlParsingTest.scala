package it.agilelab.spinframework.app.features.compiler

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class YamlParsingTest extends AnyFlatSpec with should.Matchers {

  private val parser = ParserFactory.parser()

  "This yaml" should "be parsed as a valid yaml" in {
    val validYamlDescriptor = YamlDescriptor("""
        valid-yaml-field: 1
    """)

    val parsingResult = validYamlDescriptor.parse(parser)

    parsingResult.isInvalidYaml shouldBe false
  }

  "This yaml" should "be parsed as an invalid yaml" in {
    val invalidYamlDescriptor = YamlDescriptor("""
        this-is-not-a-valid-yaml-field: 1: 2: 3
    """)

    val parsingResult = invalidYamlDescriptor.parse(parser)

    parsingResult.isInvalidYaml shouldBe true
  }
}
