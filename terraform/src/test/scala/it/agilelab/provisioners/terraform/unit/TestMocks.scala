package it.agilelab.provisioners.terraform.unit

import it.agilelab.provisioners.terraform.{ ProcessOutput, ProcessResult, Processor, TerraformLogger }

private[unit] class MockProcessor(val exitCode: Int = 0, output: String = "") extends Processor {
  private val buffer = new StringBuffer()

  override def run(command: String): ProcessResult = {
    buffer.append(command)
    new ProcessResult(exitCode, new MockProcessOutput(output))
  }

  def command: String = buffer.toString
}

class MockProcessOutput(output: String) extends ProcessOutput {
  override def clear(): Unit = {}
  override def addLine(line: String): Unit = {}
  override def buildString(): String = output
}

class MockLogger extends TerraformLogger {
  private val buffer: scala.collection.mutable.ListBuffer[String] = scala.collection.mutable.ListBuffer.empty

  override def println(line: () => String): Unit = buffer.append(line.apply())

  def lastLine: String = if (buffer.isEmpty) "" else buffer.last
}
