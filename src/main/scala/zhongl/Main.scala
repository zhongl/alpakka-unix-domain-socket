package zhongl

import java.nio.file.Files

import akka.actor.CoordinatedShutdown.Reason
import akka.actor.{ActorSystem, CoordinatedShutdown}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server._
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.stream._
import akka.stream.alpakka.unixdomainsocket.scaladsl.UnixDomainSocket
import akka.stream.scaladsl.TLSPlacebo

import scala.util.control.NonFatal

object Main extends Directives {

  def main(args: Array[String]): Unit = {
    val file = Files.createTempFile("stream", "sock").toFile
    file.delete()
    file.deleteOnExit()

    implicit val system = ActorSystem("unix-domain-socket")
    implicit val mat    = ActorMaterializer()
    implicit val ex     = system.dispatcher

    try {
      val httpServerLayer = Http().serverLayer().atop(TLSPlacebo())
      val handle          = Route.handlerFlow(pathEndOrSingleSlash { complete("ok") }).join(httpServerLayer)

      UnixDomainSocket()
        .bindAndHandle(handle, file)
        .flatMap { uds =>
          system.log.info(s"uds bound: $uds")

          val http      = Http()
          val transport = new UnixDomainSocketTransport(file)
          val settings = ConnectionPoolSettings(system)
            .withTransport(transport)
//            .withMaxConnections(1)
            .withMinConnections(2)

          http
            .singleRequest(HttpRequest(uri = "http://localhost/"), settings = settings)
            .flatMap { fs =>
              system.log.info(fs.toString())
              uds.unbind()
            }

        }
        .flatMap { _ =>
          system.log.info("terminate")
          system.terminate()
        }
        .recoverWith {
          case NonFatal(e) =>
            system.log.error(e, "terminate")
            system.terminate()
        }

    } catch {
      case NonFatal(_) => system.terminate()
    }

    system.whenTerminated flatMap (_ => CoordinatedShutdown(system).run(Quit))
  }

  object Quit extends Reason
}
