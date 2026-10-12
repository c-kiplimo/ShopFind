package com.shopfind.application.service

import com.shopfind.domain.model.Platform
import com.shopfind.domain.model.Seller
import com.shopfind.infrastructure.repository.ProductRepository
import com.shopfind.infrastructure.repository.SellerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Seller Service
 *
 * Handles all seller-related business logic.
 * Manages seller profiles, stats, and featured listings.
 */
@Service
class SellerService(
    private val sellerRepository: SellerRepository,
    private val productRepository: ProductRepository
) {
    /**
     * Find seller by username
     */
    suspend fun findByUsername(username: String): Seller? {
        return sellerRepository.findByUsername(username)
    }

    /**
     * Get all sellers
     */
    fun getAllSellers(page: Int = 0, size: Int = 20): Flow<Seller> {
        return sellerRepository.findAll(page, size)
    }

    /**
     * Create or update seller
     */
    suspend fun saveSeller(seller: Seller): Seller {
        return sellerRepository.save(seller)
    }

    /**
     * Create new seller
     */
    suspend fun createSeller(
        username: String,
        platform: Platform,
        phone: String? = null,
        whatsapp: String? = null,
        email: String? = null,
        bio: String? = null,
        location: String? = null,
        verified: Boolean = false
    ): Seller {
        val seller = Seller.create(
            username = username,
            platform = platform,
            phone = phone,
            whatsapp = whatsapp,
            email = email,
            bio = bio,
            location = location,
            verified = verified
        )
        return sellerRepository.save(seller)
    }

    /**
     * Delete seller
     */
    suspend fun deleteSeller(username: String): Boolean {
        return sellerRepository.deleteByUsername(username)
    }

    /**
     * Get featured sellers
     */
    fun getFeaturedSellers(): Flow<Seller> {
        return sellerRepository.findFeatured()
    }

    /**
     * Get verified sellers
     */
    fun getVerifiedSellers(): Flow<Seller> {
        return sellerRepository.findVerified()
    }

    /**
     * Get sellers by platform
     */
    fun getSellersByPlatform(platform: Platform): Flow<Seller> {
        return sellerRepository.findByPlatform(platform)
    }

    /**
     * Get sellers by location
     */
    fun getSellersByLocation(location: String): Flow<Seller> {
        return sellerRepository.findByLocation(location)
    }

    /**
     * Get top sellers
     */
    fun getTopSellers(limit: Int = 10): Flow<Seller> {
        return sellerRepository.findTopSellers(limit)
    }

    /**
     * Get most viewed sellers
     */
    fun getMostViewedSellers(limit: Int = 10): Flow<Seller> {
        return sellerRepository.findMostViewed(limit)
    }

    /**
     * Update seller stats after product added
     */
    suspend fun updateStatsAfterProductAdded(
        username: String,
        productPrice: BigDecimal
    ) {
        val seller = sellerRepository.findByUsername(username)
        if (seller != null) {
            val updated = seller.incrementProducts(productPrice)
            sellerRepository.save(updated)
        }
    }

    /**
     * Recalculate seller stats from products
     */
    suspend fun recalculateStats(username: String) {
        val productCount = productRepository.countBySeller(username)
        val avgPrice = productRepository.getAveragePriceBySeller(username)

        sellerRepository.updateStats(
            username = username,
            totalProducts = productCount.toInt(),
            avgPrice = avgPrice
        )
    }

    /**
     * Increment seller views
     */
    suspend fun incrementViews(username: String, count: Int = 1) {
        sellerRepository.incrementViews(username, count)
    }

    /**
     * Increment seller clicks
     */
    suspend fun incrementClicks(username: String, count: Int = 1) {
        sellerRepository.incrementClicks(username, count)
    }

    /**
     * Make seller featured
     */
    suspend fun makeFeatured(username: String, expiresAt: LocalDate): Boolean {
        return sellerRepository.updateFeatured(
            username = username,
            featured = true,
            featuredUntil = expiresAt
        )
    }

    /**
     * Remove featured status
     */
    suspend fun removeFeatured(username: String): Boolean {
        return sellerRepository.updateFeatured(
            username = username,
            featured = false,
            featuredUntil = null
        )
    }

    /**
     * Verify seller
     */
    suspend fun verifySeller(username: String): Boolean {
        return sellerRepository.updateVerified(username, true)
    }

    /**
     * Unverify seller
     */
    suspend fun unverifySeller(username: String): Boolean {
        return sellerRepository.updateVerified(username, false)
    }

    /**
     * Get sellers with expiring featured listings
     */
    fun getSellersExpiringSoon(): Flow<Seller> {
        return sellerRepository.findExpiringSoon()
    }

    /**
     * Expire featured listings (cleanup task)
     */
    suspend fun expireFeaturedListings(): Long {
        return sellerRepository.expireFeatured()
    }

    /**
     * Get seller profile with products
     */
    suspend fun getSellerProfile(username: String): SellerProfile? {
        val seller = sellerRepository.findByUsername(username) ?: return null
        val products = productRepository.findBySeller(username).toList()

        return SellerProfile(
            seller = seller,
            products = products,
            productCount = products.size,
            isFeatured = seller.isFeaturedNow(),
            featuredDaysRemaining = seller.featuredDaysRemaining()
        )
    }

    /**
     * Get seller statistics
     */
    suspend fun getSellerStats(): SellerStats {
        val totalSellers = sellerRepository.count()
        val featuredCount = sellerRepository.countFeatured()
        val locations = sellerRepository.getDistinctLocations()

        return SellerStats(
            totalSellers = totalSellers,
            featuredSellers = featuredCount,
            totalLocations = locations.size
        )
    }

    /**
     * Check if seller exists
     */
    suspend fun exists(username: String): Boolean {
        return sellerRepository.existsByUsername(username)
    }
}

/**
 * Seller Profile with Products
 */
data class SellerProfile(
    val seller: Seller,
    val products: List<com.shopscout.domain.model.Product>,
    val productCount: Int,
    val isFeatured: Boolean,
    val featuredDaysRemaining: Int?
)

/**
 * Seller Statistics
 */
data class SellerStats(
    val totalSellers: Long,
    val featuredSellers: Long,
    val totalLocations: Int
)
