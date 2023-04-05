package it.agilelab.spinframework.app.features.compiler

import DescriptorErrorType.{ EmptyField, UndefinedField }

/** The entry point to perform the validation operation.
  *
  * It offers a fluent interface to specify validation rules for the descriptor fields.
  */
object Validation {

  /** Starts the validation by creating an instance of ValidationResult.
    *
    * @return the result of the validation process
    */
  def start: ValidationResult = ValidationResult.create

  val IsValid: IsValidBuilder  = new IsValidBuilder
  val Defined: FieldValidator  = IsValid.when(field => field.defined).otherwise(UndefinedField)
  val NonEmpty: FieldValidator = Defined && IsValid.when(field => field.nonEmpty).otherwise(EmptyField)
}
