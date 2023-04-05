package it.agilelab.provisioners.terraform

private[terraform] trait ProcessOutput {
  def clear(): Unit
  def addLine(line: String): Unit
  def buildString(): String
}
