package it.agilelab.spinframework.app.features.compiler

object CompileResult                                                                            {
  def success(descriptor: ComponentDescriptor): CompileResult = CompileResult(Some(descriptor), Seq.empty)
  def failure(errors: Seq[ErrorMessage]): CompileResult       = CompileResult(None, errors)
}
case class CompileResult(optDescriptor: Option[ComponentDescriptor], errors: Seq[ErrorMessage]) {
  def descriptor: ComponentDescriptor = {
    require(this.isSuccess, "Verify that isSuccess is true before calling descriptor")
    optDescriptor.get
  }

  def isSuccess: Boolean = optDescriptor.isDefined
}
