package it.agilelab.provisioners.terraform.local

import it.agilelab.provisioners.terraform.TerraformLogger.logOnConsole
import it.agilelab.spinframework.app.features.support.test.FrameworkTestSupport
import it.agilelab.provisioners.terraform.{
  Terraform,
  TerraformResult,
  TerraformVariables,
  TerraformVariablesFromDescriptor
}
import org.scalatest.flatspec.AnyFlatSpec

/** This test serves as a sample of both a complete terraform module and a Terraform façade invocation.
  *
  * When invoked, Terraform perform the specified command (ex. apply or destroy) and produces an output.
  * The -json option makes the output is provided in json format.
  * The output can be
  *    - the variables specified in output.tf
  *    - a standard message (as "Destroy complete!") in other cases
  *    - an error message
  * In each of the above cases, the output is returned as a unique string.
  *
  * The module is in the "local-file" folder under test/resources/terraform.
  * The resource specification is in main.tf.
  * The module creates a file throughout the 'local' provider specified in provider.tf.
  * The name and content of the file are terraform variables, specified in variables.tf.
  * The output.tf specifies the output shown with the command.
  *
  * @note If you want to manually check that the file is actually created, put a breakpoint on
  * the "destroy" line, launch the test in debug mode, and when it breaks do a "reload from disk"
  * on the directory in the project structure: you will see the file just created.
  */
class TerraformLocalFileTest extends AnyFlatSpec with TerraformLocalTestBase with FrameworkTestSupport {

  private val descriptor = descriptorFrom("""
       specific:
          file:
            name: "testfile.txt"
            content: "De gustibus non disputandum est"        
    """)

  private val variables: TerraformVariables = TerraformVariablesFromDescriptor.variables(
    "file_name"    -> descriptor
      .sub("specific")
      .sub("file")
      .field("name"), // fills the file_name terraform variable
    "file_content" -> descriptor
      .sub("specific")
      .sub("file")
      .field("content") // fills the file_content terraform variable
  )
  private val terraform                     = Terraform()
    .outputInPlainText()
    .withLogger(logOnConsole)
    .onDirectory(folder("/local-file"))

  "Terraform" should "create a local file" in {
    shouldNotExist(file("testfile.txt"))

    val initResult = terraform.doInit()
    shouldBeSuccess(initResult, "Terraform has been successfully initialized!")

    val validateResult: TerraformResult = terraform.doValidate()
    shouldBeSuccess(validateResult, "Success!", "The configuration is valid.")

    val planResult: TerraformResult = terraform.doPlan(variables)
    shouldBeSuccess(planResult, "local_file.some_file", "will be created")

    val applyResult: TerraformResult = terraform.doApply(variables)
    shouldBeSuccess(applyResult, "Apply complete! Resources: 1 added")
    shouldExist(file("testfile.txt"), "De gustibus non disputandum est")

    val destroyResult = terraform.doDestroy(variables)
    shouldBeSuccess(destroyResult, "Destroy complete! Resources: 1 destroyed")
    shouldNotExist(file("testfile.txt"))
  }

}
