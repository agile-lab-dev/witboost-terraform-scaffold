package it.agilelab.spinframework.app.features.compiler

case class ErrorMessage(description: String) {
  override def toString: String = description
}

object ErrorMessages extends Enumeration {
  val InvalidDescriptor: ErrorMessage = ErrorMessage("Invalid Descriptor")
}
