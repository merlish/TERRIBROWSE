package com.lolcode.terribrowse
import ox.CSO._

class Text(in : ?[Char]) extends MaybeTagMaybeText {
  
  private val content_ = new scala.collection.mutable.StringBuilder();

  private var x = ' ';

  repeat {
    x = (in?)

    if (x != '<')
      content_.append(x)

    (x != '<') // i.e. repeat the block while x != '<'
  }{}
  
  val content = content_.toString();
  
  System.out.println("im a text containing `" + content + "'")
  
  val next = MaybeTagMaybeTextFactory.create(Util.prependChar(x, in));
  
}