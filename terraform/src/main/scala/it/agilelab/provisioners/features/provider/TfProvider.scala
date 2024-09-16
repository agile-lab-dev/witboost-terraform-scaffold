package it.agilelab.provisioners.features.provider

import io.circe.{ parser, Encoder }
import io.circe.syntax.EncoderOps
import it.agilelab.provisioners.features.descriptor.TerraformOutputsDescriptor
import it.agilelab.provisioners.terraform._
import it.agilelab.spinframework.app.api.generated.definitions.Log
import it.agilelab.spinframework.app.features.compiler.circe.CirceParsedCatalogInfo
import it.agilelab.spinframework.app.features.compiler.{
  ComponentDescriptor,
  ErrorMessage,
  Field,
  ImportBlock,
  InputParams,
  ReverseChanges,
  TerraformOutput
}
import it.agilelab.spinframework.app.features.provision.{
  CloudProvider,
  ComponentToken,
  ProvisionResult,
  ProvisioningStatus
}
import it.agilelab.spinframework.app.utils.JsonPathUtils
import it.agilelab.spinframework.app.utils.LogUtils.addLog
import org.slf4j.{ Logger, LoggerFactory }

import java.nio.file.Path
import scala.util.{ Failure, Success }

class TfProvider(terraformBuilder: TerraformBuilder, terraformModule: TerraformModule) extends CloudProvider {

  final private val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  /** This method is a wrapper around the caller (provisioning/unprovisioning/updateAcl) methods. It adds some facilities that are in common between those methods
    * In detail, this wrapper creates the terraform context and performs the init. After having executed the caller logic, this wrapper also destroys the context
    * @param descriptor the component descriptor
    * @param pathBuilder a function that transform the path based on the caller needs
    * @param stateKeyMapper a function that is applied on the rendered state key.
    * @param f the provisioning/unprovisioning/updateAcl logic. The inputs are an instance of TerraformCommands and the path of the terraform context
    * @return the result of the wrapped operation
    */
  def withContext(
    descriptor: ComponentDescriptor,
    pathBuilder: String => String,
    stateKeyMapper: Option[String => String] = None,
    f: (TerraformCommands, String) => ProvisionResult
  ): ProvisionResult =
    FilesUtils.createTfContext(terraformModule.path) match {
      case Failure(e)           =>
        val msg = "An error occurred while creating the terraform context folder"
        logger.error(msg, e)
        ProvisionResult.failure(Seq(ErrorMessage.apply(msg)))
      case Success(contextPath) =>
        val terraform: TerraformCommands = terraformBuilder
          .onDirectory(pathBuilder(contextPath))

        init(descriptor, terraform, stateKeyMapper) match {
          case Left(l)  => ProvisionResult.failure(l)
          case Right(_) =>
            // -----------------------
            // Core logic happens here
            val res = f(terraform, contextPath)
            // -----------------------
            // delete the context
            FilesUtils.deleteDirectory(Path.of(contextPath))
            res
        }
    }

  override def provision(descriptor: ComponentDescriptor, mappedOwners: Set[String]): ProvisionResult =
    withContext(
      descriptor,
      contextPath => Path.of(contextPath).toString,
      None,
      (terraform, contextPath) =>
        variablesFrom(descriptor) match {
          case Left(l)     => ProvisionResult.failure(l)
          case Right(vars) =>
            val extendedVars   = TerraformVariables(vars.variables + ("ownerPrincipals" -> mappedOwners.mkString(",")))
            val reverseChanges = ReverseChanges.reverseChangesFromDescriptor(descriptor) match {
              case Left(err) =>
                logger.warn("ImportBlocks were not present, or it was not possible to retrieve them: {}", err)
                ReverseChanges(Seq(), skipSafetyChecks = false)
              case Right(r)  => r
            }
            FilesUtils.createImportFile(reverseChanges.imports, Path.of(contextPath)) match {
              case Success(_) =>
                val applyResult = terraform.doApply(extendedVars)
                if (applyResult.isSuccess) {
                  ProvisionResult.completed(
                    applyResult.terraformOutputs match {
                      case Right(r) => r.filter(!_.sensitive).map(o => TerraformOutput(name = o.name, o.value))
                      // If the parsing of terraform output fails, an empty seq is returned.
                      // The provisioning is considered successful, but no output will be returned back to the coordinator
                      case Left(f)  =>
                        logger.error("An error occurred while trying to extract outputs from terraform execution", f)
                        Seq.empty
                    }
                  )
                } else {
                  ProvisionResult.failure(applyResult.errorMessages.map(ErrorMessage))
                }
              case Failure(f) =>
                ProvisionResult.failure(Seq(ErrorMessage(f.getMessage)))
            }
        }
    )

