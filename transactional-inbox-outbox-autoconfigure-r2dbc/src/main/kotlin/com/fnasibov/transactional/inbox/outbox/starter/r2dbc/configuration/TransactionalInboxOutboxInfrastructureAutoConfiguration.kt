package com.fnasibov.transactional.inbox.outbox.starter.r2dbc.configuration

import com.fnasibov.transactional.inbox.outbox.starter.r2dbc.domain.EventProcessingMetrics
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.transaction.ReactiveTransactionManager

@AutoConfiguration(after = [TransactionalInboxOutboxAutoconfiguration::class])
@ConditionalOnProperty(
    "transactional.enabled",
    havingValue = "true",
    matchIfMissing = false
)
@ConditionalOnClass(R2dbcEntityTemplate::class, ReactiveTransactionManager::class)
class TransactionalInboxOutboxInfrastructureAutoConfiguration {

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
}
