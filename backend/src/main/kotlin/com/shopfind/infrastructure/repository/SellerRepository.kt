package com.shopfind.infrastructure.repository

import com.shopfind.domain.model.Platform
import com.shopfind.domain.model.Seller
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.relational.core.query.Criteria
import org.springframework.data.relational.core.query.Query
import org.springframework.data.relational.core.query.Update
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.time.LocalDate

/**
 * Seller Repository
 *
 * Repository for Seller entity using R2dbcEntityTemplate.
 * Handles seller CRUD operations and queries.
 */
@Repository
class SellerRepository(
    private val template: R2dbcEntityTemplate
) {
    /**
     * Find seller by username (primary key)
     */
    suspend fun findByUsername(username: String): Seller? {
        return template.selectOne(
            Query.query(Criteria.where("username").`is`(username)),
            Seller::class.java
        ).awaitFirstOrNull()
    }

    /**
     * Find all sellers
     */
    fun findAll(): Flow<Seller> {
        return template.select(Seller::class.java)
            .all()
            .asFlow()
    }

    /**
     * Find all sellers with pagination
     */
    fun findAll(
        page: Int,
        size: Int,
        sort: Sort = Sort.by(Sort.Direction.DESC, "created_at")
    ): Flow<Seller> {
        return template.select(
            Query.empty().with(PageRequest.of(page, size, sort)),
            Seller::class.java
        ).asFlow()
    }

    /**
     * Save seller (insert or update)
     */
    suspend fun save(seller: Seller): Seller {
        return template.select(
            Query.query(Criteria.where("username").`is`(seller.username)),
            Seller::class.java
        ).awaitFirstOrNull()?.let {
            // Update existing
            template.update(seller).awaitSingle()
        } ?: run {
            // Insert new
            template.insert(seller).awaitSingle()
        }
    }

    /**
     * Delete seller by username
     */
    suspend fun deleteByUsername(username: String): Boolean {
        val result = template.delete(
            Query.query(Criteria.where("username").`is`(username)),
            Seller::class.java
        ).awaitSingle()
        return result > 0
    }

    /**
     * Check if seller exists
     */
    suspend fun existsByUsername(username: String): Boolean {
        return template.exists(
            Query.query(Criteria.where("username").`is`(username)),
            Seller::class.java
        ).awaitSingle()
    }

    /**
     * Find featured sellers
     */
    fun findFeatured(): Flow<Seller> {
        val today = LocalDate.now()
        return template.select(
            Query.query(
                Criteria.where("featured").`is`(true)
                    .and("featured_until").greaterThanOrEquals(today)
            ).sort(Sort.by(Sort.Direction.DESC, "featured_until")),
            Seller::class.java
        ).asFlow()
    }

    /**
     * Find verified sellers
     */
    fun findVerified(): Flow<Seller> {
        return template.select(
            Query.query(Criteria.where("verified").`is`(true)),
            Seller::class.java
        ).asFlow()
    }

    /**
     * Find sellers by platform
     */
    fun findByPlatform(platform: Platform): Flow<Seller> {
        return template.select(
            Query.query(Criteria.where("platform").`is`(platform.name)),
            Seller::class.java
        ).asFlow()
    }

    /**
     * Find sellers by location
     */
    fun findByLocation(location: String): Flow<Seller> {
        return template.select(
            Query.query(Criteria.where("location").`is`(location)),
            Seller::class.java
        ).asFlow()
    }

    /**
     * Find top sellers by product count
     */
    fun findTopSellers(limit: Int = 10): Flow<Seller> {
        return template.select(
            Query.empty()
                .sort(Sort.by(Sort.Direction.DESC, "total_products"))
                .limit(limit),
            Seller::class.java
        ).asFlow()
    }

    /**
     * Find sellers with most views
     */
    fun findMostViewed(limit: Int = 10): Flow<Seller> {
        return template.select(
            Query.empty()
                .sort(Sort.by(Sort.Direction.DESC, "total_views"))
                .limit(limit),
            Seller::class.java
        ).asFlow()
    }

    /**
     * Update seller stats (products, avg price)
     */
    suspend fun updateStats(
        username: String,
        totalProducts: Int,
        avgPrice: java.math.BigDecimal?
    ): Boolean {
        val update = Update.update("total_products", totalProducts)
            .set("avg_price", avgPrice)
            .set("last_scraped", java.time.LocalDateTime.now())

        val result = template.update(
            Query.query(Criteria.where("username").`is`(username)),
            update,
            Seller::class.java
        ).awaitSingle()

        return result > 0
    }

    /**
     * Increment seller views
     */
    suspend fun incrementViews(username: String, count: Int = 1): Boolean {
        val sql = """
            UPDATE sellers
            SET total_views = total_views + :count
            WHERE username = :username
        """.trimIndent()

        val result = template.databaseClient
            .sql(sql)
            .bind("username", username)
            .bind("count", count)
            .fetch()
            .rowsUpdated()
            .awaitSingle()

        return result > 0
    }

    /**
     * Increment seller clicks
     */
    suspend fun incrementClicks(username: String, count: Int = 1): Boolean {
        val sql = """
            UPDATE sellers
            SET total_clicks = total_clicks + :count
            WHERE username = :username
        """.trimIndent()

        val result = template.databaseClient
            .sql(sql)
            .bind("username", username)
            .bind("count", count)
            .fetch()
            .rowsUpdated()
            .awaitSingle()

        return result > 0
    }

    /**
     * Update featured status
     */
    suspend fun updateFeatured(
        username: String,
        featured: Boolean,
        featuredUntil: LocalDate?
    ): Boolean {
        val update = Update.update("featured", featured)
            .set("featured_until", featuredUntil)

        val result = template.update(
            Query.query(Criteria.where("username").`is`(username)),
            update,
            Seller::class.java
        ).awaitSingle()

        return result > 0
    }

    /**
     * Update verified status
     */
    suspend fun updateVerified(username: String, verified: Boolean): Boolean {
        val result = template.update(
            Query.query(Criteria.where("username").`is`(username)),
            Update.update("verified", verified),
            Seller::class.java
        ).awaitSingle()

        return result > 0
    }

    /**
     * Find sellers expiring soon (within 3 days)
     */
    fun findExpiringSoon(): Flow<Seller> {
        val threeDaysFromNow = LocalDate.now().plusDays(3)
        val today = LocalDate.now()

        return template.select(
            Query.query(
                Criteria.where("featured").`is`(true)
                    .and("featured_until").greaterThanOrEquals(today)
                    .and("featured_until").lessThanOrEquals(threeDaysFromNow)
            ),
            Seller::class.java
        ).asFlow()
    }

    /**
     * Expire featured listings
     */
    suspend fun expireFeatured(): Long {
        val today = LocalDate.now()
        val sql = """
            UPDATE sellers
            SET featured = false
            WHERE featured = true AND featured_until < :today
        """.trimIndent()

        return template.databaseClient
            .sql(sql)
            .bind("today", today)
            .fetch()
            .rowsUpdated()
            .awaitSingle()
    }

    /**
     * Count sellers
     */
    suspend fun count(): Long {
        return template.count(Query.empty(), Seller::class.java)
            .awaitSingle()
    }

    /**
     * Count featured sellers
     */
    suspend fun countFeatured(): Long {
        val today = LocalDate.now()
        return template.count(
            Query.query(
                Criteria.where("featured").`is`(true)
                    .and("featured_until").greaterThanOrEquals(today)
            ),
            Seller::class.java
        ).awaitSingle()
    }

    /**
     * Get distinct locations
     */
    suspend fun getDistinctLocations(): List<String> {
        val sql = "SELECT DISTINCT location FROM sellers WHERE location IS NOT NULL ORDER BY location"
        return template.databaseClient
            .sql(sql)
            .map { row, _ -> row.get("location", String::class.java) ?: "" }
            .all()
            .collectList()
            .awaitSingle()
    }
}
