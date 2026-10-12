package com.shopscout.domain.model

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Analytics Domain Entity
 *
 * Represents daily analytics metrics for a seller.
 * Used for seller dashboard and performance tracking.
 * Follows Domain-Driven Design principles.
 */
@Table("analytics")
data class Analytics(
    @Id
    val id: Long? = null,

    // Seller Reference
    @Column("seller_username")
    val sellerUsername: String,

    // Date
    val date: LocalDate,

    // Metrics
    val views: Int = 0,
    val clicks: Int = 0,
    @Column("whatsapp_clicks")
    val whatsappClicks: Int = 0,
    @Column("instagram_clicks")
    val instagramClicks: Int = 0,

    // Engagement
    @Column("searches_found")
    val searchesFound: Int = 0,
    @Column("avg_position")
    val avgPosition: BigDecimal? = null,

    // Metadata
    @Column("created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Calculate click-through rate (CTR)
     */
    fun clickThroughRate(): Double? {
        if (views == 0) return null
        return (clicks.toDouble() / views.toDouble()) * 100
    }

    /**
     * Calculate WhatsApp click rate
     */
    fun whatsappClickRate(): Double? {
        if (clicks == 0) return null
        return (whatsappClicks.toDouble() / clicks.toDouble()) * 100
    }

    /**
     * Calculate Instagram click rate
     */
    fun instagramClickRate(): Double? {
        if (clicks == 0) return null
        return (instagramClicks.toDouble() / clicks.toDouble()) * 100
    }

    /**
     * Get total external clicks (WhatsApp + Instagram)
     */
    fun totalExternalClicks(): Int {
        return whatsappClicks + instagramClicks
    }

    /**
     * Calculate search appearance rate
     */
    fun searchAppearanceRate(): Double? {
        if (searchesFound == 0) return null
        return (views.toDouble() / searchesFound.toDouble()) * 100
    }

    /**
     * Increment view count
     */
    fun incrementViews(count: Int = 1): Analytics {
        return copy(views = views + count)
    }

    /**
     * Increment click count
     */
    fun incrementClicks(count: Int = 1): Analytics {
        return copy(clicks = clicks + count)
    }

    /**
     * Increment WhatsApp click count
     */
    fun incrementWhatsappClicks(count: Int = 1): Analytics {
        return copy(
            clicks = clicks + count,
            whatsappClicks = whatsappClicks + count
        )
    }

    /**
     * Increment Instagram click count
     */
    fun incrementInstagramClicks(count: Int = 1): Analytics {
        return copy(
            clicks = clicks + count,
            instagramClicks = instagramClicks + count
        )
    }

    /**
     * Increment search appearances
     */
    fun incrementSearches(count: Int = 1): Analytics {
        return copy(searchesFound = searchesFound + count)
    }

    /**
     * Update average position
     */
    fun updateAvgPosition(position: BigDecimal): Analytics {
        val newAvg = if (avgPosition == null) {
            position
        } else {
            // Weighted average with previous positions
            ((avgPosition * searchesFound.toBigDecimal()) + position) /
                    (searchesFound + 1).toBigDecimal()
        }

        return copy(avgPosition = newAvg.setScale(2, RoundingMode.HALF_UP))
    }

    /**
     * Check if this is today's analytics
     */
    fun isToday(): Boolean {
        return date == LocalDate.now()
    }

    /**
     * Get age in days
     */
    fun ageInDays(): Long {
        return LocalDate.now().toEpochDay() - date.toEpochDay()
    }

    /**
     * Validate analytics invariants
     */
    init {
        require(sellerUsername.isNotBlank()) { "Seller username cannot be blank" }
        require(views >= 0) { "Views cannot be negative" }
        require(clicks >= 0) { "Clicks cannot be negative" }
        require(whatsappClicks >= 0) { "WhatsApp clicks cannot be negative" }
        require(instagramClicks >= 0) { "Instagram clicks cannot be negative" }
        require(searchesFound >= 0) { "Searches found cannot be negative" }
        require(clicks >= whatsappClicks + instagramClicks) {
            "Total clicks must be >= sum of WhatsApp and Instagram clicks"
        }
        if (avgPosition != null) {
            require(avgPosition > BigDecimal.ZERO) { "Average position must be positive" }
        }
    }

    companion object {
        /**
         * Factory method to create analytics for today
         */
        fun forToday(sellerUsername: String): Analytics {
            return Analytics(
                sellerUsername = sellerUsername,
                date = LocalDate.now()
            )
        }

        /**
         * Factory method to create analytics for a specific date
         */
        fun forDate(sellerUsername: String, date: LocalDate): Analytics {
            return Analytics(
                sellerUsername = sellerUsername,
                date = date
            )
        }
    }
}

/**
 * Analytics Summary Value Object
 *
 * Aggregated analytics over a date range
 */
data class AnalyticsSummary(
    val sellerUsername: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val totalViews: Int,
    val totalClicks: Int,
    val totalWhatsappClicks: Int,
    val totalInstagramClicks: Int,
    val totalSearchesFound: Int,
    val avgCtr: Double?,
    val avgPosition: BigDecimal?
) {
    companion object {
        fun from(
            sellerUsername: String,
            analytics: List<Analytics>,
            startDate: LocalDate,
            endDate: LocalDate
        ): AnalyticsSummary {
            val totalViews = analytics.sumOf { it.views }
            val totalClicks = analytics.sumOf { it.clicks }

            val avgCtr = if (totalViews > 0) {
                (totalClicks.toDouble() / totalViews.toDouble()) * 100
            } else null

            val positions = analytics.mapNotNull { it.avgPosition }
            val avgPosition = if (positions.isNotEmpty()) {
                positions.reduce { acc, pos -> acc + pos } / positions.size.toBigDecimal()
            } else null

            return AnalyticsSummary(
                sellerUsername = sellerUsername,
                startDate = startDate,
                endDate = endDate,
                totalViews = totalViews,
                totalClicks = totalClicks,
                totalWhatsappClicks = analytics.sumOf { it.whatsappClicks },
                totalInstagramClicks = analytics.sumOf { it.instagramClicks },
                totalSearchesFound = analytics.sumOf { it.searchesFound },
                avgCtr = avgCtr,
                avgPosition = avgPosition
            )
        }
    }
}

/**
 * Daily Analytics Snapshot Value Object
 */
data class DailySnapshot(
    val date: LocalDate,
    val views: Int,
    val clicks: Int,
    val ctr: Double?,
    val avgPosition: BigDecimal?
) {
    companion object {
        fun from(analytics: Analytics): DailySnapshot {
            return DailySnapshot(
                date = analytics.date,
                views = analytics.views,
                clicks = analytics.clicks,
                ctr = analytics.clickThroughRate(),
                avgPosition = analytics.avgPosition
            )
        }
    }
}
