package com.fnasibov.transactional.inbox.outbox.starter.r2dbc.domain

import com.fnasibov.transactional.inbox.outbox.starter.r2dbc.api.EventHandler
import com.fnasibov.transactional.inbox.outbox.starter.r2dbc.api.model.BaseEvent
import com.fnasibov.transactional.inbox.outbox.starter.r2dbc.api.model.Event
import com.fnasibov.transactional.inbox.outbox.starter.r2dbc.api.model.EventStatus
import com.fnasibov.transactional.inbox.outbox.starter.r2dbc.configuration.TransactionalProperties
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.assertFalse

class EventProcessorTest {

    @Test
    fun `stop waits for workers to drain in-flight events`() {
        runBlocking {
            val event = TestEvent()
            val handlerStarted = CompletableDeferred<Unit>()
            val handlerCanFinish = CompletableDeferred<Unit>()
            val repository = SingleEventRepository(event)
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            val processor = EventProcessor(
                handlers = mapOf(
                    TestEvent::class.java to listOf(
                        DelayingEventHandler(handlerStarted, handlerCanFinish)
                    )
                ),
                repository = repository,
                properties = TransactionalProperties(
                    polling = TransactionalProperties.Polling(
                        activeInterval = Duration.ofMillis(10)
                    ),
                    processing = TransactionalProperties.Processing(
                        shutdownTimeout = Duration.ofSeconds(1)
                    )
                ),
                scope = scope,
                metrics = null
            )

            try {
                processor.start()
                withTimeout(1_000) {
                    handlerStarted.await()
                }

                val stop = async {
                    processor.stopGracefully()
                }

                delay(50)
                assertFalse(stop.isCompleted)

                handlerCanFinish.complete(Unit)

                withTimeout(1_000) {
                    stop.await()
                    repository.processed.await()
                }
            } finally {
                scope.cancel()
            }
        }
    }

    private class TestEvent : BaseEvent(
        id = UUID.randomUUID()
    )

    private class DelayingEventHandler(
        private val started: CompletableDeferred<Unit>,
        private val canFinish: CompletableDeferred<Unit>
    ) : EventHandler<TestEvent> {

        override fun supportedEventType(): Class<TestEvent> =
            TestEvent::class.java

        override suspend fun handle(event: TestEvent) {
            started.complete(Unit)
            canFinish.await()
        }

        override suspend fun handleDeadLetter(
            event: TestEvent,
            error: Throwable
        ) = Unit
    }

    private class SingleEventRepository(
        private val event: TestEvent
    ) : EventRepository {

        private val fetched = AtomicBoolean(false)
        val processed = CompletableDeferred<TestEvent>()

        @Suppress("UNCHECKED_CAST")
        override suspend fun <E : Event> fetchBatch(eventType: Class<E>): List<E> =
            if (fetched.compareAndSet(false, true)) {
                listOf(event as E)
            } else {
                emptyList()
            }

        override suspend fun <E : Event> markAsProcessed(event: E) {
            processed.complete(event as TestEvent)
        }

        override suspend fun <E : Event> markAsDeadLetter(event: E) = Unit

        override suspend fun <E : Event> markAsFailed(event: E): EventStatus =
            EventStatus.FAILED
    }
}
