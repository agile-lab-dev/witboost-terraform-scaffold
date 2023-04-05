package it.agilelab.spinframework.app.api.dtos

import it.agilelab.spinframework.app.features.compiler.ErrorMessage

object ValidationErrorDtoObj {
  def from(errors: Seq[ErrorMessage]): ValidationErrorDto = ValidationErrorDto(errors.map(_.description).toArray)
}

case class ValidationErrorDto(errors: Array[String]) {
  override def equals(obj: Any): Boolean = {
    if (obj == null) return false
    if (!obj.isInstanceOf[ValidationErrorDto]) return false
    this.errors.toSeq.equals(obj.asInstanceOf[ValidationErrorDto].errors.toSeq)
  }

  override def hashCode(): Int = errors.hashCode()
}
