package it.agilelab.spinframework.app.features.compiler

import ErrorMessages.InvalidDescriptor

class CompileService(parser: Parser, validator: DescriptorValidator) extends Compile {

  def extractErrorMessagesFrom(validationResult: ValidationResult): Seq[ErrorMessage] =
    validationResult.errors.map(error => ErrorMessage(error.message))

  override def doCompile(descr: Descriptor): CompileResult = {
    val parsingResult: ParsingResult = descr.parse(parser)
    if (parsingResult.isInvalidInput) CompileResult.failure(Seq(InvalidDescriptor))
    else {
      val descriptor: ComponentDescriptor    = parsingResult.descriptor
      val validationResult: ValidationResult = validator.validate(descriptor)
      if (validationResult.isSuccess) CompileResult.success(descriptor)
      else CompileResult.failure(extractErrorMessagesFrom(validationResult))
    }
  }
}
