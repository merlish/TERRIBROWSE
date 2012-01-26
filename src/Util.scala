package com.lolcode.terribrowse
import ox.CSO._

object Util {

  def prependChar(pre : Char, in : ?[Char]): ?[Char] = {
    val out = OneOne[Char]

    proc {
      out!pre
      repeat {
        out!(in?)
      }
    }.fork

    return out
  }

  def omNomWhitespace(in : ?[Char]) : ?[Char] = {
    val out = OneOne[Char]

    val isWhitespace = ((x : Char) => (x == ' ') || (x == '\r') || (x == '\n'))
    
    proc {
      // om nom whitespace
      repeat {
        val x = (in?)

        if (!isWhitespace(x))
          out!x

        isWhitespace(x) // continue while om nom whitespace
      }{}
      
      // alright, just copy now
      repeat {
        out!(in?)
      }
      
      in.close
      out.close
      
    }.fork

    return out
  }
}