  private def kindJsonPath(descriptorString: String): String =
    if (JsonPathUtils.isDataProductProvisioning(descriptorString)) {
      "$.kind"
    } else {
      "$.dataProduct.components[?(@.id == '{{componentIdToProvision}}')].kind"
    }

  override def unprovision(
    descriptor: ComponentDescriptor,
    mappedOwners: Set[String],
    removeData: Boolean
  ): ProvisionResult = {

    val kind = JsonPathUtils.getValue(descriptor.toString, kindJsonPath(descriptor.toString))
    kind match {
      case Right(k) =>
        // if it's not a storage, we don't care about removeData
        if (removeData || !k.equalsIgnoreCase("storage")) {
          // delete
          withContext(
            descriptor,
            contextPath => Path.of(contextPath).toString,
            None,
            (terraform, _) =>
              variablesFrom(descriptor) match {
                case Left(l)     => ProvisionResult.failure(l)
                case Right(vars) =>
                  val extendedVars =
                    TerraformVariables(vars.variables + ("ownerPrincipals" -> mappedOwners.mkString(",")))
                  val result       = terraform.doDestroy(extendedVars)
                  if (result.isSuccess)
                    ProvisionResult.completed()
                  else
                    ProvisionResult.failure(result.errorMessages.map(ErrorMessage))
              }
          )
        } else {
          logger.warn("Component unprovisioned without actions due to the removeData field")
          ProvisionResult.completed()
        }

      case Left(error) =>
        val errorMessage = s"It was not possible to retrieve the kind of the component to unprovision. Details: $error"
        logger.error(errorMessage)
        ProvisionResult.failure(Seq(ErrorMessage(errorMessage)))
    }

  }

