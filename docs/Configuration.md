# Configuring the Terraform Specific Provisioner

Most of the provisioner configurations are handled with the Typesafe Config library. You can find the default settings in the `reference.conf` of each module. Customize them and use the `config.file` system property or the other options provided by Typesafe Config according to your needs. The provided docker image expects the config file mounted at path `/config/application.conf`.

## Provisioner configuration

| Configuration                                         | Description                                          | Default   |
|:------------------------------------------------------|:-----------------------------------------------------|:----------|
| `dataMeshProvisioner.networking.httpServer.interface` | Interface to bind the specific provisioner API layer | `0.0.0.0` |
| `dataMeshProvisioner.networking.httpServer.port`      | Port to bind the specific provisioner API layer      | `8093`    |

## Async configuration

Enable async handling on the Terraform Provisioner

| Configuration                                 | Description                                                                                                                     | Default |
|:----------------------------------------------|:--------------------------------------------------------------------------------------------------------------------------------|:--------|
| `dataMeshProvisioner.async.provision.enabled` | Enables the async provision/unprovision tasks. When enabled, this operations will return 202 with a token to be used on polling | `false` |
| `dataMeshProvisioner.async.type`              | Defines the type of repository to be used to store the status of the asynchronous tasks. Allowed values: [`cache`]              | `cache` |
| `dataMeshProvisioner.async.pool.size`         | Size of the thread pool used to execute asynchronous tasks                                                                      | 16      |


Logging is handled with Logback. Customize it and pass it using the `logback.configurationFile` system property. The provided docker image expects the logging config file mounted at path `/config/logback.xml`.

If you are deploying this provisioner with [helm](../helm), there are two places for these config files:
- values.yaml : in the values.yaml specific to your environment, you have the possibility to provide the configurations via `configOverride` and `lockbackOverride` keys. This is the preferred approach.
- [helm/files](../helm/files): contains the default values, they are not supposed to be changed by the user

## Terraform configuration

Define your infrastructure using Terraform configuration files. These files typically have the extension .tf and are written in HashiCorp Configuration Language (HCL) or JSON format. In these files, you specify the desired state of your cloud resources, such as virtual machines, networks, databases, etc.

With the multi-module feature, it is possible to define multiple isolated configuration files that can be used to handle provisioning/unprovisioning of different resources:

```
datameshProvisioner {
  ...
  terraform {
    moduleId1 {
      repositoryPath: "path-for-moduleId1"
      descriptorToVariablesMapping: {}
    }
    moduleId2 {
      repositoryPath: "path-for-moduleId2"
      descriptorToVariablesMapping: {}
    }
  }
}
```
where `moduleId1`,`moduleId2` are the `useCaseTemplateId` of the component to manage, declared in the DP Descriptor.

The configuration key `datameshProvisioner.terraform.<moduleId>.repositoryPath` must point to a folder where the complete terraform configuration is present.

## Mapping

When creating terraform resources, a way to create terraform variables from the dataproduct descriptor is needed.
The `descriptorToVariablesMapping` (defined in `datameshProvisioner.terraform.<moduleId>`) configuration is meant for this purpose: it allows to specify a list of mappings, where each one maps a terraform key to a dataproduct descriptor value.
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
-var account_name='xys-12345' -var foo='xyz'
```

In the previous example, both mappings were addressing a YAML leaf of the descriptor. By pointing to a YAML node instead, the full YAML object is extracted as a terraform variable.

For example, given the following descriptor

```yaml
dataProduct:
  name: xys-12345
  specific:
    complex:
      foo: bar
      fuz: buz
    list:
      - buzz
      - lightyear
```
and the following configuration

```
descriptorToVariablesMapping = {
    complex = "$.dataProduct.specific.complex"
    list = "$.dataProduct.specific.list"
}
```

The following vars would be produced

```terraform
-var complex='{"foo":"bar", "fuz":"buz"}' -var list='["buzz", "lightyear"]'
```

### componentIdToProvision

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
-var resource_group_name='zoo'
```

## State management
State management in Terraform is a critical aspect of its functionality, as it helps Terraform keep track of the current state of your infrastructure and enables it to make informed decisions about what changes need to be applied to achieve the desired infrastructure configuration. Terraform uses a state file to store information about the resources it manages, such as their current state, resource dependencies, and metadata.

