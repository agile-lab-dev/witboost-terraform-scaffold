package it.agilelab.spinframework.app.features.status

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.implicits.catsSyntaxParallelSequence1
import it.agilelab.spinframework.app.features.provision.ComponentToken
import it.agilelab.spinframework.app.features.status.TaskOperation.{ PROVISION, VALIDATE }
import it.agilelab.spinframework.app.features.status.TaskStatus.{ COMPLETED, FAILED, RUNNING, WAITING }
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should

class CacheRepositoryTest extends AsyncFlatSpec with should.Matchers with AsyncIOSpec {

  "CacheRepository" should "forceFail" in {
    val repo = new CacheRepository

    fill(
      repo,
      List(
        Task(ComponentToken("id1"), COMPLETED, PROVISION, Info()),
        Task(ComponentToken("id2"), RUNNING, PROVISION, Info()),
        Task(ComponentToken("id3"), WAITING, PROVISION, Info())
      )
    ) *>
      repo.forceFail().map { failedTasks =>
        failedTasks.length shouldEqual 2
        failedTasks.map(_.id.asString) should contain theSameElementsAs List("id2", "id3")
        failedTasks.foreach(_.status shouldEqual FAILED)
      }
  }

  it should "updateTask" in {
    val repo        = new CacheRepository
    val toBeUpdated = Task(ComponentToken("id2"), RUNNING, PROVISION, Info())
    fill(
      repo,
      List(
        Task(ComponentToken("id1"), COMPLETED, PROVISION, Info()),
        toBeUpdated,
        Task(ComponentToken("id3"), WAITING, PROVISION, Info())
      )
    ) *>
      repo.updateTask(toBeUpdated.copy(status = FAILED)).map { updatedTask =>
        updatedTask shouldEqual toBeUpdated.copy(status = FAILED)
      }
  }

  it should "findTask" in {
    val repo = new CacheRepository

    val id        = ComponentToken("id2")
    val toBeFound = Task(id, RUNNING, PROVISION, Info())

    fill(
      repo,
      List(
        Task(ComponentToken("id1"), COMPLETED, PROVISION, Info()),
        toBeFound,
        Task(ComponentToken("id3"), WAITING, PROVISION, Info())
      )
    ) *>
      repo.findTask(id).map { maybeTask =>
        maybeTask shouldNot be(None)
        maybeTask.get shouldEqual toBeFound
      }
  }

  it should "return None on find non-existing Task" in {
    val repo = new CacheRepository

    val id = ComponentToken("id4")

    fill(
      repo,
      List(
        Task(ComponentToken("id1"), COMPLETED, PROVISION, Info()),
        Task(ComponentToken("id2"), RUNNING, PROVISION, Info()),
        Task(ComponentToken("id3"), WAITING, PROVISION, Info())
      )
    ) *>
      repo.findTask(id).map { maybeTask =>
        maybeTask shouldBe None
      }
  }

  it should "createTask" in {
    val repo = new CacheRepository

    val toBeCreated = Task(ComponentToken("id4"), COMPLETED, VALIDATE, Info())

    fill(
      repo,
      List(
        Task(ComponentToken("id1"), COMPLETED, PROVISION, Info()),
        Task(ComponentToken("id2"), RUNNING, PROVISION, Info()),
        Task(ComponentToken("id3"), WAITING, PROVISION, Info())
      )
    ) *>
      repo.createTask(toBeCreated).map { newTask =>
        newTask shouldEqual toBeCreated
      }
  }

  it should "return stored task if already existent on createTask" in {
    val repo = new CacheRepository

    val toBeCreated  = Task(ComponentToken("id1"), RUNNING, VALIDATE, Info())
    val existingTask = Task(ComponentToken("id1"), COMPLETED, PROVISION, Info())

    fill(
      repo,
      List(
        existingTask,
        Task(ComponentToken("id2"), RUNNING, PROVISION, Info()),
        Task(ComponentToken("id3"), WAITING, PROVISION, Info())
      )
    ) *>
      repo.createTask(toBeCreated).map { newTask =>
        newTask shouldNot equal(toBeCreated)
        newTask shouldEqual existingTask
      }
  }

  it should "create existing task" in {
    val repo = new CacheRepository

    val toBeCreated = Task(ComponentToken("id4"), COMPLETED, VALIDATE, Info())

    fill(
      repo,
      List(
        Task(ComponentToken("id1"), COMPLETED, PROVISION, Info()),
        Task(ComponentToken("id2"), RUNNING, PROVISION, Info()),
        Task(ComponentToken("id3"), WAITING, PROVISION, Info())
      )
    ) *>
      repo.createTask(toBeCreated).map { newTask =>
        newTask shouldEqual toBeCreated
      }
  }

  it should "delete existing Task" in {
    val repo = new CacheRepository

    val id          = ComponentToken("id2")
    val toBeDeleted = Task(id, RUNNING, PROVISION, Info())

    fill(
      repo,
      List(
        Task(ComponentToken("id1"), COMPLETED, PROVISION, Info()),
        toBeDeleted,
        Task(ComponentToken("id3"), WAITING, PROVISION, Info())
      )
    ) *>
      repo.deleteTask(id).map { deletedTask =>
        deletedTask shouldNot be(None)
        deletedTask.get shouldEqual toBeDeleted
      }
  }

  it should "return None on deleting non-existing Task" in {
    val repo = new CacheRepository

    val id = ComponentToken("id4")

    fill(
      repo,
      List(
        Task(ComponentToken("id1"), COMPLETED, PROVISION, Info()),
        Task(ComponentToken("id2"), RUNNING, PROVISION, Info()),
        Task(ComponentToken("id3"), WAITING, PROVISION, Info())
      )
    ) *>
      repo.deleteTask(id).map { deletedTask =>
        deletedTask shouldBe None
      }
  }

  private def fill(repo: TaskRepository, tasks: Seq[Task]): IO[Seq[Task]] =
    tasks.map(task => repo.createTask(task)).parSequence

}
