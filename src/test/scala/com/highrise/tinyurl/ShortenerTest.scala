package com.highrise.tinyurl

import org.scalatest.{FlatSpec, Matchers}

class ShortenerTest extends FlatSpec with Matchers {
  "Shortener" should "encode an Int as a short URL" in {
    val encoded = Shortener.encode(1234567890)

    System.out.println(encoded)

    encoded.length should be <= 20

    val decoded = Shortener.decode(encoded)

    System.out.println(decoded)

    decoded should ===(1234567890)
  }
}
