package it.agilelab.provisioners.terraform
import org.slf4j.{ Logger, LoggerFactory }

private[terraform] class OutputStringBuffer extends ProcessOutput {

  final private val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  private val outputLines: scala.collection.mutable.ListBuffer[String] = scala.collection.mutable.ListBuffer.empty

  override def clear(): Unit = outputLines.clear()

  override def addLine(line: String): Unit = {
    logger.info(line)
    outputLines += line
  }

  override def buildString(): String = outputLines.mkString("\n")

}
