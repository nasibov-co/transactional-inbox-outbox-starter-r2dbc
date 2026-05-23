package com.fnasibov.transactional.inbox.outbox.starter.r2dbc.configuration

import com.fnasibov.transactional.inbox.outbox.starter.r2dbc.domain.EventProcessor
import com.fnasibov.transactional.inbox.outbox.starter.r2dbc.domain.EventProcessorStarter
import com.fnasibov.transactional.inbox.outbox.starter.r2dbc.domain.EventRepository
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.transaction.ReactiveTransaction
import org.springframework.transaction.ReactiveTransactionManager
import org.springframework.transaction.TransactionDefinition
import reactor.core.publisher.Mono
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TransactionalInboxOutboxAutoconfigurationTest {

    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(R2dbcInfrastructureConfiguration::class.java)
        .withConfiguration(
            org.springframework.boot.autoconfigure.AutoConfigurations.of(
                TransactionalInboxOutboxAutoconfiguration::class.java
            )
        )

    @Test
    fun `does not create processor when disabled`() {
        contextRunner.run { context ->
            assertFalse(context.containsBean("eventProcessor"))
            assertFalse(context.containsBean("eventProcessorStarter"))
        }
    }

    @Test
    fun `creates starter infrastructure when enabled`() {
        contextRunner
            .withPropertyValues("transactional.enabled=true")
            .run { context ->
                assertNotNull(context.getBean(TransactionalProperties::class.java))
                assertNotNull(context.getBean(EventRepository::class.java))
                assertNotNull(context.getBean(EventProcessor::class.java))
                assertNotNull(context.getBean(EventProcessorStarter::class.java))
            }
    }

    @Test
    fun `binds duration based properties`() {
        contextRunner
            .withPropertyValues(
                "transactional.enabled=true",
                "transactional.polling.active-interval=250ms",
                "transactional.polling.max-idle-interval=10s",
                "transactional.processing.shutdown-timeout=5s",
                "transactional.retry.initial-delay=2s",
                "transactional.retry.max-delay=30s",
                "transactional.retry.multiplier=3.0"
            )
            .run { context ->
                val properties = context.getBean(TransactionalProperties::class.java)

                assertEquals(250, properties.polling.activeInterval.toMillis())
                assertEquals(10, properties.polling.maxIdleInterval.seconds)
                assertEquals(5, properties.processing.shutdownTimeout.seconds)
                assertEquals(2, properties.retry.initialDelay.seconds)
                assertEquals(30, properties.retry.maxDelay.seconds)
                assertEquals(3.0, properties.retry.multiplier)
            }
    }

    @Test
    fun `binds deprecated property aliases`() {
        contextRunner
            .withPropertyValues(
                "transactional.enabled=true",
                "transactional.polling.active-interval-ms=300ms",
                "transactional.polling.max-idle-interval-ms=15s",
                "transactional.retry.initial-delay-ms=2500",
                "transactional.retry.max-immediate-attempts=4"
            )
            .run { context ->
                val properties = context.getBean(TransactionalProperties::class.java)

                assertEquals(300, properties.polling.activeInterval.toMillis())
                assertEquals(15, properties.polling.maxIdleInterval.seconds)
                assertEquals(2500, properties.retry.initialDelay.toMillis())
                assertEquals(4, properties.retry.maxAttempts)
            }
    }

    @Test
    fun `fails fast on invalid numeric properties`() {
        contextRunner
            .withPropertyValues(
                "transactional.enabled=true",
                "transactional.polling.batch-size=0"
            )
            .run { context ->
                assertTrue(context.startupFailure is Exception)
            }
    }

    @Test
    fun `backs off when user provides repository and coroutine scope`() {
        contextRunner
            .withUserConfiguration(CustomInfrastructureConfiguration::class.java)
            .withPropertyValues("transactional.enabled=true")
            .run { context ->
                assertEquals(
                    CustomInfrastructureConfiguration.customRepository,
                    context.getBean(EventRepository::class.java)
                )
                assertEquals(
                    CustomInfrastructureConfiguration.customScope,
                    context.getBean("transactionalCoroutineScope")
                )
            }
    }

    @Configuration(proxyBeanMethods = false)
    class R2dbcInfrastructureConfiguration {

        @Bean
        fun r2dbcEntityTemplate(): R2dbcEntityTemplate =
            mockk(relaxed = true)

        @Bean
        fun reactiveTransactionManager(): ReactiveTransactionManager =
            object : ReactiveTransactionManager {
                override fun getReactiveTransaction(
                    definition: TransactionDefinition?
                ): Mono<ReactiveTransaction> =
                    Mono.just(mockk(relaxed = true))

                override fun commit(transaction: ReactiveTransaction): Mono<Void> =
                    Mono.empty()

                override fun rollback(transaction: ReactiveTransaction): Mono<Void> =
                    Mono.empty()
            }
    }

    @Configuration(proxyBeanMethods = false)
    class CustomInfrastructureConfiguration {

        @Bean("transactionalCoroutineScope")
        fun transactionalCoroutineScope(): CoroutineScope =
            customScope

        @Bean
        fun eventRepository(): EventRepository =
            customRepository

        companion object {
            val customScope: CoroutineScope = CoroutineScope(Dispatchers.Unconfined)
            val customRepository: EventRepository = mockk(relaxed = true)
        }
    }
}
