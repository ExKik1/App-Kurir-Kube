package com.example.core.model

/**
 * Status of courier withdrawal request.
 */
enum class WithdrawalStatus(val label: String) {
    WAITING("Menunggu Admin"),
    PROCESSING("Konfirmasi Dana"),
    SUCCESS("Berhasil"),
    REJECTED("Ditolak")
}

/**
 * Represents a withdrawal record to the courier's bank account.
 */
data class WithdrawalHistoryItem(
    val id: String,
    val bankName: String = "",
    val accountNumberMasked: String = "",
    val recipientName: String = "",
    val amount: Long,
    val date: String,
    val status: WithdrawalStatus,
    val rejectionReason: String? = null,
    val transferProofUrl: String? = null,
    val processedAt: String? = null,
    val processedByName: String? = null
)

/**
 * Represents an item in the courier's commission & tip history.
 */
data class CommissionHistoryItem(
    val id: String,
    val orderId: String,
    val title: String, // e.g. "Komisi 8 balok es - Rp45.000" or "Tip Pelanggan - Rp20.000"
    val subtitle: String, // e.g. "Toko Es Segar · Hari Ini, 09:45"
    val amount: Long,
    val isTip: Boolean = false,
    val timestamp: String
)

/**
 * Breakdown of courier balances into 3 distinct categories:
 * - pendingBalance: Earnings pending verification or in transit (amber/yellow)
 * - availableBalance: Balance ready to withdraw (green)
 * - withdrawnBalance: Total balance already successfully withdrawn (blue)
 */
data class WalletBalance(
    val pendingBalance: Long = 0L,
    val availableBalance: Long = 0L,
    val withdrawnBalance: Long = 0L
)

/**
 * Masks a bank account number for security:
 * e.g. "8830192831" -> "**** **** 2831"
 */
fun maskAccountNumber(accountNumber: String): String {
    val clean = accountNumber.replace(" ", "").trim()
    return if (clean.length > 4) {
        "**** **** " + clean.takeLast(4)
    } else {
        "**** **** $clean"
    }
}
