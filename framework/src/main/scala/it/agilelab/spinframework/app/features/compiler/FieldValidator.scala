package it.agilelab.spinframework.app.features.compiler

import DescriptorErrorType._

trait FieldValidator {
  def validate(field: Field): DescriptorError
  final def &&(other: FieldValidator): AndValidator = new AndValidator(this, other)
}

trait ValidCondition {
  def isSatisfiedBy(field: Field): Boolean
  final def isNotSatisfiedBy(field: Field): Boolean = !isSatisfiedBy(field)
}

object SimpleValidator {
  def apply(validCondition: ValidCondition, error: DescriptorError) = new SimpleValidator(validCondition, error)
}

class SimpleValidator(val validCondition: ValidCondition, val error: DescriptorError) extends FieldValidator {
  override def validate(field: Field): DescriptorError = if (validCondition.isNotSatisfiedBy(field)) error else NoError
}

class AndValidator(validator1: FieldValidator, validator2: FieldValidator) extends FieldValidator {
  override def validate(field: Field): DescriptorError = {
    val firstResult = validator1.validate(field)
    if (firstResult != NoError) firstResult
    else validator2.validate(field)
  }
}
