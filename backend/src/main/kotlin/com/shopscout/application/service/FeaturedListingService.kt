package com.shopscout.application.service

import com.shopscout.domain.model.FeaturedListing
import com.shopscout.infrastructure.repository.FeaturedListingRepository
import com.shopscout.infrastructure.repository.SellerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Featured Listing Service
 *
 * Handles featured listing subscriptions and renewals.
 * Manages seller premium placements.
 */
@Service
class FeaturedListingService(
    private val featuredListingRepository: FeaturedListingRepository,
    private val sellerRepository: SellerRepository
) {
    /**
     * Create featured listing
     */
    suspend fun createFeaturedListing(
        sellerUsername: String,
        transactionId: String,
        amount: BigDecimal = FeaturedListing.STANDARD_PRICE,
        durationDays: Long = FeaturedListing.STANDARD_DURATION_DAYS,
        autoRenew: Boolean = false
    ): FeaturedListing {
        val listing = FeaturedListing.create(
            sellerUsername = sellerUsername,
            transactionId = transactionId,
            amount = amount,
            durationDays = durationDays,
            autoRenew = autoRenew
        )

        val saved = featuredListingRepository.save(listing)

        // Update seller featured status
        sellerRepository.updateFeatured(
            username = sellerUsername,
            featured = true,
            featuredUntil = saved.expiresAt.toLocalDate()
        )

        return saved
    }

    /**
     * Create promotional featured listing
     */
    suspend fun createPromoListing(
        sellerUsername: String,
        durationDays: Long = 7
    ): FeaturedListing {
        val listing = FeaturedListing.createPromo(
            sellerUsername = sellerUsername,
            durationDays = durationDays
        )

        val saved = featuredListingRepository.save(listing)

        // Update seller featured status
        sellerRepository.updateFeatured(
            username = sellerUsername,
            featured = true,
            featuredUntil = saved.expiresAt.toLocalDate()
        )

        return saved
    }

    /**
     * Find listing by ID
     */
    suspend fun findById(id: Long): FeaturedListing? {
        return featuredListingRepository.findById(id)
    }

    /**
     * Get listings for seller
     */
    fun getListingsBySeller(sellerUsername: String): Flow<FeaturedListing> {
        return featuredListingRepository.findBySeller(sellerUsername)
    }

    /**
     * Get active listing for seller
     */
    suspend fun getActiveListingForSeller(sellerUsername: String): FeaturedListing? {
        return featuredListingRepository.findActiveByTSeller(sellerUsername)
    }

    /**
     * Get all active listings
     */
    fun getActiveListings(): Flow<FeaturedListing> {
        return featuredListingRepository.findActive()
    }

    /**
     * Get listings expiring soon
     */
    fun getListingsExpiringSoon(): Flow<FeaturedListing> {
        return featuredListingRepository.findExpiringSoon()
    }

    /**
     * Renew featured listing
     */
    suspend fun renewListing(
        listingId: Long,
        transactionId: String,
        durationDays: Long = FeaturedListing.STANDARD_DURATION_DAYS
    ): FeaturedListing? {
        val listing = featuredListingRepository.findById(listingId) ?: return null

        val renewed = listing.renew(durationDays, transactionId)
        val saved = featuredListingRepository.save(renewed)

        // Update seller featured status
        sellerRepository.updateFeatured(
            username = listing.sellerUsername,
            featured = true,
            featuredUntil = saved.expiresAt.toLocalDate()
        )

        return saved
    }

    /**
     * Deactivate listing
     */
    suspend fun deactivateListing(listingId: Long): FeaturedListing? {
        val listing = featuredListingRepository.findById(listingId) ?: return null

        val deactivated = listing.deactivate()
        val saved = featuredListingRepository.save(deactivated)

        // Update seller featured status
        sellerRepository.updateFeatured(
            username = listing.sellerUsername,
            featured = false,
            featuredUntil = null
        )

        return saved
    }

    /**
     * Toggle auto-renewal
     */
    suspend fun setAutoRenew(listingId: Long, enabled: Boolean): FeaturedListing? {
        val listing = featuredListingRepository.findById(listingId) ?: return null

        val updated = listing.setAutoRenew(enabled)
        return featuredListingRepository.save(updated)
    }

    /**
     * Process expired listings (cleanup task)
     */
    suspend fun processExpiredListings(): Long {
        val expired = featuredListingRepository.findExpired().toList()

        expired.forEach { listing ->
            // Deactivate listing
            val deactivated = listing.deactivate()
            featuredListingRepository.save(deactivated)

            // Update seller status
            sellerRepository.updateFeatured(
                username = listing.sellerUsername,
                featured = false,
                featuredUntil = null
            )
        }

        return expired.size.toLong()
    }

    /**
     * Process auto-renewals
     */
    suspend fun processAutoRenewals(): List<FeaturedListing> {
        val expiringSoon = featuredListingRepository.findExpiringSoon().toList()

        val autoRenewable = expiringSoon.filter { it.autoRenew && it.isCurrentlyActive() }

        // Note: In production, this would initiate M-Pesa payments for auto-renewals
        // For now, we just return the list that needs renewal

        return autoRenewable
    }

    /**
     * Get total revenue from featured listings
     */
    suspend fun getTotalRevenue(): BigDecimal {
        return featuredListingRepository.getTotalRevenue()
    }

    /**
     * Get revenue within date range
     */
    suspend fun getRevenueByDateRange(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): BigDecimal {
        return featuredListingRepository.getRevenueByDateRange(startDate, endDate)
    }

    /**
     * Get featured listing statistics
     */
    suspend fun getListingStats(): FeaturedListingStats {
        val activeCount = featuredListingRepository.countActive()
        val totalRevenue = featuredListingRepository.getTotalRevenue()
        val expiringSoon = featuredListingRepository.findExpiringSoon().toList()

        return FeaturedListingStats(
            activeListings = activeCount,
            listingsExpiringSoon = expiringSoon.size,
            totalRevenue = totalRevenue
        )
    }

    /**
     * Check if seller has active listing
     */
    suspend fun hasActiveListing(sellerUsername: String): Boolean {
        return getActiveListingForSeller(sellerUsername) != null
    }
}

/**
 * Featured Listing Statistics
 */
data class FeaturedListingStats(
    val activeListings: Long,
    val listingsExpiringSoon: Int,
    val totalRevenue: BigDecimal
)
