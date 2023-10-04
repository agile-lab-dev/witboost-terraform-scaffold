package it.agilelab.spinframework.app.features.compiler

case class ErrorMessage(description: String)

object ErrorMessages extends Enumeration {
  val InvalidDescriptor: ErrorMessage = ErrorMessage("Invalid Descriptor")
}
