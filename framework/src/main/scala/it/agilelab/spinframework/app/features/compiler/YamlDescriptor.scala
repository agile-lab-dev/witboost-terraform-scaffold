package it.agilelab.spinframework.app.features.compiler

case class YamlDescriptor(string: String) {
  def parse(parser: Parser): ParsingResult =
    parser.parseYaml(string)
}
