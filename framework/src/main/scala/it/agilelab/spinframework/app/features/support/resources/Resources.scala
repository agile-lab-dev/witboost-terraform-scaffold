package it.agilelab.spinframework.app.features.support.resources

import scala.io.BufferedSource

class Resources() {
  def readFileAsString(fileName: String): String = {
    val resource: BufferedSource = scala.io.Source.fromResource(fileName)
    resource.getLines().mkString("\n")
  }

  def loadProperties(fileName: String): Properties =
    JavaProperties.load(readFileAsString(fileName))
}
