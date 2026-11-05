package com.example.core.model

enum class OrderStatusCategory {
    AKTIF,
    ANTREAN,
    RIWAYAT
}

enum class DeliveryStatus(val displayName: String, val badgeColorHex: Long) {
    NEW("Tugas Baru", 0xFF0288D1),
    IN_TRANSIT("Dalam Perjalanan", 0xFF00B4DB),
    ARRIVED("Tiba di Lokasi", 0xFF00C853),
    PENDING_VERIFICATION("Menunggu Verifikasi Admin", 0xFFF57C00),
    DELIVERED("Terkirim", 0xFF2E7D32),
    FAILED("Gagal / Retur", 0xFFE53935);

    val category: OrderStatusCategory
        get() = when (this) {
            NEW -> OrderStatusCategory.ANTREAN
            IN_TRANSIT, ARRIVED, PENDING_VERIFICATION -> OrderStatusCategory.AKTIF
            DELIVERED, FAILED -> OrderStatusCategory.RIWAYAT
        }

    val isAktif: Boolean get() = category == OrderStatusCategory.AKTIF
    val isAntrean: Boolean get() = category == OrderStatusCategory.ANTREAN
    val isRiwayat: Boolean get() = category == OrderStatusCategory.RIWAYAT
}
