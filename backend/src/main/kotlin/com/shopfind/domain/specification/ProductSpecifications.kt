package com.shopfind.domain.specification

import com.shopfind.domain.model.Platform
import com.shopfind.domain.model.Product
import org.springframework.data.relational.core.query.Criteria
import java.math.BigDecimal

/**
 * Product Specifications
 *
 * Type-safe, composable specifications for building dynamic Product queries.
 * Follows the Specification pattern for clean, testable query logic.
 */

/**
 * Filter by category
 */
class ProductCategorySpec(private val category: String) : Specification<Product> {
    override fun toCriteria(): Criteria = Criteria.where("category").`is`(category)
}

/**
 * Filter by subcategory
 */
class ProductSubcategorySpec(private val subcategory: String) : Specification<Product> {
    override fun toCriteria(): Criteria = Criteria.where("subcategory").`is`(subcategory)
}

/**
 * Filter by price range
 */
class ProductPriceRangeSpec(
    private val minPrice: BigDecimal? = null,
    private val maxPrice: BigDecimal? = null
) : Specification<Product> {
    override fun toCriteria(): Criteria {
        var criteria = Criteria.empty()

        if (minPrice != null) {
            criteria = criteria.and(Criteria.where("price").greaterThanOrEquals(minPrice))
        }

        if (maxPrice != null) {
            criteria = criteria.and(Criteria.where("price").lessThanOrEquals(maxPrice))
        }

        return criteria
    }
}

/**
 * Filter by seller
 */
class ProductSellerSpec(private val seller: String) : Specification<Product> {
    override fun toCriteria(): Criteria = Criteria.where("seller").`is`(seller)
}

/**
 * Filter by location
 */
class ProductLocationSpec(private val location: String) : Specification<Product> {
    override fun toCriteria(): Criteria = Criteria.where("location").`is`(location)
}

/**
 * Filter by area
 */
class ProductAreaSpec(private val area: String) : Specification<Product> {
    override fun toCriteria(): Criteria = Criteria.where("area").`is`(area)
}

/**
 * Filter by platform
 */
class ProductPlatformSpec(private val platform: Platform) : Specification<Product> {
    override fun toCriteria(): Criteria = Criteria.where("platform").`is`(platform.name)
}

/**
 * Filter active products only
 */
class ProductActiveSpec : Specification<Product> {
    override fun toCriteria(): Criteria = Criteria.where("is_active").`is`(true)
}

/**
 * Filter products with discount
 */
class ProductWithDiscountSpec : Specification<Product> {
    override fun toCriteria(): Criteria = Criteria.where("original_price").isNotNull
}

/**
 * Full-text search specification
 * Note: PostgreSQL full-text search using tsvector
 */
class ProductSearchSpec(private val searchQuery: String) : Specification<Product> {
    override fun toCriteria(): Criteria {
        // PostgreSQL full-text search query
        // Using to_tsquery for the search term
        return Criteria.where("search_vector")
            .`is`(org.springframework.data.relational.core.query.CriteriaDefinition::class.java)
            .let {
                // Custom criteria for full-text search
                // This will be translated to: WHERE search_vector @@ to_tsquery('english', ?)
                Criteria.where("search_vector @@ to_tsquery('english', ?)")
                    .`is`(searchQuery)
            }
    }
}

/**
 * Filter new products (within last 7 days)
 */
class ProductNewSpec : Specification<Product> {
    override fun toCriteria(): Criteria {
        // Products scraped within last 7 days
        return Criteria.where("scraped_date")
            .greaterThan(java.time.LocalDateTime.now().minusDays(7))
    }
}

/**
 * Filter products by multiple categories
 */
class ProductCategoriesInSpec(private val categories: List<String>) : Specification<Product> {
    override fun toCriteria(): Criteria = Criteria.where("category").`in`(categories)
}

/**
 * Filter products by currency
 */
class ProductCurrencySpec(private val currency: String) : Specification<Product> {
    override fun toCriteria(): Criteria = Criteria.where("currency").`is`(currency)
}

/**
 * Builder class for creating complex product queries
 */
class ProductSpecificationBuilder {
    private var specs = mutableListOf<Specification<Product>>()

    fun withCategory(category: String?) = apply {
        category?.let { specs.add(ProductCategorySpec(it)) }
    }

    fun withSubcategory(subcategory: String?) = apply {
        subcategory?.let { specs.add(ProductSubcategorySpec(it)) }
    }

    fun withPriceRange(minPrice: BigDecimal?, maxPrice: BigDecimal?) = apply {
        if (minPrice != null || maxPrice != null) {
            specs.add(ProductPriceRangeSpec(minPrice, maxPrice))
        }
    }

    fun withSeller(seller: String?) = apply {
        seller?.let { specs.add(ProductSellerSpec(it)) }
    }

    fun withLocation(location: String?) = apply {
        location?.let { specs.add(ProductLocationSpec(it)) }
    }

    fun withArea(area: String?) = apply {
        area?.let { specs.add(ProductAreaSpec(it)) }
    }

    fun withPlatform(platform: Platform?) = apply {
        platform?.let { specs.add(ProductPlatformSpec(it)) }
    }

    fun activeOnly() = apply {
        specs.add(ProductActiveSpec())
    }

    fun withDiscount() = apply {
        specs.add(ProductWithDiscountSpec())
    }

    fun withSearch(query: String?) = apply {
        query?.takeIf { it.isNotBlank() }?.let {
            specs.add(ProductSearchSpec(it))
        }
    }

    fun newOnly() = apply {
        specs.add(ProductNewSpec())
    }

    fun withCategories(categories: List<String>?) = apply {
        categories?.takeIf { it.isNotEmpty() }?.let {
            specs.add(ProductCategoriesInSpec(it))
        }
    }

    fun withCurrency(currency: String?) = apply {
        currency?.let { specs.add(ProductCurrencySpec(it)) }
    }

    fun build(): Specification<Product> {
        if (specs.isEmpty()) {
            // Return a specification that matches everything
            return object : Specification<Product> {
                override fun toCriteria(): Criteria = Criteria.empty()
            }
        }

        // Combine all specifications with AND
        return specs.reduce { acc, spec -> acc.and(spec) }
    }
}

/**
 * Helper function to create specification builder
 */
fun productSpec(block: ProductSpecificationBuilder.() -> Unit): Specification<Product> {
    return ProductSpecificationBuilder().apply(block).build()
}
