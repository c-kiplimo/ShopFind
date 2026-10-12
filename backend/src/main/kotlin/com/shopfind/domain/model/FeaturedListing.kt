package com.shopfind.domain.model

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Featured Listing Domain Entity
 *
 * Represents a paid featured listing subscription for a seller.
 * Enables sellers to boost visibility through premium placement.
 * Follows Domain-Driven Design principles.
 */
@Table("featured_listings")
data class FeaturedListing(
    @Id
    val id: Long? = null,

    // Seller Reference
    @Column("seller_username")
    val sellerUsername: String,

    // Subscription Period
    @Column("started_at")
    val startedAt: LocalDateTime,
    @Column("expires_at")
    val expiresAt: LocalDateTime,
    @Column("is_active")
    val isActive: Boolean = true,

    // Payment Details
    val amount: BigDecimal,
    val currency: String = "KES",
    @Column("payment_method")
    val paymentMethod: String? = null,
    @Column("transaction_id")
    val transactionId: String? = null,

    // Auto-renewal
    @Column("auto_renew")
    val autoRenew: Boolean = false,

    // Metadata
    @Column("created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Check if listing is currently active and not expired
     */
    fun isCurrentlyActive(): Boolean {
        if (!isActive) return false
        val now = LocalDateTime.now()
        return !now.isBefore(startedAt) && now.isBefore(expiresAt)
    }

    /**
     * Check if listing has expired
     */
    fun hasExpired(): Boolean {
        return LocalDateTime.now().isAfter(expiresAt)
    }

    /**
     * Get days remaining in subscription
     */
    fun daysRemaining(): Long {
        if (hasExpired()) return 0
        val now = LocalDateTime.now()
        return ChronoUnit.DAYS.between(now, expiresAt)
    }

    /**
     * Get hours remaining in subscription
     */
    fun hoursRemaining(): Long {
        if (hasExpired()) return 0
        val now = LocalDateTime.now()
        return ChronoUnit.HOURS.between(now, expiresAt)
    }

    /**
     * Get total subscription duration in days
     */
    fun subscriptionDuration(): Long {
        return ChronoUnit.DAYS.between(startedAt, expiresAt)
    }

    /**
     * Check if subscription is expiring soon (within 3 days)
     */
    fun isExpiringSoon(): Boolean {
        return isCurrentlyActive() && daysRemaining() <= 3
    }

    /**
     * Deactivate the listing
     */
    fun deactivate(): FeaturedListing {
        return copy(isActive = false)
    }

    /**
     * Activate the listing
     */
    fun activate(): FeaturedListing {
        return copy(isActive = true)
    }

    /**
     * Renew the listing for another period
     */
    fun renew(
        duration: Long = 30,
        transactionId: String
    ): FeaturedListing {
        val newStartDate = if (hasExpired()) {
            LocalDateTime.now()
        } else {
            expiresAt
        }

        return copy(
            startedAt = newStartDate,
            expiresAt = newStartDate.plusDays(duration),
            transactionId = transactionId,
            isActive = true
        )
    }

    /**
     * Toggle auto-renewal
     */
    fun setAutoRenew(enabled: Boolean): FeaturedListing {
        return copy(autoRenew = enabled)
    }

    /**
     * Validate featured listing invariants
     */
    init {
        require(sellerUsername.isNotBlank()) { "Seller username cannot be blank" }
        require(amount > BigDecimal.ZERO) { "Amount must be positive" }
        require(expiresAt.isAfter(startedAt)) {
            "Expiry date must be after start date"
        }
        if (isActive && hasExpired()) {
            throw IllegalStateException("Cannot have active status with expired date")
        }
    }

    companion object {
        /**
         * Standard subscription duration (30 days)
         */
        const val STANDARD_DURATION_DAYS = 30L

        /**
         * Standard subscription price (500 KES)
         */
        val STANDARD_PRICE = BigDecimal("500.00")

        /**
         * Factory method to create a new featured listing
         */
        fun create(
            sellerUsername: String,
            transactionId: String,
            durationDays: Long = STANDARD_DURATION_DAYS,
            amount: BigDecimal = STANDARD_PRICE,
            autoRenew: Boolean = false
        ): FeaturedListing {
            val now = LocalDateTime.now()
            return FeaturedListing(
                sellerUsername = sellerUsername,
                startedAt = now,
                expiresAt = now.plusDays(durationDays),
                amount = amount,
                transactionId = transactionId,
                paymentMethod = "M-Pesa",
                autoRenew = autoRenew,
                isActive = true
            )
        }

        /**
         * Create a test/promotional listing
         */
        fun createPromo(
            sellerUsername: String,
            durationDays: Long = 7
        ): FeaturedListing {
            val now = LocalDateTime.now()
            return FeaturedListing(
                sellerUsername = sellerUsername,
                startedAt = now,
                expiresAt = now.plusDays(durationDays),
                amount = BigDecimal.ZERO,
                transactionId = "PROMO",
                paymentMethod = "Promotional",
                autoRenew = false,
                isActive = true
            )
        }
    }
}

/**
 * Featured Listing Summary Value Object
 */
data class FeaturedListingSummary(
    val id: Long?,
    val sellerUsername: String,
    val isActive: Boolean,
    val daysRemaining: Long,
    val expiresAt: LocalDateTime,
    val isExpiringSoon: Boolean
) {
    companion object {
        fun from(listing: FeaturedListing): FeaturedListingSummary {
            return FeaturedListingSummary(
                id = listing.id,
                sellerUsername = listing.sellerUsername,
                isActive = listing.isCurrentlyActive(),
                daysRemaining = listing.daysRemaining(),
                expiresAt = listing.expiresAt,
                isExpiringSoon = listing.isExpiringSoon()
            )
        }
    }
}
