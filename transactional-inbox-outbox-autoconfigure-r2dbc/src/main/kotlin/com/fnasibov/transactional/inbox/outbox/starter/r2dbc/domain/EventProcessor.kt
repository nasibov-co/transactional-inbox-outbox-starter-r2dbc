package com.fnasibov.transactional.inbox.outbox.starter.r2dbc.domain

import com.fnasibov.transactional.inbox.outbox.starter.r2dbc.api.EventHandler
import com.fnasibov.transactional.inbox.outbox.starter.r2dbc.api.model.Event
import com.fnasibov.transactional.inbox.outbox.starter.r2dbc.configuration.TransactionalProperties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Core orchestrator of the transactional event processing pipeline.
 *
 * The processor is responsible for:
 * - starting event pollers per event type
 * - coordinating event dispatch through an internal channel
 * - delegating execution to event handlers via a worker
 *
 * The processing model is fully asynchronous and based on coroutines.
 * Each event type is polled independently, while processing is centralized
 * through a shared worker pipeline.
 *
 * Architecture overview:
 * EventPoller(s) -> Channel -> EventWorker -> EventHandler(s)
 */
class EventProcessor(
    private val handlers: Map<Class<out Event>, List<EventHandler<out Event>>>,
    private val repository: EventRepository,
    private val properties: TransactionalProperties,
    private val scope: CoroutineScope,
    private val metrics: EventProcessingMetrics?
) {

    private val started = AtomicBoolean(false)
    private val pollerJobs = mutableListOf<Job>()
    private val workerJobs = mutableListOf<Job>()
    private var channel: Channel<Event>? = null

    /**
     * Starts the event processing pipeline.
     *
     * This method:
     * - initializes one [EventPoller] per event type
     * - starts the shared [EventWorker]
     *
     * After invocation, the system continuously polls, buffers,
     * and processes events until the coroutine scope is cancelled.
     */
    fun start() {
        if (!started.compareAndSet(false, true)) {
            return
        }

        /*
         * Internal buffer used to decouple polling and processing stages.
         *
         * Capacity is configured via `transactional.polling.channel-capacity`.
         * Overflow strategy is set to SUSPEND to ensure backpressure.
         */
        val processingChannel = Channel<Event>(
            capacity = properties.polling.channelCapacity,
            onBufferOverflow = BufferOverflow.SUSPEND
        )
        channel = processingChannel

        // Start pollers for each event type
        val pollers = handlers
            .map { (eventType, _) -> eventType }
            .distinct()
            .map { eventType ->
                EventPoller(
                    eventType = eventType,
                    repository = repository,
                    properties = properties,
                    channel = processingChannel,
                    scope = scope,
                    metrics = metrics
                )
            }
        pollerJobs += pollers.map { it.start() }

        // Start worker responsible for event dispatching
        workerJobs += EventWorker(
            handlers = handlers,
            repository = repository,
            properties = properties,
            channel = processingChannel,
            scope = scope,
            metrics = metrics
        ).start()
    }

    fun stop() = runBlocking {
        stopGracefully()
    }

    suspend fun stopGracefully() {
        if (!started.compareAndSet(true, false)) {
            return
        }

        pollerJobs.forEach { it.cancelAndJoin() }
        channel?.close()

        val drained = withTimeoutOrNull(properties.processing.shutdownTimeout.toMillis()) {
            workerJobs.joinAll()
        } != null

        if (!drained) {
            workerJobs.forEach { it.cancelAndJoin() }
        }

        pollerJobs.clear()
        workerJobs.clear()
        channel = null
    }
}
