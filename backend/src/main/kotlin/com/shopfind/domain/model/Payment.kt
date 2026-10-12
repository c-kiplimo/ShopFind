package com.shopfind.domain.model

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Payment Domain Entity
 *
 * Represents a payment transaction via M-Pesa STK Push.
 * Tracks payment lifecycle from initiation to completion/failure.
 * Follows Domain-Driven Design principles.
 */
@Table("payments")
data class Payment(
    @Id
    val id: Long? = null,

    // Seller
    @Column("seller_username")
    val sellerUsername: String? = null,

    // M-Pesa Details
    @Column("phone_number")
    val phoneNumber: String,
    val amount: BigDecimal,
    @Column("transaction_id")
    val transactionId: String? = null,
    @Column("merchant_request_id")
    val merchantRequestId: String? = null,
    @Column("checkout_request_id")
    val checkoutRequestId: String? = null,

    // Status
    val status: PaymentStatus = PaymentStatus.PENDING,
    @Column("result_code")
    val resultCode: String? = null,
    @Column("result_desc")
    val resultDesc: String? = null,

    // Metadata
    @Column("created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),
    @Column("completed_at")
    val completedAt: LocalDateTime? = null
) {
    /**
     * Check if payment is completed successfully
     */
    fun isCompleted(): Boolean = status == PaymentStatus.COMPLETED

    /**
     * Check if payment is pending
     */
    fun isPending(): Boolean = status == PaymentStatus.PENDING

    /**
     * Check if payment failed
     */
    fun isFailed(): Boolean = status == PaymentStatus.FAILED

    /**
     * Check if payment was cancelled
     */
    fun isCancelled(): Boolean = status == PaymentStatus.CANCELLED

    /**
     * Mark payment as completed
     */
    fun complete(transactionId: String, resultCode: String, resultDesc: String): Payment {
        return copy(
            status = PaymentStatus.COMPLETED,
            transactionId = transactionId,
            resultCode = resultCode,
            resultDesc = resultDesc,
            completedAt = LocalDateTime.now()
        )
    }

    /**
     * Mark payment as failed
     */
    fun fail(resultCode: String, resultDesc: String): Payment {
        return copy(
            status = PaymentStatus.FAILED,
            resultCode = resultCode,
            resultDesc = resultDesc,
            completedAt = LocalDateTime.now()
        )
    }

    /**
     * Mark payment as cancelled
     */
    fun cancel(reason: String): Payment {
        return copy(
            status = PaymentStatus.CANCELLED,
            resultDesc = reason,
            completedAt = LocalDateTime.now()
        )
    }

    /**
     * Update with M-Pesa checkout request ID
     */
    fun withCheckoutRequest(
        merchantRequestId: String,
        checkoutRequestId: String
    ): Payment {
        return copy(
            merchantRequestId = merchantRequestId,
            checkoutRequestId = checkoutRequestId
        )
    }

    /**
     * Calculate payment age in minutes
     */
    fun ageInMinutes(): Long {
        return java.time.Duration.between(createdAt, LocalDateTime.now()).toMinutes()
    }

    /**
     * Check if payment has timed out (older than 5 minutes and still pending)
     */
    fun hasTimedOut(): Boolean {
        return isPending() && ageInMinutes() > 5
    }

    /**
     * Validate payment invariants
     */
    init {
        require(phoneNumber.isNotBlank()) { "Phone number cannot be blank" }
        require(phoneNumber.matches(Regex("^254[0-9]{9}$"))) {
            "Phone number must be in format 254XXXXXXXXX"
        }
        require(amount > BigDecimal.ZERO) { "Amount must be positive" }
        if (status == PaymentStatus.COMPLETED) {
            require(transactionId != null) { "Completed payments must have a transaction ID" }
            require(completedAt != null) { "Completed payments must have a completion date" }
        }
        if (status == PaymentStatus.FAILED || status == PaymentStatus.CANCELLED) {
            require(completedAt != null) { "Failed/cancelled payments must have a completion date" }
        }
    }

    companion object {
        /**
         * Factory method to create a new payment
         */
        fun create(
            sellerUsername: String?,
            phoneNumber: String,
            amount: BigDecimal
        ): Payment {
            return Payment(
                sellerUsername = sellerUsername,
                phoneNumber = phoneNumber,
                amount = amount
            )
        }

        /**
         * Featured listing price in KES
         */
        const val FEATURED_LISTING_PRICE = 500.0
    }
}

/**
 * Payment Status Enum
 */
enum class PaymentStatus {
    PENDING,
    COMPLETED,
    FAILED,
    CANCELLED;

    companion object {
        fun fromString(value: String): PaymentStatus =
            entries.find { it.name.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Invalid payment status: $value")
    }
}

/**
 * M-Pesa Callback Response Value Object
 */
data class MpesaCallback(
    val merchantRequestId: String,
    val checkoutRequestId: String,
    val resultCode: String,
    val resultDesc: String,
    val amount: BigDecimal? = null,
    val mpesaReceiptNumber: String? = null,
    val transactionDate: String? = null,
    val phoneNumber: String? = null
) {
    fun isSuccessful(): Boolean = resultCode == "0"
}
