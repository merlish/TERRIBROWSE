package com.lolcode.terribrowse
import ox.CSO._

class MaybeTagMaybeText { }

object MaybeTagMaybeTextFactory {

  def create(in : ?[Char]) : MaybeTagMaybeText = {
    val (x, inn) = nomAndPeekChan(in)

    if (x == '<')
      return new Tag(inn)
    else
      return new Text(inn)
  }
  
  private def nomAndPeekChan(in : ?[Char]) : (Char, ?[Char]) = {
    val inm = Util.omNomWhitespace(in)
    val x = (inm?)
    (x, Util.prependChar(x, inm))
  }
  
}
