package com.shopfind.application.service

import com.shopfind.domain.model.Platform
import com.shopfind.domain.model.Product
import com.shopfind.domain.specification.ProductSpecificationBuilder
import com.shopfind.domain.specification.Specification
import com.shopfind.infrastructure.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.math.BigDecimal

/**
 * Product Service
 *
 * Handles all product-related business logic.
 * Orchestrates product operations across repositories.
 */
@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val sellerService: SellerService
) {
    /**
     * Find product by ID
     */
    suspend fun findById(id: Long): Product? {
        return productRepository.findById(id)
    }

    /**
     * Search products with filters
     */
    fun searchProducts(
        query: String? = null,
        category: String? = null,
        subcategory: String? = null,
        minPrice: BigDecimal? = null,
        maxPrice: BigDecimal? = null,
        location: String? = null,
        area: String? = null,
        platform: Platform? = null,
        seller: String? = null,
        activeOnly: Boolean = true,
        newOnly: Boolean = false,
        withDiscount: Boolean = false,
        page: Int = 0,
        size: Int = 20,
        sortBy: String = "scraped_date",
        sortDirection: String = "desc"
    ): Flow<Product> {
        val spec = buildSearchSpecification(
            query, category, subcategory, minPrice, maxPrice,
            location, area, platform, seller, activeOnly, newOnly, withDiscount
        )

        val sort = Sort.by(
            if (sortDirection.lowercase() == "asc")
                Sort.Direction.ASC
            else
                Sort.Direction.DESC,
            sortBy
        )

        return productRepository.findAll(spec, page, size, sort)
    }

    /**
     * Count products matching search criteria
     */
    suspend fun countProducts(
        query: String? = null,
        category: String? = null,
        subcategory: String? = null,
        minPrice: BigDecimal? = null,
        maxPrice: BigDecimal? = null,
        location: String? = null,
        area: String? = null,
        platform: Platform? = null,
        seller: String? = null,
        activeOnly: Boolean = true,
        newOnly: Boolean = false,
        withDiscount: Boolean = false
    ): Long {
        val spec = buildSearchSpecification(
            query, category, subcategory, minPrice, maxPrice,
            location, area, platform, seller, activeOnly, newOnly, withDiscount
        )

        return productRepository.count(spec)
    }

    /**
     * Full-text search
     */
    fun fullTextSearch(query: String, page: Int = 0, size: Int = 20): Flow<Product> {
        // PostgreSQL full-text search returns results ranked by relevance
        return productRepository.search(query)
    }

    /**
     * Get products by seller
     */
    fun getProductsBySeller(seller: String): Flow<Product> {
        return productRepository.findBySeller(seller)
    }

    /**
     * Get products by category
     */
    fun getProductsByCategory(category: String): Flow<Product> {
        return productRepository.findByCategory(category)
    }

    /**
     * Get new products
     */
    fun getNewProducts(limit: Int = 20): Flow<Product> {
        return productRepository.findNew()
    }

    /**
     * Get products with discount
     */
    fun getDiscountProducts(limit: Int = 20): Flow<Product> {
        return productRepository.findWithDiscount()
    }

    /**
     * Create product
     */
    suspend fun createProduct(product: Product): Product {
        val saved = productRepository.save(product)

        // Update seller stats
        sellerService.updateStatsAfterProductAdded(
            product.seller,
            product.price
        )

        return saved
    }

    /**
     * Update product
     */
    suspend fun updateProduct(id: Long, updates: (Product) -> Product): Product? {
        val existing = productRepository.findById(id) ?: return null
        val updated = updates(existing)
        return productRepository.save(updated)
    }

    /**
     * Delete product
     */
    suspend fun deleteProduct(id: Long): Boolean {
        return productRepository.deleteById(id)
    }

    /**
     * Deactivate product
     */
    suspend fun deactivateProduct(id: Long): Boolean {
        return productRepository.updateActiveStatus(id, false)
    }

    /**
     * Activate product
     */
    suspend fun activateProduct(id: Long): Boolean {
        return productRepository.updateActiveStatus(id, true)
    }

    /**
     * Get distinct categories
     */
    suspend fun getCategories(): List<String> {
        return productRepository.getDistinctCategories()
    }

    /**
     * Get distinct locations
     */
    suspend fun getLocations(): List<String> {
        return productRepository.getDistinctLocations()
    }

    /**
     * Get featured products (from featured sellers)
     */
    suspend fun getFeaturedProducts(limit: Int = 20): List<Product> {
        val featuredSellers = sellerService.getFeaturedSellers()
            .toList()
            .map { it.username }

        if (featuredSellers.isEmpty()) return emptyList()

        // Get products from featured sellers
        return featuredSellers
            .flatMap { seller ->
                productRepository.findBySeller(seller).toList()
            }
            .sortedByDescending { it.scrapedDate }
            .take(limit)
    }

    /**
     * Build search specification from parameters
     */
    private fun buildSearchSpecification(
        query: String?,
        category: String?,
        subcategory: String?,
        minPrice: BigDecimal?,
        maxPrice: BigDecimal?,
        location: String?,
        area: String?,
        platform: Platform?,
        seller: String?,
        activeOnly: Boolean,
        newOnly: Boolean,
        withDiscount: Boolean
    ): Specification<Product> {
        return ProductSpecificationBuilder().apply {
            if (query != null) withSearch(query)
            if (category != null) withCategory(category)
            if (subcategory != null) withSubcategory(subcategory)
            if (minPrice != null || maxPrice != null) withPriceRange(minPrice, maxPrice)
            if (location != null) withLocation(location)
            if (area != null) withArea(area)
            if (platform != null) withPlatform(platform)
            if (seller != null) withSeller(seller)
            if (activeOnly) activeOnly()
            if (newOnly) newOnly()
            if (withDiscount) withDiscount()
        }.build()
    }

    /**
     * Get product statistics
     */
    suspend fun getProductStats(): ProductStats {
        val totalProducts = productRepository.count()
        val categories = productRepository.getDistinctCategories()
        val locations = productRepository.getDistinctLocations()

        return ProductStats(
            totalProducts = totalProducts,
            totalCategories = categories.size,
            totalLocations = locations.size
        )
    }
}

/**
 * Product Statistics
 */
data class ProductStats(
    val totalProducts: Long,
    val totalCategories: Int,
    val totalLocations: Int
)

/**
 * Search Result Page
 */
data class ProductSearchResult(
    val products: List<Product>,
    val total: Long,
    val page: Int,
    val size: Int,
    val totalPages: Int
) {
    companion object {
        suspend fun from(
            products: List<Product>,
            total: Long,
            page: Int,
            size: Int
        ): ProductSearchResult {
            val totalPages = if (size > 0) {
                ((total + size - 1) / size).toInt()
            } else 0

            return ProductSearchResult(
                products = products,
                total = total,
                page = page,
                size = size,
                totalPages = totalPages
            )
        }
    }
}
