package io.taig.communicator

import java.io.IOException

import monix.eval.Task
import monix.execution.Cancelable
import okhttp3.OkHttpClient

import scala.language.implicitConversions

final class Request private (val wrapped: OkHttpRequest,
                             task: Task[Response[Unit]]) {

  /**
    * Transform the Request's InputStream to an instance of T
    *
    * An implicit Parser[T] has to be in scope.
    *
    * @tparam T
    * @return Task that parses the response body
    */
  def parse[T: Parser]: Task[Response[T]] = task.map { response =>
    Response(response.wrapped, Parser[T].parse(response.wrapped))
  }

  /**
    * Ignore the server response (and close the InputStream right away)
    *
    * Calling methods of monix.Task on a Request instance (e.g. Request.map)
    * will implicitly call this method to convert the Request to a Task (and
    * therefore close the response InputStream).
    *
    * @return Task that ignores the response body
    */
  def ignoreBody: Task[Response[Unit]] = task.map { response =>
    response.wrapped.close()
    response
  }

  /**
    * Get the raw Task instance
    *
    * When calling this method it is necessary to handle and close the
    * InputStream manually. You are discouraged to use this method and should
    * only do so with good reasons.
    *
    * @return Task with an untouched Response object
    */
  def unsafeToTask: Task[Response[Unit]] = task
}

object Request {
  implicit def requestToTask(request: Request): Task[Response[Unit]] =
    request.ignoreBody

  def apply(request: OkHttpRequest)(implicit ohc: OkHttpClient): Request = {
    val task = Task.create[Response[Unit]] { (scheduler, callback) =>
      val call = ohc.newCall(request)

      var canceled = false

      scheduler.execute { () =>
        try {
          val response = call.execute()
          callback.onSuccess(Response.raw(response))
        } catch {
          case exception: Throwable =>
            if (!canceled && exception.getMessage != "Canceled") {
              callback.onError(exception)
            }
        }
      }

      Cancelable { () =>
        canceled = true
        callback.onError(new IOException("Canceled"))
        call.cancel()
      }
    }

    new Request(request, task)
  }
}
