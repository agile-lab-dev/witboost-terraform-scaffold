package it.agilelab.spinframework.app.features.support.resources

import java.io.ByteArrayInputStream

trait Properties {
  def getProperty(name: String): String

}

object JavaProperties {
  def load(content: String): JavaProperties = {
    val properties = new java.util.Properties
    properties.load(new ByteArrayInputStream(content.getBytes))
    new JavaProperties(properties)
  }
}

class JavaProperties(props: java.util.Properties) extends Properties {
  override def getProperty(name: String): String = {
    val result = props.getProperty(name)
    if (result == null || result.isEmpty) throw MissingPropertyException()
    result
  }
}
