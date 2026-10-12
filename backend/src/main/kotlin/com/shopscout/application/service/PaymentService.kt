package com.shopscout.application.service

import com.shopscout.domain.model.MpesaCallback
import com.shopscout.domain.model.Payment
import com.shopscout.domain.model.PaymentStatus
import com.shopscout.infrastructure.repository.PaymentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Payment Service
 *
 * Handles M-Pesa payment processing and tracking.
 * Manages payment lifecycle from initiation to completion/failure.
 */
@Service
class PaymentService(
    private val paymentRepository: PaymentRepository,
    private val featuredListingService: FeaturedListingService
) {
    /**
     * Initiate a new payment for featured listing
     */
    suspend fun initiatePayment(
        sellerUsername: String,
        phoneNumber: String,
        amount: BigDecimal = Payment.FEATURED_LISTING_PRICE.toBigDecimal()
    ): Payment {
        // Create payment record
        val payment = Payment.create(
            sellerUsername = sellerUsername,
            phoneNumber = phoneNumber,
            amount = amount
        )

        val saved = paymentRepository.save(payment)

        // TODO: Initiate M-Pesa STK Push
        // This would call M-Pesa API to send payment request to user's phone
        // For now, we just save the payment as pending

        return saved
    }

    /**
     * Update payment with M-Pesa checkout request ID
     */
    suspend fun updateCheckoutRequest(
        paymentId: Long,
        merchantRequestId: String,
        checkoutRequestId: String
    ): Payment? {
        val payment = paymentRepository.findById(paymentId) ?: return null
        val updated = payment.withCheckoutRequest(merchantRequestId, checkoutRequestId)
        return paymentRepository.save(updated)
    }

    /**
     * Handle M-Pesa callback
     */
    suspend fun handleCallback(callback: MpesaCallback): Payment? {
        val payment = paymentRepository.findByCheckoutRequestId(callback.checkoutRequestId)
            ?: return null

        val updated = if (callback.isSuccessful()) {
            // Payment successful
            val completed = payment.complete(
                transactionId = callback.mpesaReceiptNumber ?: callback.merchantRequestId,
                resultCode = callback.resultCode,
                resultDesc = callback.resultDesc
            )

            // Activate featured listing
            if (payment.sellerUsername != null) {
                featuredListingService.createFeaturedListing(
                    sellerUsername = payment.sellerUsername,
                    transactionId = callback.mpesaReceiptNumber ?: callback.merchantRequestId,
                    amount = payment.amount
                )
            }

            completed
        } else {
            // Payment failed
            payment.fail(
                resultCode = callback.resultCode,
                resultDesc = callback.resultDesc
            )
        }

        return paymentRepository.save(updated)
    }

    /**
     * Find payment by ID
     */
    suspend fun findById(id: Long): Payment? {
        return paymentRepository.findById(id)
    }

    /**
     * Find payment by transaction ID
     */
    suspend fun findByTransactionId(transactionId: String): Payment? {
        return paymentRepository.findByTransactionId(transactionId)
    }

    /**
     * Get payments by seller
     */
    fun getPaymentsBySeller(sellerUsername: String): Flow<Payment> {
        return paymentRepository.findBySeller(sellerUsername)
    }

    /**
     * Get pending payments
     */
    fun getPendingPayments(): Flow<Payment> {
        return paymentRepository.findPending()
    }

    /**
     * Get completed payments
     */
    fun getCompletedPayments(): Flow<Payment> {
        return paymentRepository.findCompleted()
    }

    /**
     * Get failed payments
     */
    fun getFailedPayments(): Flow<Payment> {
        return paymentRepository.findFailed()
    }

    /**
     * Get payments by status
     */
    fun getPaymentsByStatus(status: PaymentStatus): Flow<Payment> {
        return paymentRepository.findByStatus(status)
    }

    /**
     * Get payments within date range
     */
    fun getPaymentsByDateRange(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<Payment> {
        return paymentRepository.findByDateRange(startDate, endDate)
    }

    /**
     * Check payment status
     */
    suspend fun checkPaymentStatus(paymentId: Long): PaymentStatus? {
        return paymentRepository.findById(paymentId)?.status
    }

    /**
     * Cancel payment
     */
    suspend fun cancelPayment(paymentId: Long, reason: String): Payment? {
        val payment = paymentRepository.findById(paymentId) ?: return null

        if (!payment.isPending()) {
            throw IllegalStateException("Cannot cancel payment with status ${payment.status}")
        }

        val cancelled = payment.cancel(reason)
        return paymentRepository.save(cancelled)
    }

    /**
     * Process timed out payments
     * Should be called by scheduled task
     */
    suspend fun processTimedOutPayments(): List<Payment> {
        val timedOut = paymentRepository.findTimedOut().toList()

        return timedOut.map { payment ->
            val cancelled = payment.cancel("Payment timed out after 5 minutes")
            paymentRepository.save(cancelled)
        }
    }

    /**
     * Get total revenue
     */
    suspend fun getTotalRevenue(): BigDecimal {
        return paymentRepository.getTotalRevenue()
    }

    /**
     * Get revenue by seller
     */
    suspend fun getRevenueByTSeller(sellerUsername: String): BigDecimal {
        return paymentRepository.getRevenueByTotalRevenueBySeller(sellerUsername)
    }

    /**
     * Get payment statistics
     */
    suspend fun getPaymentStats(): PaymentStats {
        val totalPayments = paymentRepository.count()
        val completedCount = paymentRepository.countByStatus(PaymentStatus.COMPLETED)
        val pendingCount = paymentRepository.countByStatus(PaymentStatus.PENDING)
        val failedCount = paymentRepository.countByStatus(PaymentStatus.FAILED)
        val totalRevenue = paymentRepository.getTotalRevenue()

        return PaymentStats(
            totalPayments = totalPayments,
            completedPayments = completedCount,
            pendingPayments = pendingCount,
            failedPayments = failedCount,
            totalRevenue = totalRevenue,
            successRate = if (totalPayments > 0) {
                (completedCount.toDouble() / totalPayments.toDouble()) * 100
            } else 0.0
        )
    }

    /**
     * Clean up old failed payments
     */
    suspend fun cleanupOldFailedPayments(): Long {
        return paymentRepository.deleteOldFailedPayments()
    }
}

/**
 * Payment Statistics
 */
data class PaymentStats(
    val totalPayments: Long,
    val completedPayments: Long,
    val pendingPayments: Long,
    val failedPayments: Long,
    val totalRevenue: BigDecimal,
    val successRate: Double
)

/**
 * Payment Request DTO
 */
data class PaymentRequest(
    val sellerUsername: String,
    val phoneNumber: String,
    val amount: BigDecimal
)

/**
 * Payment Response DTO
 */
data class PaymentResponse(
    val paymentId: Long?,
    val status: PaymentStatus,
    val message: String,
    val checkoutRequestId: String? = null
)
