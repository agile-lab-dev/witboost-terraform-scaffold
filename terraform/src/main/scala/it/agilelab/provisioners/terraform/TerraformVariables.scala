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
class TerraformVariables(variables: Map[String, String]) {

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
    val valueInApexes = s""""$value""""
    s"-var $name=$valueInApexes"
  }
}
