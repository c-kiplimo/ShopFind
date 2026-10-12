package com.shopscout.presentation.controller

import com.shopscout.application.service.AnalyticsService
import com.shopscout.application.service.MessageService
import com.shopscout.application.service.ProductService
import com.shopscout.presentation.dto.*
import jakarta.validation.Valid
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Product Controller
 *
 * REST API endpoints for product operations.
 * Handles product search, CRUD, and analytics tracking.
 */
@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = ["http://localhost:3000", "https://shopscout.ke"])
class ProductController(
    private val productService: ProductService,
    private val messageService: MessageService,
    private val analyticsService: AnalyticsService
) {
    /**
     * Search products
     * GET /api/products/search
     */
    @GetMapping("/search")
    suspend fun searchProducts(
        @Valid @ModelAttribute request: ProductSearchRequest
    ): ResponseEntity<ProductSearchResponse> {
        val products = productService.searchProducts(
            query = request.query,
            category = request.category,
            subcategory = request.subcategory,
            minPrice = request.minPrice,
            maxPrice = request.maxPrice,
            location = request.location,
            area = request.area,
            platform = request.platform,
            seller = request.seller,
            activeOnly = request.activeOnly,
            newOnly = request.newOnly,
            withDiscount = request.withDiscount,
            page = request.page,
            size = request.size,
            sortBy = request.sortBy,
            sortDirection = request.sortDirection
        ).map { product ->
            val whatsappLink = messageService.generateWhatsAppLink(product)
            ProductResponse.from(product, whatsappLink)
        }.toList()

        val total = productService.countProducts(
            query = request.query,
            category = request.category,
            subcategory = request.subcategory,
            minPrice = request.minPrice,
            maxPrice = request.maxPrice,
            location = request.location,
            area = request.area,
            platform = request.platform,
            seller = request.seller,
            activeOnly = request.activeOnly,
            newOnly = request.newOnly,
            withDiscount = request.withDiscount
        )

        val totalPages = if (request.size > 0) {
            ((total + request.size - 1) / request.size).toInt()
        } else 0

        val response = ProductSearchResponse(
            products = products,
            total = total,
            page = request.page,
            size = request.size,
            totalPages = totalPages
        )

        return ResponseEntity.ok(response)
    }

    /**
     * Full-text search
     * GET /api/products/search/text
     */
    @GetMapping("/search/text")
    suspend fun fullTextSearch(
        @RequestParam query: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<List<ProductResponse>> {
        val products = productService.fullTextSearch(query, page, size)
            .map { product ->
                val whatsappLink = messageService.generateWhatsAppLink(product)
                ProductResponse.from(product, whatsappLink)
            }
            .toList()

        return ResponseEntity.ok(products)
    }

    /**
     * Get product by ID
     * GET /api/products/{id}
     */
    @GetMapping("/{id}")
    suspend fun getProductById(@PathVariable id: Long): ResponseEntity<ProductResponse> {
        val product = productService.findById(id)
            ?: return ResponseEntity.notFound().build()

        // Track view
        analyticsService.trackView(product.seller)

        val whatsappLink = messageService.generateWhatsAppLink(product)
        return ResponseEntity.ok(ProductResponse.from(product, whatsappLink))
    }

    /**
     * Get products by seller
     * GET /api/products/seller/{seller}
     */
    @GetMapping("/seller/{seller}")
    suspend fun getProductsBySeller(@PathVariable seller: String): ResponseEntity<List<ProductResponse>> {
        val products = productService.getProductsBySeller(seller)
            .map { product ->
                val whatsappLink = messageService.generateWhatsAppLink(product)
                ProductResponse.from(product, whatsappLink)
            }
            .toList()

        return ResponseEntity.ok(products)
    }

    /**
     * Get products by category
     * GET /api/products/category/{category}
     */
    @GetMapping("/category/{category}")
    suspend fun getProductsByCategory(@PathVariable category: String): ResponseEntity<List<ProductResponse>> {
        val products = productService.getProductsByCategory(category)
            .map { product ->
                val whatsappLink = messageService.generateWhatsAppLink(product)
                ProductResponse.from(product, whatsappLink)
            }
            .toList()

        return ResponseEntity.ok(products)
    }

    /**
     * Get new products
     * GET /api/products/new
     */
    @GetMapping("/new")
    suspend fun getNewProducts(
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseEntity<List<ProductResponse>> {
        val products = productService.getNewProducts(limit)
            .map { product ->
                val whatsappLink = messageService.generateWhatsAppLink(product)
                ProductResponse.from(product, whatsappLink)
            }
            .toList()
            .take(limit)

        return ResponseEntity.ok(products)
    }

    /**
     * Get products with discount
     * GET /api/products/discount
     */
    @GetMapping("/discount")
    suspend fun getDiscountProducts(
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseEntity<List<ProductResponse>> {
        val products = productService.getDiscountProducts(limit)
            .map { product ->
                val whatsappLink = messageService.generateWhatsAppLink(product)
                ProductResponse.from(product, whatsappLink)
            }
            .toList()
            .take(limit)

        return ResponseEntity.ok(products)
    }

    /**
     * Get featured products
     * GET /api/products/featured
     */
    @GetMapping("/featured")
    suspend fun getFeaturedProducts(
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseEntity<List<ProductResponse>> {
        val products = productService.getFeaturedProducts(limit)
            .map { product ->
                val whatsappLink = messageService.generateWhatsAppLink(product)
                ProductResponse.from(product, whatsappLink)
            }

        return ResponseEntity.ok(products)
    }

    /**
     * Get all categories
     * GET /api/products/categories
     */
    @GetMapping("/categories")
    suspend fun getCategories(): ResponseEntity<List<String>> {
        val categories = productService.getCategories()
        return ResponseEntity.ok(categories)
    }

    /**
     * Get all locations
     * GET /api/products/locations
     */
    @GetMapping("/locations")
    suspend fun getLocations(): ResponseEntity<List<String>> {
        val locations = productService.getLocations()
        return ResponseEntity.ok(locations)
    }

    /**
     * Create product
     * POST /api/products
     */
    @PostMapping
    suspend fun createProduct(
        @Valid @RequestBody request: ProductCreateRequest
    ): ResponseEntity<ProductResponse> {
        val product = productService.createProduct(request.toProduct())
        val whatsappLink = messageService.generateWhatsAppLink(product)

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ProductResponse.from(product, whatsappLink))
    }

    /**
     * Update product
     * PATCH /api/products/{id}
     */
    @PatchMapping("/{id}")
    suspend fun updateProduct(
        @PathVariable id: Long,
        @Valid @RequestBody request: ProductUpdateRequest
    ): ResponseEntity<ProductResponse> {
        val updated = productService.updateProduct(id) { product ->
            product.copy(
                productName = request.productName ?: product.productName,
                caption = request.caption ?: product.caption,
                price = request.price ?: product.price,
                originalPrice = request.originalPrice ?: product.originalPrice,
                category = request.category ?: product.category,
                subcategory = request.subcategory ?: product.subcategory,
                imageUrl = request.imageUrl ?: product.imageUrl,
                location = request.location ?: product.location,
                area = request.area ?: product.area,
                isActive = request.isActive ?: product.isActive
            )
        } ?: return ResponseEntity.notFound().build()

        val whatsappLink = messageService.generateWhatsAppLink(updated)
        return ResponseEntity.ok(ProductResponse.from(updated, whatsappLink))
    }

    /**
     * Delete product
     * DELETE /api/products/{id}
     */
    @DeleteMapping("/{id}")
    suspend fun deleteProduct(@PathVariable id: Long): ResponseEntity<Void> {
        val deleted = productService.deleteProduct(id)

        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Track click (WhatsApp/Instagram)
     * POST /api/products/{id}/click
     */
    @PostMapping("/{id}/click")
    suspend fun trackClick(
        @PathVariable id: Long,
        @RequestParam type: String
    ): ResponseEntity<Void> {
        val product = productService.findById(id)
            ?: return ResponseEntity.notFound().build()

        when (type.lowercase()) {
            "whatsapp" -> analyticsService.trackWhatsappClick(product.seller)
            "instagram", "tiktok", "facebook" -> analyticsService.trackInstagramClick(product.seller)
            else -> analyticsService.trackClick(product.seller)
        }

        return ResponseEntity.ok().build()
    }
}
