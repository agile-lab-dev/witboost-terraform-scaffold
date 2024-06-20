package it.agilelab.spinframework.app.api.mapping

import io.circe.generic.auto._
import io.circe.generic.semiauto.deriveDecoder
import io.circe.syntax._
import io.circe.{ Decoder, Json, JsonObject }
import it.agilelab.spinframework.app.api.generated.definitions.Info
import it.agilelab.spinframework.app.features.provision.ProvisionResult
import org.slf4j.{ Logger, LoggerFactory }

object ProvisioningInfoMapper {

  final private val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  case class OutputsWrapper(outputs: Map[String, InnerInfoJson])
  case class InnerInfoJson(value: String)
  case class InnerJson(`type`: String, label: String, value: String, href: Option[String])

  /** Given a provisionResult descriptor, it extracts the optional Info object
    * If the output name is public_info, the output is returned in the publicInfo section, if it is compliant with the structure defined by Witboost
    * All other ourputs are put if in the private_info privateInfo section
    * @param result: the provisionResult object
    * @return Optionally returns an Info object
    */
  def from(result: ProvisionResult): Option[Info] = {

    implicit val innerJsonDecoder: Decoder[InnerJson] = deriveDecoder[InnerJson]

    if (result.outputs.isEmpty)
      return None

    val (publicInfo, privateInfo) = result.outputs.partition(p => p.name.equals("public_info"))

    // privateInfo must be string, as they have to be injected as variables in the updateAcl module
    // non-string outputs are ignored
    val (outputsOk, outputsKo) = privateInfo.map(o => (o.name, o.value.asString)).partition(o => o._2.nonEmpty)
    val privateInfoMap         = outputsOk.map(o => (o._1, InnerInfoJson(o._2.get))).toMap
    outputsKo.foreach(ko => logger.warn(s"Output ${ko._1} is not a string. Will be ignored"))

    val publicInfoMap = if (publicInfo.nonEmpty) {
      // by definition, outputs have unique names, it is safe to head
      publicInfo.head.value.as[Map[String, Json]] match {
        case Right(r) =>
          r.partitionMap { p =>
            p._2.as[InnerJson] match {
              case Right(r) => Right(p._1 -> r)
              case Left(f)  =>
                logger.warn(
                  s"Terraform output ${p._1} does not match the schema required from Witboost, it will not be returned",
                  f
                )
                Left(p._1 -> p._2)
            }
          }._2.toMap.asJson
        case Left(f)  =>
          logger.error(s"Terraform output public_info cannot be parsed, it will not be returned", f)
          JsonObject.empty.asJson

      }
    } else JsonObject.empty.asJson

    Some(
      Info(
        publicInfo = publicInfoMap,
        privateInfo = OutputsWrapper(privateInfoMap).asJson
      )
    )
  }
}
