package com.fnasibov.transactional.inbox.outbox.starter.r2dbc.configuration

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.transaction.ReactiveTransactionManager

/**
 * Base auto-configuration for the Transactional Inbox/Outbox starter.
 *
 * The starter is activated only when `transactional.enabled=true`.
 */
@AutoConfiguration
@ConditionalOnProperty(
    "transactional.enabled",
    havingValue = "true",
    matchIfMissing = false
)
@ConditionalOnClass(R2dbcEntityTemplate::class, ReactiveTransactionManager::class)
@EnableConfigurationProperties(TransactionalProperties::class)
class TransactionalInboxOutboxAutoconfiguration