Each configured module must handle its own state management, making sure to appropriately segregate DP components with a reasonable `state key` to avoid collisions and use a fault-tolerant and lockable `state store` (remote backends, such as Amazon S3, Azure Blob Storage, or HashiCorp Consul, are a good fit as they provide also better collaboration and security).
In order to dynamically set the `state key`, refer to the [Backend configurations](#backend-configurations) chapter.

It is important to notice that the backend configurations will be shared among the `main` and `acl` module. Since those two module must use separate state file, they cannot share the same state key.
For this reason, we preventively append the ".acl" suffix to the rendered state key.

### Backend configurations

In order to make the backend configuration dynamic, the `backendConfigs` block allow you to set [backend configurations](https://developer.hashicorp.com/terraform/language/settings/backends/configuration#command-line-key-value-pairs).
This block requires two object:
- `configs`: is a map that allows specifying key/value pairs, where keys will be the backendConfigs keys, while values will be processed as JsonPath, exactly as in the `descriptorToVariablesMapping` block.
- `stateKey`: is the string that identifies, in the `configs` map, the key corresponding to the state key. Since the stateKey can have different names depending on the provider (common names are "key" and "prefix") we cannot guess it. Since we need to apply further processing on it, we must know which one is the key.

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

and the following configuration snippet

```
backendConfigs = {
    stateKey = "key"
    configs = {
      key = "$.dataProduct.name"
      foo = "$.dataProduct.components[?(@.id == '{{componentIdToProvision}}')].specific.resourceGroup"
    }
  }
```

The following command would be produced for the main module:

```terraform
terraform init [...] -backend-config="key=xys-12345" -backend-config="foo=zoo"
```

The following command would be produced for the acl module:
```terraform
terraform init [...] -backend-config="key=xys-12345.acl" -backend-config="foo=zoo"
```


## UpdateACL

The `updateAcl` endpoint is invoked to request access to the resources provisioned by the `provision` endpoint.

> If we imagine the `provisioning` endpoint to create an S3 bucket, you will want to use the `updateAcl` to grant some users the priledges to access the bucket.

Given that this SP is cloud and technology-agnostic, the definition of the resources that will grant access is demanded to the module developer.

Within the terraform module, an `acl` folder can contain terraform files. This submodule is applied upon the invocation of the `updateAcl` endpoint.

> Going back to the bucket example, this submodule would contain the needed IAM resources, e.g. IAM policies, roles etc.

You can view a full example [here](../terraform/src/main/resources/terraform).

### Dynamic variables

When defining the acl resources, there will be variables that you will know only at runtime, i.e. upon invocation of the `updateAcl` endpoint.
For this reason, when terraform is run, some variables will be injected.
You need to put the definition of these variables in a `.tf` file.

*Example*
```
variable "principals" {}
variable "output1" {}
variable "output2" {}
variable "outputN" {}
```

The list of variables you need to expect follows.

**Principals**

The list of principals to use.
```
key: principals
value: list,of,principals
type: string
```

Example
> In the bucket example, principals can be ARNs of users and/or roles.
> The resulting terraform command would be
> ```
> terraform apply [...] -var principals=arn:aws:iam::12345:user/alice@foo.com,arn:aws:iam::12345:role/queen
> ```

**Terraform outputs**

The outputs generated by the `provisioning` endpoint. If you specified some outputs in the main module, they will be injected as variables during the apply of the acl module.
```
key: your-output-name
value: your-output-value
type: string
```

Example
> If your output.tf contains the following:
> ```
> output "storage_account_id" {
>   value = azurerm_storage_account.st_account.id
> }
> ```
> The resulting terraform command would be
> ```
> terraform apply [...] -var storage_account_id=foo.bar 
> ```

### Principal Mapping Plugin

When the `updateAcl` is queried, it requires a list of identities as parameter. This is the list of users/groups/roles that need the access.
These identities have a meaning in the Witboost context, but might not have sense in the context of your resources.

For this reason, the mapping is demanded to the `principalMappingPlugin`, which you can define with your own logic and simply plug it.

**Example**

> The plugin can map the Witboost identity into an AWS ARN
> ```
> `user:alice@foo.bar`  ->  `arn:aws:iam::12345:user/alice@foo.bar`
> ```

Here some [samples](../principalmapping-samples/README.md) we provide.

## Provisioning

The `provision` endpoint takes care about creating the requested resources.

In this phase, the `Data Product Owner` and the `Development Group` are extracted from the Data Product Descriptor and a mapping operation is executed, based on the configured [plugin](#Principal-Mapping-Plugin). If the mapping succeeds, the mapped principals are injected as a Terraform variable named `ownerPrincipals` (the entries are separated by `,`). For this reason, in every module you create, the following variable definition is needed:
```terraform
variable "ownerPrincipals" {
  type        = string
  description = "The identities that own the Data Product"
  default = ""
}
```
If you do not require this information, you can simply not use it in your terraform code.

### Outputs

Specific provisioners provide the possibility of returning results to witboost within the `Info` object. These results can be either private or public.
The `Info` object is a field that can contain different public and private details. Only details in the publicInfo object will be displayed to consumers in the Marketplace, while all the values put in the privateInfo will be stored in the deployed descriptor, but will not be shown in the Marketplace UI.
There is no limit to how many values can be set in those fields, but only the ones compliant with the following specification will be rendered in the "Technical Information" card of the Marketplace module. Invalid data is ignored and if there is no valid data to be shown, the "Technical Information" card will not be displayed.

By default the terraform provisioners will inject all non-sensitive outputs as **private info**.

If, on the other side, you want to return public info, you need to create an output called `public_info`, which must honor the schema requested by Witboost. This output will not be present in the `private_info`.

In the following example, a `public_info` that contains two elements is returned.

```terraform
output "public_info" {
  value = {
    saLink = {
      type  = "link"
      label = "Endpoint link"
      value = "Endpoint link"
      href  = "adls://foo.bar"
    },
    saName = {
      type  = "string"
      label = "Storage Account Name"
      value = "Foo"
    }
  }
}
```

You can refer to Witboost documentation for a better understanding of the requested public_info schema.

## Unprovisioning

The `unprovision` endpoint takes care about destroying the provisioned resources. The endpoint honours the `removeData` parameter, it will therefore skip the destroy operation for components of type `storage` when `false`.
