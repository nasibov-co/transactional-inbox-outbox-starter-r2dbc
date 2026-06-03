package com.fnasibov.transactional.inbox.outbox.demo

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface DemoEventRepository : CoroutineCrudRepository<DemoEvent, UUID> {
}