package it.agilelab.spinframework.app.features.compiler

trait Parser {
  def parseYaml(string: String): ParsingResult
  def parseJson(string: String): ParsingResult
}
