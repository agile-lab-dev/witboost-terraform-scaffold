package it.agilelab.provisioners.terraform

/** The result coming from the execution of a Terraform command.
  *
  * @param processResult the exitCode-output pair resulting from
  *                      the execution of a local process
  */
class TerraformResult(processResult: ProcessResult) {

  /** Allows to establish if the execution has been successful or not.
    *
    * @return true if the execution of the comand has raised no error.
    */
  def isSuccess: Boolean = processResult.exitCode == 0

  /** Builds a single string that encompass all the output returned by Terraform
    *  on the execution of the command.
    *  N.B.: this is a potentially risky operation: in case the output is too large to be concatenated,
    *  the operation could fail.
    *
    * @return build a unique string that encompasses the output.
    */
  def buildOutputString: String = processResult.buildOutputString
}
