# Principal-Mapping Samples

This module provides few implementations of [principalmapping-api](../principalmapping-api).

They are referred as plugins because we are leveraging SPI mechanism, hence they are pluggable in the specific provisioner.

## Identity mapping

This is an identity plugin, which means that the input is returned as output without any transformation.


## AWS IAM mapping

This plugin interacts with AWS IAM to return the ARN of the user matching the input.

```
["user:alice.foo.bar", "group:devs"] -> ["arn:aws:iam::account-id:user/alice", "arn:aws:iam::account-id:group/devs"]
```

The field that is looked up in the IAM directory to find a user (principals starting with `user:`) is the `username`, in this case `alice.foo.bar`.
The field that is looked up in the IAM directory to find a group (principals starting with `group:`)is the `group name`, in this case `devs`.

## Azure mapping

This plugin interacts with Azure to return the `ObjectId` of the user/group matching the input.

| Witboost identity           | Azure Object Id                      |
|-----------------------------|--------------------------------------|
| user:name.surname_email.com | 00000000-0000-0000-0000-000000000000 |
| group:dev                   | 11111111-1111-1111-1111-111111111111 |

The field that is looked up to find a user (principals starting with `user:`) is the `mail`, in this case `name.surname@email.com`.

The field that is looked up to find a group (principals starting with `group:`) is the `displayName`, in this case `dev`.

The plugin uses a service principal to authenticate against Microsoft Graph API. The following permissions are required for the service principal:
- `User.Read.All`
- `GroupMember.Read.All`

Here's an example configuration:

```
  principalMappingPlugin {
     pluginClass: "it.agilelab.plugin.principalsmapping.impl.azure.AzureMapperFactory"
     azure: {
        tenantId: "<Tenant Id>"
        clientId: "<Client Id>"
        clientSecret: "<Client Secret>"
     }
  }
```