  override def validate(descriptor: ComponentDescriptor): ProvisionResult = {

    val tfPath  = Path.of(terraformModule.path)
    val aclPath = Path.of(tfPath.toString, "acl")

    val aclModuleExists = FilesUtils.checkDirectory(aclPath) match {
      case Success(s) => s
      case Failure(f) =>
        logger.warn("Couldn't check the existence of the acl submodule, skipping its validation..", f)
        false
    }

    // Validate acl module
    val validateAclModule: ProvisionResult = if (aclModuleExists) {
      val r = withContext(
        descriptor,
        contextPath => Path.of(contextPath, "acl").toString,
        None,
        (terraform, _) => {
          // At this time we don't have principals, hence a "tf plan" is not possible
          // We proceed with a simpler "tf validate"
          val result = terraform.doValidate()
          if (result.isSuccess) {
            ProvisionResult.completed()
          } else {
            ProvisionResult.failure(result.validationErrors.map(ErrorMessage))
          }
        }
      )
      // Repack the errors to make the module explicit
      if (!r.isSuccessful) {
        ProvisionResult.failure(r.errors.map(e => ErrorMessage(s"[ACL module] ${e.description}")))
      } else {
        r
      }

    } else {
      logger.warn("Acl module doesn't exists, skipping..")
      ProvisionResult.completed()
    }

    // Validate and Plan main module
    val validateMainModule: ProvisionResult = {
      val r = withContext(
        descriptor,
        contextPath => Path.of(contextPath).toString,
        None,
        (terraform, contextPath) =>
          variablesFrom(descriptor) match {
            case Left(l)     => ProvisionResult.failure(l)
            case Right(vars) =>
              val reverseChanges = ReverseChanges.reverseChangesFromDescriptor(descriptor) match {
                case Left(err) =>
                  logger.warn("ImportBlocks were not present, or it was not possible to retrieve them: {}", err)
                  ReverseChanges(Seq(), skipSafetyChecks = false)
                case Right(r)  => r
              }
              FilesUtils.createImportFile(reverseChanges.imports, Path.of(contextPath)) match {
                case Success(_) =>
                  val res = terraform.doPlan(vars)
                  if (res.isSuccess) {
                    val plan = terraform.getHumanReadablePlan(res)
                    plan.fold(logger.warn("It was not possible to extract an human readable version of the plan"))(s =>
                      logger.debug(s)
                    )
                    res.terraformChanges match {
                      case Left(l)  =>
                        val error =
                          "It was not possible to parse the result of the plan, for safety reason we need to fail"
                        logger.error(error, l)
                        ProvisionResult.failure(Seq(ErrorMessage(l)))
                      case Right(r) =>
                        if (!reverseChanges.skipSafetyChecks && r.changes.removals > 0) {
                          val error =
                            s"The plan is proposing to destroy ${r.changes.removals} resources, but the skipSafetyChecks is disabled."
                          logger.warn(error)
                          ProvisionResult.failure(Seq(ErrorMessage(error)))
                        } else {
                          if (r.changes.removals > 0) {
                            logger.warn(
                              s"The plan is proposing to destroy ${r.changes.removals} resources, and skipSafetyChecks is enabled. Proceeding."
                            )
                          }
                          ProvisionResult.completed()
                        }
                    }
                  } else {
                    ProvisionResult.failure(res.errorMessages.map(ErrorMessage))
                  }
                case Failure(f) =>
                  ProvisionResult.failure(Seq(ErrorMessage(f.getMessage)))
              }
          }
      )
      // Repack the errors to make the module explicit
      if (!r.isSuccessful) {
        ProvisionResult.failure(r.errors.map(e => ErrorMessage(s"[Main module] ${e.description}")))
      } else {
        r
      }
    }

    if (validateAclModule.isSuccessful && validateMainModule.isSuccessful)
      ProvisionResult.completed()
    else {
      ProvisionResult.failure(validateAclModule.errors ++ validateMainModule.errors)
    }

  }

  override def updateAcl(
    resultDescriptor: ComponentDescriptor,
    requestDescriptor: ComponentDescriptor,
    refs: Set[String]
  ): ProvisionResult = {

    // since the key is the same for both main and acl modules, we need to differentiate them.
    // This mapper adds the suffix ".acl" to the key
    val stateKeyMapper: String => String = s => s"$s.acl"

    withContext(
      requestDescriptor,
      contextPath => Path.of(contextPath, "acl").toString,
      Some(stateKeyMapper),
      (terraformAcl, _) =>
        TerraformOutputsDescriptor(resultDescriptor).mapOutputs match {
          case Right(m) =>
            val v           = m + ("principals" -> refs.mkString(","))
            val vars        = new TerraformVariables(v)
            val applyResult = terraformAcl.doApply(vars)
            if (applyResult.isSuccess)
              ProvisionResult.completed()
            else
              ProvisionResult.failure(applyResult.errorMessages.map(ErrorMessage))
          case Left(l)  => ProvisionResult.failure(Seq(ErrorMessage(l.getMessage)))
        }
    )
  }

