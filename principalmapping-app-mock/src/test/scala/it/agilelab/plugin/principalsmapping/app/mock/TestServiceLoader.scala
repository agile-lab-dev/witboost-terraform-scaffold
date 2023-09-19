package it.agilelab.plugin.principalsmapping.app.mock

import it.agilelab.plugin.principalsmapping.api.MapperFactory
import it.agilelab.plugin.principalsmapping.impl.aws.IamMapperFactory
import it.agilelab.plugin.principalsmapping.impl.identity.IdentityMapperFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

import java.util.ServiceLoader
import scala.jdk.CollectionConverters._

class TestServiceLoader extends AnyFlatSpec with should.Matchers  {

  val mappers: Iterable[MapperFactory] = ServiceLoader
    .load(classOf[MapperFactory])
    .asScala


  val identityMapper: MapperFactory = mappers
    .filter( _.getClass.eq(classOf[IdentityMapperFactory]))
    .head


  val iamIdentityMapperFactory : MapperFactory = mappers
    .filter( _.getClass.eq(classOf[IamMapperFactory]) )
    .head

  "ServiceLoader" should "load identityMapper" in {

    val principals = Set("user:Bob", "group:devs")
    val mapper = identityMapper.create()

    mapper.isSuccess shouldBe true

    val res = mapper.getOrElse(null).map(principals)

    res.size shouldBe 2
    res.head._2 shouldEqual Right(principals.head)
    res.last._2 shouldEqual Right(principals.last)

  }
}
