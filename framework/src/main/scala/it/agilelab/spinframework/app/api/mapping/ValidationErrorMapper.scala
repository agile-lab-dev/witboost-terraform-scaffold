package it.agilelab.spinframework.app.api.mapping

import it.agilelab.spinframework.app.api.generated.definitions.ValidationError
import it.agilelab.spinframework.app.features.compiler.CompileResult
import it.agilelab.spinframework.app.features.provision.ProvisionResult

object ValidationErrorMapper {

  def from(result: ProvisionResult): ValidationError = ValidationError(result.errors.map(_.description).toVector)
  def from(result: CompileResult): ValidationError   = ValidationError(result.errors.map(_.description).toVector)

}
