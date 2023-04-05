package it.agilelab.spinframework.app.features.compiler.circe

import io.circe.{ yaml, Json, ParsingFailure }
import it.agilelab.spinframework.app.features.compiler.{ Parser, ParsingResult }
import it.agilelab.spinframework.app.features.compiler.{ Parser, ParsingResult }

class CirceParser extends Parser {
  override def parseYaml(string: String): ParsingResult = {
    val failureOrJson: Either[ParsingFailure, Json] = yaml.parser.parse(string)
    def isInvalidYaml: Boolean                      = failureOrJson.isLeft

    val json = failureOrJson.getOrElse(Json.Null)
    ParsingResult(isInvalidYaml, CirceParsedDescriptor(json))
  }

}
