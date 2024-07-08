package it.agilelab.provisioners.terraform

private[terraform] class TerraformCommandsWrapper(
  directory: String,
  processor: Processor,
  outputInJson: Boolean,
  logger: TerraformLogger
) extends TerraformCommands {

  private val jsonOption = if (outputInJson) "-json" else ""

  private val inputOption = "-input=false"

  private def run(command: String) = {
    logger.println(() => command)
    val processResult = processor.run(command)
    val result        = new TerraformResult(processResult)
    logger.println(() => result.buildOutputString)
    result
  }

  override def doApply(vars: TerraformVariables): TerraformResult =
    run(s"terraform -chdir=$directory apply ${vars.toOptions} -auto-approve $jsonOption $inputOption")

  override def doPlan(vars: TerraformVariables): TerraformResult = {
    val result = run(s"terraform -chdir=$directory plan ${vars.toOptions} $jsonOption $inputOption -out tfplan")
    if (result.isSuccess) {
      processor.run(s"terraform -chdir=$directory show tfplan")
    }
    result
  }

  override def doInit(configs: BackendConfigs): TerraformResult =
    run(
      s"terraform -chdir=$directory init -reconfigure ${configs.toOptions}"
    ) // N.B. this command does not allow the -json option.

  override def doValidate(): TerraformResult =
    run(s"terraform -chdir=$directory validate $jsonOption")

  override def doDestroy(vars: TerraformVariables): TerraformResult =
    run(s"terraform -chdir=$directory destroy ${vars.toOptions} -auto-approve $jsonOption $inputOption")
}
