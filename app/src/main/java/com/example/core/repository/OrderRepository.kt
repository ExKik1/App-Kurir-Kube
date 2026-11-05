package com.example.core.repository

import com.example.core.model.Courier
import com.example.core.model.DeliveryOrder
import com.example.core.model.DeliveryStatus
import com.example.core.model.CommissionHistoryItem
import com.example.core.model.WalletBalance
import com.example.core.model.WithdrawalHistoryItem
import com.example.core.model.WithdrawalStatus
import com.example.core.model.maskAccountNumber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class OrderRepository {

    // 20 Daily Orders for "Hari Ini":
    // 12 DELIVERED + 5 IN_TRANSIT + 3 NEW + 0 FAILED = 20 Total
    private val initialOrders = listOf(
        // === 5 Sedang Diantar (In Transit / Arrived) ===
        DeliveryOrder(
            id = "ESB-00128",
            customerName = "Toko Es Segar",
            address = "Jl. Melati No. 12",
            subDistrict = "Kec. Purwokerto Barat, Kab. Banyumas",
            phoneNumber = "+62 812 3456 7890",
            itemDescription = "10 balok es",
            paymentStatus = "Belum dibayar",
            priority = "Normal",
            estimatedTimeMinutes = 10,
            distanceKm = 2.3,
            status = DeliveryStatus.IN_TRANSIT,
            assignedTime = "09:15 | 28 Okt 2026",
            inTransitTime = "09:45 | 28 Okt 2026"
        ),
        DeliveryOrder(
            id = "ESB-00127",
            customerName = "Depot Es Buah Segar",
            address = "Jl. Gatot Subroto No. 3",
            subDistrict = "Kec. Purwokerto Barat",
            phoneNumber = "+62 815 6677 8899",
            itemDescription = "8 balok es",
            paymentStatus = "Lunas",
            priority = "Normal",
            estimatedTimeMinutes = 15,
            distanceKm = 3.1,
            status = DeliveryStatus.IN_TRANSIT,
            assignedTime = "08:45 | 28 Okt 2026",
            inTransitTime = "09:10 | 28 Okt 2026"
        ),
        DeliveryOrder(
            id = "ESB-00132",
            customerName = "Es Kelapa Muda Berkah",
            address = "Jl. Overste Isdiman No. 14",
            subDistrict = "Kec. Purwokerto Timur",
            phoneNumber = "+62 813 4455 6677",
            itemDescription = "5 balok es",
            paymentStatus = "Lunas",
            priority = "Normal",
            estimatedTimeMinutes = 20,
            distanceKm = 3.8,
            status = DeliveryStatus.IN_TRANSIT,
            assignedTime = "09:20 | 28 Okt 2026",
            inTransitTime = "09:50 | 28 Okt 2026"
        ),
        DeliveryOrder(
            id = "ESB-00133",
            customerName = "Rumah Makan Padang Minang",
            address = "Jl. HR Bunyamin No. 22",
            subDistrict = "Kec. Purwokerto Utara",
            phoneNumber = "+62 856 7788 9900",
            itemDescription = "12 balok es",
            paymentStatus = "Belum dibayar",
            priority = "Tinggi",
            estimatedTimeMinutes = 30,
            distanceKm = 5.2,
            status = DeliveryStatus.IN_TRANSIT,
            assignedTime = "09:25 | 28 Okt 2026",
            inTransitTime = "09:55 | 28 Okt 2026"
        ),
        DeliveryOrder(
            id = "ESB-00134",
            customerName = "Kedai Jus Segar Murni",
            address = "Jl. Prof. Dr. Suharso No. 9",
            subDistrict = "Kec. Purwokerto Timur",
            phoneNumber = "+62 821 3344 5566",
            itemDescription = "4 kantong es tube kristal",
            paymentStatus = "Lunas",
            priority = "Normal",
            estimatedTimeMinutes = 35,
            distanceKm = 6.0,
            status = DeliveryStatus.IN_TRANSIT,
            assignedTime = "09:30 | 28 Okt 2026",
            inTransitTime = "10:00 | 28 Okt 2026"
        ),

        // === 3 Tugas Baru (Menunggu Konfirmasi / Ambil) ===
        DeliveryOrder(
            id = "ESB-00129",
            customerName = "Warung Segar",
            address = "Jl. Diponegoro No. 45",
            subDistrict = "Kec. Sokaraja",
            phoneNumber = "+62 813 1122 3344",
            itemDescription = "4 kantong es kristal",
            paymentStatus = "Lunas",
            priority = "Normal",
            estimatedTimeMinutes = 25,
            distanceKm = 4.8,
            status = DeliveryStatus.NEW,
            assignedTime = "09:30 | 28 Okt 2026"
        ),
        DeliveryOrder(
            id = "ESB-00130",
            customerName = "Resto Seafood Bahari",
            address = "Kawasan Pelabuhan No. 8",
            subDistrict = "Kec. Cilacap Selatan",
            phoneNumber = "+62 857 9988 7766",
            itemDescription = "15 balok es",
            paymentStatus = "Belum dibayar",
            priority = "Tinggi (Mendesak)",
            estimatedTimeMinutes = 40,
            distanceKm = 7.2,
            status = DeliveryStatus.NEW,
            assignedTime = "10:00 | 28 Okt 2026"
        ),
        DeliveryOrder(
            id = "ESB-00131",
            customerName = "Cafe Kopi Kube",
            address = "Jl. Jenderal Sudirman No. 88",
            subDistrict = "Kec. Purwokerto Timur",
            phoneNumber = "+62 821 4455 6677",
            itemDescription = "6 kantong es tube kristal",
            paymentStatus = "Lunas",
            priority = "Normal",
            estimatedTimeMinutes = 55,
            distanceKm = 9.1,
            status = DeliveryStatus.NEW,
            assignedTime = "10:15 | 28 Okt 2026"
        ),

        // === 12 Selesai / Terkirim Hari Ini ===
        DeliveryOrder(
            id = "ESB-00126",
            customerName = "Supermarket Sehat",
            address = "Jl. Pahlawan No. 20",
            subDistrict = "Kec. Purwokerto Utara",
            phoneNumber = "+62 812 9900 1122",
            itemDescription = "20 balok es",
            paymentStatus = "Lunas",
            priority = "Normal",
            estimatedTimeMinutes = 0,
            distanceKm = 0.0,
            status = DeliveryStatus.DELIVERED,
            assignedTime = "08:00 | 28 Okt 2026",
            inTransitTime = "08:20 | 28 Okt 2026",
            deliveredTime = "08:50 | 28 Okt 2026"
        ),
        DeliveryOrder(
            id = "ESB-00125",
            customerName = "Hotel Grand Kube",
            address = "Jl. Merdeka No. 1",
            subDistrict = "Kec. Purwokerto Pusat",
            phoneNumber = "+62 818 2233 4455",
            itemDescription = "30 balok es",
            paymentStatus = "Lunas",
            priority = "Tinggi",
            estimatedTimeMinutes = 0,
            distanceKm = 0.0,
            status = DeliveryStatus.DELIVERED,
            assignedTime = "07:30 | 28 Okt 2026",
            inTransitTime = "07:50 | 28 Okt 2026",
            deliveredTime = "08:15 | 28 Okt 2026"
        ),
        DeliveryOrder(
            id = "ESB-00124",
            customerName = "Kantin Kampus",
            address = "Kampus C No. 5",
            subDistrict = "Kec. Sumbang",
            phoneNumber = "+62 822 5566 7788",
            itemDescription = "3 kantong es kristal",
            paymentStatus = "Lunas",
            priority = "Normal",
            estimatedTimeMinutes = 0,
            distanceKm = 0.0,
            status = DeliveryStatus.DELIVERED,
            assignedTime = "07:00 | 28 Okt 2026",
            inTransitTime = "07:15 | 28 Okt 2026",
            deliveredTime = "07:35 | 28 Okt 2026"
        ),
        DeliveryOrder(
            id = "ESB-00122",
            customerName = "Toko Roti Aroma",
            address = "Jl. Jend. Soedirman No. 42",
            subDistrict = "Kec. Purwokerto Timur",
            phoneNumber = "+62 813 8877 6655",
            itemDescription = "6 balok es",
            paymentStatus = "Lunas",
            priority = "Normal",
            status = DeliveryStatus.DELIVERED,
            assignedTime = "06:45 | 28 Okt 2026",
            inTransitTime = "07:00 | 28 Okt 2026",
            deliveredTime = "07:20 | 28 Okt 2026"
        ),
        DeliveryOrder(
            id = "ESB-00121",
            customerName = "Rumah Makan Sari Laut",
            address = "Jl. Bank No. 15",
            subDistrict = "Kec. Purwokerto Barat",
            phoneNumber = "+62 815 1122 9988",
            itemDescription = "18 balok es",
            paymentStatus = "Lunas",
            priority = "Tinggi",
            status = DeliveryStatus.DELIVERED,
            assignedTime = "06:30 | 28 Okt 2026",
            inTransitTime = "06:45 | 28 Okt 2026",
            deliveredTime = "07:10 | 28 Okt 2026"
        ),
        DeliveryOrder(
            id = "ESB-00120",
            customerName = "Katering Bu Endang",
            address = "Jl. Dr. Angka No. 33",
            subDistrict = "Kec. Purwokerto Utara",
            phoneNumber = "+62 857 2233 4411",
            itemDescription = "10 kantong es kristal",
            paymentStatus = "Lunas",
            priority = "Normal",
            status = DeliveryStatus.DELIVERED,
            assignedTime = "06:15 | 28 Okt 2026",
            inTransitTime = "06:30 | 28 Okt 2026",
            deliveredTime = "06:55 | 28 Okt 2026"
        ),
        DeliveryOrder(
            id = "ESB-00119",
            customerName = "Cafe Senja Utama",
            address = "Jl. Kombas No. 7",
            subDistrict = "Kec. Purwokerto Timur",
            phoneNumber = "+62 821 9988 7766",
            itemDescription = "8 kantong es cube",
            paymentStatus = "Lunas",
            priority = "Normal",
            status = DeliveryStatus.DELIVERED,
            assignedTime = "06:00 | 28 Okt 2026",
            inTransitTime = "06:15 | 28 Okt 2026",
            deliveredTime = "06:40 | 28 Okt 2026"
        ),
        DeliveryOrder(
            id = "ESB-00118",
            customerName = "Bakso Lapangan Kota",
            address = "Jl. Kauman Lama No. 19",
            subDistrict = "Kec. Purwokerto Barat",
            phoneNumber = "+62 812 3322 1100",
            itemDescription = "5 balok es",
            paymentStatus = "Lunas",
            priority = "Normal",
            status = DeliveryStatus.DELIVERED,
            assignedTime = "05:45 | 28 Okt 2026",
            inTransitTime = "06:00 | 28 Okt 2026",
            deliveredTime = "06:25 | 28 Okt 2026"
        ),
        DeliveryOrder(
            id = "ESB-00117",
            customerName = "Depot Minuman Segar",
            address = "Jl. Pasirmuncang No. 6",
            subDistrict = "Kec. Purwokerto Barat",
            phoneNumber = "+62 856 5544 3322",
            itemDescription = "6 kantong es tube",
            paymentStatus = "Lunas",
            priority = "Normal",
            status = DeliveryStatus.DELIVERED,
            assignedTime = "05:30 | 28 Okt 2026",
            inTransitTime = "05:45 | 28 Okt 2026",
            deliveredTime = "06:05 | 28 Okt 2026"
        ),
        DeliveryOrder(
            id = "ESB-00116",
            customerName = "Warung Kopi Corner",
            address = "Jl. Martadireja No. 11",
            subDistrict = "Kec. Purwokerto Timur",
            phoneNumber = "+62 813 7766 5544",
            itemDescription = "4 kantong es kristal",
            paymentStatus = "Lunas",
            priority = "Normal",
            status = DeliveryStatus.DELIVERED,
            assignedTime = "05:15 | 28 Okt 2026",
            inTransitTime = "05:30 | 28 Okt 2026",
            deliveredTime = "05:50 | 28 Okt 2026"
        ),
        DeliveryOrder(
            id = "ESB-00115",
            customerName = "Kedai Es Goyobod",
            address = "Jl. Situmpur No. 24",
            subDistrict = "Kec. Purwokerto Selatan",
            phoneNumber = "+62 822 6655 4433",
            itemDescription = "7 balok es",
            paymentStatus = "Lunas",
            priority = "Normal",
            status = DeliveryStatus.DELIVERED,
            assignedTime = "05:00 | 28 Okt 2026",
            inTransitTime = "05:15 | 28 Okt 2026",
            deliveredTime = "05:35 | 28 Okt 2026"
        ),
        DeliveryOrder(
            id = "ESB-00114",
            customerName = "Resto Dapur Nusantara",
            address = "Jl. Masjid No. 2",
            subDistrict = "Kec. Purwokerto Barat",
            phoneNumber = "+62 819 8877 6655",
            itemDescription = "12 balok es",
            paymentStatus = "Lunas",
            priority = "Tinggi",
            status = DeliveryStatus.DELIVERED,
            assignedTime = "04:45 | 28 Okt 2026",
            inTransitTime = "05:00 | 28 Okt 2026",
            deliveredTime = "05:25 | 28 Okt 2026"
        )
    )

    private val _orders = MutableStateFlow(initialOrders)
    val orders: StateFlow<List<DeliveryOrder>> = _orders.asStateFlow()

    val dailyStatsProvider: DailyStatsProvider = DailyStatsProvider.getInstance(this)
    val dailyStats: StateFlow<DailyStatsState> = dailyStatsProvider.stats

    private val _courier = MutableStateFlow(
        Courier(
            id = "CR-098",
            name = "Maryono Supardi",
            email = "maryonosupardi@kube.com",
            phone = "+62 813 9876 5432",
            statusText = "Selamat Bertugas",
            isActiveDuty = true,
            balance = 850000L,
            pendingBalance = 145000L,
            withdrawnBalance = 2450000L,
            bankAccountNumber = "8830192831",
            bankName = "BCA",
            bankAccountHolder = "MARYONO SUPARDI",
            completedDeliveries = 12,
            totalTarget = 20,
            inTransitCount = 5,
            waitingCount = 3
        )
    )
    val courier: StateFlow<Courier> = _courier.asStateFlow()

    private val _walletBalance = MutableStateFlow(
        WalletBalance(
            pendingBalance = 145000L,
            availableBalance = 850000L,
            withdrawnBalance = 2450000L
        )
    )
    val walletBalance: StateFlow<WalletBalance> = _walletBalance.asStateFlow()

    private val _commissionHistory = MutableStateFlow(
        listOf(
            CommissionHistoryItem(
                id = "COM-101",
                orderId = "#ESB-00127",
                title = "Komisi 8 balok es - Rp45.000",
                subtitle = "Depot Es Buah Segar (#ESB-00127) · Hari Ini, 09:10",
                amount = 45000L,
                isTip = false,
                timestamp = "09:10 | 28 Okt 2026"
            ),
            CommissionHistoryItem(
                id = "COM-102",
                orderId = "#ESB-00126",
                title = "Tip Pelanggan - Rp20.000",
                subtitle = "Supermarket Sehat (#ESB-00126) · Hari Ini, 08:50",
                amount = 20000L,
                isTip = true,
                timestamp = "08:50 | 28 Okt 2026"
            ),
            CommissionHistoryItem(
                id = "COM-103",
                orderId = "#ESB-00128",
                title = "Komisi 10 balok es - Rp55.000",
                subtitle = "Toko Es Segar (#ESB-00128) · Hari Ini, 08:20",
                amount = 55000L,
                isTip = false,
                timestamp = "08:20 | 28 Okt 2026"
            ),
            CommissionHistoryItem(
                id = "COM-104",
                orderId = "#ESB-00125",
                title = "Tip Pelanggan - Rp15.000",
                subtitle = "Hotel Grand Kube (#ESB-00125) · Hari Ini, 07:50",
                amount = 15000L,
                isTip = true,
                timestamp = "07:50 | 28 Okt 2026"
            ),
            CommissionHistoryItem(
                id = "COM-105",
                orderId = "#ESB-00114",
                title = "Komisi 12 balok es - Rp65.000",
                subtitle = "Resto Dapur Nusantara (#ESB-00114) · Hari Ini, 05:25",
                amount = 65000L,
                isTip = false,
                timestamp = "05:25 | 28 Okt 2026"
            ),
            CommissionHistoryItem(
                id = "COM-106",
                orderId = "#ESB-00134",
                title = "Komisi 4 kantong es tube - Rp25.000",
                subtitle = "Kedai Jus Segar Murni (#ESB-00134) · Hari Ini, 04:30",
                amount = 25000L,
                isTip = false,
                timestamp = "04:30 | 28 Okt 2026"
            )
        )
    )
    val commissionHistory: StateFlow<List<CommissionHistoryItem>> = _commissionHistory.asStateFlow()

    private val _withdrawalHistory = MutableStateFlow(
        listOf(
            WithdrawalHistoryItem(
                id = "WD-99401",
                bankName = "BCA",
                accountNumberMasked = maskAccountNumber("8830192831"),
                recipientName = "MARYONO SUPARDI",
                amount = 500000L,
                date = "28 Okt 2026, 09:30",
                status = WithdrawalStatus.WAITING,
                rejectionReason = null
            ),
            WithdrawalHistoryItem(
                id = "WD-99388",
                bankName = "BCA",
                accountNumberMasked = maskAccountNumber("8830192831"),
                recipientName = "MARYONO SUPARDI",
                amount = 750000L,
                date = "27 Okt 2026, 16:15",
                status = WithdrawalStatus.PROCESSING,
                rejectionReason = null
            ),
            WithdrawalHistoryItem(
                id = "WD-99210",
                bankName = "BCA",
                accountNumberMasked = maskAccountNumber("8830192831"),
                recipientName = "MARYONO SUPARDI",
                amount = 1200000L,
                date = "24 Okt 2026, 11:00",
                status = WithdrawalStatus.SUCCESS,
                rejectionReason = null
            ),
            WithdrawalHistoryItem(
                id = "WD-99105",
                bankName = "BCA",
                accountNumberMasked = maskAccountNumber("8830192831"),
                recipientName = "MARYONO SUPARDI",
                amount = 350000L,
                date = "21 Okt 2026, 15:40",
                status = WithdrawalStatus.REJECTED,
                rejectionReason = "Nama pemilik rekening tidak sesuai dengan buku tabungan BCA kurir terdaftar."
            )
        )
    )
    val withdrawalHistory: StateFlow<List<WithdrawalHistoryItem>> = _withdrawalHistory.asStateFlow()

    fun requestWithdrawal(amount: Long): Boolean {
        if (amount <= 0 || amount > _walletBalance.value.availableBalance) return false
        val newAvailable = _walletBalance.value.availableBalance - amount
        val newPending = _walletBalance.value.pendingBalance + amount
        _walletBalance.update {
            it.copy(
                availableBalance = newAvailable,
                pendingBalance = newPending
            )
        }
        _courier.update {
            it.copy(
                balance = newAvailable,
                pendingBalance = newPending
            )
        }
        val newItem = WithdrawalHistoryItem(
            id = "WD-${(10000..99999).random()}",
            bankName = "BCA",
            accountNumberMasked = maskAccountNumber(_courier.value.bankAccountNumber),
            recipientName = _courier.value.bankAccountHolder,
            amount = amount,
            date = "Hari ini, 10:20",
            status = WithdrawalStatus.WAITING,
            rejectionReason = null
        )
        _withdrawalHistory.update { listOf(newItem) + it }
        return true
    }

    init {
        dailyStatsProvider.updateFromOrders(initialOrders)
        recalculateCourierStats()
    }

    fun updateOrderStatus(orderId: String, newStatus: DeliveryStatus) {
        _orders.update { currentList ->
            currentList.map { order ->
                if (order.id == orderId) {
                    val currentTime = "10:10 | 28 Okt 2026"
                    when (newStatus) {
                        DeliveryStatus.IN_TRANSIT -> order.copy(
                            status = newStatus,
                            inTransitTime = currentTime
                        )
                        DeliveryStatus.ARRIVED -> order.copy(
                            status = newStatus
                        )
                        DeliveryStatus.PENDING_VERIFICATION -> order.copy(
                            status = newStatus
                        )
                        DeliveryStatus.DELIVERED -> order.copy(
                            status = newStatus,
                            deliveredTime = currentTime
                        )
                        DeliveryStatus.FAILED -> order.copy(
                            status = newStatus
                        )
                        DeliveryStatus.NEW -> order.copy(
                            status = newStatus
                        )
                    }
                } else order
            }
        }
        dailyStatsProvider.updateFromOrders(_orders.value)
        recalculateCourierStats()
    }

    /**
     * Submits Proof of Delivery (PoD) and sets order status to PENDING_VERIFICATION.
     * The order does not immediately become DELIVERED until admin verifies the report.
     */
    fun submitProofOfDelivery(
        orderId: String,
        recipientName: String,
        photoUri: String?,
        confirmedQuantity: Int,
        notes: String?
    ) {
        val currentTime = "10:15 | 28 Okt 2026"
        val pod = com.example.core.model.ProofOfDelivery(
            recipientName = recipientName.trim(),
            photoUri = photoUri,
            confirmedQuantity = confirmedQuantity,
            notes = notes?.trim()?.ifBlank { null },
            submittedAt = currentTime
        )
        _orders.update { currentList ->
            currentList.map { order ->
                if (order.id == orderId) {
                    order.copy(
                        status = DeliveryStatus.PENDING_VERIFICATION,
                        proofOfDelivery = pod
                    )
                } else order
            }
        }
        dailyStatsProvider.updateFromOrders(_orders.value)
        recalculateCourierStats()
    }

    private fun recalculateCourierStats() {
        val currentOrders = _orders.value
        val completed = currentOrders.count { it.status == DeliveryStatus.DELIVERED }
        val inTransit = currentOrders.count {
            it.status == DeliveryStatus.IN_TRANSIT ||
            it.status == DeliveryStatus.ARRIVED ||
            it.status == DeliveryStatus.PENDING_VERIFICATION
        }
        val waiting = currentOrders.count { it.status == DeliveryStatus.NEW }
        val total = currentOrders.size

        _courier.update {
            it.copy(
                completedDeliveries = completed,
                totalTarget = total,
                inTransitCount = inTransit,
                waitingCount = waiting
            )
        }
    }

    fun updateCourierDuty(isOnDuty: Boolean) {
        _courier.update {
            it.copy(
                isActiveDuty = isOnDuty,
                statusText = if (isOnDuty) "Selamat Bertugas" else "Sedang Istirahat"
            )
        }
    }

    fun updateBiometricEnabled(enabled: Boolean) {
        _courier.update {
            it.copy(isBiometricEnabled = enabled)
        }
    }

    fun updateCourierProfile(phone: String, address: String) {
        _courier.update {
            it.copy(
                phone = phone.trim(),
                address = address.trim()
            )
        }
    }

    fun getOrderById(id: String): DeliveryOrder? {
        return _orders.value.find { it.id == id } ?: _orders.value.firstOrNull()
    }

    companion object {
        val instance = OrderRepository()
    }
}
