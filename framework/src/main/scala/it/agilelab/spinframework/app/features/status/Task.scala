package it.agilelab.spinframework.app.features.status

import it.agilelab.spinframework.app.features.compiler.{ ErrorMessage, TerraformOutput }
import it.agilelab.spinframework.app.features.provision.ProvisioningStatus.{ Completed, Failed, Running }
import it.agilelab.spinframework.app.features.provision.{ ComponentToken, ProvisionResult, ProvisioningStatus }
import it.agilelab.spinframework.app.features.status.TaskOperation.TaskOperation
import it.agilelab.spinframework.app.features.status.TaskStatus.TaskStatus

// Task doesn't include provisionerId against the LLD as we only are implement Async Provisioning V1
case class Task(id: ComponentToken, status: TaskStatus, operation: TaskOperation, info: Info)

object Task {
  def fromProvisionResult(result: ProvisionResult, taskOperation: TaskOperation): Task =
    Task(
      result.componentToken,
      TaskStatus.fromProvisioningStatus(result.provisioningStatus),
      taskOperation,
      Info(
        result.errors,
        result.outputs
      )
    )

  def toProvisionResult(task: Task): ProvisionResult =
    ProvisionResult(
      TaskStatus.toProvisioningStatus(task.status),
      task.id,
      Option(task.info.errors).getOrElse(Seq.empty),
      Option(task.info.outputs).getOrElse(Seq.empty)
    )
}

object TaskStatus extends Enumeration {
  type TaskStatus = Value
  val WAITING, RUNNING, COMPLETED, FAILED = Value
  // where WAITING|RUNNING => RUNNING at API level

  def fromProvisioningStatus(status: ProvisioningStatus): TaskStatus = status match {
    case Completed => COMPLETED
    case Running   => RUNNING
    case Failed    => FAILED
  }

  def toProvisioningStatus(status: TaskStatus): ProvisioningStatus = status match {
    case WAITING | RUNNING => Running
    case COMPLETED         => Completed
    case FAILED            => Failed
  }

  def hasTerminated(status: TaskStatus): Boolean = status.equals(COMPLETED) || status.equals(FAILED)
}

object TaskOperation extends Enumeration {
  type TaskOperation = Value
  val VALIDATE, PROVISION, UNPROVISION, UPDATEACL = Value
}

case class Info(
  errors: Seq[ErrorMessage] = Seq.empty,
  outputs: Seq[TerraformOutput] = Seq.empty
)
