package io.taig.communicator

import okhttp3.mockwebserver.MockResponse

class ResponseTest extends Suite {
  it should "have a useful toString implementation" in {
    val builder = http { server =>
      server.enqueue(new MockResponse().setBody("foobar"))
    }

    val request = builder.build()

    Request(request).parse[String].runToFuture.map { resp =>
      val r = """
                |>>> http://localhost:\d+/
                |\[No headers\]
                |<<< 200 OK
                |Content-Length: 6
              """.stripMargin.trim.r
       r.matches(resp.toString) mustEqual true
    }
  }

  it should "proxy all okhttp.Response methods" in {
    val builder = http { server =>
      server.enqueue(new MockResponse().setBody("foobar"))
    }

    import scala.jdk.CollectionConverters._

    val request = builder.build()

    Request(request).parse[String].runToFuture.map { response =>
      response.code mustEqual response.wrapped.code()
      response.message.orNull mustEqual response.wrapped.message()
      response.headers mustEqual response.wrapped.headers()
      response.request mustEqual response.wrapped.request()
      response.protocol mustEqual response.wrapped.protocol()
      response.handshake.orNull mustEqual response.wrapped.handshake()
      response.isSuccessful mustEqual response.wrapped.isSuccessful
      response.isRedirect mustEqual response.wrapped.isRedirect

      response.challenges mustEqual response.wrapped.challenges().asScala.toList

      response.cacheControl mustEqual response.wrapped.cacheControl()
      response.sentRequestAtMillis mustEqual response.wrapped
        .sentRequestAtMillis()
      response.receivedResponseAtMillis mustEqual response.wrapped
        .receivedResponseAtMillis()
      response.cacheResponse.map(_.wrapped).orNull mustEqual response.wrapped
        .cacheResponse()
      response.networkResponse.map(_.wrapped).orNull mustEqual response.wrapped
        .networkResponse()
      response.priorResponse.map(_.wrapped).orNull mustEqual response.wrapped
        .priorResponse()
    }
  }

  it should "unapply a Response" in {
    val builder = http { server =>
      server.enqueue(new MockResponse().setResponseCode(201).setBody("foobar"))
    }

    val request = builder.build()

    Request(request).parse[String].runToFuture.map {
      case Response(code, body) =>
        code mustEqual 201
        body mustEqual "foobar"
    }
  }
}
