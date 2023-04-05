package it.agilelab.spinframework.app.features.compiler.circe

import io.circe.{ yaml, Json }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

// This test is a form of documentation for how Circe library works
// It is a library that allows you to parse a yaml file into a json object,
// and than traverses it via a cursor to extract specific fields.
class CirceTest extends AnyFlatSpec with should.Matchers {

  "Circe" should "parse a simple yaml string" in {

    val yamlString = """
        aField: aValue
    """

    val json  = yaml.parser.parse(yamlString).getOrElse(Json.Null)
    val value = json.hcursor.downField("aField").as[String].getOrElse("no such field: 'aField'")

    value shouldBe "aValue"
  }

  "Circe" should "detect a not-valid yaml string" in {

    val yamlString = """
        this-is-not-a-valid-yaml-field: 1: 2: 3
    """
    val json: Json = yaml.parser.parse(yamlString).getOrElse(Json.Null)

    json shouldBe Json.Null
  }
}
