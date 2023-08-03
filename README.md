<p align="center">
    <a href="https://www.agilelab.it/witboost">
        <img src="docs/img/witboost_logo.svg" alt="witboost" width=600 >
    </a>
</p>

Designed by [Agile Lab](https://www.agilelab.it/), witboost is a versatile platform that addresses a wide range of sophisticated data engineering challenges. It enables businesses to discover, enhance, and productize their data, fostering the creation of automated data platforms that adhere to the highest standards of data governance. Want to know more about witboost? Check it out [here](https://www.agilelab.it/witboost) or [contact us!](https://www.agilelab.it/contacts)

This repository is part of our [Open Source projects](https://github.com/agile-lab-dev/witboost-open-source-projects) meant to showcase witboost's integration capabilities and provide a "batteries-included" product.

# Terraform Specific Provisioner

- [Overview](#overview)
- [Building](#building)
- [Running](#running)
- [Configuring](#configuring)
- [Deploying](#deploying)
- [HLD](docs/HLD.md)
- [API specification](docs/API.md)

## Overview

This project implements a simple Specific Provisioner that uses [Terraform](https://www.terraform.io/) to provision infrastructure as code. After deploying this microservice and configuring witboost to use it, the platform can deploy components using the Terraform capabilities.

### What's a Specific Provisioner?

A Specific Provisioner is a microservice which is in charge of deploying components that use a specific technology. When the deployment of a Data Product is triggered, the platform generates it descriptor and orchestrates the deployment of every component contained in the Data Product. For every such component the platform knows which Specific Provisioner is responsible for its deployment, and can thus send a provisioning request with the descriptor to it so that the Specific Provisioner can perform whatever operation is required to fulfill this request and report back the outcome to the platform.

You can learn more about how the Specific Provisioners fit in the broader picture [here](https://docs.witboost.agilelab.it/docs/p2_arch/p1_intro/#deploy-flow).

### Terraform

Terraform is an open-source infrastructure as code (IaC) tool developed by HashiCorp. It allows users to define and manage their cloud infrastructure in a declarative manner. With Terraform, you can define the desired state of your infrastructure using configuration files, typically written in HashiCorp Configuration Language (HCL) or JSON. These configuration files describe the various cloud resources and their configurations, such as virtual machines, storage, networks, and other services.

When you apply the Terraform configuration, it compares the current state of your infrastructure with the desired state defined in the configuration files. Terraform then automatically creates, updates, or deletes resources to align the infrastructure with the specified state. This enables consistent and reproducible deployments, making it easier to manage complex cloud environments and ensuring that the infrastructure stays in the desired state over time.

Terraform supports multiple cloud providers, including AWS, Microsoft Azure, Google Cloud Platform, and others, as well as various on-premises infrastructure providers. It has gained popularity among developers, sysadmins, and DevOps engineers for its simplicity, flexibility, and ability to automate the provisioning and management of cloud resources. You can find more information on it [here](https://developer.hashicorp.com/terraform).

### Software stack

This microservice is written in Scala 2.13, using HTTP4S for the HTTP layer. Project is built with SBT and supports packaging as JAR, fat-JAR and Docker image, ideal for Kubernetes deployments (which is the preferred option).

## Building

**Requirements:**

- Java 11
- SBT
- Terraform v1.4.4 or later (for running tests and for local usage)
- Docker (for building images only)

**Generating sources:** this project uses OpenAPI as standard API specification and the [sbt-guardrail](https://github.com/guardrail-dev/sbt-guardrail) plugin to generate server code from the [specification](./framework/src/main/openapi/interface-specification.yml).

The code generation is done automatically in the compile phase:

```bash
sbt compile
```

**Tests:** are handled by the standard task as well:

```bash
sbt test
```

**Artifacts & Docker image:** the project uses SBT Native Packager for packaging. Build artifacts with:

```
sbt package
```

The Docker image can be built with the standard docker CLI:

```
docker build -t <IMAGE TAG> .
```

*Note:* the version for the project is automatically computed using information gathered from Git, using branch name and tags. Unless you are on a release branch `1.2.x` or a tag `v1.2.3` it will end up being `0.0.0`. You can follow this branch/tag convention or update the version computation to match your preferred strategy.

**CI/CD:** the pipeline is based on GitLab CI as that's what we use internally. It's configured by the `.gitlab-ci.yaml` file in the root of the repository. You can use that as a starting point for your customizations.

## Running

To run the server locally, use:

```bash
sbt compile run
```

By default, the server binds to port 8080 on localhost. After it's up and running you can make provisioning requests to this address.

## Configuring

Most application configurations are handled with the Typesafe Config library. You can find the default settings in the `reference.conf`. Customize them and use the `config.file` system property or the other options provided by Typesafe Config according to your needs. The provided docker image expects the config file mounted at path `/config/application.conf`.

Logging is handled with Logback. Customize it and pass it using the `logback.configurationFile` system property. The provided docker image expects the logging config file mounted at path `/config/logback.xml`.

### Terraform configuration

Define your infrastructure using Terraform configuration files. These files typically have the extension .tf and are written in HashiCorp Configuration Language (HCL) or JSON format. In these files, you specify the desired state of your cloud resources, such as virtual machines, networks, databases, etc.

The configuration key `datameshProvisioner.terraform.repositoryPath` must point to a folder where the complete terraform configuration is present.

### Mapping

When creating terraform resources, a way to create terraform variables from the dataproduct descriptor is needed.
The `descriptorToVariablesMapping` (defined in `datameshProvisioner.terraform`) configuration is meant for this purpose: it allows to specify a list of mappings, where each one maps a terraform key to a dataproduct descriptor value.
The dataproduct descriptor value is accessed via JsonPath, which allows full flexibility in traversing the descriptor.

**Example**

Given the following descriptor

```yaml
dataProduct:
  name: xys-12345
  somearray: 
    - name: abc
      bar: xyz
```

and the following configuration

```
descriptorToVariablesMapping = {
    account_name = "$.dataProduct.name"
    foo = "$.dataProduct.somearray[1].bar"
}
```

The following vars would be produced

```terraform
-var account_name="xys-12345" -var foo="xyz"
```

#### componentIdToProvision

The incoming dataproduct descriptor contains all the components, not only the one that is going to be provisioned. In the descriptor, the correct component to provision is specified in the `componentIdToProvision` field.
In order to address the fields of that specific component, it is possible to use the placeholder `{{componentIdToProvision}}` that will be replaced with the correct id.

**Example**

In this example is shown how the `componentIdToProvision` replacement allows to address the `specific.resourceGroup` field of the component that is going to be provisioned.

Given the following descriptor:

```yaml
dataProduct:
  name: xys-12345
  components:
    - id: comp1
      specific:
        foo1: bar1
    - id: comp2
      specific:
        foo2: bar2
        resourceGroup: zoo
  componentIdToProvision: comp2
```

and the following configuration:

```
descriptorToVariablesMapping = {
    resource_group_name = "$.dataProduct.components[?(@.id == '{{componentIdToProvision}}')].specific.resourceGroup"
}
```
The following var would be produced:

```terraform
-var resource_group_name="zoo"
```

## Deploying

This microservice is meant to be deployed to a Kubernetes cluster.

## License

This project is available under the [Apache License, Version 2.0](https://opensource.org/licenses/Apache-2.0); see [LICENSE](LICENSE) for full details.

## About us

<p align="center">
    <a href="https://www.agilelab.it">
        <img src="docs/img/agilelab_logo.jpg" alt="Agile Lab" width=600>
    </a>
</p>

Agile Lab creates value for its Clients in data-intensive environments through customizable solutions to establish performance driven processes, sustainable architectures, and automated platforms driven by data governance best practices.

Since 2014 we have implemented 100+ successful Elite Data Engineering initiatives and used that experience to create Witboost: a technology agnostic, modular platform, that empowers modern enterprises to discover, elevate and productize their data both in traditional environments and on fully compliant Data mesh architectures.

[Contact us](https://www.agilelab.it/contacts) or follow us on:
- [LinkedIn](https://www.linkedin.com/company/agile-lab/)
- [Instagram](https://www.instagram.com/agilelab_official/)
- [YouTube](https://www.youtube.com/channel/UCTWdhr7_4JmZIpZFhMdLzAA)
- [Twitter](https://twitter.com/agile__lab)


