package com.shopscout.application.service

import com.shopscout.domain.model.Product
import com.shopscout.domain.model.Seller
import org.springframework.stereotype.Service
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Message Service
 *
 * Generates WhatsApp messages and contact links for sellers.
 * Helps buyers connect with sellers efficiently.
 */
@Service
class MessageService {
    /**
     * Generate WhatsApp message for product inquiry
     */
    fun generateProductInquiry(product: Product): String {
        return buildString {
            append("Hi! I'm interested in your product:\n\n")
            append("*${product.productName}*\n")
            if (product.caption != null) {
                append("${product.caption}\n\n")
            }
            append("Price: KES ${product.price}\n")
            append("\nIs this still available?")
        }
    }

    /**
     * Generate WhatsApp link for product
     */
    fun generateWhatsAppLink(product: Product): String? {
        val phone = product.whatsapp ?: product.phone ?: return null
        val message = generateProductInquiry(product)
        val encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8)

        return "https://wa.me/$phone?text=$encodedMessage"
    }

    /**
     * Generate WhatsApp link for seller
     */
    fun generateSellerWhatsAppLink(seller: Seller, customMessage: String? = null): String? {
        val phone = seller.whatsapp ?: seller.phone ?: return null

        val message = customMessage ?: buildString {
            append("Hi ${seller.username}! ")
            append("I found your products on ShopScout. ")
            append("I'd like to know more about what you offer.")
        }

        val encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8)
        return "https://wa.me/$phone?text=$encodedMessage"
    }

    /**
     * Generate Instagram profile link
     */
    fun generateInstagramLink(username: String): String {
        return "https://instagram.com/$username"
    }

    /**
     * Generate TikTok profile link
     */
    fun generateTikTokLink(username: String): String {
        return "https://tiktok.com/@$username"
    }

    /**
     * Generate Facebook profile link
     */
    fun generateFacebookLink(username: String): String {
        return "https://facebook.com/$username"
    }

    /**
     * Generate platform link based on seller's platform
     */
    fun generatePlatformLink(seller: Seller): String {
        return when (seller.platform) {
            com.shopscout.domain.model.Platform.INSTAGRAM -> generateInstagramLink(seller.username)
            com.shopscout.domain.model.Platform.TIKTOK -> generateTikTokLink(seller.username)
            com.shopscout.domain.model.Platform.FACEBOOK -> generateFacebookLink(seller.username)
        }
    }

    /**
     * Format phone number for display
     */
    fun formatPhoneNumber(phone: String): String {
        // Convert 254XXXXXXXXX to +254 XXX XXX XXX
        return if (phone.startsWith("254") && phone.length == 12) {
            "+254 ${phone.substring(3, 6)} ${phone.substring(6, 9)} ${phone.substring(9)}"
        } else {
            phone
        }
    }

    /**
     * Validate Kenyan phone number
     */
    fun validateKenyanPhone(phone: String): Boolean {
        return phone.matches(Regex("^254[0-9]{9}$"))
    }

    /**
     * Normalize phone number to 254XXXXXXXXX format
     */
    fun normalizePhoneNumber(phone: String): String? {
        val cleaned = phone.replace(Regex("[^0-9]"), "")

        return when {
            // Already in 254XXXXXXXXX format
            cleaned.startsWith("254") && cleaned.length == 12 -> cleaned

            // 0XXXXXXXXX format (Kenyan)
            cleaned.startsWith("0") && cleaned.length == 10 -> "254${cleaned.substring(1)}"

            // 7XXXXXXXX or 1XXXXXXXX format (Kenyan mobile)
            cleaned.length == 9 && cleaned[0] in "71" -> "254$cleaned"

            else -> null
        }
    }

    /**
     * Generate contact card for seller
     */
    fun generateContactCard(seller: Seller): ContactCard {
        return ContactCard(
            sellerUsername = seller.username,
            platform = seller.platform.name,
            platformLink = generatePlatformLink(seller),
            phone = seller.phone?.let { formatPhoneNumber(it) },
            whatsapp = seller.whatsapp,
            whatsappLink = seller.whatsapp?.let { "https://wa.me/$it" },
            email = seller.email,
            location = seller.location,
            hasContact = seller.hasContact()
        )
    }

    /**
     * Generate share message for product
     */
    fun generateShareMessage(product: Product): String {
        return buildString {
            append("Check out this product on ShopScout!\n\n")
            append("${product.productName}\n")
            append("Price: KES ${product.price}\n")
            if (product.originalPrice != null) {
                val discount = product.discountPercentage()
                append("Was: KES ${product.originalPrice} (${discount}% OFF!)\n")
            }
            append("\nSeller: ${product.seller}\n")
            append("Location: ${product.location}\n")
            // In production, add actual product URL
            // append("\nView on ShopScout: https://shopscout.ke/products/${product.id}")
        }
    }
}

/**
 * Contact Card DTO
 */
data class ContactCard(
    val sellerUsername: String,
    val platform: String,
    val platformLink: String,
    val phone: String?,
    val whatsapp: String?,
    val whatsappLink: String?,
    val email: String?,
    val location: String?,
    val hasContact: Boolean
)
