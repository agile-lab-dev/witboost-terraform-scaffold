package it.agilelab.spinframework.app.features.compiler

trait Descriptor {
  def parse(parser: Parser): ParsingResult
}

case class YamlDescriptor(string: String) extends Descriptor {
  override def parse(parser: Parser): ParsingResult =
    parser.parseYaml(string)
}

case class JsonDescriptor(string: String) extends Descriptor {
  override def parse(parser: Parser): ParsingResult =
    parser.parseJson(string)
}
