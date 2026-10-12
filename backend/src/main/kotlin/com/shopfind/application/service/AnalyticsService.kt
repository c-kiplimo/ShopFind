package com.shopfind.application.service

import com.shopfind.domain.model.Analytics
import com.shopfind.domain.model.AnalyticsSummary
import com.shopfind.domain.model.DailySnapshot
import com.shopfind.infrastructure.repository.AnalyticsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Analytics Service
 *
 * Handles seller analytics and performance tracking.
 * Provides insights and metrics for seller dashboard.
 */
@Service
class AnalyticsService(
    private val analyticsRepository: AnalyticsRepository
) {
    /**
     * Track product view
     */
    suspend fun trackView(sellerUsername: String, count: Int = 1) {
        analyticsRepository.incrementViewsToday(sellerUsername, count)
    }

    /**
     * Track product click
     */
    suspend fun trackClick(sellerUsername: String, count: Int = 1) {
        analyticsRepository.incrementClicksToday(sellerUsername, count)
    }

    /**
     * Track WhatsApp click
     */
    suspend fun trackWhatsappClick(sellerUsername: String, count: Int = 1) {
        analyticsRepository.incrementWhatsappClicksToday(sellerUsername, count)
    }

    /**
     * Track Instagram click
     */
    suspend fun trackInstagramClick(sellerUsername: String, count: Int = 1) {
        analyticsRepository.incrementInstagramClicksToday(sellerUsername, count)
    }

    /**
     * Track search appearance
     */
    suspend fun trackSearchAppearance(sellerUsername: String, position: Int, count: Int = 1) {
        analyticsRepository.incrementSearchesToday(sellerUsername, count)
        analyticsRepository.updateAvgPositionToday(sellerUsername, BigDecimal(position))
    }

    /**
     * Get today's analytics for seller
     */
    suspend fun getTodayAnalytics(sellerUsername: String): Analytics? {
        return analyticsRepository.findTodayBySeller(sellerUsername)
    }

    /**
     * Get analytics for seller on specific date
     */
    suspend fun getAnalyticsByDate(
        sellerUsername: String,
        date: LocalDate
    ): Analytics? {
        return analyticsRepository.findBySellerAndDate(sellerUsername, date)
    }

    /**
     * Get analytics for seller within date range
     */
    fun getAnalyticsByDateRange(
        sellerUsername: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<Analytics> {
        return analyticsRepository.findBySellerAndDateRange(sellerUsername, startDate, endDate)
    }

    /**
     * Get last N days analytics
     */
    fun getLastNDays(sellerUsername: String, days: Int): Flow<Analytics> {
        return analyticsRepository.findLastNDays(sellerUsername, days)
    }

    /**
     * Get this week's analytics
     */
    fun getThisWeek(sellerUsername: String): Flow<Analytics> {
        return analyticsRepository.findThisWeek(sellerUsername)
    }

    /**
     * Get this month's analytics
     */
    fun getThisMonth(sellerUsername: String): Flow<Analytics> {
        return analyticsRepository.findThisMonth(sellerUsername)
    }

    /**
     * Get analytics summary for date range
     */
    suspend fun getSummary(
        sellerUsername: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): AnalyticsSummary {
        val analytics = analyticsRepository
            .findBySellerAndDateRange(sellerUsername, startDate, endDate)
            .toList()

        return AnalyticsSummary.from(
            sellerUsername = sellerUsername,
            analytics = analytics,
            startDate = startDate,
            endDate = endDate
        )
    }

    /**
     * Get daily snapshots for chart
     */
    suspend fun getDailySnapshots(
        sellerUsername: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DailySnapshot> {
        val analytics = analyticsRepository
            .findBySellerAndDateRange(sellerUsername, startDate, endDate)
            .toList()

        return analytics.map { DailySnapshot.from(it) }
    }

    /**
     * Get top sellers by views
     */
    suspend fun getTopSellersByViews(
        startDate: LocalDate,
        endDate: LocalDate,
        limit: Int = 10
    ): List<Pair<String, Int>> {
        return analyticsRepository.getTopSellersByViews(startDate, endDate, limit)
    }

    /**
     * Get top sellers by clicks
     */
    suspend fun getTopSellersByClicks(
        startDate: LocalDate,
        endDate: LocalDate,
        limit: Int = 10
    ): List<Pair<String, Int>> {
        return analyticsRepository.getTopSellersByClicks(startDate, endDate, limit)
    }

    /**
     * Get aggregated stats
     */
    suspend fun getAggregatedStats(
        sellerUsername: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Map<String, Any> {
        return analyticsRepository.getAggregatedStats(sellerUsername, startDate, endDate)
    }

    /**
     * Get seller dashboard data
     */
    suspend fun getSellerDashboard(sellerUsername: String): SellerDashboard {
        val today = getTodayAnalytics(sellerUsername)
        val last7Days = getLastNDays(sellerUsername, 7).toList()
        val last30Days = getLastNDays(sellerUsername, 30).toList()

        val summary7Days = AnalyticsSummary.from(
            sellerUsername = sellerUsername,
            analytics = last7Days,
            startDate = LocalDate.now().minusDays(6),
            endDate = LocalDate.now()
        )

        val summary30Days = AnalyticsSummary.from(
            sellerUsername = sellerUsername,
            analytics = last30Days,
            startDate = LocalDate.now().minusDays(29),
            endDate = LocalDate.now()
        )

        return SellerDashboard(
            today = today,
            last7Days = summary7Days,
            last30Days = summary30Days,
            dailyData = last30Days.map { DailySnapshot.from(it) }
        )
    }

    /**
     * Clean up old analytics (scheduled task)
     */
    suspend fun cleanupOldAnalytics(): Long {
        return analyticsRepository.deleteOldAnalytics()
    }
}

/**
 * Seller Dashboard Data
 */
data class SellerDashboard(
    val today: Analytics?,
    val last7Days: AnalyticsSummary,
    val last30Days: AnalyticsSummary,
    val dailyData: List<DailySnapshot>
)
