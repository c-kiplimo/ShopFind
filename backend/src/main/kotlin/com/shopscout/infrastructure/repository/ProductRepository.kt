package com.shopscout.infrastructure.repository

import com.shopscout.domain.model.Product
import com.shopscout.domain.specification.Specification
import com.shopscout.domain.specification.countWithSpec
import com.shopscout.domain.specification.selectWithSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.relational.core.query.Criteria
import org.springframework.data.relational.core.query.Query
import org.springframework.data.relational.core.query.Update
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Product Repository
 *
 * Repository for Product entity using R2dbcEntityTemplate (not Spring Data repositories).
 * Implements Specification pattern for dynamic query building.
 * All operations are reactive using Kotlin Coroutines.
 */
@Repository
class ProductRepository(
    private val template: R2dbcEntityTemplate
) {
    /**
     * Find product by ID
     */
    suspend fun findById(id: Long): Product? {
        return template.selectOne(
            Query.query(Criteria.where("id").`is`(id)),
            Product::class.java
        ).awaitFirstOrNull()
    }

    /**
     * Find all products
     */
    fun findAll(): Flow<Product> {
        return template.select(Product::class.java)
            .all()
            .asFlow()
    }

    /**
     * Find products with specification
     */
    fun findAll(spec: Specification<Product>): Flow<Product> {
        return template.selectWithSpec(spec, Product::class.java)
            .all()
            .asFlow()
    }

    /**
     * Find products with specification and pagination
     */
    fun findAll(
        spec: Specification<Product>,
        page: Int,
        size: Int,
        sort: Sort = Sort.by(Sort.Direction.DESC, "scraped_date")
    ): Flow<Product> {
        val query = spec.toQuery()
            .with(PageRequest.of(page, size, sort))

        return template.select(query, Product::class.java)
            .asFlow()
    }

    /**
     * Count products matching specification
     */
    suspend fun count(spec: Specification<Product>): Long {
        return template.countWithSpec(spec, Product::class.java)
            .awaitSingle()
    }

    /**
     * Count all products
     */
    suspend fun count(): Long {
        return template.count(Query.empty(), Product::class.java)
            .awaitSingle()
    }

    /**
     * Save product (insert or update)
     */
    suspend fun save(product: Product): Product {
        return if (product.id == null) {
            template.insert(product).awaitSingle()
        } else {
            template.update(product).awaitSingle()
        }
    }

    /**
     * Save all products
     */
    fun saveAll(products: Iterable<Product>): Flow<Product> {
        return Flux.fromIterable(products)
            .flatMap { product ->
                if (product.id == null) {
                    template.insert(product)
                } else {
                    template.update(product)
                }
            }
            .asFlow()
    }

    /**
     * Delete product by ID
     */
    suspend fun deleteById(id: Long): Boolean {
        val result = template.delete(
            Query.query(Criteria.where("id").`is`(id)),
            Product::class.java
        ).awaitSingle()
        return result > 0
    }

    /**
     * Delete product
     */
    suspend fun delete(product: Product): Boolean {
        return product.id?.let { deleteById(it) } ?: false
    }

    /**
     * Check if product exists by ID
     */
    suspend fun existsById(id: Long): Boolean {
        return template.exists(
            Query.query(Criteria.where("id").`is`(id)),
            Product::class.java
        ).awaitSingle()
    }

    /**
     * Find products by seller username
     */
    fun findBySeller(seller: String): Flow<Product> {
        return template.select(
            Query.query(Criteria.where("seller").`is`(seller)),
            Product::class.java
        ).asFlow()
    }

    /**
     * Find products by category
     */
    fun findByCategory(category: String): Flow<Product> {
        return template.select(
            Query.query(Criteria.where("category").`is`(category)),
            Product::class.java
        ).asFlow()
    }

    /**
     * Find active products only
     */
    fun findActive(): Flow<Product> {
        return template.select(
            Query.query(Criteria.where("is_active").`is`(true)),
            Product::class.java
        ).asFlow()
    }

    /**
     * Update product active status
     */
    suspend fun updateActiveStatus(id: Long, isActive: Boolean): Boolean {
        val result = template.update(
            Query.query(Criteria.where("id").`is`(id)),
            Update.update("is_active", isActive)
                .set("last_updated", java.time.LocalDateTime.now()),
            Product::class.java
        ).awaitSingle()
        return result > 0
    }

    /**
     * Find products by price range
     */
    fun findByPriceRange(
        minPrice: java.math.BigDecimal,
        maxPrice: java.math.BigDecimal
    ): Flow<Product> {
        return template.select(
            Query.query(
                Criteria.where("price").greaterThanOrEquals(minPrice)
                    .and("price").lessThanOrEquals(maxPrice)
            ),
            Product::class.java
        ).asFlow()
    }

    /**
     * Find products with discount
     */
    fun findWithDiscount(): Flow<Product> {
        return template.select(
            Query.query(Criteria.where("original_price").isNotNull),
            Product::class.java
        ).asFlow()
    }

    /**
     * Find new products (within last 7 days)
     */
    fun findNew(): Flow<Product> {
        val sevenDaysAgo = java.time.LocalDateTime.now().minusDays(7)
        return template.select(
            Query.query(Criteria.where("scraped_date").greaterThan(sevenDaysAgo)),
            Product::class.java
        ).asFlow()
    }

    /**
     * Full-text search products
     * Uses PostgreSQL's full-text search capabilities
     */
    fun search(query: String): Flow<Product> {
        // Note: This uses native SQL for full-text search
        // R2dbcEntityTemplate doesn't directly support custom operators like @@
        // We'll use a custom query approach
        val sql = """
            SELECT * FROM products
            WHERE search_vector @@ to_tsquery('english', :query)
            AND is_active = true
            ORDER BY ts_rank(search_vector, to_tsquery('english', :query)) DESC
        """.trimIndent()

        return template.databaseClient
            .sql(sql)
            .bind("query", query)
            .map { row, _ ->
                Product(
                    id = row.get("id", java.lang.Long::class.java)?.toLong(),
                    seller = row.get("seller", String::class.java) ?: "",
                    productName = row.get("product_name", String::class.java) ?: "",
                    caption = row.get("caption", String::class.java),
                    price = row.get("price", java.math.BigDecimal::class.java) ?: java.math.BigDecimal.ZERO,
                    originalPrice = row.get("original_price", java.math.BigDecimal::class.java),
                    currency = row.get("currency", String::class.java) ?: "KES",
                    category = row.get("category", String::class.java) ?: "",
                    subcategory = row.get("subcategory", String::class.java),
                    imageUrl = row.get("image_url", String::class.java) ?: "",
                    postUrl = row.get("post_url", String::class.java) ?: "",
                    phone = row.get("phone", String::class.java),
                    whatsapp = row.get("whatsapp", String::class.java),
                    email = row.get("email", String::class.java),
                    location = row.get("location", String::class.java) ?: "Nairobi",
                    area = row.get("area", String::class.java),
                    platform = com.shopscout.domain.model.Platform.valueOf(
                        row.get("platform", String::class.java)?.uppercase() ?: "INSTAGRAM"
                    ),
                    scrapedDate = row.get("scraped_date", java.time.LocalDateTime::class.java)
                        ?: java.time.LocalDateTime.now(),
                    lastUpdated = row.get("last_updated", java.time.LocalDateTime::class.java)
                        ?: java.time.LocalDateTime.now(),
                    isActive = row.get("is_active", java.lang.Boolean::class.java) ?: true
                )
            }
            .all()
            .asFlow()
    }

    /**
     * Get distinct categories
     */
    suspend fun getDistinctCategories(): List<String> {
        val sql = "SELECT DISTINCT category FROM products WHERE is_active = true ORDER BY category"
        return template.databaseClient
            .sql(sql)
            .map { row, _ -> row.get("category", String::class.java) ?: "" }
            .all()
            .collectList()
            .awaitSingle()
    }

    /**
     * Get distinct locations
     */
    suspend fun getDistinctLocations(): List<String> {
        val sql = "SELECT DISTINCT location FROM products WHERE is_active = true ORDER BY location"
        return template.databaseClient
            .sql(sql)
            .map { row, _ -> row.get("location", String::class.java) ?: "" }
            .all()
            .collectList()
            .awaitSingle()
    }

    /**
     * Count products by seller
     */
    suspend fun countBySeller(seller: String): Long {
        return template.count(
            Query.query(Criteria.where("seller").`is`(seller)),
            Product::class.java
        ).awaitSingle()
    }

    /**
     * Get average price by seller
     */
    suspend fun getAveragePriceBySeller(seller: String): java.math.BigDecimal? {
        val sql = "SELECT AVG(price) as avg_price FROM products WHERE seller = :seller"
        return template.databaseClient
            .sql(sql)
            .bind("seller", seller)
            .map { row, _ -> row.get("avg_price", java.math.BigDecimal::class.java) }
            .awaitFirstOrNull()
    }
}
