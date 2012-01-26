package com.lolcode.terribrowse

import ox.CSO._

object Main {
  private def writestr(strong: String) : ?[Char] = {
    val out = OneOne[Char]

    proc {
      var i = 0
      repeat {
        out!(strong(i))
        i += 1

        (i < strong.length) // i.e. repeat the block while i < strong.length
      }{}
      out.close

      println("input complete :)")
    }.fork

    return out
  }
  
  def main(args: Array[String]) {
    com.lolcode.terribrowse.MaybeTagMaybeTextFactory.create(writestr("""<body class="question-page">
    <noscript><div id="noscript-padding"></div></noscript>
      <div id="notify-container"></div>
      <div id="overlay-header"></div>
      <div id="custom-header"></div>

      <div class="container">
        <div id="header">
          <div id="portalLink">
            <a class="genu" href="http://stackexchange.com" onclick="StackExchange.ready(function(){genuwine.click();});return false;">Stack Exchange</a>
          </div>
          <div id="topbar">
            <div id="hlinks">
              <span id="hlinks-user">

              </span>
"""))
  }
}