package com.boxpay.checkout.server

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class MockWebServerRule : TestRule {

    val server = MockWebServer()

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            @Throws(Throwable::class)
            override fun evaluate() {
                server.start()
                try {
                    base.evaluate()
                } finally {
                    server.shutdown()
                }
            }
        }
    }

    fun enqueueResponse(body: String, code: Int = 200) {
        server.enqueue(
            MockResponse()
                .setResponseCode(code)
                .setBody(body)
        )
    }
}
