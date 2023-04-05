package it.agilelab.spinframework.app.features.compiler

object ValidationResult {
  def apply(errors: Seq[DescriptorError]) = new ValidationResult(errors)
  def create: ValidationResult            = ValidationResult(Seq.empty)
}

class ValidationResult(allErrors: Seq[DescriptorError]) {
  private val filteredErrors: Seq[DescriptorError] =
    allErrors.filter(err => err.errorType != DescriptorErrorType.NoError)

  private def withError(descriptorError: DescriptorError, field: Field): ValidationResult =
    ValidationResult(filteredErrors :+ DescriptorError(descriptorError, field))

  def errors: Seq[DescriptorError] = filteredErrors

  def isSuccess: Boolean = errors.isEmpty

  def check(field: Field, validator: FieldValidator): ValidationResult =
    this.withError(validator.validate(field), field)

  def checkOption(field: Field, validator: FieldValidator): ValidationResult = {
    if (field.undefined || field.empty) return this
    this.check(field, validator)
  }

  override def toString: String = filteredErrors.toString()
}
