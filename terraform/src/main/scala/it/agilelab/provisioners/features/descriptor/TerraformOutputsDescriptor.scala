package it.agilelab.provisioners.features.descriptor

import io.circe.generic.auto._
import io.circe.parser.parse
import it.agilelab.spinframework.app.api.mapping.ProvisioningInfoMapper.InnerInfoJson
import it.agilelab.spinframework.app.features.compiler.ComponentDescriptor

case class TerraformOutputsDescriptor(descriptor: ComponentDescriptor) {

  /** Attempts to extract the outputs from the privateInfo section of the descriptor
    * @return the extracted outputs or a failure
    */
  def outputs: Either[Throwable, ComponentDescriptor] = {
    val d = descriptor.sub("info").sub("privateInfo").sub("outputs")
    if (d.succeeded)
      Right(d)
    else
      Left(new Throwable("It was not possible to extract outputs from the privateInfo object"))

  }

  /** Attempts to flatten the outputs
    * @return the outputs as a map or a failure
    */
  def mapOutputs: Either[Throwable, Map[String, String]] =
    for {
      o <- outputs
      p <- parse(o.toString)
      a <- p.as[Map[String, InnerInfoJson]]
    } yield a.map(k => (k._1, k._2.value))
}
