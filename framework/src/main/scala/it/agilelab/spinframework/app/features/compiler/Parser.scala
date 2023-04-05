package it.agilelab.spinframework.app.features.compiler

trait Parser {
  def parseYaml(string: String): ParsingResult
}
