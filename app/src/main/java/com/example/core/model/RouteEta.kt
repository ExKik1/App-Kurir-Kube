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
    val durationMinutes: Int,
    val durationSeconds: Int = 0,
    val distanceKm: Double,
    val trafficCondition: String = "Lalu Lintas Lancar",
    val trafficLevel: TrafficLevel = TrafficLevel.SMOOTH,
    val trafficColorHex: Long = 0xFF00C853,
    val formattedEta: String = "$durationMinutes menit lagi",
    val nextInstruction: String = "200m belok kanan ke Jl. Melati",
    val courierSpeedKmh: Int = 28,
    val originLat: Double = -7.4285,
    val originLng: Double = 109.2215,
    val currentLat: Double = -7.4243,
    val currentLng: Double = 109.2302,
    val destLat: Double = -7.4190,
    val destLng: Double = 109.2410,
    val destinationName: String = "Toko Es Segar",
    val destinationAddress: String = "Jl. Melati No. 12, Purwokerto",
    val progressFraction: Float = 0.45f,
    val steps: List<RouteStep> = listOf(
        RouteStep("Mulai dari Hub Kube Pabrik Es", 500, 60, "depart"),
        RouteStep("Belok kanan ke Jl. Jend. Soedirman", 1200, 240, "turn-right"),
        RouteStep("200m belok kanan ke Jl. Melati", 400, 90, "turn-right"),
        RouteStep("Tiba di Toko Es Segar di sebelah kiri", 200, 50, "arrive")
    ),
    val isRealTimeLive: Boolean = true,
    val lastUpdatedText: String = "Baru saja diperbarui"
)
