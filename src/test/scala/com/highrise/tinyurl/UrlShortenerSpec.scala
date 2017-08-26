package com.highrise.tinyurl

import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}

class UrlShortenerSpec extends WordSpec with Matchers with ScalaFutures with ScalatestRouteTest
  with UrlShortenerRoutes {
  lazy val routes = urlShortenerRoutes

  "UrlShortenerRoutes" should {
    "return our form" in {
      // note that there's no need for the host part in the uri:
      val request = HttpRequest(uri = "/start")

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)

        // we expect the response to be json:
        contentType should ===(ContentTypes.`text/html(UTF-8)`)

        // and no entries should be in the list:
        entityAs[String] should include regex "html"
      }
    }

    "return a shortened url" in {
      // note that there's no need for the host part in the uri:
      val request = HttpRequest(uri = "/shorten?url=https:1234/some.url.com/path")

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)

        // we expect the response to be json:
        contentType should ===(ContentTypes.`text/plain(UTF-8)`)

        // and no entries should be in the list:
        entityAs[String] should include regex "shortenedUrl"
      }
    }
  }
}