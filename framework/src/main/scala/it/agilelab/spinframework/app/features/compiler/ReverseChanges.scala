package it.agilelab.spinframework.app.features.compiler

import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.{ Decoder, Json, _ }
import it.agilelab.spinframework.app.utils.JsonPathUtils

/*
  This case class represents the changes returned by the reverse provisioning operation
 */
case class ReverseChanges(imports: Seq[ImportBlock], skipSafetyChecks: Boolean)

object ReverseChanges {

  private val importsPath: String          = "spec.mesh.specific.reverse.imports"
  private val skipSafetyChecksPath: String = "spec.mesh.specific.reverse.skipSafetyChecks"

  val customEncoder: Encoder[ReverseChanges] = (a: ReverseChanges) =>
    Json.obj(
      (importsPath, a.imports.asJson),
      (skipSafetyChecksPath, a.skipSafetyChecks.asJson)
    )

  val customDecoder: Decoder[ReverseChanges] = (c: HCursor) => {
    for {
      importBlocks     <- c.downField(importsPath).as[Seq[ImportBlock]]
      skipSafetyChecks <- c.downField(skipSafetyChecksPath).as[Boolean]
    } yield ReverseChanges(importBlocks, skipSafetyChecks)
  }

  def reverseChangesFromDescriptor(descriptor: ComponentDescriptor): Either[String, ReverseChanges] = {

    import io.circe.generic.auto._
    val importPath = "$.dataProduct.components[?(@.id == '{{componentIdToProvision}}')].specific.reverse"

    for {
      value          <- JsonPathUtils.getValue(descriptor.toString, importPath)
      json           <- parser.parse(value).left.map(_.message)
      reverseChanges <- json.as[ReverseChanges].left.map(_.message)
    } yield reverseChanges

  }

}
