package it.agilelab.provisioners.terraform

/** The terraform package represents a Terraform façade, which has the purpose of
  * simplifying the execution of Terraform commands by the application.
  *
  * This class is the entry point to the façade.
  */
object Terraform {

  /** Provides a starting point in a fluent interface for declaring and executing a terraform command.
    *
    * @return an instance of [[TerraformBuilder]].
    */
  def apply() = new TerraformBuilder
}
