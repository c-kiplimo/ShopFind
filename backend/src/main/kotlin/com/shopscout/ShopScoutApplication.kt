package com.shopscout

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories

/**
 * ShopScout Backend Application
 *
 * A social commerce search engine for Kenya - helping buyers discover
 * products from Instagram, TikTok, and Facebook sellers.
 *
 * Built with:
 * - Kotlin + Spring Boot WebFlux (Reactive)
 * - R2DBC for reactive database access
 * - PostgreSQL for data persistence
 * - Specification pattern for dynamic queries
 */
@SpringBootApplication
@EnableR2dbcRepositories
class ShopScoutApplication

fun main(args: Array<String>) {
    runApplication<ShopScoutApplication>(*args)
}
