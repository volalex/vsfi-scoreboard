package utils

import eu.henkelmann.actuarius.{Transformer, Decorator}

/**
 * Created with IntelliJ IDEA.
 * User: volal_000
 * Date: 22.06.13
 * Time: 11:29
 * To change this template use File | Settings | File Templates.
 */
class XSSTransformer extends Transformer with Decorator {
  override def allowVerbatimXml(): Boolean = false

  override def deco(): Decorator = this


}
