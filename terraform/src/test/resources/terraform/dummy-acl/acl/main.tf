resource "random_string" "random" {
  length           = 20
  special          = true
  override_special = "/@£$"
}