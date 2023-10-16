package it.agilelab.provisioners.terraform.unit

import it.agilelab.provisioners.features.provider.TfProvider
import it.agilelab.provisioners.terraform.{ Terraform, TerraformModuleLoader }
import it.agilelab.spinframework.app.features.compiler.ComponentDescriptor
import it.agilelab.spinframework.app.features.provision.ProvisioningStatus
import it.agilelab.spinframework.app.features.support.test._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import it.agilelab.provisioners.features.descriptor.TerraformOutputsDescriptor
import it.agilelab.provisioners.features.provider.TfProvider
import it.agilelab.provisioners.terraform.Terraform
import it.agilelab.spinframework.app.features.compiler.{ ComponentDescriptor, Parser, ParserFactory }
import it.agilelab.spinframework.app.features.provision.ProvisioningStatus
import it.agilelab.spinframework.app.features.support.test._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class TerraformVariablesTest extends AnyFlatSpec with should.Matchers with FrameworkTestSupport {

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
      |      - kind: workload
      |        id: urn:dmb:cmp:healthcare:vaccinations-nb:0:airbyte-workload
      |        description: Airbyte workload
      |        name: Airbyte Workload
      |        fullyQualifiedName: Airbyte Workload
      |        version: 0.0.0
      |        infrastructureTemplateId: urn:dmb:itm:airbyte-provisioner:0
      |        useCaseTemplateId: urn:dmb:utm:airbyte-standard:0.0.0
      |        dependsOn:
      |          - urn:dmb:cmp:healthcare:vaccinations-nb:0:snowflake-storage-vaccinations
      |          - urn:dmb:cmp:healthcare:vaccinations-nb:0:dbt-transformation-workload
      |        platform: Snowflake
      |        technology: airbyte
      |        workloadType: batch
      |        connectionType: DataPipeline
      |        tags: []
      |        readsFrom: []
      |        specific:
      |          source:
      |            name: healthcare.vaccinations-nb.0.File
      |            connectionConfiguration:
      |              url: https://storage.googleapis.com/covid19-open-data/v3/latest/vaccinations.csv
      |              format: csv
      |              provider:
      |                storage: HTTPS
      |                user_agent: true
      |              dataset_name: vaccinations_raw
      |          destination:
      |            name: healthcare.vaccinations-nb.0.Snowflake
      |            connectionConfiguration:
      |              database: HEALTHCARE
      |              schema: VACCINATIONSNB_0
      |          connection:
      |            name: healthcare.vaccinations-nb.0.Vaccinations NB File <> Snowflake
      |            dbtGitUrl: https://gitlab.com/AgileDmbSandbox/popeye/mesh.repository/sandbox/vaccinations_nb/dbt_transformation_workload.git
      |      - kind: workload
      |        id: urn:dmb:cmp:healthcare:vaccinations-nb:0:airflow-workload
      |        description: Scheduling for the Vaccinations DP
      |        name: Airflow Workload
      |        fullyQualifiedName: Airflow Workload
      |        version: 0.0.0
      |        infrastructureTemplateId: urn:dmb:itm:aws-workload-airflow-provisioner:0
      |        useCaseTemplateId: urn:dmb:utm:aws-airflow-workload-template:0.0.0
      |        dependsOn:
      |          - urn:dmb:cmp:healthcare:vaccinations-nb:0:snowflake-output-port
      |          - urn:dmb:cmp:healthcare:vaccinations-nb:0:airbyte-workload
      |        platform: AWS
      |        technology: airflow
      |        workloadType: batch
      |        connectionType: DataPipeline
      |        tags: []
      |        readsFrom: []
      |        specific:
      |          scheduleCron: 5 5 * * *
      |          dagName: airbyte_snowflake_dag.py
      |          destinationPath: dags/
      |          sourcePath: source/
      |          bucketName: aws-ia-mwaa-eu-west-1-621415221771
      |      - kind: workload
      |        id: urn:dmb:cmp:healthcare:vaccinations-nb:0:asdasd
      |        description: qqqq
      |        name: asdasd
      |        fullyQualifiedName: qweqwe
      |        version: 0.0.0
      |        infrastructureTemplateId: urn:dmb:itm:aws-workload-snowflake-sql-provisioner:0
      |        useCaseTemplateId: urn:dmb:utm:aws-workload-snowflake-sql-template:0.0.0
      |        dependsOn: []
      |        platform: AWS
      |        technology: airflow
      |        workloadType: batch
      |        connectionType: DataPipeline
      |        tags: []
      |        readsFrom: []
      |        specific: {}
      |      - kind: workload
      |        id: urn:dmb:cmp:healthcare:vaccinations-nb:0:dbt-transformation-workload
      |        description: DBT workload on Snowflake via Airbyte
      |        name: DBT Transformation Workload
      |        fullyQualifiedName: DBT Transformation Workload
      |        version: 0.0.0
      |        infrastructureTemplateId: urn:dmb:itm:aws-workload-dbt-transformation-provisioner:0
      |        useCaseTemplateId: urn:dmb:utm:aws-workload-dbt-transformation-template:0.0.0
      |        dependsOn: []
      |        platform: AWS
      |        technology: airflow
      |        workloadType: batch
      |        connectionType: DataPipeline
      |        tags: []
      |        readsFrom: []
      |        specific:
      |          dbtProjectName: dmb_dbt_transform
      |          gitUrl: https://gitlab.com/AgileDmbSandbox/popeye/mesh.repository/sandbox/vaccinations_nb/dbt_transformation_workload.git
      |      - kind: outputport
      |        id: urn:dmb:cmp:healthcare:vaccinations-nb:0:snowflake-output-port
      |        description: Output Port for vaccinations data using Snowflake
      |        name: Snowflake Output Port
      |        fullyQualifiedName: Snowflake Output Port
      |        version: 0.0.0
      |        infrastructureTemplateId: urn:dmb:itm:snowflake-outputport-provisioner:0
      |        useCaseTemplateId: urn:dmb:utm:snowflake-outputport-template:0.0.0
      |        dependsOn:
      |          - urn:dmb:cmp:healthcare:vaccinations-nb:0:snowflake-storage-vaccinations
      |        platform: Snowflake
      |        technology: Snowflake
      |        outputPortType: SQL
      |        creationDate: 2023-03-02T15:54:17.447Z
      |        startDate: 2023-03-02T15:54:17.447Z
      |        dataContract:
      |          schema:
      |            - name: date
      |              dataType: DATE
      |            - name: location_key
      |              dataType: TEXT
      |              constraint: PRIMARY_KEY
      |            - name: new_persons_vaccinated
      |              dataType: NUMBER
      |            - name: new_persons_fully_vaccinated
      |              dataType: NUMBER
      |            - name: new_vaccine_doses_administered
      |              dataType: NUMBER
      |            - name: cumulative_persons_vaccinated
      |              dataType: NUMBER
      |            - name: cumulative_persons_fully_vaccinated
      |              dataType: NUMBER
      |            - name: cumulative_vaccine_doses_administered
      |              dataType: NUMBER
      |        tags: []
      |        sampleData: {}
      |        semanticLinking: []
      |        specific:
      |          viewName: vaccinations_clean_view
      |          tableName: vaccinations_clean
      |          database: HEALTHCARE
      |          schema: VACCINATIONSNB_0
      |      - kind: storage
      |        id: urn:dmb:cmp:healthcare:vaccinations-nb:0:snowflake-storage-vaccinations
      |        description: Vaccinations data storage (schema) in Snowflake
      |        name: Snowflake Storage Vaccinations
      |        fullyQualifiedName: Snowflake Storage Vaccinations
      |        version: 0.0.0
      |        infrastructureTemplateId: urn:dmb:itm:snowflake-storage-provisioner:0
      |        useCaseTemplateId: urn:dmb:utm:snowflake-storage-template:0.0.0
      |        dependsOn: []
      |        platform: Snowflake
      |        technology: Snowflake
      |        StorageType: Database
      |        tags: []
      |        specific:
      |          database: HEALTHCARE
      |          schema: VACCINATIONSNB_0
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
      |            - name: new_persons_vaccinated
      |              dataType: NUMBER
      |            - name: new_persons_fully_vaccinated
      |              dataType: NUMBER
      |            - name: new_vaccine_doses_administered
      |              dataType: NUMBER
      |            - name: cumulative_persons_vaccinated
      |              dataType: NUMBER
      |            - name: cumulative_persons_fully_vaccinated
      |              dataType: NUMBER
      |            - name: cumulative_vaccine_doses_administered
      |              dataType: NUMBER
      |        tags: []
      |        sampleData: {}
      |        semanticLinking: []
      |        specific:
      |            customTableName: healthcare_vaccinationsnb_0_hasuraoutputportvaccinations
      |            select: healthcare_vaccinationsnb_0_hasuraoutputportvaccinations
      |            selectByPk: healthcare_vaccinationsnb_0_hasuraoutputportvaccinations_by_pk
      |            selectAggregate: healthcare_vaccinationsnb_0_hasuraoutputportvaccinations_agg
      |            selectStream: healthcare_vaccinationsnb_0_hasuraoutputportvaccinations_stream
      |            resourceGroup: healthcare_rg
      |componentIdToProvision: urn:dmb:cmp:healthcare:vaccinations-nb:0:hasura-output-port
      |
      |""".stripMargin
  )
  private val mockProcessor                   = new MockProcessor(0, "output")
  private val terraformBuilder                = Terraform()
    .processor(mockProcessor)
  private val tfProvider                      =
    new TfProvider(terraformBuilder, TerraformModuleLoader.from("urn:dmb:utm:airbyte-standard:0.0.0").getOrElse(null))

  "variablesFrom" should "return correct vars" in {

    val variableMappings = Some(
      Map(
        "resource_group_name" -> "$.dataProduct.components[?(@.id == '{{componentIdToProvision}}')].specific.resourceGroup",
        "some_type"           -> "$.dataProduct.components[?(@.id == '{{componentIdToProvision}}')].dataContract.schema[1].name",
        "some_double"         -> "$.dataProduct.doubleField"
      )
    )

    val vars = tfProvider.variablesFrom(descriptor, variableMappings)

    vars.isLeft shouldBe false
    vars.getOrElse(null).toOptions should (include("""-var some_type="location_key"""") and include(
      """-var some_double="33.9""""
    ))

  }

  "variablesFrom" should "return a failure" in {

    val variableMappings = Some(
      Map(
        "resource_group_name" -> "$.dataProduct.components[?(@.id == '{{componentIdToProvision}}')].specific.resourceGroup",
        "some_type"           -> "$.dataProduct.components[?(@.id == '{{componentIdToProvision}}')].dataContract.schema[1].name",
        "let_it_fail"         -> "$.dataProduct.components[?(@.id == '{{componentIdToProvision}}')].dataContract.schema[1].doesnt_exist"
      )
    )

    val vars = tfProvider.variablesFrom(descriptor, variableMappings)

    println(vars.left.getOrElse(null))

    vars.isLeft shouldBe true
    vars.left.getOrElse(null).size shouldBe 1

  }

  "variablesFrom" should "should take mappings from configs" in {

    val vars = tfProvider.variablesFrom(descriptor)

    vars.isLeft shouldBe false
    vars.getOrElse(null).toOptions should (include("""-var some_type="location_key""""))

  }

  "variablesFrom" should "fail with jsonPath exception" in {

    val variableMappings = Some(
      Map(
        "owner_name" -> "'$.dataProduct.dataProductOwnerDisplayName'"
      )
    )
    val vars             = tfProvider.variablesFrom(descriptor, variableMappings)
    vars.isLeft shouldBe true

  }

  "variablesFrom" should "fail with generic exception" in {

    val variableMappings = Some(
      Map(
        "resource_group_name" -> ""
      )
    )
    val vars             = tfProvider.variablesFrom(descriptor, variableMappings)
    vars.isLeft shouldBe true

  }

  "provisioning" should "succeed with variables from config" in {

    val res = tfProvider.provision(descriptor)
    res.provisioningStatus shouldBe ProvisioningStatus.Completed

  }

  "unprovisioning" should "succeed with variables from config" in {

    val res = tfProvider.unprovision(descriptor)
    res.provisioningStatus shouldBe ProvisioningStatus.Completed

  }

  "TerraformOutputsDescriptor" should "extracts the outputs from the descriptor" in {

    val descriptor = descriptorFrom(
      """
        |id: urn:dmb:dp:sales:invoices:0
        |info:
        |  publicInfo:
        |    taskId:
        |      type: string
        |      label: Task ID
        |      value: b4a8a5a2-af34-4b51-aeb8-7b9e9c9f48d4
        |  privateInfo:
        |    outputs:
        |      comp_id:
        |        value: "/subscriptions/61eabe24-6c0f-40d4-bd5c-4a7f9026e819/resourceGroups/witboost/providers/Microsoft.Storage/storageAccounts/tfspecificprovisioner"
        |      foo:
        |        value: bar
        |  latestProvisioningOperation:
        |    status: Successful
        |    operation: Deploy
        |    kind: dataproduct
        |name: Invoices
        |tags: []
        |""".stripMargin
    )

    val r = TerraformOutputsDescriptor(descriptor).mapOutputs

    r.isRight shouldBe true
    r.getOrElse(null).size shouldBe 2
    r.getOrElse(null).tail shouldBe Map("foo" -> "bar")
  }

  "The parser" should "parse the descriptor in json format correctly" in {

    val s =
      "{\"status\":\"COMPLETED\",\"result\":\"\",\"info\":{\"publicInfo\":{},\"privateInfo\":{\"outputs\":{\"comp_id\":{\"value\":\"/subscriptions/61eabe24-6c0f-40d4-bd5c-4a7f9026e819/resourceGroups/witboost/providers/Microsoft.Storage/storageAccounts/tfspecificprovisionertwo\"},\"comp_name\":{\"value\":\"tfspecificprovisionertwo\"}}}},\"logs\":null}"

    val parser: Parser = ParserFactory.parser()
    val p              = parser.parseJson(s)

    p.isInvalidInput shouldBe false
    p.descriptor.sub("info").sub("publicInfo").succeeded shouldBe true

  }

}
