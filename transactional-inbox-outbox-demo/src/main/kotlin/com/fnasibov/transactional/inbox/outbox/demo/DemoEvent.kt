package com.fnasibov.transactional.inbox.outbox.demo

import com.fnasibov.transactional.inbox.outbox.starter.r2dbc.api.model.Event
import com.fnasibov.transactional.inbox.outbox.starter.r2dbc.api.model.EventStatus
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.ZonedDateTime
import java.util.UUID

@Table("demo_events")
data class DemoEvent(
    @Id
    override val id: UUID? = null,
    override val status: EventStatus = EventStatus.PENDING,
    override val createdAt: ZonedDateTime = ZonedDateTime.now(),
    override val updatedAt: ZonedDateTime? = null,
    override val retryCount: Int = 0,
    override val lastAttemptAt: ZonedDateTime? = null,
    override val nextRetryAt: ZonedDateTime? = null,
    val payload: String
) : Event
