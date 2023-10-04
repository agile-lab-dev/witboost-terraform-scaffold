package it.agilelab.spinframework.app.features.compiler.circe

import io.circe.{ yaml, Json, ParsingFailure }
import it.agilelab.spinframework.app.features.compiler.{ Parser, ParsingResult }

class CirceParser extends Parser {
  override def parseYaml(string: String): ParsingResult = {
    val failureOrJson: Either[ParsingFailure, Json] = yaml.parser.parse(string)
    def isInvalidInput: Boolean                     = failureOrJson.isLeft
    val json                                        = failureOrJson.getOrElse(Json.Null)
    ParsingResult(isInvalidInput, CirceParsedDescriptor(json))
  }

  override def parseJson(string: String): ParsingResult = {
    import io.circe._, io.circe.parser._
    val failureOrJson: Either[ParsingFailure, Json] = parse(string)
    def isInvalidInput: Boolean                     = failureOrJson.isLeft
    val json                                        = failureOrJson.getOrElse(Json.Null)
    ParsingResult(isInvalidInput, CirceParsedDescriptor(json))
  }
}
