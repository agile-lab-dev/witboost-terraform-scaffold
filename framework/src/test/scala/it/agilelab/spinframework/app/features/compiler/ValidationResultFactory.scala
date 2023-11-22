package it.agilelab.spinframework.app.features.compiler

import it.agilelab.spinframework.app.features.compiler.DescriptorErrorType._

object ValidationResultFactory {

  def validationResultWithErrors(fieldNames: String*): ValidationResult = {
    val errors: Seq[DescriptorError] = for {
      fieldName <- fieldNames
    } yield DescriptorError(UndefinedField, SimpleField.undefined(fieldName))

    ValidationResult(errors)
  }
}
