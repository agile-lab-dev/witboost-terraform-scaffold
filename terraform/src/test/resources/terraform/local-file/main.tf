// creates a file whose name and contents are specified as variables
resource "local_file" "some_file" {
  filename = var.file_name
  content  = var.file_content
}
