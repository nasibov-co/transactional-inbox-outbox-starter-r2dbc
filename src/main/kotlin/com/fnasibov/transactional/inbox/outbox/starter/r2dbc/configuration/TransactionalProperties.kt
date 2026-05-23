package com.fnasibov.transactional.inbox.outbox.starter.r2dbc.configuration

import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty
import org.springframework.validation.annotation.Validated
import java.time.Duration

/**
 * Configuration properties for transactional inbox/outbox processing.
 *
 * Defines polling, processing, and retry behavior
 * used by the event processing infrastructure.
 */
@Validated
@ConfigurationProperties(prefix = "transactional")
data class TransactionalProperties(

    /**
     * Enables or disables transactional inbox/outbox processing.
     */
    var enabled: Boolean = false,

    /**
     * Polling configuration.
     */
    @field:Valid
    var polling: Polling = Polling(),

    /**
     * Event processing configuration.
     */
    @field:Valid
    var processing: Processing = Processing(),

    /**
     * Retry configuration.
     */
    @field:Valid
    var retry: Retry = Retry()

) {

    /**
     * Polling behavior configuration.
     */
    data class Polling(

        /**
         * Maximum interval between polling cycles when no events are available.
         *
         * Default: 30 seconds.
         */
        var maxIdleInterval: Duration = Duration.ofSeconds(30),

        /**
         * Polling interval while events are actively being processed.
         *
         * Default: 100 milliseconds.
         */
        var activeInterval: Duration = Duration.ofMillis(100),

        /**
         * Maximum number of events fetched in a single batch.
         *
         * Default: 15.
         */
        @field:Min(1)
        var batchSize: Int = 15,

        /**
         * Internal channel capacity used for buffering fetched events.
         *
         * Default: 25.
         */
        @field:Min(1)
        var channelCapacity: Int = 25
    ) {

        @Deprecated(
            message = "Use maxIdleInterval instead.",
            replaceWith = ReplaceWith("maxIdleInterval")
        )
        @get:DeprecatedConfigurationProperty(
            reason = "Renamed for Spring Boot duration property naming.",
            replacement = "transactional.polling.max-idle-interval"
        )
        var maxIdleIntervalMs: Duration
            get() = maxIdleInterval
            set(value) {
                maxIdleInterval = value
            }

        @Deprecated(
            message = "Use activeInterval instead.",
            replaceWith = ReplaceWith("activeInterval")
        )
        @get:DeprecatedConfigurationProperty(
            reason = "Renamed for Spring Boot duration property naming.",
            replacement = "transactional.polling.active-interval"
        )
        var activeIntervalMs: Duration
            get() = activeInterval
            set(value) {
                activeInterval = value
            }

        @Deprecated(
            message = "Use processing.concurrency instead.",
            replaceWith = ReplaceWith("processing.concurrency")
        )
        @get:DeprecatedConfigurationProperty(
            reason = "This polling-level setting was unused and has been removed.",
            replacement = "transactional.processing.concurrency"
        )
        var maxConcurrency: Int = 5
    }

    /**
     * Event processing execution settings.
     */
    data class Processing(

        /**
         * Number of concurrent event processing workers.
         *
         * Default: 5.
         */
        @field:Min(1)
        var concurrency: Int = 5,

        /**
         * Maximum time to wait for workers to drain already fetched events
         * during application shutdown.
         *
         * Default: 30 seconds.
         */
        var shutdownTimeout: Duration = Duration.ofSeconds(30)
    )

    /**
     * Retry behavior configuration.
     */
    data class Retry(

        /**
         * Maximum number of retry attempts before moving
         * an event to dead-letter state.
         *
         * Default: 3.
         */
        @field:Min(1)
        var maxAttempts: Int = 3,

        /**
         * Initial retry delay used.
         *
         * Default: 1000 milliseconds.
         */
        var initialDelay: Duration = Duration.ofSeconds(1),

        /**
         * Multiplier applied to retry delay after each failed attempt.
         *
         * Default: 2.0.
         */
        @field:DecimalMin("1.0")
        var multiplier: Double = 2.0,

        /**
         * Maximum retry delay.
         *
         * Default: 1 minute.
         */
        var maxDelay: Duration = Duration.ofMinutes(1),
    ) {

        @Deprecated(
            message = "Use maxAttempts instead.",
            replaceWith = ReplaceWith("maxAttempts")
        )
        @get:DeprecatedConfigurationProperty(
            reason = "Renamed for clearer retry semantics.",
            replacement = "transactional.retry.max-attempts"
        )
        var maxImmediateAttempts: Int
            get() = maxAttempts
            set(value) {
                maxAttempts = value
            }

        @Deprecated(
            message = "Use initialDelay instead.",
            replaceWith = ReplaceWith("initialDelay")
        )
        @get:DeprecatedConfigurationProperty(
            reason = "Replaced by a Duration property.",
            replacement = "transactional.retry.initial-delay"
        )
        var initialDelayMs: Long
            get() = initialDelay.toMillis()
            set(value) {
                initialDelay = Duration.ofMillis(value)
            }
    }
}
