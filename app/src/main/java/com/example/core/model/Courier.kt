package com.example.core.model

data class Courier(
    val id: String = "CR-098",
    val name: String = "Maryono Supardi",
    val email: String = "maryonosupardi@kube.com",
    val phone: String = "+62 813 9876 5432",
    val address: String = "Jl. Karang Pucung No. 18, Kec. Purwokerto Selatan, Kab. Banyumas",
    val statusText: String = "Selamat Bertugas",
    val isActiveDuty: Boolean = true,
    val balance: Long = 850000L,
    val pendingBalance: Long = 145000L,
    val withdrawnBalance: Long = 2450000L,
    val bankAccountNumber: String = "8830192831",
    val bankName: String = "BCA",
    val bankAccountHolder: String = "MARYONO SUPARDI",
    val completedDeliveries: Int = 12,
    val totalTarget: Int = 20,
    val inTransitCount: Int = 5,
    val waitingCount: Int = 3,
    val vehicleType: String = "Truk Box Pendingin (Freezer)",
    val vehiclePlateNumber: String = "R 8921 KB",
    val vehicleCapacity: String = "Maks. 50 Balok Es (± 1.250 kg)",
    val vehicleStatus: String = "Prima & Layak Jalan",
    val isBiometricEnabled: Boolean = true
)
