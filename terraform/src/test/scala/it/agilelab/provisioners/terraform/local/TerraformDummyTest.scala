package it.agilelab.provisioners.terraform.local

import it.agilelab.provisioners.TestConfig
import it.agilelab.provisioners.features.provider.TfProvider
import it.agilelab.provisioners.terraform.TerraformLogger.logOnConsole
import it.agilelab.provisioners.terraform.{ Terraform, TerraformResult }
import it.agilelab.spinframework.app.config.{ ConfigurationModel => CM }
import it.agilelab.spinframework.app.features.support.test.FrameworkTestSupport
import org.scalatest.flatspec.AnyFlatSpec

/** Test the invocation of a terraform repo "dummy-module".
  *
  *  This test uses the terraform facade to execute init, validate, plan, apply
  *  and destroy on the repo "dummy-module". The dummy-module takes two input
  *  variables `numeric` and `length` and returns a random string as an output
  *  variable.
  *
  *  @note By default this test uses the "dummy-module" included under
  *        `src/test/resources/terraform/dummy-module`. In order to point to
  *        an external repo instead, you need to override the configuration
  *        key `datamesh.terraform.repositoryPath`. The default path is
  *        configured in `test/resources/dummy-module-config.conf`.
  */
class TerraformDummyTest extends AnyFlatSpec with TerraformLocalTestBase with FrameworkTestSupport {

  private val terraform_repositoryPath = TestConfig
    .load("dummy-module-config.conf")
    .getString(CM.terraform_repositoryPath)

  private val descriptor = descriptorFrom("""
       specific:
          account: xys-12345
          isNumeric: false
          length: 6
    """)

  private val variableMappings = Some(
    Map(
      "numeric" -> "$.specific.isNumeric",
      "length"  -> "$.specific.length"
    )
  )

  private val terraform = Terraform()
    .outputInPlainText()
    .withLogger(logOnConsole)
    .onDirectory(terraform_repositoryPath)

  private val terraformJson = Terraform()
    .outputInJson()
    .withLogger(logOnConsole)
    .onDirectory(terraform_repositoryPath)

  val tfProvider = new TfProvider(terraform)

  "Terraform" should "generate a random string" in {

    val variables = tfProvider.variablesFrom(descriptor, variableMappings) match {
      case Right(r) => r
      case Left(l)  => fail(l.mkString("\n"))
    }

    val initResult: TerraformResult = terraform.doInit()
    shouldBeSuccess(initResult, "Terraform has been successfully initialized!")

    val validateResult: TerraformResult = terraform.doValidate()
    shouldBeSuccess(validateResult, "Success!", "The configuration is valid.")

    val planResult: TerraformResult = terraform.doPlan(variables)
    shouldBeSuccess(planResult, "resource \"random_string\" \"random\"", "length", "numeric")

    val applyResult: TerraformResult = terraform.doApply(variables)
    shouldBeSuccess(applyResult, "Apply complete! Resources: 1 added")

    val destroyResult: TerraformResult = terraform.doDestroy(variables)
    shouldBeSuccess(destroyResult, "Destroy complete! Resources: 1 destroyed")
  }

  "Terraform" should "generate a random string when -json flag is provided" in {

    val variables = tfProvider.variablesFrom(descriptor, variableMappings) match {
      case Right(r) => r
      case Left(l)  => fail(l.mkString("\n"))
    }

    val initResultJson: TerraformResult = terraformJson.doInit()
    shouldBeSuccess(initResultJson)

    val validateResultJson: TerraformResult = terraformJson.doValidate()
    shouldBeSuccess(validateResultJson)

    val planResultJson: TerraformResult = terraformJson.doPlan(variables)
    shouldBeSuccess(planResultJson)

    val applyResultJson: TerraformResult = terraformJson.doApply(variables)
    shouldBeSuccess(applyResultJson)

    val destroyResultJson: TerraformResult = terraformJson.doDestroy(variables)
    shouldBeSuccess(destroyResultJson)
  }

}
