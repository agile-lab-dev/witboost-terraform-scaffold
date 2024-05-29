package it.agilelab.provisioners.terraform

/** A companion to [[TerraformVariables]] class to group some convenience methods.
  */
object TerraformVariables {

  /** Allows to specify that no variable is provided for the command.
    *
    * @return an empty instance of [[TerraformVariables]]
    */
  def noVariable() = new TerraformVariables(Map.empty)

  /** Allows easy construction of terraform variables instance.
    *
    * @param nameValuePairs a list of variables, each expressed as a name-value pair.
    * @return an instance of [[TerraformVariables]]
    */
  def variables(nameValuePairs: (String, String)*): TerraformVariables =
    new TerraformVariables(nameValuePairs.toMap)

}

/** A list of variables to parameterize Terraform configuration.
  * Each variable is a name-value pair.
  *
  * @param variables a map the contains all the variables.
  */
case class TerraformVariables(variables: Map[String, String]) {

  /** Converts each variable in a Terraform compliant format.
    * Ex. the variable ("file_content" -> "hello, guy!") is translated to
    * the string '-var file_content="hello, guy!"'
    *
    * @return the variables as a list of parameters for command line.
    */
  def toOptions: String =
    variables.map(buildOption).mkString(" ")

  private def buildOption(variable: (String, String)): String = {
    val (name, value) = variable
    // wrapping in single quotes is mandatory, in order to allow complex values, and to be sure not to brake the terraform command if single quotes are present in the value
    // This can lead to unwanted behaviour, that is, the escape will be propagated in the resource itself
    // This can only be managed at TF module level (i.e. by removing it)
    val escapedValue  = value.replace("'", "\\'")
    s"-var $name='$escapedValue'"
  }
}
