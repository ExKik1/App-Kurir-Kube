package com.example.core.model

data class RouteStep(
    val instruction: String,
    val distanceMeters: Int,
    val durationSeconds: Int,
    val maneuver: String? = null
)

enum class TrafficLevel(val displayName: String, val colorHex: Long) {
    SMOOTH("Lancar", 0xFF00C853),
    NORMAL("Normal", 0xFF0288D1),
    MODERATE("Padat Merayap", 0xFFFFA000),
    HEAVY("Macet", 0xFFD32F2F)
}

data class RouteEta(
    val durationMinutes: Int = 15,
    val durationSeconds: Int = 900,
    val distanceKm: Double = 0.0,
    val trafficCondition: String = "Lalu Lintas Lancar",
    val trafficLevel: TrafficLevel = TrafficLevel.SMOOTH,
    val trafficColorHex: Long = 0xFF00C853,
    val formattedEta: String = "$durationMinutes menit lagi",
    val nextInstruction: String = "Ikuti rute menuju lokasi pelanggan",
    val courierSpeedKmh: Int = 28,
    val originLat: Double = -7.4285,
    val originLng: Double = 109.2215,
    val currentLat: Double = -7.4243,
    val currentLng: Double = 109.2302,
    val destLat: Double? = null,
    val destLng: Double? = null,
    val destinationName: String = "Lokasi Pelanggan",
    val destinationAddress: String = "Menunggu tujuan pengiriman",
    val progressFraction: Float = 0.35f,
    val steps: List<RouteStep> = emptyList(),
    val isRealTimeLive: Boolean = true,
    val lastUpdatedText: String = "Estimasi Rute Aktif"
)
