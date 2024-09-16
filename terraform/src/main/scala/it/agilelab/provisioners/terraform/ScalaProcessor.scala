package it.agilelab.provisioners.terraform

import scala.sys.process.{ Process, ProcessLogger }

private[terraform] class ScalaProcessor() extends Processor {
  override def run(command: String): ProcessResult = {
    val output                = new OutputStringBuffer()
    val logger: ProcessLogger = ProcessLogger({ line: String => output.addLine(line) })
    val exitCode              = Process(command).!(logger)
    new ProcessResult(exitCode, output)
  }
}
