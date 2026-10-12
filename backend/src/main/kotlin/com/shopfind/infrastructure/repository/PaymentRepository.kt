package com.shopfind.infrastructure.repository

import com.shopfind.domain.model.Payment
import com.shopfind.domain.model.PaymentStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.relational.core.query.Criteria
import org.springframework.data.relational.core.query.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * Payment Repository
 *
 * Repository for Payment entity using R2dbcEntityTemplate.
 * Handles M-Pesa payment tracking and queries.
 */
@Repository
class PaymentRepository(
    private val template: R2dbcEntityTemplate
) {
    /**
     * Find payment by ID
     */
    suspend fun findById(id: Long): Payment? {
        return template.selectOne(
            Query.query(Criteria.where("id").`is`(id)),
            Payment::class.java
        ).awaitFirstOrNull()
    }

    /**
     * Find payment by transaction ID
     */
    suspend fun findByTransactionId(transactionId: String): Payment? {
        return template.selectOne(
            Query.query(Criteria.where("transaction_id").`is`(transactionId)),
            Payment::class.java
        ).awaitFirstOrNull()
    }

    /**
     * Find payment by checkout request ID
     */
    suspend fun findByCheckoutRequestId(checkoutRequestId: String): Payment? {
        return template.selectOne(
            Query.query(Criteria.where("checkout_request_id").`is`(checkoutRequestId)),
            Payment::class.java
        ).awaitFirstOrNull()
    }

    /**
     * Find all payments
     */
    fun findAll(): Flow<Payment> {
        return template.select(Payment::class.java)
            .all()
            .asFlow()
    }

    /**
     * Find all payments with pagination
     */
    fun findAll(
        page: Int,
        size: Int,
        sort: Sort = Sort.by(Sort.Direction.DESC, "created_at")
    ): Flow<Payment> {
        return template.select(
            Query.empty().with(PageRequest.of(page, size, sort)),
            Payment::class.java
        ).asFlow()
    }

    /**
     * Save payment
     */
    suspend fun save(payment: Payment): Payment {
        return if (payment.id == null) {
            template.insert(payment).awaitSingle()
        } else {
            template.update(payment).awaitSingle()
        }
    }

    /**
     * Find payments by seller
     */
    fun findBySeller(sellerUsername: String): Flow<Payment> {
        return template.select(
            Query.query(Criteria.where("seller_username").`is`(sellerUsername))
                .sort(Sort.by(Sort.Direction.DESC, "created_at")),
            Payment::class.java
        ).asFlow()
    }

    /**
     * Find payments by status
     */
    fun findByStatus(status: PaymentStatus): Flow<Payment> {
        return template.select(
            Query.query(Criteria.where("status").`is`(status.name))
                .sort(Sort.by(Sort.Direction.DESC, "created_at")),
            Payment::class.java
        ).asFlow()
    }

    /**
     * Find pending payments
     */
    fun findPending(): Flow<Payment> {
        return findByStatus(PaymentStatus.PENDING)
    }

    /**
     * Find completed payments
     */
    fun findCompleted(): Flow<Payment> {
        return findByStatus(PaymentStatus.COMPLETED)
    }

    /**
     * Find failed payments
     */
    fun findFailed(): Flow<Payment> {
        return findByStatus(PaymentStatus.FAILED)
    }

    /**
     * Find payments by seller and status
     */
    fun findBySellerAndStatus(
        sellerUsername: String,
        status: PaymentStatus
    ): Flow<Payment> {
        return template.select(
            Query.query(
                Criteria.where("seller_username").`is`(sellerUsername)
                    .and("status").`is`(status.name)
            ).sort(Sort.by(Sort.Direction.DESC, "created_at")),
            Payment::class.java
        ).asFlow()
    }

    /**
     * Find payments by phone number
     */
    fun findByPhoneNumber(phoneNumber: String): Flow<Payment> {
        return template.select(
            Query.query(Criteria.where("phone_number").`is`(phoneNumber))
                .sort(Sort.by(Sort.Direction.DESC, "created_at")),
            Payment::class.java
        ).asFlow()
    }

    /**
     * Find payments within date range
     */
    fun findByDateRange(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<Payment> {
        return template.select(
            Query.query(
                Criteria.where("created_at").greaterThanOrEquals(startDate)
                    .and("created_at").lessThanOrEquals(endDate)
            ).sort(Sort.by(Sort.Direction.DESC, "created_at")),
            Payment::class.java
        ).asFlow()
    }

    /**
     * Find timed out payments (pending for more than 5 minutes)
     */
    fun findTimedOut(): Flow<Payment> {
        val fiveMinutesAgo = LocalDateTime.now().minusMinutes(5)
        return template.select(
            Query.query(
                Criteria.where("status").`is`(PaymentStatus.PENDING.name)
                    .and("created_at").lessThan(fiveMinutesAgo)
            ),
            Payment::class.java
        ).asFlow()
    }

    /**
     * Count payments by status
     */
    suspend fun countByStatus(status: PaymentStatus): Long {
        return template.count(
            Query.query(Criteria.where("status").`is`(status.name)),
            Payment::class.java
        ).awaitSingle()
    }

    /**
     * Count total payments
     */
    suspend fun count(): Long {
        return template.count(Query.empty(), Payment::class.java)
            .awaitSingle()
    }

    /**
     * Get total revenue (sum of completed payments)
     */
    suspend fun getTotalRevenue(): java.math.BigDecimal {
        val sql = """
            SELECT COALESCE(SUM(amount), 0) as total
            FROM payments
            WHERE status = 'COMPLETED'
        """.trimIndent()

        return template.databaseClient
            .sql(sql)
            .map { row, _ ->
                row.get("total", java.math.BigDecimal::class.java)
                    ?: java.math.BigDecimal.ZERO
            }
            .awaitSingle()
    }

    /**
     * Get revenue by seller
     */
    suspend fun getRevenueByTotalRevenueBySeller(sellerUsername: String): java.math.BigDecimal {
        val sql = """
            SELECT COALESCE(SUM(amount), 0) as total
            FROM payments
            WHERE seller_username = :seller AND status = 'COMPLETED'
        """.trimIndent()

        return template.databaseClient
            .sql(sql)
            .bind("seller", sellerUsername)
            .map { row, _ ->
                row.get("total", java.math.BigDecimal::class.java)
                    ?: java.math.BigDecimal.ZERO
            }
            .awaitSingle()
    }

    /**
     * Delete old failed payments (older than 30 days)
     */
    suspend fun deleteOldFailedPayments(): Long {
        val thirtyDaysAgo = LocalDateTime.now().minusDays(30)
        return template.delete(
            Query.query(
                Criteria.where("status").`is`(PaymentStatus.FAILED.name)
                    .and("created_at").lessThan(thirtyDaysAgo)
            ),
            Payment::class.java
        ).awaitSingle()
    }
}
