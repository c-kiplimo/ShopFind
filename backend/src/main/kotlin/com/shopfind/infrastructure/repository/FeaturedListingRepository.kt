package com.shopfind.infrastructure.repository

import com.shopfind.domain.model.FeaturedListing
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
 * Featured Listing Repository
 *
 * Repository for FeaturedListing entity using R2dbcEntityTemplate.
 * Handles featured listing subscriptions and renewals.
 */
@Repository
class FeaturedListingRepository(
    private val template: R2dbcEntityTemplate
) {
    /**
     * Find featured listing by ID
     */
    suspend fun findById(id: Long): FeaturedListing? {
        return template.selectOne(
            Query.query(Criteria.where("id").`is`(id)),
            FeaturedListing::class.java
        ).awaitFirstOrNull()
    }

    /**
     * Find all featured listings
     */
    fun findAll(): Flow<FeaturedListing> {
        return template.select(FeaturedListing::class.java)
            .all()
            .asFlow()
    }

    /**
     * Find all featured listings with pagination
     */
    fun findAll(
        page: Int,
        size: Int,
        sort: Sort = Sort.by(Sort.Direction.DESC, "created_at")
    ): Flow<FeaturedListing> {
        return template.select(
            Query.empty().with(PageRequest.of(page, size, sort)),
            FeaturedListing::class.java
        ).asFlow()
    }

    /**
     * Save featured listing
     */
    suspend fun save(listing: FeaturedListing): FeaturedListing {
        return if (listing.id == null) {
            template.insert(listing).awaitSingle()
        } else {
            template.update(listing).awaitSingle()
        }
    }

    /**
     * Delete featured listing
     */
    suspend fun deleteById(id: Long): Boolean {
        val result = template.delete(
            Query.query(Criteria.where("id").`is`(id)),
            FeaturedListing::class.java
        ).awaitSingle()
        return result > 0
    }

    /**
     * Find listings by seller
     */
    fun findBySeller(sellerUsername: String): Flow<FeaturedListing> {
        return template.select(
            Query.query(Criteria.where("seller_username").`is`(sellerUsername))
                .sort(Sort.by(Sort.Direction.DESC, "created_at")),
            FeaturedListing::class.java
        ).asFlow()
    }

    /**
     * Find active listing for seller
     */
    suspend fun findActiveByTSeller(sellerUsername: String): FeaturedListing? {
        val now = LocalDateTime.now()
        return template.selectOne(
            Query.query(
                Criteria.where("seller_username").`is`(sellerUsername)
                    .and("is_active").`is`(true)
                    .and("expires_at").greaterThan(now)
            ).sort(Sort.by(Sort.Direction.DESC, "expires_at")),
            FeaturedListing::class.java
        ).awaitFirstOrNull()
    }

    /**
     * Find all active listings
     */
    fun findActive(): Flow<FeaturedListing> {
        val now = LocalDateTime.now()
        return template.select(
            Query.query(
                Criteria.where("is_active").`is`(true)
                    .and("expires_at").greaterThan(now)
            ).sort(Sort.by(Sort.Direction.DESC, "expires_at")),
            FeaturedListing::class.java
        ).asFlow()
    }

    /**
     * Find expired listings
     */
    fun findExpired(): Flow<FeaturedListing> {
        val now = LocalDateTime.now()
        return template.select(
            Query.query(
                Criteria.where("is_active").`is`(true)
                    .and("expires_at").lessThan(now)
            ),
            FeaturedListing::class.java
        ).asFlow()
    }

    /**
     * Find listings expiring soon (within 3 days)
     */
    fun findExpiringSoon(): Flow<FeaturedListing> {
        val now = LocalDateTime.now()
        val threeDaysFromNow = now.plusDays(3)

        return template.select(
            Query.query(
                Criteria.where("is_active").`is`(true)
                    .and("expires_at").greaterThan(now)
                    .and("expires_at").lessThan(threeDaysFromNow)
            ),
            FeaturedListing::class.java
        ).asFlow()
    }

    /**
     * Find listings with auto-renewal enabled
     */
    fun findAutoRenewable(): Flow<FeaturedListing> {
        return template.select(
            Query.query(
                Criteria.where("auto_renew").`is`(true)
                    .and("is_active").`is`(true)
            ),
            FeaturedListing::class.java
        ).asFlow()
    }

    /**
     * Find listings by transaction ID
     */
    suspend fun findByTransactionId(transactionId: String): FeaturedListing? {
        return template.selectOne(
            Query.query(Criteria.where("transaction_id").`is`(transactionId)),
            FeaturedListing::class.java
        ).awaitFirstOrNull()
    }

    /**
     * Deactivate expired listings
     */
    suspend fun deactivateExpired(): Long {
        val now = LocalDateTime.now()
        val sql = """
            UPDATE featured_listings
            SET is_active = false
            WHERE is_active = true AND expires_at < :now
        """.trimIndent()

        return template.databaseClient
            .sql(sql)
            .bind("now", now)
            .fetch()
            .rowsUpdated()
            .awaitSingle()
    }

    /**
     * Count active listings
     */
    suspend fun countActive(): Long {
        val now = LocalDateTime.now()
        return template.count(
            Query.query(
                Criteria.where("is_active").`is`(true)
                    .and("expires_at").greaterThan(now)
            ),
            FeaturedListing::class.java
        ).awaitSingle()
    }

    /**
     * Count listings by seller
     */
    suspend fun countBySeller(sellerUsername: String): Long {
        return template.count(
            Query.query(Criteria.where("seller_username").`is`(sellerUsername)),
            FeaturedListing::class.java
        ).awaitSingle()
    }

    /**
     * Get total revenue from featured listings
     */
    suspend fun getTotalRevenue(): java.math.BigDecimal {
        val sql = "SELECT COALESCE(SUM(amount), 0) as total FROM featured_listings"
        return template.databaseClient
            .sql(sql)
            .map { row, _ ->
                row.get("total", java.math.BigDecimal::class.java)
                    ?: java.math.BigDecimal.ZERO
            }
            .awaitSingle()
    }

    /**
     * Get revenue within date range
     */
    suspend fun getRevenueByDateRange(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): java.math.BigDecimal {
        val sql = """
            SELECT COALESCE(SUM(amount), 0) as total
            FROM featured_listings
            WHERE created_at >= :startDate AND created_at <= :endDate
        """.trimIndent()

        return template.databaseClient
            .sql(sql)
            .bind("startDate", startDate)
            .bind("endDate", endDate)
            .map { row, _ ->
                row.get("total", java.math.BigDecimal::class.java)
                    ?: java.math.BigDecimal.ZERO
            }
            .awaitSingle()
    }
}
