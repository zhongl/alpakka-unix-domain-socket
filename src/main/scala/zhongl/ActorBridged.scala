package zhongl

import akka.NotUsed
import akka.actor.{Actor, ActorRefFactory, Props}
import akka.stream._
import akka.stream.scaladsl.{Flow, Keep, Sink, SinkQueue, Source, SourceQueueWithComplete}
import akka.util.Timeout

import scala.reflect.ClassTag
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

object ActorBridged {
  def apply[I, O](
      flow: Graph[FlowShape[I, O], Any],
      name: String
  )(implicit factory: ActorRefFactory, mat: Materializer, timeout: Timeout, tag: ClassTag[O]): Graph[FlowShape[I, O], NotUsed] = {
    Flow[I].ask[O](factory.actorOf(props(flow), "transport"))
  }

  private def props[I, O](flow: Graph[FlowShape[I, O], Any])(implicit mat: Materializer): Props = {
    Source
      .queue[I](1, OverflowStrategy.backpressure)
      .via(flow)
      .toMat(Sink.queue())(Keep.both)
      .run() match {
      case (sink, source) => Props(new Bridge(sink, source))
    }
  }

  /**
    * @param sink
    * @param source
    * @tparam I
    * @tparam O
    */
  final class Bridge[-I, +O](sink: SourceQueueWithComplete[I], source: SinkQueue[O]) extends Actor {
    private implicit val ex = context.dispatcher

    private val log = context.system.log

    override def receive: Receive = {
      case in: I =>
        val upstream = sender()
        sink
          .offer(in)
          .recover { case NonFatal(cause) => QueueOfferResult.Failure(cause) }
          .map {
            case QueueOfferResult.Enqueued =>
              source.pull().onComplete {
                case Success(Some(e)) => upstream ! e
                case Success(None)    => log.debug("pull nothing")
                case Failure(cause)   => sink.fail(cause)
              }

            case QueueOfferResult.QueueClosed =>
              context.stop(self)

            case QueueOfferResult.Failure(cause) =>
              log.error(cause, "Downstream failed")
              context.stop(self)

            case QueueOfferResult.Dropped =>
              log.error("Downstream drop element, it should not be happen")
              context.stop(self)
          }
    }

  }
}
