package it.agilelab.spinframework.app.features.compiler

import ErrorMessages.InvalidYamlDescriptor

class CompileService(parser: Parser, validator: DescriptorValidator) extends Compile {

  def extractErrorMessagesFrom(validationResult: ValidationResult): Seq[ErrorMessage] =
    validationResult.errors.map(error => ErrorMessage(error.message))

  override def doCompile(yamlDescriptor: YamlDescriptor): CompileResult = {
    val parsingResult: ParsingResult = yamlDescriptor.parse(parser)
    if (parsingResult.isInvalidYaml) return CompileResult.failure(Seq(InvalidYamlDescriptor))

    val descriptor: ComponentDescriptor    = parsingResult.descriptor
    val validationResult: ValidationResult = validator.validate(descriptor)
    if (validationResult.isSuccess) CompileResult.success(descriptor)
    else CompileResult.failure(extractErrorMessagesFrom(validationResult))
  }
}
