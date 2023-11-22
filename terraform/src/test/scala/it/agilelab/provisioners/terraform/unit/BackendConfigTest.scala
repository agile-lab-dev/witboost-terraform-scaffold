package it.agilelab.provisioners.terraform.unit

import it.agilelab.provisioners.features.provider.TfProvider
import it.agilelab.provisioners.terraform.{ Terraform, TerraformModuleLoader }
import it.agilelab.spinframework.app.features.compiler.ComponentDescriptor
import it.agilelab.spinframework.app.features.support.test.FrameworkTestSupport
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class BackendConfigTest extends AnyFlatSpec with should.Matchers with FrameworkTestSupport {

  private val descriptor: ComponentDescriptor = descriptorFrom(
    """
      |dataProduct:
      |    dataProductOwnerDisplayName: Nicolò Bidotti
      |    intField: 33
      |    doubleField: 33.9
      |    environment: development
      |    domain: healthcare
      |    kind: dataproduct
      |    domainId: urn:dmb:dmn:healthcare
      |    id: urn:dmb:dp:healthcare:vaccinations-nb:0
      |    description: DP about vaccinations
      |    devGroup: popeye
      |    ownerGroup: nicolo.bidotti_agilelab.it
      |    dataProductOwner: user:nicolo.bidotti_agilelab.it
      |    email: nicolo.bidotti@gmail.com
      |    version: 0.1.0
      |    fullyQualifiedName: Vaccinations NB
      |    name: Vaccinations NB
      |    informationSLA: 2BD
      |    maturity: Tactical
      |    useCaseTemplateId: urn:dmb:utm:dataproduct-template:0.0.0
      |    infrastructureTemplateId: urn:dmb:itm:dataproduct-provisioner:1
      |    billing: {}
      |    tags: []
      |    specific: {}
      |    components:
      |      - kind: outputport
      |        id: urn:dmb:cmp:healthcare:vaccinations-nb:0:hasura-output-port
      |        description: Output Port for vaccinations data using Hasura
      |        name: Hasura Output Port
      |        fullyQualifiedName: Hasura Output Port
      |        version: 0.0.0
      |        infrastructureTemplateId: urn:dmb:itm:hasura-outputport-provisioner:0
      |        useCaseTemplateId: urn:dmb:utm:hasura-outputport-template:0.0.0
      |        dependsOn:
      |          - urn:dmb:cmp:healthcare:vaccinations-nb:0:snowflake-output-port
      |        platform: Hasura
      |        technology: Hasura
      |        outputPortType: GraphQL
      |        creationDate: 2023-06-12T12:52:11.737Z
      |        startDate: 2023-06-12T12:52:11.737Z
      |        dataContract:
      |          schema:
      |            - name: date
      |              dataType: DATE
      |            - name: location_key
      |              dataType: TEXT
      |              constraint: PRIMARY_KEY
      |        tags: []
      |        sampleData: {}
      |        semanticLinking: []
      |        specific:
      |            resourceGroup: healthcare_rg
      |componentIdToProvision: urn:dmb:cmp:healthcare:vaccinations-nb:0:hasura-output-port
      |
      |""".stripMargin
  )
  private val mockProcessor                   = new MockProcessor(0, "output")
  private val terraformBuilder                = Terraform()
    .processor(mockProcessor)
  private val tfModule                        = TerraformModuleLoader.from("urn:dmb:utm:airbyte-standard:0.0.0")
  private val tfProvider: TfProvider          = new TfProvider(terraformBuilder, tfModule.getOrElse(null))

  "backendConfigsFrom" should "successfully extract values from descriptor" in {

    val configs = Some(
      Map(
        "key" -> "$.dataProduct.name",
        "foo" -> "$.dataProduct.intField"
      )
    )

    val res = tfProvider.backendConfigsFrom(descriptor, None, Some("key"), configs)
    res.isLeft shouldBe false
    res.getOrElse(null).toOptions should include("""-backend-config="key=Vaccinations NB"""")
    res.getOrElse(null).toOptions should include("""-backend-config="foo=33"""")

  }

  "backendConfigsFrom" should "fail if extraction does not work" in {

    val configs = Some(
      Map(
        "key" -> "$.dataProduct.name",
        "foo" -> "fail"
      )
    )

    val res = tfProvider.backendConfigsFrom(descriptor, None, Some("key"), configs)
    res.isLeft shouldBe true

  }

  "backendConfigsFrom" should "fail on empty mappings" in {

    val configs = Some(Map.empty[String, String])
    val res     = tfProvider.backendConfigsFrom(descriptor, None, Some("key"), configs)
    res.isLeft shouldBe true

  }

  "backendConfigsFrom" should "enforce config mappers" in {

    val descriptor: ComponentDescriptor = descriptorFrom(
      """
        |dataProduct:
        |    dataProductOwnerDisplayName: Jhon
        |    intField: 33
        |    billing: {}
        |    tags: []
        |
        |""".stripMargin
    )
    val configs                         = Some(
      Map(
        "key" -> "$.dataProduct.dataProductOwnerDisplayName",
        "foo" -> "$.dataProduct.intField"
      )
    )

    val f: String => String = (s: String) => s"$s.acl"

    val res = tfProvider.backendConfigsFrom(descriptor, Some(f), Some("key"), configs)

    res.isLeft shouldBe false
    res.getOrElse(null).toOptions should include("""-backend-config="key=Jhon.acl"""")

  }

  "backendConfigsFrom" should "use a different state key" in {

    val descriptor: ComponentDescriptor = descriptorFrom(
      """
        |dataProduct:
        |    dataProductOwnerDisplayName: Jhon
        |    intField: 33
        |    billing: {}
        |    tags: []
        |
        |""".stripMargin
    )
    val configs                         = Some(
      Map(
        "bar" -> "$.dataProduct.dataProductOwnerDisplayName",
        "foo" -> "$.dataProduct.intField"
      )
    )

    val f: String => String = (s: String) => s"$s.acl"

    val res = tfProvider.backendConfigsFrom(descriptor, Some(f), Some("bar"), configs)

    res.isLeft shouldBe false
    res.getOrElse(null).toOptions should include("""-backend-config="bar=Jhon.acl"""")

  }

  "backendConfigsFrom" should "use the default state key mapper" in {

    val descriptor: ComponentDescriptor = descriptorFrom(
      """
        |dataProduct:
        |    dataProductOwnerDisplayName: Jhon
        |    intField: 33
        |    billing: {}
        |    tags: []
        |
        |""".stripMargin
    )
    val configs                         = Some(
      Map(
        "key" -> "$.dataProduct.dataProductOwnerDisplayName",
        "foo" -> "$.dataProduct.intField"
      )
    )

    val res = tfProvider.backendConfigsFrom(descriptor, None, Some("key"), configs)

    res.isLeft shouldBe false
    res.getOrElse(null).toOptions should include("""-backend-config="key=Jhon"""")

  }
  "backendConfigsFrom" should "handle lefts" in {

    val descriptor: ComponentDescriptor = descriptorFrom(
      """
        |dataProduct:
        |    dataProductOwnerDisplayName: Jhon
        |    intField: 33
        |    billing: {}
        |    tags: []
        |
        |""".stripMargin
    )
    val configs                         = Some(
      Map(
        "key" -> "$.dataProduct.dataProductOwnerDisplayName",
        "foo" -> "$.dataProduct.doesntexist"
      )
    )

    val res = tfProvider.backendConfigsFrom(descriptor, None, Some("key"), configs)

    res.isLeft shouldBe true
    res.left.getOrElse(null).size shouldBe 1

  }

}
