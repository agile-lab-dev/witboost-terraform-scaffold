package it.agilelab.provisioners.terraform

private[terraform] class OutputStringBuffer extends ProcessOutput {
  private val outputLines: scala.collection.mutable.ListBuffer[String] = scala.collection.mutable.ListBuffer.empty

  override def clear(): Unit = outputLines.clear()

  override def addLine(line: String): Unit = outputLines += line

  override def buildString(): String = outputLines.mkString("\n")

}
