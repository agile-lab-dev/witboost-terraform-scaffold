output "storage_account_id" {
  value = azurerm_storage_account.st_account.id
}
output "storage_account_name" {
  value = azurerm_storage_account.st_account.name
}

output "public_info" {
  value = {
    saLink = {
      type  = "string"
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