package it.agilelab.plugin.principalsmapping.impl.aws

import it.agilelab.plugin.principalsmapping.api.Mapper
import org.slf4j.{Logger, LoggerFactory}
import software.amazon.awssdk.services.iam.IamClient
import software.amazon.awssdk.services.iam.model.{GetGroupRequest, GetUserRequest}

import scala.util.{Failure, Success, Try}


class IamMapper(client: IamClient) extends Mapper {

  final private val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  override def map(subjects: Set[String]): Map[String, Either[Throwable, String]] = {
    subjects.map( subj => {
      val mapping : Either[Throwable, String] = SubjectType.mapType(subj) match {
        case SubjectType.user => getUserByUsername(SubjectType.extract(subj), client)
        case SubjectType.group => getGroupByName(SubjectType.extract(subj), client)
        case SubjectType.unknown => Left(new Throwable(s"$subj is unknown"))
      }
      (subj -> mapping)
    }).toMap
  }

  private def makeRequest( f : => String ): Either[Throwable, String] = {
    Try {
      f
    } match {
      case Success(x) => Right(x)
      case Failure(f) =>
        logger.error("Got an error while looking up the user/group in IAM", f)
        Left(f)
    }
  }

  def getGroupByName(name: String, iam: IamClient): Either[Throwable, String] = {
    val request = GetGroupRequest.builder().groupName(name).build()
    makeRequest(
      iam.getGroup(request).group.arn
    )
  }

  def getUserByUsername(username: String, iam: IamClient): Either[Throwable, String] = {
    val request = GetUserRequest.builder().userName(username).build()
    makeRequest{
      iam.getUser(request).user.arn
    }
  }
}