package it.agilelab.spinframework.app.features.compiler

import it.agilelab.spinframework.app.features.compiler.circe.CirceParser

object ParserFactory {
  def parser(): Parser = new CirceParser
}
