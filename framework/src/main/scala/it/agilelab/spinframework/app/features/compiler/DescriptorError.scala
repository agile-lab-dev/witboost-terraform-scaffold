package it.agilelab.spinframework.app.features.compiler

/** Represents a validation error with two information: an error type and a message.
  */
trait DescriptorError {
  final override def toString: String = errorType.name

  def errorType: DescriptorErrorType

  def message: String
}

/** An error type identified by a name, that defines a template for the corresponding message.
  * A message template usually contains some placholder that have to be replaced with actual values,
  * which depend on the context in which the error raised.
  *
  * @param name a short description of the error
  * @param messageTemplate a message string with eventual placeholders
  */
class DescriptorErrorType(val name: String, messageTemplate: String) extends DescriptorError {
  override def errorType: DescriptorErrorType = this
  override def message: String                = messageTemplate
}

/** A decorator for a [DescriptorError], whose purpose is to replace placeholders with actual value.
  *
  * @param wrapped the provider of the error message
  * @param placeholder the word to replace with an actual value
  * @param actualValue the value to replace the placeholder
  */
class PlaceholderDescriptorError(wrapped: DescriptorError, placeholder: String, actualValue: String)
    extends DescriptorError {
  override def errorType: DescriptorErrorType = wrapped.errorType
  override def message: String                = wrapped.message.replaceFirst(placeholder, actualValue)
}

/** All possible descriptor error types are defined here.
  */
object DescriptorErrorType {
  val NoError = new DescriptorErrorType("NoError", "No error")

  val UndefinedField = new DescriptorErrorType("", s"missing field: <fieldname>")

  val EmptyField = new DescriptorErrorType("", s"empty field: <fieldname>")

  val GenericError = new DescriptorErrorType("GenericError", "Generic error")

  val NonExistentValue =
    new DescriptorErrorType("NonExistentValue", s"invalid field: <fieldname> with non existent value: <fieldvalue>")

  val MalformedValue =
    new DescriptorErrorType("MalformedValue", s"invalid field: <fieldname> with malformed value: <fieldvalue>")

  val NotAvailableValue =
    new DescriptorErrorType("NotAvailableValue", s"invalid field: <fieldname> with not available value: <fieldvalue>")

  val NotInRangeValue =
    new DescriptorErrorType("NotInRangeValue", s"invalid field: <fieldname> with value: <fieldvalue> not in <range>")
}

object DescriptorError {
  def apply(wrapped: DescriptorError, field: Field): DescriptorError = {
    val replaceFieldName  = new PlaceholderDescriptorError(wrapped, "<fieldname>", field.name)
    val replaceFieldValue = new PlaceholderDescriptorError(replaceFieldName, "<fieldvalue>", field.value)
    replaceFieldValue
  }
}
