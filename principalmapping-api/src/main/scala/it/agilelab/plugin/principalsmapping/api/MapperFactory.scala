package it.agilelab.plugin.principalsmapping.api

import com.typesafe.config.Config

import scala.util.Try

trait MapperFactory {
  /**
   * Creates a Mapper object that will be used to map subjects
   * @param config the config object that might be needed to create the Mapper
   * @return the mapper
   */
  def create( config : Config = null) : Try[Mapper]

  /**
   * The [[configIdentifier]] defines the config key that the consumer will use to specify the configuration of this plugin inside the `principalmappingplugin` block.
   * {{{
   *   principalmappingplugin {
   *     mymapperid {
   *       foo: "bar"
   *     }
   *   }
   * }}}
   * @return the identifier
   */
  def configIdentifier : String
}
