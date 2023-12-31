# Dummy Module

[[_TOC_]]
## Infrastructure Requirement

> :warning:  Nothing.


## Resource Schema

![](docs/resource-schema.png)

## Notes


## Requirements

| Name | Version |
|------|---------|
| <a name="requirement_random"></a> [random](#requirement_random) | >=3.3.0 |
## Providers

| Name | Version |
|------|---------|
| <a name="provider_random"></a> [random](#provider_random) | >=3.3.0 |
## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| <a name="input_length"></a> [length](#input_length) | Include numeric characters in the result | `number` | n/a | yes |
| <a name="input_numeric"></a> [numeric](#input_numeric) | The length of the string desired. | `bool` | n/a | yes |
## Outputs

| Name | Description |
|------|-------------|
| <a name="output_string"></a> [string](#output_string) | random string |
## Modules

No modules.
## Resources

| Name | Type |
|------|------|
| [random_string.random](https://registry.terraform.io/providers/hashicorp/random/latest/docs/resources/string) | resource |


## Usage
```terraform
module "dummy" {
  source  = "../../dummy-module"
  length  = 16
  numeric = true
}
```
## Alternate Usage
<details>
<summary>Click to browse more module usage examples.</summary>

```terraform

```

</details>

## Examples


## Contributing
* setup pre-commit

    ```bash
    pip3 install pre-commit
    ```

* install hooks

    ```bash
    pre-commit install
    ```

* (Optional) - Run against all the files

    ```bash
    pre-commit run --all-files
    ```
## Changelog

