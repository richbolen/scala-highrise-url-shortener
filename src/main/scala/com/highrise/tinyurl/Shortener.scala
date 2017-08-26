package com.highrise.tinyurl

object Shortener {
  val Alphabet: String = "23456789bcdfghjkmnpqrstvwxyzBCDFGHJKLMNPQRSTVWXYZ-_"
  val Base: Int = Alphabet.length()

  def encode(num: Int): String = {
    var str = new StringBuilder()
    var n = num * -1

    while (n > 0) {
      str.insert(0, Alphabet.charAt(n % Base))
      n = n / Base
    }

    return str.toString()
  }

  def decode(str: String): Int = {
    return 0.to(str.length() - 1).foldLeft(0)((num, i) => num * Base + Alphabet.indexOf(str.charAt(i)))
  }

}
