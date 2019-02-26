package zhongl

import java.io.File
import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.http.scaladsl.Http.OutgoingConnection
import akka.http.scaladsl.settings.ClientConnectionSettings
import akka.http.scaladsl.{ClientTransport, Http}
import akka.stream.alpakka.unixdomainsocket.scaladsl.UnixDomainSocket
import akka.stream.scaladsl.Flow
import akka.util.ByteString

import scala.concurrent.Future

final class UnixDomainSocketTransport(file: File) extends ClientTransport {
  override def connectTo(
      host: String,
      port: Int,
      settings: ClientConnectionSettings
  )(implicit system: ActorSystem): Flow[ByteString, ByteString, Future[OutgoingConnection]] = {
    implicit val ex = system.dispatcher
    val address     = InetSocketAddress.createUnresolved(host, port)
    UnixDomainSocket()
      .outgoingConnection(file)
      .mapMaterializedValue(_.map { c =>
        system.log.info(s"materialized $c")
        Http.OutgoingConnection(address, address)
      })
  }
}
