package com.shopfind.domain.specification

import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.relational.core.query.Criteria
import org.springframework.data.relational.core.query.Query

/**
 * Specification Pattern Interface
 *
 * Enables building dynamic database queries in a type-safe, composable way.
 * Used with R2dbcEntityTemplate for reactive database access.
 */
interface Specification<T> {
    /**
     * Convert specification to R2DBC Criteria
     */
    fun toCriteria(): Criteria

    /**
     * Combine specifications with AND logic
     */
    fun and(other: Specification<T>): Specification<T> {
        return AndSpecification(this, other)
    }

    /**
     * Combine specifications with OR logic
     */
    fun or(other: Specification<T>): Specification<T> {
        return OrSpecification(this, other)
    }

    /**
     * Negate this specification
     */
    fun not(): Specification<T> {
        return NotSpecification(this)
    }

    /**
     * Convert specification to R2DBC Query
     */
    fun toQuery(): Query {
        return Query.query(toCriteria())
    }
}

/**
 * AND Specification - Combines two specifications with AND logic
 */
class AndSpecification<T>(
    private val left: Specification<T>,
    private val right: Specification<T>
) : Specification<T> {
    override fun toCriteria(): Criteria {
        return left.toCriteria().and(right.toCriteria())
    }
}

/**
 * OR Specification - Combines two specifications with OR logic
 */
class OrSpecification<T>(
    private val left: Specification<T>,
    private val right: Specification<T>
) : Specification<T> {
    override fun toCriteria(): Criteria {
        return left.toCriteria().or(right.toCriteria())
    }
}

/**
 * NOT Specification - Negates a specification
 */
class NotSpecification<T>(
    private val spec: Specification<T>
) : Specification<T> {
    override fun toCriteria(): Criteria {
        return spec.toCriteria().not()
    }
}

/**
 * Extension function to build queries with specifications
 */
fun <T> R2dbcEntityTemplate.selectWithSpec(
    spec: Specification<T>,
    entityClass: Class<T>
) = select(spec.toQuery(), entityClass)

/**
 * Extension function to count with specifications
 */
fun <T> R2dbcEntityTemplate.countWithSpec(
    spec: Specification<T>,
    entityClass: Class<T>
) = count(spec.toQuery(), entityClass)

/**
 * Extension function to delete with specifications
 */
fun <T> R2dbcEntityTemplate.deleteWithSpec(
    spec: Specification<T>,
    entityClass: Class<T>
) = delete(spec.toQuery(), entityClass)
