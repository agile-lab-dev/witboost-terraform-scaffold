package it.agilelab.spinframework.app.features.compiler

import DescriptorErrorType.NotInRangeValue

class IsValidBuilder() {
  def when(condition: ValidCondition): OtherwiseBuilder = new OtherwiseBuilder(condition)
  def whenInRange(values: Seq[String]): FieldValidator = {
    val error = new PlaceholderDescriptorError(NotInRangeValue, "<range>", values.toString())
    when(field => values.contains(field.value)).otherwise(error)
  }
}

class OtherwiseBuilder(condition: ValidCondition) {
  def otherwise(error: DescriptorError): FieldValidator =
    SimpleValidator(field => condition.isSatisfiedBy(field), error)
}
