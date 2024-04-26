package it.agilelab.provisioners.features.provider

import it.agilelab.provisioners.features.descriptor.TerraformOutputsDescriptor
import it.agilelab.provisioners.terraform._
import it.agilelab.spinframework.app.features.compiler.{ ComponentDescriptor, ErrorMessage, TerraformOutput }
import it.agilelab.spinframework.app.features.provision.{ CloudProvider, ProvisionResult }
import it.agilelab.spinframework.app.utils.JsonPathUtils
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
    * @param f the provisioning/unprovisioning/updateAcl logic
    * @return the result of the wrapped operation
    */
  def withContext(
    descriptor: ComponentDescriptor,
    pathBuilder: String => String,
    stateKeyMapper: Option[String => String] = None,
    f: TerraformCommands => ProvisionResult
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
            val res = f(terraform)
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
      terraform =>
        variablesFrom(descriptor) match {
          case Left(l)     => ProvisionResult.failure(l)
          case Right(vars) =>
            val extendedVars = TerraformVariables(vars.variables + ("ownerPrincipals" -> mappedOwners.mkString(",")))
            val applyResult  = terraform.doApply(extendedVars)
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
            } else
              ProvisionResult.failure(applyResult.errorMessages.map(ErrorMessage))
        }
    )

  private val kindJsonPath = "$.dataProduct.components[?(@.id == '{{componentIdToProvision}}')].kind"

  override def unprovision(descriptor: ComponentDescriptor, removeData: Boolean): ProvisionResult = {

    val kind = JsonPathUtils.getValue(descriptor.toString, kindJsonPath)
    kind match {
      case Right(k) =>
        // if it's not a storage, we don't care about removeData
        if (removeData || !k.equalsIgnoreCase("storage")) {
          // delete
          withContext(
            descriptor,
            contextPath => Path.of(contextPath).toString,
            None,
            terraform =>
              variablesFrom(descriptor) match {
                case Left(l)     => ProvisionResult.failure(l)
                case Right(vars) =>
                  // when unprovisioning we can pass an empty set for ownerPrincipals, without doing the mapping
                  val extendedVars = TerraformVariables(vars.variables + ("ownerPrincipals" -> ""))
                  val result       = terraform.doDestroy(extendedVars)
                  if (result.isSuccess)
                    ProvisionResult.completed()
                  else
                    ProvisionResult.failure(result.errorMessages.map(ErrorMessage))
              }
          )
        } else {
          logger.warn(s"Component unprovisioned without actions due to the removeData field")
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
        terraform => {
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
        terraform =>
          variablesFrom(descriptor) match {
            case Left(l)     => ProvisionResult.failure(l)
            case Right(vars) =>
              val res = terraform.doPlan(vars)
              if (res.isSuccess) {
                ProvisionResult.completed()
              } else {
                ProvisionResult.failure(res.errorMessages.map(ErrorMessage))
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
      terraformAcl =>
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

  def variablesFrom(
    descriptor: ComponentDescriptor,
    variableMappings: Option[Map[String, String]] = None
  ): Either[Seq[ErrorMessage], TerraformVariables] = {

    // read mappings from configs
    // e.g. resource_group_name -> component.specific.resource_group_name
    val mappings: Map[String, String] = variableMappings match {
      case None    =>
        terraformModule.mappings
      case Some(x) => x
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
      case None    =>
        terraformModule.backendConfigs
      case Some(x) => x
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
