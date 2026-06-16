package com.fnasibov.transactional.inbox.outbox.starter.r2dbc.configuration

import com.fnasibov.transactional.inbox.outbox.starter.r2dbc.api.EventHandler
import com.fnasibov.transactional.inbox.outbox.starter.r2dbc.api.model.Event
import com.fnasibov.transactional.inbox.outbox.starter.r2dbc.domain.EventProcessingMetrics
import com.fnasibov.transactional.inbox.outbox.starter.r2dbc.domain.EventProcessor
import com.fnasibov.transactional.inbox.outbox.starter.r2dbc.domain.EventRepository
import kotlinx.coroutines.CoroutineScope
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.transaction.ReactiveTransactionManager

@AutoConfiguration(after = [TransactionalInboxOutboxRepositoryAutoConfiguration::class])
@ConditionalOnProperty(
    "transactional.enabled",
    havingValue = "true",
    matchIfMissing = false
)
@ConditionalOnClass(R2dbcEntityTemplate::class, ReactiveTransactionManager::class)
class TransactionalInboxOutboxProcessorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun eventProcessor(
        handlers: List<EventHandler<out Event>>,
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
}
