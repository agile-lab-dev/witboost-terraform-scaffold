package it.agilelab.provisioners.terraform

import it.agilelab.spinframework.app.features.compiler.Field

/** Allows to define a simple mapping between Terraform variables and fields in the component descriptor,
  * specifying which field provides the value for each variable.
  */
object TerraformVariablesFromDescriptor {

  /** Translates a list of (variableName, field) pairs to a [[TerraformVariables]] instance,
    * that is a list of (variableName, variableValue) pairs, by extracting values from fields.
    *
    * @param varNameFieldPairs The list of matching between descriptor's fields and Terraform variables.
    * @return
    */
  def variables(varNameFieldPairs: (String, Field)*): TerraformVariables = {
    val varNameVarValuePairs: Seq[(String, String)] = varNameFieldPairs.map { (pair: (String, Field)) =>
      val (variableName, field) = pair
      (variableName, field.value)
    }
    new TerraformVariables(Map.from(varNameVarValuePairs))
  }
}