  override def reverse(
    useCaseTemplateId: String,
    catalogInfo: ComponentDescriptor,
    inputParams: InputParams
  ): ProvisionResult = {
    withContext(
      catalogInfo,
      contextPath => Path.of(contextPath).toString,
      None,
      (terraform, contextPath) =>
        FilesUtils.createImportFile(inputParams.importBlocks, Path.of(contextPath)) match {
          case Success(_)  =>
            variablesFrom(catalogInfo) match {
              case Right(vars)  =>
                val res            = terraform.doPlan(vars)
                val prettifiedPlan = terraform
                  .getHumanReadablePlan(res)
                  .getOrElse("It was not possible to extract an human readable version of the plan")
                if (res.isSuccess) {
                  val logs: Seq[Log] = Seq(addLog(prettifiedPlan, Log.Level.Info))
                  res.terraformChanges match {
                    case Right(planChanges) =>
                      val changes = planChanges.changes
                      if (!inputParams.skipSafetyChecks && changes.imports == 0) {
                        val error =
                          "Plan results in 0 resources to import. As a safety measure, the operation must be aborted."
                        ProvisionResult.failureWithLogs(logs ++ Seq(addLog(error, Log.Level.Error)))
                      } else if (!inputParams.skipSafetyChecks && changes.removals > 0) {
                        val error =
                          s"Plan results in the destroy of ${changes.removals} resources. As a safety measure, the operation must be aborted."
                        ProvisionResult.failureWithLogs(logs ++ Seq(addLog(error, Log.Level.Error)))
                      } else {
                        val changes = ReverseChanges(inputParams.importBlocks, inputParams.skipSafetyChecks).asJson(
                          ReverseChanges.customEncoder
                        )
                        ProvisionResult.completed(changes, logs)
                      }
                    case Left(l)            =>
                      val error = "It was not possible to parse the result of the plan"
                      logger.error(error, l)
                      ProvisionResult.failureWithLogs(
                        Seq(addLog(res.buildOutputString, Log.Level.Info), addLog(error, Log.Level.Error))
                      )
                  }
                } else {
                  ProvisionResult.failureWithLogs(res.errorMessages.map(em => addLog(em, Log.Level.Error)))
                }
              case Left(errors) =>
                errors.foreach(e =>
                  logger.error("It was not possible to extract variables from the catalog info: {}", e)
                )
                ProvisionResult.failureWithLogs(errors.map(e => addLog(e.description, Log.Level.Error)))
            }
          case Failure(ex) =>
            logger.error("It was not possible to create the import.tf file", ex)
            ProvisionResult.failureWithLogs(Seq(addLog(ex.getMessage, Log.Level.Error)))
        }
    ) match {
      case r @ ProvisionResult(ProvisioningStatus.Failed, _, _, _, _, _) =>
        r.copy(errors = Seq.empty, logs = r.logs ++ r.errors.map(_.description).map(d => addLog(d, Log.Level.Error)))
      case r                                                             => r
    }

  }

  def variablesFrom(
    descriptor: ComponentDescriptor,
    variableMappings: Option[Map[String, String]] = None
  ): Either[Seq[ErrorMessage], TerraformVariables] = {

    // read mappings from configs
    // e.g. resource_group_name -> component.specific.resource_group_name
    val mappings: Map[String, String] = variableMappings match {
      case None                                                         =>
        terraformModule.mappings
      case Some(x) if (descriptor.isInstanceOf[CirceParsedCatalogInfo]) =>
        // This is needed during the reverse provisioning, as I receive the catalog-info and not the DP descriptor
        // I need to replace the jsonPath coordinates in order to resolve the mappings
        // Mappings that are out of the scope of the component will fail to parse
        // Ticket for requesting the descriptor: WS-522
        x.map { m =>
          val newPath = m._2.replaceAll("\\$\\.dataProduct\\.components\\[.*\\]", "\\$.spec.mesh")
          (m._1, newPath)
        }
      case Some(x)                                                      => x
    }

    // for each key, take the corresponding value
    // e.g. resource_group_name -> sample_name
    val (lefts, right) = mappings
      .map(mapping =>
        JsonPathUtils.getValue(descriptor.toString, mapping._2) match {
          case Right(r) => Right(mapping._1 -> r)
          case Left(l)  => Left(l)
        }
      )
      .partitionMap(identity)

    if (lefts.isEmpty)
      Right(new TerraformVariables(right.toMap))
    else {
      Left(lefts.map(err => ErrorMessage(err)).toSeq)
    }
  }

