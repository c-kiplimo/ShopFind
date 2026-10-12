package com.shopfind.presentation.controller

import com.shopfind.application.service.MessageService
import com.shopfind.application.service.SellerService
import com.shopfind.domain.model.Platform
import com.shopfind.domain.model.Seller
import com.shopfind.presentation.dto.SellerCreateRequest
import com.shopfind.presentation.dto.SellerResponse
import com.shopfind.presentation.dto.SellerUpdateRequest
import jakarta.validation.Valid
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Seller Controller
 *
 * REST API endpoints for seller operations.
 */
@RestController
@RequestMapping("/api/sellers")
@CrossOrigin(origins = ["http://localhost:3000", "https://shopfind.ke"])
class SellerController(
    private val sellerService: SellerService,
    private val messageService: MessageService
) {
    /**
     * Get all sellers
     * GET /api/sellers
     */
    @GetMapping
    suspend fun getAllSellers(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<List<SellerResponse>> {
        val sellers = sellerService.getAllSellers(page, size)
            .map { seller -> toSellerResponse(seller) }
            .toList()

        return ResponseEntity.ok(sellers)
    }

    /**
     * Get seller by username
     * GET /api/sellers/{username}
     */
    @GetMapping("/{username}")
    suspend fun getSellerByUsername(@PathVariable username: String): ResponseEntity<SellerResponse> {
        val seller = sellerService.findByUsername(username)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(toSellerResponse(seller))
    }

    /**
     * Get featured sellers
     * GET /api/sellers/featured
     */
    @GetMapping("/featured")
    suspend fun getFeaturedSellers(): ResponseEntity<List<SellerResponse>> {
        val sellers = sellerService.getFeaturedSellers()
            .map { seller -> toSellerResponse(seller) }
            .toList()

        return ResponseEntity.ok(sellers)
    }

    /**
     * Get verified sellers
     * GET /api/sellers/verified
     */
    @GetMapping("/verified")
    suspend fun getVerifiedSellers(): ResponseEntity<List<SellerResponse>> {
        val sellers = sellerService.getVerifiedSellers()
            .map { seller -> toSellerResponse(seller) }
            .toList()

        return ResponseEntity.ok(sellers)
    }

    /**
     * Get top sellers
     * GET /api/sellers/top
     */
    @GetMapping("/top")
    suspend fun getTopSellers(
        @RequestParam(defaultValue = "10") limit: Int
    ): ResponseEntity<List<SellerResponse>> {
        val sellers = sellerService.getTopSellers(limit)
            .map { seller -> toSellerResponse(seller) }
            .toList()

        return ResponseEntity.ok(sellers)
    }

    /**
     * Get sellers by platform
     * GET /api/sellers/platform/{platform}
     */
    @GetMapping("/platform/{platform}")
    suspend fun getSellersByPlatform(@PathVariable platform: String): ResponseEntity<List<SellerResponse>> {
        val platformEnum = try {
            Platform.valueOf(platform.uppercase())
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.badRequest().build()
        }

        val sellers = sellerService.getSellersByPlatform(platformEnum)
            .map { seller -> toSellerResponse(seller) }
            .toList()

        return ResponseEntity.ok(sellers)
    }

    /**
     * Create seller
     * POST /api/sellers
     */
    @PostMapping
    suspend fun createSeller(
        @Valid @RequestBody request: SellerCreateRequest
    ): ResponseEntity<SellerResponse> {
        val seller = sellerService.createSeller(
            username = request.username,
            platform = request.platform,
            phone = request.phone,
            whatsapp = request.whatsapp,
            email = request.email,
            bio = request.bio,
            location = request.location,
            verified = request.verified
        )

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(toSellerResponse(seller))
    }

    /**
     * Update seller
     * PATCH /api/sellers/{username}
     */
    @PatchMapping("/{username}")
    suspend fun updateSeller(
        @PathVariable username: String,
        @Valid @RequestBody request: SellerUpdateRequest
    ): ResponseEntity<SellerResponse> {
        val existing = sellerService.findByUsername(username)
            ?: return ResponseEntity.notFound().build()

        val updated = existing.copy(
            phone = request.phone ?: existing.phone,
            whatsapp = request.whatsapp ?: existing.whatsapp,
            email = request.email ?: existing.email,
            website = request.website ?: existing.website,
            bio = request.bio ?: existing.bio,
            profilePicUrl = request.profilePicUrl ?: existing.profilePicUrl,
            location = request.location ?: existing.location,
            area = request.area ?: existing.area
        )

        val saved = sellerService.saveSeller(updated)
        return ResponseEntity.ok(toSellerResponse(saved))
    }

    /**
     * Delete seller
     * DELETE /api/sellers/{username}
     */
    @DeleteMapping("/{username}")
    suspend fun deleteSeller(@PathVariable username: String): ResponseEntity<Void> {
        val deleted = sellerService.deleteSeller(username)

        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Helper to convert Seller to SellerResponse
     */
    private fun toSellerResponse(seller: Seller): SellerResponse {
        val whatsappLink = messageService.generateSellerWhatsAppLink(seller)
        val platformLink = messageService.generatePlatformLink(seller)
        return SellerResponse.from(seller, whatsappLink, platformLink)
    }
}
