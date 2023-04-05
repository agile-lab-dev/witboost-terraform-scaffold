package it.agilelab.provisioners.features.descriptor

import it.agilelab.spinframework.app.features.compiler.{ ComponentDescriptor, Field }
import it.agilelab.spinframework.app.features.provision.ComponentToken

case class SpecificDescriptor(descriptor: ComponentDescriptor) {
  def specific = descriptor.sub("component").sub("specific")

}
