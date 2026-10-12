package com.shopfind.domain.model

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Seller Domain Entity
 *
 * Represents a seller on social media platforms (Instagram, TikTok, Facebook).
 * Sellers can be featured through paid subscriptions.
 * Follows Domain-Driven Design principles with rich domain logic.
 */
@Table("sellers")
data class Seller(
    @Id
    val username: String,

    // Platform
    val platform: Platform,

    // Contact Information
    val phone: String? = null,
    val whatsapp: String? = null,
    val email: String? = null,
    val website: String? = null,

    // Profile
    val bio: String? = null,
    @Column("profile_pic_url")
    val profilePicUrl: String? = null,
    @Column("follower_count")
    val followerCount: Int = 0,

    // Location
    val location: String? = null,
    val area: String? = null,

    // Stats
    @Column("total_products")
    val totalProducts: Int = 0,
    @Column("avg_price")
    val avgPrice: BigDecimal? = null,
    @Column("total_views")
    val totalViews: Int = 0,
    @Column("total_clicks")
    val totalClicks: Int = 0,

    // Features
    val verified: Boolean = false,
    val featured: Boolean = false,
    @Column("featured_until")
    val featuredUntil: LocalDate? = null,

    // Metadata
    @Column("created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),
    @Column("last_scraped")
    val lastScraped: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Check if seller is currently featured
     */
    fun isFeaturedNow(): Boolean {
        if (!featured) return false
        if (featuredUntil == null) return false
        return !featuredUntil.isBefore(LocalDate.now())
    }

    /**
     * Get days remaining for featured listing
     */
    fun featuredDaysRemaining(): Int? {
        if (!isFeaturedNow() || featuredUntil == null) return null
        val today = LocalDate.now()
        return featuredUntil.toEpochDay().toInt() - today.toEpochDay().toInt()
    }

    /**
     * Calculate engagement rate (clicks / views)
     */
    fun engagementRate(): Double? {
        if (totalViews == 0) return null
        return (totalClicks.toDouble() / totalViews.toDouble()) * 100
    }

    /**
     * Check if seller has contact information
     */
    fun hasContact(): Boolean {
        return phone != null || whatsapp != null || email != null
    }

    /**
     * Get primary contact method
     */
    fun primaryContact(): ContactMethod? = when {
        whatsapp != null -> ContactMethod.WHATSAPP
        phone != null -> ContactMethod.PHONE
        email != null -> ContactMethod.EMAIL
        else -> null
    }

    /**
     * Update seller with new featured listing
     */
    fun updateFeatured(expiresAt: LocalDate): Seller {
        return copy(
            featured = true,
            featuredUntil = expiresAt
        )
    }

    /**
     * Remove featured status
     */
    fun removeFeatured(): Seller {
        return copy(
            featured = false,
            featuredUntil = null
        )
    }

    /**
     * Update stats after product addition
     */
    fun incrementProducts(productPrice: BigDecimal): Seller {
        val newTotal = totalProducts + 1
        val newAvgPrice = if (avgPrice == null) {
            productPrice
        } else {
            ((avgPrice * totalProducts.toBigDecimal()) + productPrice) / newTotal.toBigDecimal()
        }

        return copy(
            totalProducts = newTotal,
            avgPrice = newAvgPrice
        )
    }

    /**
     * Update view count
     */
    fun incrementViews(count: Int = 1): Seller {
        return copy(totalViews = totalViews + count)
    }

    /**
     * Update click count
     */
    fun incrementClicks(count: Int = 1): Seller {
        return copy(totalClicks = totalClicks + count)
    }

    /**
     * Validate seller invariants
     */
    init {
        require(username.isNotBlank()) { "Username cannot be blank" }
        require(followerCount >= 0) { "Follower count cannot be negative" }
        require(totalProducts >= 0) { "Total products cannot be negative" }
        require(totalViews >= 0) { "Total views cannot be negative" }
        require(totalClicks >= 0) { "Total clicks cannot be negative" }
        if (avgPrice != null) {
            require(avgPrice >= BigDecimal.ZERO) { "Average price must be non-negative" }
        }
        if (featured) {
            require(featuredUntil != null) { "Featured sellers must have a featured_until date" }
        }
    }

    companion object {
        /**
         * Factory method to create a new seller
         */
        fun create(
            username: String,
            platform: Platform,
            phone: String? = null,
            whatsapp: String? = null,
            email: String? = null,
            bio: String? = null,
            location: String? = null,
            verified: Boolean = false
        ): Seller {
            return Seller(
                username = username,
                platform = platform,
                phone = phone,
                whatsapp = whatsapp,
                email = email,
                bio = bio,
                location = location,
                verified = verified
            )
        }
    }
}

/**
 * Seller Statistics Value Object
 */
data class SellerStats(
    val totalProducts: Int,
    val avgPrice: BigDecimal?,
    val totalViews: Int,
    val totalClicks: Int,
    val engagementRate: Double?
) {
    companion object {
        fun from(seller: Seller): SellerStats {
            return SellerStats(
                totalProducts = seller.totalProducts,
                avgPrice = seller.avgPrice,
                totalViews = seller.totalViews,
                totalClicks = seller.totalClicks,
                engagementRate = seller.engagementRate()
            )
        }
    }
}
