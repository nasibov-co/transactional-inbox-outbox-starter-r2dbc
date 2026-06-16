package com.fnasibov.transactional.inbox.outbox.starter.r2dbc.configuration

import com.fnasibov.transactional.inbox.outbox.starter.r2dbc.domain.EventProcessor
import com.fnasibov.transactional.inbox.outbox.starter.r2dbc.domain.EventProcessorStarter
import kotlinx.coroutines.CoroutineScope
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.transaction.ReactiveTransactionManager

@AutoConfiguration(after = [TransactionalInboxOutboxProcessorAutoConfiguration::class])
@ConditionalOnProperty(
    "transactional.enabled",
    havingValue = "true",
    matchIfMissing = false
)
@ConditionalOnClass(R2dbcEntityTemplate::class, ReactiveTransactionManager::class)
class TransactionalInboxOutboxProcessorStarterAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun eventProcessorStarter(
        processor: EventProcessor,
        @Qualifier("transactionalCoroutineScope")
        transactionalCoroutineScope: CoroutineScope
    ): EventProcessorStarter =
        EventProcessorStarter(processor, transactionalCoroutineScope)
}
