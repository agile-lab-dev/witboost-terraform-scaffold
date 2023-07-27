# witboost.Mesh.Provisioning.Terraform.SpecificProvisioner



## Getting started

...


## Configuration

...

### descriptorToVariablesMapping

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

Given the following descriptor

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

and the following configuration

```
descriptorToVariablesMapping = {
    resource_group_name = "$.dataProduct.components[?(@.id == '{{componentIdToProvision}}')].specific.resourceGroup"
}
```
The following var would be produced

```terraform
-var resource_group_name="zoo"
```