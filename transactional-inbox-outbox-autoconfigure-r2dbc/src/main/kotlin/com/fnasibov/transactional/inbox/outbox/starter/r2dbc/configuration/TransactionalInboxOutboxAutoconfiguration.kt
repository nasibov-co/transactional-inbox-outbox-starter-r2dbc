package com.fnasibov.transactional.inbox.outbox.starter.r2dbc.configuration

import com.fnasibov.transactional.inbox.outbox.starter.r2dbc.api.EventHandler
import com.fnasibov.transactional.inbox.outbox.starter.r2dbc.api.FetchBatchStrategy
import com.fnasibov.transactional.inbox.outbox.starter.r2dbc.api.model.Event
import com.fnasibov.transactional.inbox.outbox.starter.r2dbc.domain.BaseEventRepository
import com.fnasibov.transactional.inbox.outbox.starter.r2dbc.domain.EventProcessingMetrics
import com.fnasibov.transactional.inbox.outbox.starter.r2dbc.domain.EventProcessor
import com.fnasibov.transactional.inbox.outbox.starter.r2dbc.domain.EventProcessorStarter
import com.fnasibov.transactional.inbox.outbox.starter.r2dbc.domain.EventRepository
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.transaction.ReactiveTransactionManager
import org.springframework.transaction.reactive.TransactionalOperator

/**
 * Auto-configuration for the Transactional Inbox/Outbox starter.
 *
 * The configuration is activated only when `transactional.enabled=true`.
 */
@AutoConfiguration
@ConditionalOnProperty(
    "transactional.enabled",
    havingValue = "true",
    matchIfMissing = false
)
@ConditionalOnClass(R2dbcEntityTemplate::class, ReactiveTransactionManager::class)
@EnableConfigurationProperties(TransactionalProperties::class)
class TransactionalInboxOutboxAutoconfiguration(
    private val handlers: List<EventHandler<out Event>>
) {

    @Bean("transactionalCoroutineScope")
    @ConditionalOnMissingBean(name = ["transactionalCoroutineScope"])
    fun transactionalCoroutineScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Bean
    @ConditionalOnBean(MeterRegistry::class)
    @ConditionalOnMissingBean
    fun eventProcessingMetrics(
        meterRegistry: MeterRegistry
    ): EventProcessingMetrics =
        EventProcessingMetrics(meterRegistry)

    @Bean
    @ConditionalOnMissingBean(EventRepository::class)
    fun eventRepository(
        template: R2dbcEntityTemplate,
        reactiveTransactionManager: ReactiveTransactionManager,
        properties: TransactionalProperties,
        strategies: List<FetchBatchStrategy<out Event>>
    ): EventRepository {
        val transactionalOperator =
            TransactionalOperator.create(reactiveTransactionManager)

        return BaseEventRepository(
            template = template,
            properties = properties,
            transactionalOperator = transactionalOperator,
            strategiesByEventType = strategies.associateBy { it.eventType }
        )
    }

    @Bean
    @ConditionalOnMissingBean
    fun eventProcessor(
        transactionalProperties: TransactionalProperties,
        repository: EventRepository,
        @Qualifier("transactionalCoroutineScope")
        transactionalCoroutineScope: CoroutineScope,
        eventProcessingMetrics: ObjectProvider<EventProcessingMetrics>
    ): EventProcessor {
        val handlerMap = handlers.groupBy { handler ->
            handler.supportedEventType()
        }

        return EventProcessor(
            handlers = handlerMap,
            repository = repository,
            properties = transactionalProperties,
            scope = transactionalCoroutineScope,
            metrics = eventProcessingMetrics.ifAvailable
        )
    }

    @Bean
    @ConditionalOnMissingBean
    fun eventProcessorStarter(
        processor: EventProcessor,
        @Qualifier("transactionalCoroutineScope")
        transactionalCoroutineScope: CoroutineScope
    ): EventProcessorStarter =
        EventProcessorStarter(processor, transactionalCoroutineScope)

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = ["org.springframework.boot.health.contributor.HealthIndicator"])
    class ActuatorConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = ["transactionalInboxOutboxHealthIndicator"])
        fun transactionalInboxOutboxHealthIndicator(
            starter: EventProcessorStarter
        ): TransactionalInboxOutboxHealthIndicator =
            TransactionalInboxOutboxHealthIndicator(starter)
    }
}
