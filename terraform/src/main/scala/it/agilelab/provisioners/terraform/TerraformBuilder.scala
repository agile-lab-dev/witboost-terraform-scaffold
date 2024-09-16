package it.agilelab.provisioners.terraform

/** A builder that allows to specify a series of options and parameters to interact with Terraform.
  */
class TerraformBuilder {
  private var processor: Processor = new ScalaProcessor
  private var jsonOption           = false
  private var logger               = TerraformLogger.noLog

  /** Sets the class in charge to process command
    *
    * @param processor the processor executor
    */
  def processor(processor: Processor): TerraformBuilder = { this.processor = processor; this }

  /** Sets Json as the output format.
    *
    * @return this builder.
    */
  def outputInJson(): TerraformBuilder = { jsonOption = true; this }

  /** Sets plain text as the output format.
    *
    * @return this builder.
    */
  def outputInPlainText(): TerraformBuilder = { jsonOption = false; this }

  /** Sets the logger in charge to capture command line and its output.
    *
    * @return this builder.
    */
  def withLogger(logger: TerraformLogger): TerraformBuilder = { this.logger = logger; this }

  /** Sets the working directory for the execution of commands,
    * and returns an instance of the [[TerraformCommands]] interface,
    * on which you can actually invoke the desired command.
    *
    * @param directory is the working directory, containing Terraform configuration.
    * @return an instance of [[TerraformCommands]].
    */
  def onDirectory(directory: String): TerraformCommands =
    buildWrapper(directory)

  private def buildWrapper(directory: String) =
    new TerraformCommandsWrapper(directory, processor, jsonOption, logger)
}
