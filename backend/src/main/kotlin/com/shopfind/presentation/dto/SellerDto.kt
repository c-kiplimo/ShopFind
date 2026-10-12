package com.shopfind.presentation.dto

import com.shopfind.domain.model.Platform
import com.shopfind.domain.model.Seller
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Seller Response DTO
 */
data class SellerResponse(
    val username: String,
    val platform: String,
    val phone: String?,
    val whatsapp: String?,
    val whatsappLink: String?,
    val email: String?,
    val website: String?,
    val bio: String?,
    val profilePicUrl: String?,
    val followerCount: Int,
    val location: String?,
    val area: String?,
    val totalProducts: Int,
    val avgPrice: BigDecimal?,
    val totalViews: Int,
    val totalClicks: Int,
    val engagementRate: Double?,
    val verified: Boolean,
    val featured: Boolean,
    val featuredUntil: LocalDate?,
    val isFeaturedNow: Boolean,
    val featuredDaysRemaining: Int?,
    val platformLink: String
) {
    companion object {
        fun from(seller: Seller, whatsappLink: String?, platformLink: String): SellerResponse {
            return SellerResponse(
                username = seller.username,
                platform = seller.platform.name,
                phone = seller.phone,
                whatsapp = seller.whatsapp,
                whatsappLink = whatsappLink,
                email = seller.email,
                website = seller.website,
                bio = seller.bio,
                profilePicUrl = seller.profilePicUrl,
                followerCount = seller.followerCount,
                location = seller.location,
                area = seller.area,
                totalProducts = seller.totalProducts,
                avgPrice = seller.avgPrice,
                totalViews = seller.totalViews,
                totalClicks = seller.totalClicks,
                engagementRate = seller.engagementRate(),
                verified = seller.verified,
                featured = seller.featured,
                featuredUntil = seller.featuredUntil,
                isFeaturedNow = seller.isFeaturedNow(),
                featuredDaysRemaining = seller.featuredDaysRemaining(),
                platformLink = platformLink
            )
        }
    }
}

/**
 * Seller Create Request DTO
 */
data class SellerCreateRequest(
    @field:NotBlank(message = "Username is required")
    val username: String,

    @field:NotNull(message = "Platform is required")
    val platform: Platform,

    val phone: String? = null,
    val whatsapp: String? = null,
    val email: String? = null,
    val bio: String? = null,
    val location: String? = null,
    val verified: Boolean = false
)

/**
 * Seller Update Request DTO
 */
data class SellerUpdateRequest(
    val phone: String? = null,
    val whatsapp: String? = null,
    val email: String? = null,
    val website: String? = null,
    val bio: String? = null,
    val profilePicUrl: String? = null,
    val location: String? = null,
    val area: String? = null
)
