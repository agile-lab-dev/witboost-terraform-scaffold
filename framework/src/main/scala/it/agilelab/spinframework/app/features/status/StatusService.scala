package it.agilelab.spinframework.app.features.status

import cats.effect.IO
import it.agilelab.spinframework.app.features.provision.{ ComponentToken, ProvisionResult }

class StatusService(taskRepository: TaskRepository) extends GetStatus {
  override def statusOf(token: ComponentToken): IO[Option[ProvisionResult]] =
    taskRepository.findTask(token).map(io => io.map(Task.toProvisionResult))
}
