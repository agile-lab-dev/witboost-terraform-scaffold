package it.agilelab.spinframework.app.features.support.test

import it.agilelab.spinframework.app.features.compiler._

trait FrameworkTestSupport {
  def descriptorFrom(yamlDescriptor: String): ComponentDescriptor = {
    val parser        = ParserFactory.parser()
    val parsingResult = YamlDescriptor(yamlDescriptor).parse(parser)
    if (parsingResult.isInvalidYaml) throw new IllegalArgumentException("A no valid yaml file is provided")
    parsingResult.descriptor
  }

  def errorsIn(validationResult: ValidationResult): Seq[DescriptorErrorType] =
    validationResult.errors.map(_.errorType)
}
