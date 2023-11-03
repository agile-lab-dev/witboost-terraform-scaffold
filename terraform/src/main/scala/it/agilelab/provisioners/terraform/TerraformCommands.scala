package it.agilelab.provisioners.terraform

import TerraformVariables.noVariable

/** Specifies all executable Terraform commands
  */
trait TerraformCommands {

  /** Performs a 'terraform init'.
    *
    * @param configs an instance of BackendConfigs to specify backend configurations.
    *                The default value is 'no backend configs'
    * @return the command result.
    */
  def doInit(configs: BackendConfigs): TerraformResult

  /** Performs a 'terraform plan'.
    *
    * @param variables an instance of TerraformVariables to specify configuration paramters.
    *                  The default value is 'no variable'
    * @return the command result.
    */
  def doPlan(variables: TerraformVariables = noVariable()): TerraformResult

  /** Performs a 'terraform apply'.
    *
    * @param variables an instance of TerraformVariables to specify configuration paramters.
    *                  The default value is 'no variable'
    * @return the command result.
    */
  def doApply(variables: TerraformVariables = noVariable()): TerraformResult

  /** Performs a 'terraform validate'.
    * If the configuration is syntactically valid, the result is ok,
    * otherwise a list of errors is returned.
    *
    * @return the command result.
    */
  def doValidate(): TerraformResult

  /** Performs a 'terraform destroy'.
    * Always remember to pass the right variables to correctly perform the operation.
    * For this reason the parameter has no default value as in doApply.
    *
    * @param variables an instance of TerraformVariables to specify configuration paramters.
    * @return the command result.
    */
  def doDestroy(variables: TerraformVariables): TerraformResult
}
