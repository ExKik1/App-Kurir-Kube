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

    private val initialOrders: List<DeliveryOrder> = emptyList()

    private val _orders = MutableStateFlow(initialOrders)
    val orders: StateFlow<List<DeliveryOrder>> = _orders.asStateFlow()

    val dailyStatsProvider: DailyStatsProvider = DailyStatsProvider.getInstance(this)
    val dailyStats: StateFlow<DailyStatsState> = dailyStatsProvider.stats

    private val _courier = MutableStateFlow(Courier())
    val courier: StateFlow<Courier> = _courier.asStateFlow()

    private val _walletBalance = MutableStateFlow(
        WalletBalance(
            pendingBalance = 0L,
            availableBalance = 0L,
            withdrawnBalance = 0L
        )
    )
    val walletBalance: StateFlow<WalletBalance> = _walletBalance.asStateFlow()

    private val _commissionHistory = MutableStateFlow<List<CommissionHistoryItem>>(emptyList())
    val commissionHistory: StateFlow<List<CommissionHistoryItem>> = _commissionHistory.asStateFlow()

    private val _withdrawalHistory = MutableStateFlow<List<WithdrawalHistoryItem>>(emptyList())
    val withdrawalHistory: StateFlow<List<WithdrawalHistoryItem>> = _withdrawalHistory.asStateFlow()

    /**
     * ATURAN 1 & 3: Logika Database (Tabel Withdrawal) & Penguncian Saldo (Balance Lock).
     * Saat kurir menekan 'Tarik Saldo', aplikasi mengirim permintaan INSERT ke tabel `withdrawals`
     * di Supabase backend dengan parameter:
     * - courier_id: id kurir aktif yang terautentikasi
     * - amount: nominal penarikan
     * - bank_account_id: id rekening bank utama kurir
     * - status: 'pending' (WAITING)
     *
     * CATATAN ARSITEKTUR BACKEND:
     * Data penarikan berstatus 'pending' ini nantinya HANYA akan dibaca dan di-update statusnya
     * menjadi 'success' oleh Web Admin secara manual setelah Admin mentransfer uang ke rekening kurir.
     *
     * Saldo dipotong seketika dari 'Saldo Tersedia' dan dipindahkan ke 'Saldo Diproses/Tertunda'
     * agar kurir tidak dapat menarik uang yang sama dua kali (Double-Withdrawal Prevention).
     */
    suspend fun requestWithdrawal(amount: Long): Result<WithdrawalHistoryItem> {
        if (amount <= 0) return Result.failure(IllegalArgumentException("Nominal penarikan harus lebih dari 0"))
        if (amount > _walletBalance.value.availableBalance) {
            return Result.failure(IllegalStateException("Saldo tersedia tidak mencukupi untuk penarikan"))
        }

        // Simulasi network latency request INSERT ke Supabase
        kotlinx.coroutines.delay(800)

        val newAvailable = _walletBalance.value.availableBalance - amount
        val newPending = _walletBalance.value.pendingBalance + amount

        // ATURAN 3: Penguncian Saldo (Balance Lock)
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

        val withdrawalId = "WD-${(10000..99999).random()}"
        val newItem = WithdrawalHistoryItem(
            id = withdrawalId,
            bankName = _courier.value.bankName.ifBlank { "Bank" },
            accountNumberMasked = maskAccountNumber(_courier.value.bankAccountNumber),
            recipientName = _courier.value.bankAccountHolder.ifBlank { _courier.value.name },
            amount = amount,
            date = "Baru saja",
            status = WithdrawalStatus.WAITING, // 'pending' status
            rejectionReason = null
        )

        // Insert ke Riwayat Pencairan (Real-time Flow)
        _withdrawalHistory.update { listOf(newItem) + it }
        return Result.success(newItem)
    }

    /**
     * ATURAN 4: Simulasi Pembaruan Status Real-Time oleh Web Admin
     * Ketika Web Admin menyetujui transfer di dashboard admin, status di Supabase berubah menjadi 'success'.
     * Saldo dipindahkan dari 'pendingBalance' ke 'withdrawnBalance', dan UI otomatis berubah dari Kuning (Menunggu)
     * ke Hijau (Berhasil) secara real-time via StateFlow observer.
     */
    fun simulateAdminApproval(withdrawalId: String): Boolean {
        var found = false
        var approvedAmount = 0L

        _withdrawalHistory.update { currentList ->
            currentList.map { item ->
                if (item.id == withdrawalId && (item.status == WithdrawalStatus.WAITING || item.status == WithdrawalStatus.PROCESSING)) {
                    found = true
                    approvedAmount = item.amount
                    item.copy(status = WithdrawalStatus.SUCCESS)
                } else item
            }
        }

        if (found && approvedAmount > 0) {
            _walletBalance.update {
                it.copy(
                    pendingBalance = (it.pendingBalance - approvedAmount).coerceAtLeast(0L),
                    withdrawnBalance = it.withdrawnBalance + approvedAmount
                )
            }
            _courier.update {
                it.copy(
                    pendingBalance = (it.pendingBalance - approvedAmount).coerceAtLeast(0L),
                    withdrawnBalance = it.withdrawnBalance + approvedAmount
                )
            }
        }
        return found
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
     * ATURAN 3: Rombak Logika Tombol Submit PoD
     * Saat kurir menekan tombol 'Submit' pada form Proof of Delivery, status pesanan
     * langsung diubah menjadi 'DELIVERED' (Terkirim / Selesai) tanpa perlu verifikasi admin.
     * Mengisi waktu deliveredTime, memindahkan pesanan ke riwayat selesai, dan
     * mencatat komisi pengiriman otomatis ke Saldo Tertunda hingga diverifikasi Admin di web.
     */
    fun submitProofOfDelivery(
        orderId: String,
        recipientName: String,
        photoUri: String?,
        confirmedQuantity: Int,
        notes: String?,
        isCodPaymentConfirmed: Boolean = false
    ) {
        val existingOrder = _orders.value.find { it.id == orderId }
        val wasAlreadyDelivered = existingOrder?.status == DeliveryStatus.DELIVERED
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
                    val updatedPaymentStatus = if (order.isCod && isCodPaymentConfirmed) {
                        "Lunas"
                    } else {
                        order.paymentStatus
                    }
                    order.copy(
                        status = DeliveryStatus.DELIVERED,
                        deliveredTime = currentTime,
                        paymentStatus = updatedPaymentStatus,
                        proofOfDelivery = pod
                    )
                } else order
            }
        }

        if (!wasAlreadyDelivered && existingOrder != null) {
            val earnedCommission = (confirmedQuantity.coerceAtLeast(1) * 5000L)
            val formattedAmount = "Rp ${"%,d".format(earnedCommission).replace(',', '.')}"
            val newPending = _walletBalance.value.pendingBalance + earnedCommission
            _walletBalance.update {
                it.copy(pendingBalance = newPending)
            }
            _courier.update {
                it.copy(pendingBalance = newPending)
            }
            val newCommissionItem = CommissionHistoryItem(
                id = "COM-${System.currentTimeMillis()}",
                orderId = orderId,
                title = "Komisi $confirmedQuantity unit es - $formattedAmount (Tertunda)",
                subtitle = "${existingOrder.customerName} · Hari Ini, 10:15",
                amount = earnedCommission,
                isTip = false,
                timestamp = "Hari Ini, 10:15"
            )
            _commissionHistory.update { listOf(newCommissionItem) + it }
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

    /**
     * Sinkronisasi state pesanan dari Supabase Realtime Stream (`deliveries`) setelah join hydration.
     * Menjaga konsistensi `DailyStatsProvider` dan metrik dashboard secara real-time,
     * termasuk ketika Supabase mengembalikan daftar kosong (`emptyList()`).
     */
    fun syncOrdersFromStream(remoteOrders: List<DeliveryOrder>) {
        _orders.update { current ->
            val currentById = current.associateBy { it.id }
            remoteOrders.map { remote ->
                val existing = currentById[remote.id]
                if (existing != null) {
                    remote.copy(
                        customerName = remote.customerName.ifBlank { existing.customerName },
                        address = remote.address.ifBlank { existing.address },
                        subDistrict = remote.subDistrict.ifBlank { existing.subDistrict },
                        phoneNumber = remote.phoneNumber.ifBlank { existing.phoneNumber },
                        itemDescription = remote.itemDescription.ifBlank { existing.itemDescription },
                        paymentMethod = remote.paymentMethod.ifBlank { existing.paymentMethod },
                        totalAmount = if (remote.totalAmount > 0L) remote.totalAmount else existing.totalAmount,
                        unitPrice = if (remote.unitPrice > 0L) remote.unitPrice else existing.unitPrice,
                        proofOfDelivery = remote.proofOfDelivery ?: existing.proofOfDelivery
                    )
                } else {
                    remote
                }
            }
        }
        dailyStatsProvider.updateFromOrders(_orders.value)
        recalculateCourierStats()
    }

    /**
     * Mereset seluruh state lokal menjadi kosong/0L (misalnya saat logout atau ganti akun kurir).
     */
    fun resetAllData() {
        _orders.value = emptyList()
        _courier.value = Courier()
        _walletBalance.value = WalletBalance(0L, 0L, 0L)
        _commissionHistory.value = emptyList()
        _withdrawalHistory.value = emptyList()
        dailyStatsProvider.updateFromOrders(emptyList())
    }

    /**
     * Sinkronisasi profil kurir real-time dari tabel `courier_profiles` / `couriers`.
     */
    fun syncCourierFromStream(remoteCourier: Courier) {
        _courier.update { current ->
            current.copy(
                id = remoteCourier.id.ifBlank { current.id },
                name = remoteCourier.name.ifBlank { current.name },
                email = remoteCourier.email.ifBlank { current.email },
                phone = remoteCourier.phone.ifBlank { current.phone },
                address = remoteCourier.address.ifBlank { current.address },
                isActiveDuty = remoteCourier.isActiveDuty,
                statusText = if (remoteCourier.isActiveDuty) "Selamat Bertugas" else "Sedang Istirahat",
                bankAccountNumber = remoteCourier.bankAccountNumber.ifBlank { current.bankAccountNumber },
                bankName = remoteCourier.bankName.ifBlank { current.bankName },
                bankAccountHolder = remoteCourier.bankAccountHolder.ifBlank { current.bankAccountHolder },
                vehicleType = remoteCourier.vehicleType.ifBlank { current.vehicleType },
                vehiclePlateNumber = remoteCourier.vehiclePlateNumber.ifBlank { current.vehiclePlateNumber },
                vehicleCapacity = remoteCourier.vehicleCapacity.ifBlank { current.vehicleCapacity },
                vehicleStatus = remoteCourier.vehicleStatus.ifBlank { current.vehicleStatus }
            )
        }
    }

    /**
     * Sinkronisasi ringkasan saldo real-time dari tabel `wallets` Supabase.
     */
    fun syncWalletBalanceFromStream(remoteWallet: WalletBalance) {
        _walletBalance.value = remoteWallet
        _courier.update { current ->
            current.copy(
                balance = remoteWallet.availableBalance,
                pendingBalance = remoteWallet.pendingBalance,
                withdrawnBalance = remoteWallet.withdrawnBalance
            )
        }
    }

    /**
     * Sinkronisasi riwayat transaksi komisi/tip dari stream tabel `wallet_transactions`.
     */
    fun syncCommissionHistoryFromStream(remoteCommissions: List<CommissionHistoryItem>) {
        if (remoteCommissions.isEmpty()) {
            _commissionHistory.value = emptyList()
            return
        }
        _commissionHistory.update { current ->
            val remoteIds = remoteCommissions.mapTo(HashSet()) { it.id }
            remoteCommissions + current.filterNot { it.id in remoteIds }
        }
    }

    /**
     * Sinkronisasi real-time tabel `withdrawals` dari Web Admin ke state lokal.
     * Menangani transisi status otomatis:
     * - WAITING / PROCESSING -> SUCCESS: Saldo Tertunda dipindahkan ke Saldo Dicairkan
     * - WAITING / PROCESSING -> REJECTED: Saldo Tertunda dikembalikan ke Saldo Tersedia + tampil alasan penolakan
     */
    fun syncWithdrawalsFromStream(remoteWithdrawals: List<WithdrawalHistoryItem>) {
        if (remoteWithdrawals.isEmpty()) {
            _withdrawalHistory.value = emptyList()
            return
        }

        var pendingDelta = 0L
        var availableDelta = 0L
        var withdrawnDelta = 0L

        _withdrawalHistory.update { currentList ->
            val currentById = currentList.associateBy { it.id }
            val updatedRemote = remoteWithdrawals.map { remote ->
                val prev = currentById[remote.id]
                if (prev != null && (prev.status == WithdrawalStatus.WAITING || prev.status == WithdrawalStatus.PROCESSING)) {
                    when (remote.status) {
                        WithdrawalStatus.SUCCESS -> {
                            pendingDelta -= remote.amount
                            withdrawnDelta += remote.amount
                        }
                        WithdrawalStatus.REJECTED -> {
                            pendingDelta -= remote.amount
                            availableDelta += remote.amount
                        }
                        else -> {}
                    }
                }
                remote
            }
            val remoteIds = updatedRemote.mapTo(HashSet()) { it.id }
            updatedRemote + currentList.filterNot { it.id in remoteIds }
        }

        if (pendingDelta != 0L || availableDelta != 0L || withdrawnDelta != 0L) {
            _walletBalance.update { w ->
                w.copy(
                    pendingBalance = (w.pendingBalance + pendingDelta).coerceAtLeast(0L),
                    availableBalance = (w.availableBalance + availableDelta).coerceAtLeast(0L),
                    withdrawnBalance = (w.withdrawnBalance + withdrawnDelta).coerceAtLeast(0L)
                )
            }
            val updatedWallet = _walletBalance.value
            _courier.update { c ->
                c.copy(
                    balance = updatedWallet.availableBalance,
                    pendingBalance = updatedWallet.pendingBalance,
                    withdrawnBalance = updatedWallet.withdrawnBalance
                )
            }
        }
    }

    fun getOrderById(id: String): DeliveryOrder? {
        return _orders.value.find { it.id == id } ?: _orders.value.firstOrNull()
    }

    companion object {
        val instance = OrderRepository()
    }
}
