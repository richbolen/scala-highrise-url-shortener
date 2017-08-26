package com.highrise.tinyurl

import java.net.URL

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import reactivemongo.api.MongoDriver
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future, Promise}
import scala.util.{Failure, Success, Try}

trait UrlShortenerRoutes {

  case class ShortenedUrl(_id: BSONObjectID, uniqueId: Int, url: String, shortenedUrl: String)

  implicit def system: ActorSystem

  lazy val log = Logging(system, classOf[UrlShortenerRoutes])

  import scala.concurrent.ExecutionContext.Implicits.global

  var urlsCollection: BSONCollection =
    Await.result(
      new MongoDriver()
        .connection(List("localhost"))
        .database("url-shortener")
        .map(_.collection("urls")), 5 seconds)

  implicit def urlWriter: BSONDocumentWriter[ShortenedUrl] = Macros.writer[ShortenedUrl]

  implicit def urlReader: BSONDocumentReader[ShortenedUrl] = Macros.reader[ShortenedUrl]

  lazy val urlShortenerRoutes: Route =
    path("start") {
      get {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`,
          """<!DOCTYPE html>
            |
            |<html>
            |  <body>
            |    <form action="shorten">
            |      URL:<input type="text" size=100 name="url"/></br>
            |      <input type="submit" value="Shorten">
            |    </form>
            |  </body>
            |<html>
          """.stripMargin))
      }
    } ~
      path("shorten") {
        get {
          parameters('url) { (url) => {
            complete {
              shorten(url)
            }
          }
          }
        }
      }

  def shorten(url: String): Future[String] = {
    val p = Promise[String]

    findUrl(url) onComplete {
      case Success(maybeUrl) =>
        if (maybeUrl.isDefined) {
          System.out.println(s"Found URL ${maybeUrl.get}")

          p.success(s"""{ shortenedUrl="${maybeUrl.get.shortenedUrl}" }""")
        }
        else {
          System.out.println(s"URL $url not found, inserting...")
          Try {
            new URL(url)
          } match {
            case Success(urlObject) =>
              val id = BSONObjectID.generate()
              val encodedUrl = Shortener.encode(id.hashCode)

              System.out.println(s"Encoded URL $encodedUrl")

              val portString = if (urlObject.getPort > 0) s":${urlObject.getPort}" else ""
              val shortenedUrlStr = s"${urlObject.getProtocol}://our.redir$portString/$encodedUrl"

              p.success(s"""{ shortenedUrl="$shortenedUrlStr" }""")

              val shortenedUrl = ShortenedUrl(id, id.hashCode, url, shortenedUrlStr)

              urlsCollection.insert[ShortenedUrl](shortenedUrl) onComplete {
                case Success(_) => System.out.println(s"Successfully inserted ShortenedUrl $shortenedUrl")
                case Failure(t) => error(url, t, Some(p))
              }
            case Failure(_) => p.success(s"Invalid URL $url")
          }
        }
      case Failure(t) => error(url, t, Some(p))
    }

    p.future
  }

  def findUrl(url: String): Future[Option[ShortenedUrl]] =
    urlsCollection
      .find(BSONDocument("url" -> BSONString(url)))
      .one[ShortenedUrl]

  def error(url: String, t: Throwable, p: Option[Promise[String]]): Unit = {
    System.out.println(s"Error inserting ShortenedUrl $url")
    t.printStackTrace()
    if (p.isDefined) p.get.failure(t)
  }
}
