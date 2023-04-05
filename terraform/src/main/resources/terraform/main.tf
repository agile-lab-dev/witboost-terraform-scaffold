data "azurerm_resource_group" "rg" {
  name     = var.resource_group_name
}

resource "azurerm_storage_account" "st_account" {
  name                     = var.storage_account_name
  resource_group_name      = data.azurerm_resource_group.rg.name
  location                 =  var.storage_account_location
  account_tier             = "Standard"
  account_replication_type = "LRS"
  account_kind             = "StorageV2"
  is_hns_enabled           = "true"
}

resource "azurerm_storage_data_lake_gen2_filesystem" "filesystem" {
  name               = var.filesystem_name
  storage_account_id = azurerm_storage_account.st_account.id
}

resource "azurerm_storage_data_lake_gen2_path" "path" {
  storage_account_id = azurerm_storage_account.st_account.id
  filesystem_name = azurerm_storage_data_lake_gen2_filesystem.filesystem.name
  path = var.path
  resource = "directory"
}