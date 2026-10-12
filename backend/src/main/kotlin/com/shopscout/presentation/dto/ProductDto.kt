package com.shopscout.presentation.dto

import com.shopscout.domain.model.Platform
import com.shopscout.domain.model.Product
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Product DTOs (Data Transfer Objects)
 *
 * Used for API request/response serialization.
 */

/**
 * Product Response DTO
 */
data class ProductResponse(
    val id: Long?,
    val seller: String,
    val productName: String,
    val caption: String?,
    val price: BigDecimal,
    val originalPrice: BigDecimal?,
    val currency: String,
    val category: String,
    val subcategory: String?,
    val imageUrl: String,
    val postUrl: String,
    val phone: String?,
    val whatsapp: String?,
    val whatsappLink: String?,
    val email: String?,
    val location: String,
    val area: String?,
    val platform: String,
    val scrapedDate: LocalDateTime,
    val lastUpdated: LocalDateTime,
    val isActive: Boolean,
    val hasDiscount: Boolean,
    val discountPercentage: Int?,
    val isNew: Boolean
) {
    companion object {
        fun from(product: Product, whatsappLink: String? = null): ProductResponse {
            return ProductResponse(
                id = product.id,
                seller = product.seller,
                productName = product.productName,
                caption = product.caption,
                price = product.price,
                originalPrice = product.originalPrice,
                currency = product.currency,
                category = product.category,
                subcategory = product.subcategory,
                imageUrl = product.imageUrl,
                postUrl = product.postUrl,
                phone = product.phone,
                whatsapp = product.whatsapp,
                whatsappLink = whatsappLink,
                email = product.email,
                location = product.location,
                area = product.area,
                platform = product.platform.name,
                scrapedDate = product.scrapedDate,
                lastUpdated = product.lastUpdated,
                isActive = product.isActive,
                hasDiscount = product.hasDiscount(),
                discountPercentage = product.discountPercentage(),
                isNew = product.isNew()
            )
        }
    }
}

/**
 * Product Create Request DTO
 */
data class ProductCreateRequest(
    @field:NotBlank(message = "Seller username is required")
    val seller: String,

    @field:NotBlank(message = "Product name is required")
    val productName: String,

    val caption: String? = null,

    @field:NotNull(message = "Price is required")
    @field:DecimalMin(value = "0.0", message = "Price must be non-negative")
    val price: BigDecimal,

    @field:DecimalMin(value = "0.0", message = "Original price must be non-negative")
    val originalPrice: BigDecimal? = null,

    val currency: String = "KES",

    @field:NotBlank(message = "Category is required")
    val category: String,

    val subcategory: String? = null,

    @field:NotBlank(message = "Image URL is required")
    val imageUrl: String,

    @field:NotBlank(message = "Post URL is required")
    val postUrl: String,

    val phone: String? = null,
    val whatsapp: String? = null,
    val email: String? = null,
    val location: String = "Nairobi",
    val area: String? = null,

    @field:NotNull(message = "Platform is required")
    val platform: Platform = Platform.INSTAGRAM
) {
    fun toProduct(): Product {
        return Product.create(
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

/**
 * Product Update Request DTO
 */
data class ProductUpdateRequest(
    val productName: String? = null,
    val caption: String? = null,
    val price: BigDecimal? = null,
    val originalPrice: BigDecimal? = null,
    val category: String? = null,
    val subcategory: String? = null,
    val imageUrl: String? = null,
    val location: String? = null,
    val area: String? = null,
    val isActive: Boolean? = null
)

/**
 * Product Search Request DTO
 */
data class ProductSearchRequest(
    val query: String? = null,
    val category: String? = null,
    val subcategory: String? = null,

    @field:DecimalMin(value = "0.0", message = "Min price must be non-negative")
    val minPrice: BigDecimal? = null,

    @field:DecimalMin(value = "0.0", message = "Max price must be non-negative")
    val maxPrice: BigDecimal? = null,

    val location: String? = null,
    val area: String? = null,
    val platform: Platform? = null,
    val seller: String? = null,
    val activeOnly: Boolean = true,
    val newOnly: Boolean = false,
    val withDiscount: Boolean = false,

    @field:Min(value = 0, message = "Page must be non-negative")
    val page: Int = 0,

    @field:Min(value = 1, message = "Size must be at least 1")
    @field:Max(value = 100, message = "Size must not exceed 100")
    val size: Int = 20,

    val sortBy: String = "scraped_date",
    val sortDirection: String = "desc"
)

/**
 * Product Search Response DTO
 */
data class ProductSearchResponse(
    val products: List<ProductResponse>,
    val total: Long,
    val page: Int,
    val size: Int,
    val totalPages: Int
)
