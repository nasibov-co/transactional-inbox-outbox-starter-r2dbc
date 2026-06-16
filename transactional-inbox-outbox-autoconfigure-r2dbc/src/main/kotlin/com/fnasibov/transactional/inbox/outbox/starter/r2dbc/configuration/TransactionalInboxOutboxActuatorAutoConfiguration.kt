package com.fnasibov.transactional.inbox.outbox.starter.r2dbc.configuration

import com.fnasibov.transactional.inbox.outbox.starter.r2dbc.domain.EventProcessorStarter
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.transaction.ReactiveTransactionManager

@AutoConfiguration(after = [TransactionalInboxOutboxProcessorStarterAutoConfiguration::class])
@ConditionalOnProperty(
    "transactional.enabled",
    havingValue = "true",
    matchIfMissing = false
)
@ConditionalOnClass(
    R2dbcEntityTemplate::class,
    ReactiveTransactionManager::class,
    name = ["org.springframework.boot.health.contributor.HealthIndicator"]
)
class TransactionalInboxOutboxActuatorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = ["transactionalInboxOutboxHealthIndicator"])
    fun transactionalInboxOutboxHealthIndicator(
        starter: EventProcessorStarter
    ): TransactionalInboxOutboxHealthIndicator =
        TransactionalInboxOutboxHealthIndicator(starter)
}
