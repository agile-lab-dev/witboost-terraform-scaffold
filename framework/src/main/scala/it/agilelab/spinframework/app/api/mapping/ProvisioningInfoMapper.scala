package it.agilelab.spinframework.app.api.mapping

import io.circe.JsonObject
import io.circe.generic.auto._
import io.circe.syntax._
import it.agilelab.spinframework.app.api.generated.definitions.Info
import it.agilelab.spinframework.app.features.provision.ProvisionResult

object ProvisioningInfoMapper {

  case class OutputsWrapper(outputs: Map[String, InnerInfoJson])
  case class InnerInfoJson(value: String)

  /** Given a provisionResult descriptor, it extracts the optional Info object
    * @param result: the provisionResult object
    */
  def from(result: ProvisionResult): Option[Info] = {

    if (result.outputs.isEmpty)
      return None

    val privateInfoMap: Map[String, InnerInfoJson] =
      result.outputs.map(o => (o.name, InnerInfoJson(o.value))).toMap

    Some(
      Info(
        publicInfo = JsonObject.empty.asJson,
        privateInfo = OutputsWrapper(privateInfoMap).asJson
      )
    )
  }
}
