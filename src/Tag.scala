package com.lolcode.terribrowse
import ox.CSO._

class Tag(private val in: ?[Char]) extends MaybeTagMaybeText {

  private val content_ = new scala.collection.mutable.StringBuilder;

  repeat {
    val x = (in?)

    content_.append(x)

    (x != '>') // i.e. repeat the block while x != '>'
  }{}



  val content = content_.toString()

  System.out.println("im a Tag containing `" + content + "'")

  val next = MaybeTagMaybeTextFactory.create(in)

}