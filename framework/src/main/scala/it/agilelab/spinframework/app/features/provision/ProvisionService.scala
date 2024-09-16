package it.agilelab.spinframework.app.features.provision

import com.typesafe.config.Config
import io.circe.Json
import io.circe.generic.codec.DerivedAsObjectCodec.deriveCodec
import it.agilelab.spinframework.app.api.generated.definitions.{ Log, ProvisionInfo }
import it.agilelab.spinframework.app.config.Configuration.provisionerConfig
import it.agilelab.spinframework.app.config.{ PrincipalMapperPluginLoader, SpecificProvisionerDependencies }
import it.agilelab.spinframework.app.features.compiler._
import it.agilelab.spinframework.app.features.compiler.circe.{ CirceParsedCatalogInfo, CirceParsedDescriptor }
import it.agilelab.spinframework.app.utils.JsonPathUtils
import it.agilelab.spinframework.app.utils.LogUtils.addLog
import org.slf4j.{ Logger, LoggerFactory }

import scala.util.{ Failure, Success }

class ProvisionService(
  compile: Compile,
  specific: SpecificProvisionerDependencies,
  principalMapperPluginLoader: PrincipalMapperPluginLoader
) extends Provision {

  final private val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  private def useCaseTemplateIdJsonPath(descriptorString: String): String =
    if (JsonPathUtils.isDataProductProvisioning(descriptorString)) {
      "$.useCaseTemplateId"
    } else {
      "$.dataProduct.components[?(@.id == '{{componentIdToProvision}}')].useCaseTemplateId"
    }

  override def doProvisioning(yamlDescriptor: YamlDescriptor, cfg: Config = provisionerConfig): ProvisionResult = {
    logger.info("Starting provisioning process")
    val result: CompileResult = compile.doCompile(yamlDescriptor)
    if (!result.isSuccess) return ProvisionResult.failure(result.errors)

    val res: Either[String, (CloudProvider, String, Set[String])] = for {
      useCaseTemplateId <-
        JsonPathUtils.getValue(result.descriptor.toString, useCaseTemplateIdJsonPath(result.descriptor.toString))
      cloudProvider     <- specific.cloudProvider(useCaseTemplateId)
      owners            <- extractOwners(result.descriptor)
    } yield (cloudProvider, useCaseTemplateId, owners)

    res match {
      case Left(message)                                     => ProvisionResult.failure(Seq(ErrorMessage(message)))
      case Right((cloudProvider, useCaseTemplateId, owners)) =>
        val moduleConfig = cfg.getConfig(s"""terraform."$useCaseTemplateId"""")
        mapWitOwners(owners, moduleConfig) match {
          case Right(mappedOwners) =>
            cloudProvider.provision(result.descriptor, mappedOwners)
          case Left(seq)           =>
            ProvisionResult.failure(seq.map(ErrorMessage))
        }

    }
  }

  override def doUnprovisioning(
    yamlDescriptor: YamlDescriptor,
    removeData: Boolean,
    cfg: Config = provisionerConfig
  ): ProvisionResult = {
    logger.info("Starting unprovisioning process")
    val result: CompileResult = compile.doCompile(yamlDescriptor)
    if (!result.isSuccess) return ProvisionResult.failure(result.errors)

    val res: Either[String, (CloudProvider, String, Set[String])] = for {
      useCaseTemplateId <-
        JsonPathUtils.getValue(result.descriptor.toString, useCaseTemplateIdJsonPath(result.descriptor.toString))
      cloudProvider     <- specific.cloudProvider(useCaseTemplateId)
      owners            <- extractOwners(result.descriptor)
    } yield (cloudProvider, useCaseTemplateId, owners)

    res match {
      case Left(message)                                     => ProvisionResult.failure(Seq(ErrorMessage(message)))
      case Right((cloudProvider, useCaseTemplateId, owners)) =>
        val moduleConfig = cfg.getConfig(s"""terraform."$useCaseTemplateId"""")
        mapWitOwners(owners, moduleConfig) match {
          case Right(mappedOwners) =>
            cloudProvider.unprovision(result.descriptor, mappedOwners, removeData)
          case Left(seq)           =>
            ProvisionResult.failure(seq.map(ErrorMessage))
        }

    }
  }

  override def doValidate(yamlDescriptor: YamlDescriptor): ProvisionResult = {

    val result: CompileResult = compile.doCompile(yamlDescriptor)
    if (!result.isSuccess) return ProvisionResult.failure(result.errors)

    val res = for {
      useCaseTemplateId <-
        JsonPathUtils.getValue(result.descriptor.toString, useCaseTemplateIdJsonPath(result.descriptor.toString))
      cloudProvider     <- specific.cloudProvider(useCaseTemplateId)
    } yield cloudProvider
    res match {
      case Right(cloudProvider) => cloudProvider.validate(result.descriptor)
      case Left(message)        => ProvisionResult.failure(Seq(ErrorMessage(message)))
    }
  }

  override def doUpdateAcl(
    provisionInfo: ProvisionInfo,
    refs: Set[String],
    cfg: Config = provisionerConfig
  ): ProvisionResult = {

    val jsonDescriptor            = JsonDescriptor(provisionInfo.result)
    val jsonResult: CompileResult = compile.doCompile(jsonDescriptor)
    if (!jsonResult.isSuccess) return ProvisionResult.failure(jsonResult.errors)

    val yamlDescriptor            = YamlDescriptor(provisionInfo.request)
    val yamlResult: CompileResult = compile.doCompile(yamlDescriptor)
    if (!yamlResult.isSuccess) return ProvisionResult.failure(yamlResult.errors)

    val res = for {
      useCaseTemplateId <- JsonPathUtils.getValue(
                             yamlResult.descriptor.toString,
                             useCaseTemplateIdJsonPath(yamlResult.descriptor.toString)
                           )
      cloudProvider     <- specific.cloudProvider(useCaseTemplateId)
    } yield (cloudProvider, useCaseTemplateId)

    res match {
      case Left(message)                             => ProvisionResult.failure(Seq(ErrorMessage(message)))
      case Right((cloudProvider, useCaseTemplateId)) =>
        val moduleConfig = cfg.getConfig(s"""terraform."$useCaseTemplateId"""")
        principalMapperPluginLoader.load(moduleConfig) match {
          case Success(m) =>
            m.map(refs).partition(_._2.isLeft) match {
              // Fail if there's one failed mapping
              case (l, _) if l.nonEmpty =>
                ProvisionResult.failure(
                  l.map(e =>
                    ErrorMessage(s"An error occurred while mapping the subject `${e._1}`. Detailed error: ${e._2.left}")
                  ).toSeq
                )
              case (_, r)               =>
                val principals = r.map(_._2.getOrElse(null)).toSet
                cloudProvider.updateAcl(jsonResult.descriptor, yamlResult.descriptor, principals)
            }
          case Failure(f) =>
            logger.error("Error in doUpdateAcl", f)
            ProvisionResult.failure(
              Seq(
                ErrorMessage(
                  s"An unexpected error occurred while instantiating the Principal Mapper Plugin. Please try again later. If the issue still persists, contact the platform team for assistance! Detailed error: ${f.getMessage}"
                )
              )
            )
        }
    }

  }

  override def doReverse(useCaseTemplateId: String, catalogInfo: Json, rawInputParams: Json): ProvisionResult = {
    logger.info("Starting reverse provisioning process")

    val componentDescriptor: ComponentDescriptor = CirceParsedCatalogInfo(catalogInfo)
    val inputParams: Either[String, InputParams] = rawInputParams
      .as[InputParams]
      .fold(
        l => {
          val msg = "Could not decode input params"
          logger.error(msg, l)
          Left(msg)
        },
        r => Right(r)
      )

    val res: Either[String, (CloudProvider, String, InputParams)] = for {
      inputParams   <- inputParams
      cloudProvider <- specific.cloudProvider(useCaseTemplateId)
    } yield (cloudProvider, useCaseTemplateId, inputParams)

    res match {
      case Right((cloudProvider, useCaseTemplateId, inputParams)) =>
        cloudProvider.reverse(useCaseTemplateId, componentDescriptor, inputParams)
      case Left(message)                                          =>
        logger.error("Could not build the cloud provider: {}", message)
        ProvisionResult.failureWithLogs(Seq(addLog(message, Log.Level.Error)))
    }

  }

  private def mapWitOwners(owners: Set[String], config: Config): Either[Seq[String], Set[String]] =
    principalMapperPluginLoader.load(config) match {
      case Success(m) =>
        m.map(owners).partition(_._2.isLeft) match {
          // Fail if there's one failed mapping
          case (l, _) if l.nonEmpty =>
            Left(
              l.map(e =>
                s"An error occurred while mapping the subject `${e._1}`. Detailed error: ${e._2.left.getOrElse(null).getMessage}"
              ).toSeq
            )
          case (_, r)               =>
            val mappedOwners = r.map(_._2.getOrElse(null)).toSet
            Right(mappedOwners)
        }
      case Failure(f) =>
        logger.error("Error in mapping Witboost identities in Cloud identities", f)
        Left(
          Seq(
            s"An unexpected error occurred while instantiating the Principal Mapper Plugin. Please try again later. If the issue still persists, contact the platform team for assistance! Detailed error: ${f.getMessage}"
          )
        )
    }

  private def extractOwners(descriptor: ComponentDescriptor): Either[String, Set[String]] = {
    val prefix           = if (JsonPathUtils.isDataProductProvisioning(descriptor.toString)) "" else ".dataProduct"
    val dpOwnerJsonPath  = "$" + s"${prefix}.dataProductOwner"
    val devGroupJsonPath = "$" + s"${prefix}.devGroup"
    for {
      dpOwner          <- JsonPathUtils.getValue(descriptor.toString, dpOwnerJsonPath)
      devGroup         <- JsonPathUtils.getValue(descriptor.toString, devGroupJsonPath)
      devGroupPrefixed <- if (devGroup.startsWith("group:")) Right(devGroup) else Right("group:".concat(devGroup))
    } yield Set(dpOwner, devGroupPrefixed)
  }

}
