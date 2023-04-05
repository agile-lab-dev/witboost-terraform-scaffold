package it.agilelab.provisioners.terraform

import scala.sys.process.{ Process, ProcessLogger }

private[terraform] class ScalaProcessor(output: ProcessOutput) extends Processor {
  override def run(command: String): ProcessResult = {
    output.clear()
    val logger: ProcessLogger = ProcessLogger({ line: String => output.addLine(line) })
    val exitCode              = Process(command).!(logger)
    new ProcessResult(exitCode, output)
  }
}
