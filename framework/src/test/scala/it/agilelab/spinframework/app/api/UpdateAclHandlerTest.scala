package it.agilelab.spinframework.app.api

import cats.effect.IO
import com.typesafe.config.Config
import io.circe.Json
import it.agilelab.spinframework.app.api.generated.Resource
import it.agilelab.spinframework.app.api.generated.definitions.{
  ProvisionInfo,
  ProvisioningStatus,
  SystemError,
  UpdateAclRequest
}
import it.agilelab.spinframework.app.api.helpers.HandlerTestBase
import it.agilelab.spinframework.app.features.compiler.{ Compile, CompileResult, ErrorMessage, YamlDescriptor }
import it.agilelab.spinframework.app.features.provision.{ AsyncProvision, Provision, ProvisionResult }
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{ Method, Request, Response, Status }

class UpdateAclHandlerTest extends HandlerTestBase {

  class ProvisionStub extends Provision {
    override def doProvisioning(yamlDescriptor: YamlDescriptor, cfg: Config): ProvisionResult                =
      ProvisionResult.completed()
    override def doUnprovisioning(yaml: YamlDescriptor, removeData: Boolean, cfg: Config): ProvisionResult   =
      ProvisionResult.completed()
    override def doUpdateAcl(provisionInfo: ProvisionInfo, refs: Set[String], cfg: Config): ProvisionResult  =
      ProvisionResult.completed()
    override def doValidate(yamlDescriptor: YamlDescriptor, cfg: Config): ProvisionResult                    = ProvisionResult.completed()
    override def doReverse(useCaseTemplateId: String, catalogInfo: Json, inputParams: Json): ProvisionResult =
      ProvisionResult.completed()
  }

  "The server" should "return a 500 error" in {
    val failing = new ProvisionStub {
      override def doUpdateAcl(provisionInfo: ProvisionInfo, refs: Set[String], cfg: Config): ProvisionResult =
        throw new Exception("Error!")
    }

    val compileStub: Compile       = _ => CompileResult.failure(List(ErrorMessage("Error")))
    val handler                    = new SpecificProvisionerHandler(AsyncProvision.fromSyncProvision(failing), compileStub, null)
    val response: IO[Response[IO]] = new Resource[IO]()
      .routes(handler)
      .orNotFound
      .run(
        Request(method = Method.POST, uri = uri"datamesh.specificprovisioner/v1/updateacl")
          .withEntity(UpdateAclRequest(refs = Vector.empty, provisionInfo = ProvisionInfo("", "")))
      )

    check[SystemError](response, Status.InternalServerError) shouldBe true
  }

  "The server" should "return a 200 ok" in {

    val handler                    = new SpecificProvisionerHandler(AsyncProvision.fromSyncProvision(new ProvisionStub), null, null)
    val response: IO[Response[IO]] = new Resource[IO]()
      .routes(handler)
      .orNotFound
      .run(
        Request(method = Method.POST, uri = uri"datamesh.specificprovisioner/v1/updateacl")
          .withEntity(UpdateAclRequest(refs = Vector(), provisionInfo = ProvisionInfo("", "")))
      )

    val expected = new ProvisioningStatus(ProvisioningStatus.Status.Completed, "")
    check[ProvisioningStatus](response, Status.Ok, Some(expected)) shouldBe true
  }

}
