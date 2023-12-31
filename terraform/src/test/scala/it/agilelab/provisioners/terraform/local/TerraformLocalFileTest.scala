package it.agilelab.provisioners.terraform.local

import it.agilelab.provisioners.features.provider.TfProvider
import it.agilelab.provisioners.terraform.TerraformLogger.logOnConsole
import it.agilelab.provisioners.terraform.{ BackendConfigs, Terraform, TerraformModule, TerraformResult }
import it.agilelab.spinframework.app.features.support.test.FrameworkTestSupport
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

  val variableMappings: Option[Map[String, String]] = Some(
    Map(
      "file_name"    -> "$.specific.file.name",
      "file_content" -> "$.specific.file.content"
    )
  )

  private val terraformBuilder = Terraform()
    .outputInPlainText()
    .withLogger(logOnConsole)
  private val folderPath       = folder("/local-file")
  private val tfProvider       =
    new TfProvider(terraformBuilder, TerraformModule(folderPath, Map.empty, Map.empty, ""))
  private val terraform        = terraformBuilder.onDirectory(folderPath)

  private val terraformJsonBuilder = Terraform()
    .outputInJson()
    .withLogger(logOnConsole)
  private val terraformJson        = terraformJsonBuilder.onDirectory(folderPath)

  "Terraform" should "create a local file" in {
    shouldNotExist(file("testfile.txt"))

    val variables = tfProvider.variablesFrom(descriptor, variableMappings) match {
      case Right(r) => r
      case Left(l)  => fail(l.mkString("\n"))
    }

    val backendConfigs = BackendConfigs.configs(("foo" -> "bar"))

    val initResult = terraform.doInit(backendConfigs)
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

  "Terraform" should "create a local file using -json flag" in {

    val variables = tfProvider.variablesFrom(descriptor, variableMappings) match {
      case Right(r) => r
      case Left(l)  => fail(l.mkString("\n"))
    }

    shouldNotExist(file("testfile.txt"))

    val initResultJson: TerraformResult = terraformJson.doInit(BackendConfigs.noConfig())
    shouldBeSuccess(initResultJson)

    val validateResultJson: TerraformResult = terraformJson.doValidate()
    shouldBeSuccess(validateResultJson)

    val planResultJson: TerraformResult = terraformJson.doPlan(variables)
    shouldBeSuccess(planResultJson)

    val applyResultJson: TerraformResult = terraformJson.doApply(variables)
    shouldBeSuccess(applyResultJson)
    shouldExist(file("testfile.txt"), "De gustibus non disputandum est")

    val destroyResultJson: TerraformResult = terraformJson.doDestroy(variables)
    shouldBeSuccess(destroyResultJson)
    shouldNotExist(file("testfile.txt"))
  }

}
