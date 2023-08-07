package it.agilelab.provisioners.terraform.local

import it.agilelab.provisioners.TestConfig
import it.agilelab.provisioners.terraform.TerraformLogger.logOnConsole
import it.agilelab.spinframework.app.config.ConfigurationModel
import it.agilelab.spinframework.app.features.support.test.FrameworkTestSupport
import it.agilelab.provisioners.terraform.{ Terraform, TerraformResult, TerraformVariables }
import org.scalatest.Assertion
import org.scalatest.concurrent.TimeLimitedTests
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.time.{ Millis, Span }

import java.util.concurrent.{ Executor, Executors }
import scala.concurrent.{ Await, ExecutionContext, Future }

/** Test the Terraform plan and apply operations does not wait for user input.
  *
  *  This test uses the terraform facade to execute init, validate, plan and apply
  *  on the repo "dummy-module". The dummy-module takes two input
  *  variables `numeric` and `length` and returns a random string as an output
  *  variable. For this test suite, the variable `length` is omitted.
  *
  *  If plan, apply and destroy are stuck waiting for user input,
  *  tests fail after a 3 seconds timeout.
  *
  *  @note By default this test uses the "dummy-module" included under
  *        `src/test/resources/terraform/dummy-module`. In order to point to
  *        an external repo instead, you need to override the configuration
  *        key `datamesh.terraform.repositoryPath`. The default path is
  *        configured in `test/resources/dummy-module-config.conf`.
  */
class TerraformTimeoutTest
    extends AnyFlatSpec
    with TerraformLocalTestBase
    with FrameworkTestSupport
    with TimeLimitedTests {

  private val terraform_repositoryPath = TestConfig
    .load("dummy-module-config.conf")
    .getString(ConfigurationModel.terraform_repositoryPath)

  private val terraform = Terraform()
    .outputInPlainText()
    .withLogger(logOnConsole)
    .onDirectory(terraform_repositoryPath)

  private val terraformJson = Terraform()
    .outputInJson()
    .withLogger(logOnConsole)
    .onDirectory(terraform_repositoryPath)

  private val variables: TerraformVariables = new TerraformVariables(
    Map(
      "numeric" -> "false"
      // Keep this variable omitted to check that Terraform notices it's missing.
      //"length" -> "6"
    )
  )

  override def timeLimit: Span = Span(3000, Millis)

  val singleThread: Executor                                  = Executors.newSingleThreadExecutor()
  implicit val singleThreadExecutionContext: ExecutionContext =
    ExecutionContext.fromExecutor(singleThread)

  private def shouldFail(terraformResult: TerraformResult, outputStrings: String*): Unit = {
    terraformResult.isSuccess shouldBe false
    for (outputString <- outputStrings) yield terraformResult.buildOutputString should include(outputString)
  }

  private def shouldFailJson(terraformResult: TerraformResult): Assertion = {
    terraformResult.isSuccess shouldBe false
    terraformResult.errorMessages.size should be > 0
  }

  // Expected error outputs of Terraform plan, apply and destroy operations
  // when any variable is missing.
  private val NO_VALUE_SET: String = "No value for required variable"
  private val LENGTH_ERROR: String = "\"length\" is not set"

  "Terraform" should "perform init and validate correctly" in {
    val initResult = terraform.doInit()
    shouldBeSuccess(initResult, "Terraform has been successfully initialized!")

    val validateResult: TerraformResult = terraform.doValidate()
    shouldBeSuccess(validateResult, "Success!", "The configuration is valid.")
  }

  "Terraform" should "perform init and validate correctly using -json option" in {
    val initResultJson = terraformJson.doInit()
    shouldBeSuccess(initResultJson)

    val validateResultJson: TerraformResult = terraformJson.doValidate()
    shouldBeSuccess(validateResultJson)
  }

  it should "not wait for user input during plan" in {
    val planResult = Await.result(
      Future {
        terraform.doPlan(variables)
      },
      timeLimit
    )

    shouldFail(planResult, NO_VALUE_SET, LENGTH_ERROR)
  }

  it should "not wait for user input during plan using -json flag" in {
    val planResultJson = Await.result(
      Future {
        terraformJson.doPlan(variables)
      },
      timeLimit
    )

    shouldFailJson(planResultJson)
  }

  it should "not wait for user input during apply" in {
    val applyResult = Await.result(
      Future {
        terraform.doApply(variables)
      },
      timeLimit
    )

    shouldFail(applyResult, NO_VALUE_SET, LENGTH_ERROR)
  }

  it should "not wait for user input during apply using -json flag" in {
    val applyResult = Await.result(
      Future {
        terraformJson.doApply(variables)
      },
      timeLimit
    )

    shouldFailJson(applyResult)
  }

  it should "not wait for user input during destroy" in {
    val destroyResult = Await.result(
      Future {
        terraform.doDestroy(variables)
      },
      timeLimit
    )

    shouldFail(destroyResult, NO_VALUE_SET, LENGTH_ERROR)
  }

  it should "not wait for user input during destroy using -json flag" in {
    val destroyResult = Await.result(
      Future {
        terraformJson.doDestroy(variables)
      },
      timeLimit
    )

    shouldFailJson(destroyResult)
  }

}
