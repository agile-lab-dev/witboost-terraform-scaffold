package it.agilelab.spinframework.app.features.compiler

import io.circe.generic.codec.DerivedAsObjectCodec.deriveCodec
import io.circe.{ parser, Json }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class ReverseInputParamsTest extends AnyFlatSpec with should.Matchers {

  it should "correctly decode input params" in {

    val input =
      parser
        .parse("""
                 |{
                 |        "importBlocks": [
                 |            {
                 |                "id": "/subscriptions/00000000-0000-0000-0000-000000000000/resourceGroups/witboost/providers/Microsoft.Storage/storageAccounts/storageacctestreverse",
                 |                "to": "azurerm_storage_account.storage_account"
                 |            },
                 |            {
                 |                "id" : "https://storageacctestreverse.dfs.core.windows.net/default",
                 |                "to" : "azurerm_storage_data_lake_gen2_filesystem.filesystem[\"default\"]"
                 |            }
                 |        ],
                 |        "environmentSpecificConfig": {},
                 |        "skipSafetyChecks" : false
                 |
                 |    }
                 |""".stripMargin)
        .getOrElse(Json.Null)

    val decoded = (input.as[InputParams]).getOrElse(null)
    decoded.skipSafetyChecks shouldBe false
    decoded.importBlocks.size shouldBe 2
    decoded.importBlocks.head.id shouldBe "/subscriptions/00000000-0000-0000-0000-000000000000/resourceGroups/witboost/providers/Microsoft.Storage/storageAccounts/storageacctestreverse"
    decoded.importBlocks.head.to shouldBe "azurerm_storage_account.storage_account"
    decoded.importBlocks.last.id shouldBe "https://storageacctestreverse.dfs.core.windows.net/default"
    decoded.importBlocks.last.to shouldBe "azurerm_storage_data_lake_gen2_filesystem.filesystem[\"default\"]"

  }

}
