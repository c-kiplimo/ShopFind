package com.shopscout.infrastructure.repository

import com.shopscout.domain.model.Analytics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.data.domain.Sort
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.relational.core.query.Criteria
import org.springframework.data.relational.core.query.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate

/**
 * Analytics Repository
 *
 * Repository for Analytics entity using R2dbcEntityTemplate.
 * Handles seller analytics and performance metrics.
 */
@Repository
class AnalyticsRepository(
    private val template: R2dbcEntityTemplate
) {
    /**
     * Find analytics by ID
     */
    suspend fun findById(id: Long): Analytics? {
        return template.selectOne(
            Query.query(Criteria.where("id").`is`(id)),
            Analytics::class.java
        ).awaitFirstOrNull()
    }

    /**
     * Find analytics by seller and date
     */
    suspend fun findBySellerAndDate(
        sellerUsername: String,
        date: LocalDate
    ): Analytics? {
        return template.selectOne(
            Query.query(
                Criteria.where("seller_username").`is`(sellerUsername)
                    .and("date").`is`(date)
            ),
            Analytics::class.java
        ).awaitFirstOrNull()
    }

    /**
     * Find all analytics for a seller
     */
    fun findBySeller(sellerUsername: String): Flow<Analytics> {
        return template.select(
            Query.query(Criteria.where("seller_username").`is`(sellerUsername))
                .sort(Sort.by(Sort.Direction.DESC, "date")),
            Analytics::class.java
        ).asFlow()
    }

    /**
     * Find analytics for seller within date range
     */
    fun findBySellerAndDateRange(
        sellerUsername: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<Analytics> {
        return template.select(
            Query.query(
                Criteria.where("seller_username").`is`(sellerUsername)
                    .and("date").greaterThanOrEquals(startDate)
                    .and("date").lessThanOrEquals(endDate)
            ).sort(Sort.by(Sort.Direction.ASC, "date")),
            Analytics::class.java
        ).asFlow()
    }

    /**
     * Find analytics for a specific date
     */
    fun findByDate(date: LocalDate): Flow<Analytics> {
        return template.select(
            Query.query(Criteria.where("date").`is`(date)),
            Analytics::class.java
        ).asFlow()
    }

    /**
     * Find today's analytics for a seller
     */
    suspend fun findTodayBySeller(sellerUsername: String): Analytics? {
        return findBySellerAndDate(sellerUsername, LocalDate.now())
    }

    /**
     * Save analytics
     */
    suspend fun save(analytics: Analytics): Analytics {
        return if (analytics.id == null) {
            template.insert(analytics).awaitSingle()
        } else {
            template.update(analytics).awaitSingle()
        }
    }

    /**
     * Save or update today's analytics for a seller
     */
    suspend fun saveOrUpdateToday(
        sellerUsername: String,
        updater: (Analytics) -> Analytics
    ): Analytics {
        val existing = findTodayBySeller(sellerUsername)
        val updated = if (existing != null) {
            updater(existing)
        } else {
            updater(Analytics.forToday(sellerUsername))
        }
        return save(updated)
    }

    /**
     * Increment views for today
     */
    suspend fun incrementViewsToday(sellerUsername: String, count: Int = 1): Analytics {
        return saveOrUpdateToday(sellerUsername) { it.incrementViews(count) }
    }

    /**
     * Increment clicks for today
     */
    suspend fun incrementClicksToday(sellerUsername: String, count: Int = 1): Analytics {
        return saveOrUpdateToday(sellerUsername) { it.incrementClicks(count) }
    }

    /**
     * Increment WhatsApp clicks for today
     */
    suspend fun incrementWhatsappClicksToday(sellerUsername: String, count: Int = 1): Analytics {
        return saveOrUpdateToday(sellerUsername) { it.incrementWhatsappClicks(count) }
    }

    /**
     * Increment Instagram clicks for today
     */
    suspend fun incrementInstagramClicksToday(sellerUsername: String, count: Int = 1): Analytics {
        return saveOrUpdateToday(sellerUsername) { it.incrementInstagramClicks(count) }
    }

    /**
     * Increment searches for today
     */
    suspend fun incrementSearchesToday(sellerUsername: String, count: Int = 1): Analytics {
        return saveOrUpdateToday(sellerUsername) { it.incrementSearches(count) }
    }

    /**
     * Update average position for today
     */
    suspend fun updateAvgPositionToday(
        sellerUsername: String,
        position: java.math.BigDecimal
    ): Analytics {
        return saveOrUpdateToday(sellerUsername) { it.updateAvgPosition(position) }
    }

    /**
     * Get last N days of analytics for a seller
     */
    fun findLastNDays(sellerUsername: String, days: Int): Flow<Analytics> {
        val startDate = LocalDate.now().minusDays(days.toLong() - 1)
        val endDate = LocalDate.now()
        return findBySellerAndDateRange(sellerUsername, startDate, endDate)
    }

    /**
     * Get this week's analytics for a seller
     */
    fun findThisWeek(sellerUsername: String): Flow<Analytics> {
        return findLastNDays(sellerUsername, 7)
    }

    /**
     * Get this month's analytics for a seller
     */
    fun findThisMonth(sellerUsername: String): Flow<Analytics> {
        return findLastNDays(sellerUsername, 30)
    }

    /**
     * Delete old analytics (older than 1 year)
     */
    suspend fun deleteOldAnalytics(): Long {
        val oneYearAgo = LocalDate.now().minusYears(1)
        return template.delete(
            Query.query(Criteria.where("date").lessThan(oneYearAgo)),
            Analytics::class.java
        ).awaitSingle()
    }

    /**
     * Get top sellers by views in date range
     */
    suspend fun getTopSellersByViews(
        startDate: LocalDate,
        endDate: LocalDate,
        limit: Int = 10
    ): List<Pair<String, Int>> {
        val sql = """
            SELECT seller_username, SUM(views) as total_views
            FROM analytics
            WHERE date >= :startDate AND date <= :endDate
            GROUP BY seller_username
            ORDER BY total_views DESC
            LIMIT :limit
        """.trimIndent()

        return template.databaseClient
            .sql(sql)
            .bind("startDate", startDate)
            .bind("endDate", endDate)
            .bind("limit", limit)
            .map { row, _ ->
                val seller = row.get("seller_username", String::class.java) ?: ""
                val views = row.get("total_views", java.lang.Long::class.java)?.toInt() ?: 0
                Pair(seller, views)
            }
            .all()
            .collectList()
            .awaitSingle()
    }

    /**
     * Get top sellers by clicks in date range
     */
    suspend fun getTopSellersByClicks(
        startDate: LocalDate,
        endDate: LocalDate,
        limit: Int = 10
    ): List<Pair<String, Int>> {
        val sql = """
            SELECT seller_username, SUM(clicks) as total_clicks
            FROM analytics
            WHERE date >= :startDate AND date <= :endDate
            GROUP BY seller_username
            ORDER BY total_clicks DESC
            LIMIT :limit
        """.trimIndent()

        return template.databaseClient
            .sql(sql)
            .bind("startDate", startDate)
            .bind("endDate", endDate)
            .bind("limit", limit)
            .map { row, _ ->
                val seller = row.get("seller_username", String::class.java) ?: ""
                val clicks = row.get("total_clicks", java.lang.Long::class.java)?.toInt() ?: 0
                Pair(seller, clicks)
            }
            .all()
            .collectList()
            .awaitSingle()
    }

    /**
     * Get aggregated stats for seller in date range
     */
    suspend fun getAggregatedStats(
        sellerUsername: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Map<String, Any> {
        val sql = """
            SELECT
                COALESCE(SUM(views), 0) as total_views,
                COALESCE(SUM(clicks), 0) as total_clicks,
                COALESCE(SUM(whatsapp_clicks), 0) as total_whatsapp_clicks,
                COALESCE(SUM(instagram_clicks), 0) as total_instagram_clicks,
                COALESCE(SUM(searches_found), 0) as total_searches,
                COALESCE(AVG(avg_position), 0) as avg_position
            FROM analytics
            WHERE seller_username = :seller
            AND date >= :startDate AND date <= :endDate
        """.trimIndent()

        return template.databaseClient
            .sql(sql)
            .bind("seller", sellerUsername)
            .bind("startDate", startDate)
            .bind("endDate", endDate)
            .map { row, _ ->
                mapOf(
                    "totalViews" to (row.get("total_views", java.lang.Long::class.java)?.toInt() ?: 0),
                    "totalClicks" to (row.get("total_clicks", java.lang.Long::class.java)?.toInt() ?: 0),
                    "totalWhatsappClicks" to (row.get("total_whatsapp_clicks", java.lang.Long::class.java)?.toInt() ?: 0),
                    "totalInstagramClicks" to (row.get("total_instagram_clicks", java.lang.Long::class.java)?.toInt() ?: 0),
                    "totalSearches" to (row.get("total_searches", java.lang.Long::class.java)?.toInt() ?: 0),
                    "avgPosition" to (row.get("avg_position", java.math.BigDecimal::class.java) ?: java.math.BigDecimal.ZERO)
                )
            }
            .awaitSingle()
    }
}
