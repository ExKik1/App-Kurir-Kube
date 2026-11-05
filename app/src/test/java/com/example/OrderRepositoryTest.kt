package com.example

import com.example.core.model.DeliveryStatus
import com.example.core.repository.OrderRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderRepositoryTest {

    @Test
    fun repository_initialOrders_containsActiveDelivery() {
        val repo = OrderRepository()
        val orders = repo.orders.value
        assertTrue("Orders list should not be empty", orders.isNotEmpty())

        val activeOrder = repo.getOrderById("ESB-00128")
        assertNotNull("Order ESB-00128 should exist", activeOrder)
        assertEquals("Toko Es Segar", activeOrder?.customerName)
        assertEquals(DeliveryStatus.IN_TRANSIT, activeOrder?.status)
    }

    @Test
    fun repository_dailyStats_isSingleSourceOfTruthAndMathematicallyAccurate() {
        val repo = OrderRepository()
        val stats = repo.dailyStats.value

        assertEquals("Hari Ini", stats.periodLabel)
        assertEquals(20, stats.totalOrders)
        assertEquals(12, stats.completedOrders)
        assertEquals(5, stats.inDeliveryOrders)
        assertEquals(3, stats.newTasksOrders)
        assertEquals(0, stats.failedReturnOrders)

        // Mathematical check: 20 == 12 + 5 + 3 + 0
        assertTrue("Daily stats must be mathematically consistent", stats.isMathematicallyConsistent)
        assertEquals(
            stats.totalOrders,
            stats.completedOrders + stats.inDeliveryOrders + stats.newTasksOrders + stats.failedReturnOrders
        )
    }

    @Test
    fun repository_updateOrderStatus_updatesDailyStatsAndPreservesMathematicalInvariant() {
        val repo = OrderRepository()

        // Transition order ESB-00128 from IN_TRANSIT to DELIVERED
        repo.updateOrderStatus("ESB-00128", DeliveryStatus.DELIVERED)

        val updatedOrder = repo.getOrderById("ESB-00128")
        assertEquals(DeliveryStatus.DELIVERED, updatedOrder?.status)
        assertNotNull(updatedOrder?.deliveredTime)

        val stats = repo.dailyStats.value
        assertEquals(13, stats.completedOrders)
        assertEquals(4, stats.inDeliveryOrders)
        assertEquals(3, stats.newTasksOrders)
        assertEquals(0, stats.failedReturnOrders)
        assertEquals(20, stats.totalOrders)

        // Mathematical invariant: 20 == 13 + 4 + 3 + 0
        assertTrue("Daily stats must remain mathematically consistent after transition", stats.isMathematicallyConsistent)
    }

    @Test
    fun repository_updateCourierDuty_updatesDutyState() {
        val repo = OrderRepository()
        repo.updateCourierDuty(false)
        assertEquals(false, repo.courier.value.isActiveDuty)
        assertEquals("Sedang Istirahat", repo.courier.value.statusText)

        repo.updateCourierDuty(true)
        assertEquals(true, repo.courier.value.isActiveDuty)
        assertEquals("Selamat Bertugas", repo.courier.value.statusText)
    }

    @Test
    fun deliveryFiltering_orderStatusCategoriesAreAbsolute() {
        // Aturan 1: Status Model Data memiliki kategori absolut
        assertTrue(DeliveryStatus.NEW.isAntrean)
        assertTrue(!DeliveryStatus.NEW.isAktif)
        assertTrue(!DeliveryStatus.NEW.isRiwayat)

        assertTrue(DeliveryStatus.IN_TRANSIT.isAktif)
        assertTrue(DeliveryStatus.ARRIVED.isAktif)
        assertTrue(!DeliveryStatus.IN_TRANSIT.isAntrean)
        assertTrue(!DeliveryStatus.IN_TRANSIT.isRiwayat)

        assertTrue(DeliveryStatus.DELIVERED.isRiwayat)
        assertTrue(DeliveryStatus.FAILED.isRiwayat)
        assertTrue(!DeliveryStatus.DELIVERED.isAktif)
        assertTrue(!DeliveryStatus.DELIVERED.isAntrean)
    }

    @Test
    fun deliveryFiltering_activeAndQueueSectionsAreStrictlyPartitioned() {
        val repo = OrderRepository()
        val allOrders = repo.orders.value

        // Aturan 2: Seksi 'Pengiriman Aktif' menampilkan maksimal 1 atau 2 pesanan aktif
        val activeDeliveries = allOrders.filter { it.isAktif }.take(2)
        assertTrue("Active section must contain at most 2 items", activeDeliveries.size <= 2)
        assertTrue(activeDeliveries.all { it.isAktif })

        // Aturan 3: Seksi 'Antrean Berikutnya' HANYA memuat pesanan dengan status antrean (NEW)
        val queueOrders = allOrders.filter { it.isAntrean }
        assertEquals(3, queueOrders.size)
        assertTrue("Queue must only contain NEW status", queueOrders.all { it.status == DeliveryStatus.NEW })
        assertTrue("Queue must NOT contain any DELIVERED orders", queueOrders.none { it.status == DeliveryStatus.DELIVERED })
        assertTrue("Queue must NOT contain any FAILED orders", queueOrders.none { it.status == DeliveryStatus.FAILED })
        assertTrue("Queue must NOT contain any IN_TRANSIT orders", queueOrders.none { it.status == DeliveryStatus.IN_TRANSIT })

        // Aturan 4: Pemisahan Tab Selesai (Riwayat)
        val historyOrders = allOrders.filter { it.isRiwayat }
        assertEquals(12, historyOrders.size)
        assertTrue("History orders must only contain DELIVERED or FAILED", historyOrders.all { it.isRiwayat })
    }

    @Test
    fun deliveryFiltering_dynamicSynchronization_orderTransitionsMoveCleanlyBetweenSections() {
        // Aturan 5: Sinkronisasi Data otomatis saat status berubah
        val repo = OrderRepository()

        val initialActive = repo.orders.value.filter { it.isAktif }.take(2)
        assertTrue(initialActive.any { it.id == "ESB-00128" })

        // Kurir menyelesaikan pengiriman ESB-00128
        repo.updateOrderStatus("ESB-00128", DeliveryStatus.DELIVERED)

        val updatedOrders = repo.orders.value
        val updatedActive = updatedOrders.filter { it.isAktif }
        val updatedQueue = updatedOrders.filter { it.isAntrean }
        val updatedHistory = updatedOrders.filter { it.isRiwayat }

        // 1. Otomatis menghilang dari Pengiriman Aktif
        assertTrue("Completed order must disappear from active deliveries", updatedActive.none { it.id == "ESB-00128" })

        // 2. Tidak pernah muncul di Antrean Berikutnya
        assertTrue("Completed order must NEVER enter the queue", updatedQueue.none { it.id == "ESB-00128" })

        // 3. Otomatis berpindah ke Riwayat Selesai
        assertTrue("Completed order must appear in history list", updatedHistory.any { it.id == "ESB-00128" })
        assertEquals(13, updatedHistory.size)
    }

    @Test
    fun proofOfDelivery_submissionSetsStatusToPendingVerificationNotDirectlyDelivered() {
        val repo = OrderRepository()

        // Kurir tiba di lokasi
        repo.updateOrderStatus("ESB-00128", DeliveryStatus.ARRIVED)
        val arrivedOrder = repo.getOrderById("ESB-00128")
        assertEquals(DeliveryStatus.ARRIVED, arrivedOrder?.status)

        // Kurir mengisi form PoD dan menekan Kirim Laporan
        repo.submitProofOfDelivery(
            orderId = "ESB-00128",
            recipientName = "Bpk. Bambang Sutrisno",
            photoUri = "android.resource://com.example/drawable/sample_ice.jpg",
            confirmedQuantity = 10,
            notes = "Ditaruh di freezer belakang toko"
        )

        val pendingOrder = repo.getOrderById("ESB-00128")
        assertNotNull("Order should exist", pendingOrder)

        // Logika Submit: Wajib berstatus Menunggu Verifikasi Admin (TIDAK langsung Selesai / DELIVERED)
        assertEquals(DeliveryStatus.PENDING_VERIFICATION, pendingOrder?.status)
        assertTrue("Order awaiting verification must still be AKTIF", pendingOrder?.isAktif == true)
        assertTrue("Order awaiting verification must NOT be in RIWAYAT (Selesai) yet", pendingOrder?.isRiwayat == false)

        // Data PoD tersimpan lengkap
        val pod = pendingOrder?.proofOfDelivery
        assertNotNull("PoD details must be recorded", pod)
        assertEquals("Bpk. Bambang Sutrisno", pod?.recipientName)
        assertEquals(10, pod?.confirmedQuantity)
        assertEquals("Ditaruh di freezer belakang toko", pod?.notes)
        assertNotNull(pod?.photoUri)

        // Invarian statistik harian tetap konsisten (20 == 12 Selesai + 5 Diantar/Verifikasi + 3 Menunggu + 0 Retur)
        val stats = repo.dailyStats.value
        assertEquals(12, stats.completedOrders) // Belum bertambah karena masih diverifikasi admin
        assertEquals(5, stats.inDeliveryOrders)
        assertEquals(3, stats.newTasksOrders)
        assertEquals(20, stats.totalOrders)
        assertTrue(stats.isMathematicallyConsistent)
    }

    @Test
    fun wallet_balanceSeparation_containsThreeDistinctBalances() {
        // Aturan 1: Pemisahan Saldo (Saldo Tertunda, Saldo Tersedia, Saldo Dicairkan)
        val repo = OrderRepository()
        val wallet = repo.walletBalance.value

        assertEquals("Saldo Tertunda harus Rp 145.000", 145000L, wallet.pendingBalance)
        assertEquals("Saldo Tersedia harus Rp 850.000", 850000L, wallet.availableBalance)
        assertEquals("Saldo Dicairkan harus Rp 2.450.000", 2450000L, wallet.withdrawnBalance)
    }

    @Test
    fun wallet_commissionHistory_clarifiesIncomeDetails() {
        // Aturan 2: Detail Riwayat Komisi memperjelas jenis pendapatan
        val repo = OrderRepository()
        val commissions = repo.commissionHistory.value
        assertTrue("Commission list must not be empty", commissions.isNotEmpty())

        val commissionItem = commissions.find { it.title.contains("Komisi 8 balok es") }
        assertNotNull("Must include specific commission description", commissionItem)
        assertEquals(45000L, commissionItem?.amount)

        val tipItem = commissions.find { it.title.contains("Tip Pelanggan") }
        assertNotNull("Must include tip description", tipItem)
        assertTrue(tipItem?.isTip == true)
    }

    @Test
    fun wallet_withdrawalHistory_containsProperStatusesAndRejectionReasons() {
        // Aturan 3: Status Pencairan (Menunggu, Diproses, Berhasil, Ditolak dengan alasan)
        val repo = OrderRepository()
        val withdrawals = repo.withdrawalHistory.value
        assertTrue("Withdrawal list must not be empty", withdrawals.isNotEmpty())

        val waiting = withdrawals.find { it.status == com.example.core.model.WithdrawalStatus.WAITING }
        assertNotNull("Must have Menunggu withdrawal", waiting)
        assertEquals("Menunggu", waiting?.status?.label)

        val processing = withdrawals.find { it.status == com.example.core.model.WithdrawalStatus.PROCESSING }
        assertNotNull("Must have Diproses withdrawal", processing)
        assertEquals("Diproses", processing?.status?.label)

        val success = withdrawals.find { it.status == com.example.core.model.WithdrawalStatus.SUCCESS }
        assertNotNull("Must have Berhasil withdrawal", success)
        assertEquals("Berhasil", success?.status?.label)

        val rejected = withdrawals.find { it.status == com.example.core.model.WithdrawalStatus.REJECTED }
        assertNotNull("Must have Ditolak withdrawal", rejected)
        assertEquals("Ditolak", rejected?.status?.label)
        assertNotNull("Rejected withdrawal MUST have rejectionReason", rejected?.rejectionReason)
        assertTrue("Rejection reason must be informative", rejected?.rejectionReason?.isNotBlank() == true)
    }

    @Test
    fun wallet_securityMasking_masksAccountNumberCorrectly() {
        // Aturan 4: Keamanan Rekening (Masking 8830192831 -> **** **** 2831)
        val fullAccountNumber = "8830192831"
        val masked = com.example.core.model.maskAccountNumber(fullAccountNumber)
        assertEquals("**** **** 2831", masked)

        val maskedWithSpaces = com.example.core.model.maskAccountNumber("8830 1928 31")
        assertEquals("**** **** 2831", maskedWithSpaces)
    }

    @Test
    fun wallet_requestWithdrawal_updatesBalancesCorrectly() = kotlinx.coroutines.runBlocking {
        val repo = OrderRepository()
        val initialAvailable = repo.walletBalance.value.availableBalance
        val initialPending = repo.walletBalance.value.pendingBalance

        val requestedAmount = 200000L
        val result = repo.requestWithdrawal(requestedAmount)
        assertTrue("Withdrawal request must succeed", result.isSuccess)

        val updatedWallet = repo.walletBalance.value
        assertEquals(initialAvailable - requestedAmount, updatedWallet.availableBalance)
        assertEquals(initialPending + requestedAmount, updatedWallet.pendingBalance)

        val latestWithdrawal = repo.withdrawalHistory.value.first()
        assertEquals(requestedAmount, latestWithdrawal.amount)
        assertEquals(com.example.core.model.WithdrawalStatus.WAITING, latestWithdrawal.status)
        assertEquals("**** **** 2831", latestWithdrawal.accountNumberMasked)
    }

    @Test
    fun wallet_adminApproval_realtimeTransitionToSuccess() = kotlinx.coroutines.runBlocking {
        // ATURAN 4: Pembaruan Riwayat Pencairan (Real-time perubahan status dari Web Admin)
        val repo = OrderRepository()
        val requestedAmount = 150000L
        val result = repo.requestWithdrawal(requestedAmount)
        assertTrue("Request must succeed", result.isSuccess)
        val withdrawal = result.getOrThrow()
        assertEquals(com.example.core.model.WithdrawalStatus.WAITING, withdrawal.status)

        // Web Admin melakukan verifikasi dan transfer uang secara manual, lalu menekan approve
        val approved = repo.simulateAdminApproval(withdrawal.id)
        assertTrue("Admin approval simulation must succeed", approved)

        val updatedItem = repo.withdrawalHistory.value.find { it.id == withdrawal.id }
        assertNotNull(updatedItem)
        // Status otomatis berubah menjadi SUCCESS (Badge Hijau)
        assertEquals(com.example.core.model.WithdrawalStatus.SUCCESS, updatedItem?.status)

        // Saldo pending otomatis berkurang dan berpindah ke withdrawnBalance
        val updatedWallet = repo.walletBalance.value
        assertEquals(145000L, updatedWallet.pendingBalance) // kembali ke balance awal
        assertEquals(2450000L + requestedAmount, updatedWallet.withdrawnBalance)
    }

    @Test
    fun profile_vehicleInformation_containsLogisticsSpecs() {
        // Aturan 2: Informasi Kendaraan (Jenis Kendaraan, Nomor Polisi, Kapasitas Angkut, Status)
        val repo = OrderRepository()
        val courier = repo.courier.value

        assertEquals("Truk Box Pendingin (Freezer)", courier.vehicleType)
        assertEquals("R 8921 KB", courier.vehiclePlateNumber)
        assertTrue("Vehicle capacity must specify maximum ice capacity", courier.vehicleCapacity.contains("Maks. 50 Balok"))
        assertEquals("Prima & Layak Jalan", courier.vehicleStatus)
    }

    @Test
    fun profile_credentialsAndBiometrics_handlesToggleAndReadOnlyState() {
        // Aturan 3: Info Kredensial & Biometrik (ID Kurir & Email read-only, Biometric Switch)
        val repo = OrderRepository()
        val courier = repo.courier.value

        assertEquals("CR-098", courier.id)
        assertEquals("maryonosupardi@kube.com", courier.email)
        assertTrue("Biometrics should be enabled by default", courier.isBiometricEnabled)

        // Toggle biometrik
        repo.updateBiometricEnabled(false)
        assertEquals(false, repo.courier.value.isBiometricEnabled)

        repo.updateBiometricEnabled(true)
        assertEquals(true, repo.courier.value.isBiometricEnabled)
    }

    @Test
    fun editProfile_updatesPhoneAndAddress_preservesNameAndEmail() {
        val repo = OrderRepository()
        val originalName = repo.courier.value.name
        val originalEmail = repo.courier.value.email

        val newPhone = "+62 821 7788 9900"
        val newAddress = "Jl. Patriot No. 10, Karangpucung, Purwokerto"

        repo.updateCourierProfile(newPhone, newAddress)

        val updatedCourier = repo.courier.value
        assertEquals(newPhone, updatedCourier.phone)
        assertEquals(newAddress, updatedCourier.address)
        // Read-only invariants: Name and Email must not change
        assertEquals(originalName, updatedCourier.name)
        assertEquals(originalEmail, updatedCourier.email)
    }

    @Test
    fun deliveryHistory_filtersOnlyDeliveredAndFailedOrders() {
        val repo = OrderRepository()
        val allOrders = repo.orders.value

        val historyOrders = allOrders.filter {
            it.status == com.example.core.model.DeliveryStatus.DELIVERED ||
            it.status == com.example.core.model.DeliveryStatus.FAILED
        }

        assertTrue("History orders must not be empty", historyOrders.isNotEmpty())
        assertTrue("All history orders must be DELIVERED or FAILED", historyOrders.all {
            it.status == com.example.core.model.DeliveryStatus.DELIVERED ||
            it.status == com.example.core.model.DeliveryStatus.FAILED
        })
        assertTrue("No active IN_TRANSIT orders in history", historyOrders.none {
            it.status == com.example.core.model.DeliveryStatus.IN_TRANSIT
        })
        assertTrue("No NEW queue orders in history", historyOrders.none {
            it.status == com.example.core.model.DeliveryStatus.NEW
        })
    }
}
