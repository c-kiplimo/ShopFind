package com.shopfind.domain.model

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Product Domain Entity
 *
 * Represents a product listed by a seller on social media platforms.
 * Follows Domain-Driven Design principles with rich domain logic.
 */
@Table("products")
data class Product(
    @Id
    val id: Long? = null,

    // Basic Information
    val seller: String,
    @Column("product_name")
    val productName: String,
    val caption: String? = null,

    // Pricing
    val price: BigDecimal,
    @Column("original_price")
    val originalPrice: BigDecimal? = null,
    val currency: String = "KES",

    // Categorization
    val category: String,
    val subcategory: String? = null,

    // Media
    @Column("image_url")
    val imageUrl: String,
    @Column("post_url")
    val postUrl: String,

    // Contact Information
    val phone: String? = null,
    val whatsapp: String? = null,
    val email: String? = null,

    // Location
    val location: String = "Nairobi",
    val area: String? = null,

    // Platform
    val platform: Platform = Platform.INSTAGRAM,

    // Metadata
    @Column("scraped_date")
    val scrapedDate: LocalDateTime = LocalDateTime.now(),
    @Column("last_updated")
    val lastUpdated: LocalDateTime = LocalDateTime.now(),
    @Column("is_active")
    val isActive: Boolean = true
) {
    /**
     * Check if product has a discount
     */
    fun hasDiscount(): Boolean =
        originalPrice != null && originalPrice > price

    /**
     * Calculate discount percentage
     */
    fun discountPercentage(): Int? =
        if (hasDiscount() && originalPrice != null) {
            ((originalPrice - price) / originalPrice * BigDecimal(100)).toInt()
        } else null

    /**
     * Check if product is recently added (within last 7 days)
     */
    fun isNew(): Boolean {
        val sevenDaysAgo = LocalDateTime.now().minusDays(7)
        return scrapedDate.isAfter(sevenDaysAgo)
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
     * Validate product invariants
     */
    init {
        require(productName.isNotBlank()) { "Product name cannot be blank" }
        require(price >= BigDecimal.ZERO) { "Price must be non-negative" }
        require(imageUrl.isNotBlank()) { "Image URL is required" }
        require(postUrl.isNotBlank()) { "Post URL is required" }
        if (originalPrice != null) {
            require(originalPrice >= price) { "Original price must be greater than or equal to current price" }
        }
    }

    companion object {
        /**
         * Factory method to create a new product
         */
        fun create(
            seller: String,
            productName: String,
            price: BigDecimal,
            category: String,
            imageUrl: String,
            postUrl: String,
            platform: Platform = Platform.INSTAGRAM,
            caption: String? = null,
            originalPrice: BigDecimal? = null,
            phone: String? = null,
            whatsapp: String? = null,
            location: String = "Nairobi"
        ): Product {
            return Product(
                seller = seller,
                productName = productName,
                price = price,
                category = category,
                imageUrl = imageUrl,
                postUrl = postUrl,
                platform = platform,
                caption = caption,
                originalPrice = originalPrice,
                phone = phone,
                whatsapp = whatsapp,
                location = location
            )
        }
    }
}

/**
 * Platform Enum
 */
enum class Platform {
    INSTAGRAM,
    TIKTOK,
    FACEBOOK;

    companion object {
        fun fromString(value: String): Platform =
            entries.find { it.name.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Invalid platform: $value")
    }
}

/**
 * Contact Method Enum
 */
enum class ContactMethod {
    WHATSAPP,
    PHONE,
    EMAIL
}
