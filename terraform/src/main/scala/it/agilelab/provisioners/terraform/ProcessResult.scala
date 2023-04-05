package it.agilelab.provisioners.terraform

private[terraform] class ProcessResult(val exitCode: Int, output: ProcessOutput) {
  def buildOutputString: String = output.buildString()
}
