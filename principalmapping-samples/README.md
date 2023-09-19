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