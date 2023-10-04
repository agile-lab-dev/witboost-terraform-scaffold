resource "azurerm_role_assignment" "example" {
  for_each = toset(split(",",var.principals))
  scope              = var.storage_account_id
  role_definition_name = "Storage Blob Data Reader"
  principal_id       = each.key
}