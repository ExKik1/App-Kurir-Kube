package com.example.core.model

data class Courier(
    val id: String = "",
    val name: String = "Kurir Kube",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val statusText: String = "Siap Bertugas",
    val isActiveDuty: Boolean = false,
    val balance: Long = 0L,
    val pendingBalance: Long = 0L,
    val withdrawnBalance: Long = 0L,
    val bankAccountNumber: String = "",
    val bankName: String = "",
    val bankAccountHolder: String = "",
    val completedDeliveries: Int = 0,
    val totalTarget: Int = 0,
    val inTransitCount: Int = 0,
    val waitingCount: Int = 0,
    val vehicleType: String = "-",
    val vehiclePlateNumber: String = "-",
    val vehicleCapacity: String = "-",
    val vehicleStatus: String = "-",
    val isBiometricEnabled: Boolean = true
)