  /** This method wraps the terraform init operation. It first prepares the backendConfigs, then inits.
    * @param descriptor the component descriptor
    * @param terraform the terraform executable wrapper
    * @param stateKeyMapper a function that is applied on the rendered state key.
    * @return if successful, the result of the init operation
    */
  def init(
    descriptor: ComponentDescriptor,
    terraform: TerraformCommands,
    stateKeyMapper: Option[String => String] = None
  ): Either[Seq[ErrorMessage], TerraformResult] =
    backendConfigsFrom(descriptor, stateKeyMapper) match {
      case Left(l)        =>
        logger.error("error while reading backendConfigs", l)
        Left(l)
      case Right(configs) =>
        val terraformInitResult = terraform.doInit(configs)
        if (!terraformInitResult.isSuccess) {
          // Since the `init` doesn't support json output, we're not able to provide detailed errors
          Left(
            List(
              "Failure during module initialization, we were unable to extract errors. There might be syntax errors."
            ).map(ErrorMessage)
          )
        } else {
          Right(terraformInitResult)
        }
    }

  /** This method renders the backendConfigs
    * @param descriptor the descriptor from which the values will be extracted
    * @param stateKeyMapper a function that is applied on the rendered state key.
    * @param stateKeyOverride meant for tests, is used to inject an arbitrary stateKey. This overrides the config file
    * @param backendConfigsMappingsOverride meant for tests, is used to inject the backendConfigs. This overrides the config file
    * @return
    */
  def backendConfigsFrom(
    descriptor: ComponentDescriptor,
    stateKeyMapper: Option[String => String] = None,
    stateKeyOverride: Option[String] = None,
    backendConfigsMappingsOverride: Option[Map[String, String]] = None
  ): Either[Seq[ErrorMessage], BackendConfigs] = {

    // use an identity mapping if the mapper is not defined
    val skMapper: (String => String) = stateKeyMapper match {
      case Some(m) => m
      case None    => (s) => s
    }

    // read mappings from configs
    // e.g. resource_group_name -> component.specific.resource_group_name
    val mappings: Map[String, String] = backendConfigsMappingsOverride match {
      case None                                                         =>
        terraformModule.backendConfigs
      case Some(x) if (descriptor.isInstanceOf[CirceParsedCatalogInfo]) =>
        // This is needed during the reverse provisioning, as I receive the catalog-info and not the DP descriptor
        // I need to replace the jsonPath coordinates in order to resolve the mappings
        // Mappings that are out of the scope of the component will fail to parse
        // Ticket for requesting the descriptor: WS-522
        x.map { m =>
          val newPath = m._2.replaceAll("\\$\\.dataProduct\\.components\\[.*\\]", "\\$.spec.mesh")
          (m._1, newPath)
        }
      case Some(x)                                                      => x
    }

    val stateKey: String = stateKeyOverride match {
      case None    =>
        terraformModule.stateKey
      case Some(x) => x
    }

    // for each key, take the corresponding value
    // e.g. resource_group_name -> sample_name
    val (lefts, right) = mappings
      // filter out the stateKey, it will be processed later on
      .filter(mapping => !mapping._1.equalsIgnoreCase(stateKey))
      .map(mapping =>
        JsonPathUtils.getValue(descriptor.toString, mapping._2) match {
          case Right(r) => Right(mapping._1 -> r)
          case Left(l)  => Left(l)
        }
      )
      .partitionMap(identity)

    // Render the stateKey and post-process with the key mapper
    val key = JsonPathUtils.getValue(descriptor.toString, mappings.getOrElse(stateKey, "")) match {
      case Right(r) => Right((stateKey, skMapper(r)))
      case Left(l)  => Left(l)
    }

    if (lefts.nonEmpty || key.isLeft) {
      Left((lefts.toSeq ++ key.left.toOption).map(err => ErrorMessage(err)))
    } else {
      Right(new BackendConfigs(right.toMap, key.getOrElse(null)))
    }
  }

}
