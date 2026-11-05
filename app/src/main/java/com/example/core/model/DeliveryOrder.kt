package com.example.core.model

data class ProofOfDelivery(
    val recipientName: String,
    val photoUri: String? = null,
    val confirmedQuantity: Int,
    val notes: String? = null,
    val submittedAt: String = "10:15 | 28 Okt 2026"
)

data class DeliveryOrder(
    val id: String,
    val customerName: String,
    val address: String,
    val subDistrict: String = "Kec. Purwokerto Barat, Kab. Banyumas",
    val phoneNumber: String = "+62 812 3456 7890",
    val itemDescription: String,
    val paymentStatus: String = "Belum dibayar",
    val priority: String = "Normal",
    val estimatedTimeMinutes: Int = 10,
    val distanceKm: Double = 2.3,
    val status: DeliveryStatus = DeliveryStatus.IN_TRANSIT,
    val assignedTime: String = "09:15 | 28 Okt 2026",
    val inTransitTime: String? = "09:45 | 28 Okt 2026",
    val deliveredTime: String? = null,
    val notes: String? = "Harap berhati-hati, simpan di kotak pendingin utama.",
    val proofOfDelivery: ProofOfDelivery? = null
) {
    val category: OrderStatusCategory get() = status.category
    val isAktif: Boolean get() = status.isAktif
    val isAntrean: Boolean get() = status.isAntrean
    val isRiwayat: Boolean get() = status.isRiwayat

    /**
     * Extracts numerical item count from itemDescription (e.g. '10 balok es' -> 10)
     */
    val defaultQuantity: Int
        get() = Regex("\\d+").find(itemDescription)?.value?.toIntOrNull() ?: 1
}
