package com.example

import com.example.core.model.CommissionHistoryItem
import com.example.core.model.Courier
import com.example.core.model.DeliveryOrder
import com.example.core.model.DeliveryStatus
import com.example.core.model.WalletBalance
import com.example.core.model.WithdrawalHistoryItem
import com.example.core.model.WithdrawalStatus
import com.example.core.model.maskAccountNumber
import com.example.core.network.CourierBankAccountRecord
import com.example.core.network.CourierSupabaseService
import com.example.core.network.OrderPaymentMeta
import com.example.core.network.SupportContactRecord
import com.example.core.repository.OrderRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderRepositoryTest {

    private fun createSeededRepository(): OrderRepository {
        val repo = OrderRepository()
        repo.syncCourierFromStream(
            Courier(
                id = "101",
                name = "Budi Santoso",
                email = "budi@kube.com",
                phone = "+62 813 9876 5432",
                address = "Jl. Patriot No. 1, Purwokerto",
                isActiveDuty = true,
                balance = 850000L,
                pendingBalance = 145000L,
                withdrawnBalance = 2450000L,
                bankAccountNumber = "8830192831",
                bankName = "BCA",
                bankAccountHolder = "BUDI SANTOSO",
                vehicleType = "Truk Box Pendingin (Freezer)",
                vehiclePlateNumber = "R 8921 KB",
                vehicleCapacity = "Maks. 50 Balok Es (± 1.250 kg)",
                vehicleStatus = "Prima & Layak Jalan",
                isBiometricEnabled = true
            )
        )
        repo.syncWalletBalanceFromStream(
            WalletBalance(
                pendingBalance = 145000L,
                availableBalance = 850000L,
                withdrawnBalance = 2450000L
            )
        )
        repo.syncOrdersFromStream(
            listOf(
                DeliveryOrder(
                    id = "ESB-00128",
                    customerName = "Toko Es Segar",
                    address = "Jl. Melati No. 12",
                    itemDescription = "10 balok es",
                    paymentStatus = "Belum dibayar",
                    status = DeliveryStatus.IN_TRANSIT,
                    paymentMethod = "COD",
                    totalAmount = 150000L,
                    unitPrice = 15000L
                ),
                DeliveryOrder(
                    id = "ESB-00127",
                    customerName = "Depot Es Buah Segar",
                    address = "Jl. Gatot Subroto No. 3",
                    itemDescription = "8 balok es",
                    paymentStatus = "Lunas",
                    status = DeliveryStatus.IN_TRANSIT,
                    paymentMethod = "QRIS",
                    totalAmount = 120000L,
                    unitPrice = 15000L
                ),
                DeliveryOrder(
                    id = "ESB-00129",
                    customerName = "Warung Segar",
                    address = "Jl. Diponegoro No. 45",
                    itemDescription = "4 kantong es kristal",
                    paymentStatus = "Lunas",
                    status = DeliveryStatus.NEW,
                    paymentMethod = "TRANSFER",
                    totalAmount = 60000L,
                    unitPrice = 15000L
                ),
                DeliveryOrder(
                    id = "ESB-00126",
                    customerName = "Supermarket Sehat",
                    address = "Jl. Pahlawan No. 20",
                    itemDescription = "20 balok es",
                    paymentStatus = "Lunas",
                    status = DeliveryStatus.DELIVERED,
                    paymentMethod = "QRIS",
                    totalAmount = 300000L,
                    unitPrice = 15000L
                )
            )
        )
        repo.syncCommissionHistoryFromStream(
            listOf(
                CommissionHistoryItem(
                    id = "COM-101",
                    orderId = "#ESB-00127",
                    title = "Komisi 8 balok es - Rp45.000",
                    subtitle = "Depot Es Buah Segar (#ESB-00127)",
                    amount = 45000L,
                    isTip = false,
                    timestamp = "09:10"
                ),
                CommissionHistoryItem(
                    id = "COM-102",
                    orderId = "#ESB-00126",
                    title = "Tip Pelanggan - Rp20.000",
                    subtitle = "Supermarket Sehat (#ESB-00126)",
                    amount = 20000L,
                    isTip = true,
                    timestamp = "08:50"
                )
            )
        )
        repo.syncWithdrawalsFromStream(
            listOf(
                WithdrawalHistoryItem(
                    id = "WD-99401",
                    bankName = "BCA",
                    accountNumberMasked = maskAccountNumber("8830192831"),
                    recipientName = "BUDI SANTOSO",
                    amount = 500000L,
                    date = "28 Okt 2026, 09:30",
                    status = WithdrawalStatus.WAITING,
                    rejectionReason = null
                ),
                WithdrawalHistoryItem(
                    id = "WD-99388",
                    bankName = "BCA",
                    accountNumberMasked = maskAccountNumber("8830192831"),
                    recipientName = "BUDI SANTOSO",
                    amount = 750000L,
                    date = "27 Okt 2026, 16:15",
                    status = WithdrawalStatus.PROCESSING,
                    rejectionReason = null
                ),
                WithdrawalHistoryItem(
                    id = "WD-99210",
                    bankName = "BCA",
                    accountNumberMasked = maskAccountNumber("8830192831"),
                    recipientName = "BUDI SANTOSO",
                    amount = 1200000L,
                    date = "24 Okt 2026, 11:00",
                    status = WithdrawalStatus.SUCCESS,
                    rejectionReason = null
                ),
                WithdrawalHistoryItem(
                    id = "WD-99105",
                    bankName = "BCA",
                    accountNumberMasked = maskAccountNumber("8830192831"),
                    recipientName = "BUDI SANTOSO",
                    amount = 350000L,
                    date = "21 Okt 2026, 15:40",
                    status = WithdrawalStatus.REJECTED,
                    rejectionReason = "Nama pemilik rekening tidak sesuai."
                )
            )
        )
        return repo
    }

    @Test
    fun repository_initialState_startsEmptyAndZeroBalances() {
        val repo = OrderRepository()
        assertTrue("Initial orders must be empty", repo.orders.value.isEmpty())
        assertTrue("Initial commissions must be empty", repo.commissionHistory.value.isEmpty())
        assertTrue("Initial withdrawals must be empty", repo.withdrawalHistory.value.isEmpty())

        val wallet = repo.walletBalance.value
        assertEquals(0L, wallet.pendingBalance)
        assertEquals(0L, wallet.availableBalance)
        assertEquals(0L, wallet.withdrawnBalance)

        val defaultWallet = WalletBalance()
        assertEquals(0L, defaultWallet.pendingBalance)
        assertEquals(0L, defaultWallet.availableBalance)
        assertEquals(0L, defaultWallet.withdrawnBalance)

        val stats = repo.dailyStats.value
        assertEquals(0, stats.totalOrders)
        assertEquals(0, stats.completedOrders)
        assertEquals(0, stats.inDeliveryOrders)
        assertEquals(0, stats.newTasksOrders)
        assertEquals(0, stats.failedReturnOrders)
        assertTrue(stats.isMathematicallyConsistent)
    }

    @Test
    fun repository_syncEmptyStreams_clearsExistingListsForAccurateEmptyState() {
        val repo = createSeededRepository()
        assertFalse(repo.orders.value.isEmpty())
        assertFalse(repo.commissionHistory.value.isEmpty())
        assertFalse(repo.withdrawalHistory.value.isEmpty())

        // Simulasikan Supabase mengembalikan array kosong []
        repo.syncOrdersFromStream(emptyList())
        repo.syncCommissionHistoryFromStream(emptyList())
        repo.syncWithdrawalsFromStream(emptyList())

        assertTrue("Orders list must clear when Supabase returns empty array", repo.orders.value.isEmpty())
        assertTrue("Commissions must clear when Supabase returns empty array", repo.commissionHistory.value.isEmpty())
        assertTrue("Withdrawals must clear when Supabase returns empty array", repo.withdrawalHistory.value.isEmpty())
        assertEquals(0, repo.dailyStats.value.totalOrders)
        assertTrue(repo.dailyStats.value.isMathematicallyConsistent)
    }

    @Test
    fun repository_dailyStats_isSingleSourceOfTruthAndMathematicallyAccurate() {
        val repo = createSeededRepository()
        val stats = repo.dailyStats.value

        assertEquals("Hari Ini", stats.periodLabel)
        assertEquals(4, stats.totalOrders)
        assertEquals(1, stats.completedOrders)
        assertEquals(2, stats.inDeliveryOrders)
        assertEquals(1, stats.newTasksOrders)
        assertEquals(0, stats.failedReturnOrders)
        assertTrue(stats.isMathematicallyConsistent)
    }

    @Test
    fun repository_updateOrderStatus_updatesDailyStatsAndPreservesMathematicalInvariant() {
        val repo = createSeededRepository()

        repo.updateOrderStatus("ESB-00128", DeliveryStatus.DELIVERED)

        val updatedOrder = repo.getOrderById("ESB-00128")
        assertEquals(DeliveryStatus.DELIVERED, updatedOrder?.status)
        assertNotNull(updatedOrder?.deliveredTime)

        val stats = repo.dailyStats.value
        assertEquals(2, stats.completedOrders)
        assertEquals(1, stats.inDeliveryOrders)
        assertEquals(1, stats.newTasksOrders)
        assertEquals(0, stats.failedReturnOrders)
        assertEquals(4, stats.totalOrders)
        assertTrue(stats.isMathematicallyConsistent)
    }

    @Test
    fun repository_updateCourierDuty_updatesDutyState() {
        val repo = createSeededRepository()
        repo.updateCourierDuty(false)
        assertEquals(false, repo.courier.value.isActiveDuty)
        assertEquals("Sedang Istirahat", repo.courier.value.statusText)

        repo.updateCourierDuty(true)
        assertEquals(true, repo.courier.value.isActiveDuty)
        assertEquals("Selamat Bertugas", repo.courier.value.statusText)
    }

    @Test
    fun deliveryFiltering_orderStatusCategoriesAreAbsolute() {
        assertTrue(DeliveryStatus.NEW.isAntrean)
        assertFalse(DeliveryStatus.NEW.isAktif)
        assertFalse(DeliveryStatus.NEW.isRiwayat)

        assertTrue(DeliveryStatus.IN_TRANSIT.isAktif)
        assertTrue(DeliveryStatus.ARRIVED.isAktif)
        assertFalse(DeliveryStatus.IN_TRANSIT.isAntrean)
        assertFalse(DeliveryStatus.IN_TRANSIT.isRiwayat)

        assertTrue(DeliveryStatus.DELIVERED.isRiwayat)
        assertTrue(DeliveryStatus.FAILED.isRiwayat)
        assertFalse(DeliveryStatus.DELIVERED.isAktif)
        assertFalse(DeliveryStatus.DELIVERED.isAntrean)
    }

    @Test
    fun deliveryFiltering_activeAndQueueSectionsAreStrictlyPartitioned() {
        val repo = createSeededRepository()
        val allOrders = repo.orders.value

        val activeDeliveries = allOrders.filter { it.isAktif }.take(2)
        assertTrue(activeDeliveries.size <= 2)
        assertTrue(activeDeliveries.all { it.isAktif })

        val queueOrders = allOrders.filter { it.isAntrean }
        assertEquals(1, queueOrders.size)
        assertTrue(queueOrders.all { it.status == DeliveryStatus.NEW })

        val historyOrders = allOrders.filter { it.isRiwayat }
        assertEquals(1, historyOrders.size)
        assertTrue(historyOrders.all { it.isRiwayat })
    }

    @Test
    fun deliveryFiltering_dynamicSynchronization_orderTransitionsMoveCleanlyBetweenSections() {
        val repo = createSeededRepository()

        assertTrue(repo.orders.value.filter { it.isAktif }.any { it.id == "ESB-00128" })

        repo.updateOrderStatus("ESB-00128", DeliveryStatus.DELIVERED)

        val updatedOrders = repo.orders.value
        assertTrue(updatedOrders.filter { it.isAktif }.none { it.id == "ESB-00128" })
        assertTrue(updatedOrders.filter { it.isAntrean }.none { it.id == "ESB-00128" })
        assertTrue(updatedOrders.filter { it.isRiwayat }.any { it.id == "ESB-00128" })
    }

    @Test
    fun proofOfDelivery_submissionImmediatelySetsStatusToDeliveredAndMovesToHistory() {
        val repo = createSeededRepository()

        repo.updateOrderStatus("ESB-00128", DeliveryStatus.ARRIVED)
        assertEquals(DeliveryStatus.ARRIVED, repo.getOrderById("ESB-00128")?.status)

        repo.submitProofOfDelivery(
            orderId = "ESB-00128",
            recipientName = "Bpk. Bambang Sutrisno",
            photoUri = "https://wopqozbstvtuhxkuciun.supabase.co/storage/v1/object/public/pod-photos/sample.jpg",
            confirmedQuantity = 10,
            notes = "Ditaruh di freezer belakang toko"
        )

        val deliveredOrder = repo.getOrderById("ESB-00128")
        assertNotNull(deliveredOrder)
        assertEquals(DeliveryStatus.DELIVERED, deliveredOrder?.status)
        assertFalse(deliveredOrder?.isAktif ?: true)
        assertTrue(deliveredOrder?.isRiwayat ?: false)

        val pod = deliveredOrder?.proofOfDelivery
        assertEquals("Bpk. Bambang Sutrisno", pod?.recipientName)
        assertEquals(10, pod?.confirmedQuantity)
        assertEquals("Ditaruh di freezer belakang toko", pod?.notes)

        val stats = repo.dailyStats.value
        assertEquals(2, stats.completedOrders)
        assertEquals(1, stats.inDeliveryOrders)
        assertTrue(stats.isMathematicallyConsistent)
    }

    @Test
    fun wallet_balanceSeparation_andHistoriesWorkFromSyncedData() {
        val repo = createSeededRepository()
        val wallet = repo.walletBalance.value

        assertEquals(145000L, wallet.pendingBalance)
        assertEquals(850000L, wallet.availableBalance)
        assertEquals(2450000L, wallet.withdrawnBalance)

        val commissions = repo.commissionHistory.value
        assertEquals(2, commissions.size)
        assertEquals(45000L, commissions.find { it.title.contains("Komisi 8 balok es") }?.amount)
        assertTrue(commissions.find { it.title.contains("Tip Pelanggan") }?.isTip == true)

        val withdrawals = repo.withdrawalHistory.value
        assertEquals(4, withdrawals.size)
        assertNotNull(withdrawals.find { it.status == WithdrawalStatus.WAITING })
        assertNotNull(withdrawals.find { it.status == WithdrawalStatus.PROCESSING })
        assertNotNull(withdrawals.find { it.status == WithdrawalStatus.SUCCESS })
        val rejected = withdrawals.find { it.status == WithdrawalStatus.REJECTED }
        assertTrue(rejected?.rejectionReason?.isNotBlank() == true)
    }

    @Test
    fun wallet_securityMasking_masksAccountNumberCorrectly() {
        assertEquals("**** **** 2831", maskAccountNumber("8830192831"))
        assertEquals("**** **** 2831", maskAccountNumber("8830 1928 31"))
    }

    @Test
    fun wallet_requestWithdrawal_updatesBalancesCorrectly() = runBlocking {
        val repo = createSeededRepository()
        val initialAvailable = repo.walletBalance.value.availableBalance
        val initialPending = repo.walletBalance.value.pendingBalance

        val requestedAmount = 200000L
        val result = repo.requestWithdrawal(requestedAmount)
        assertTrue(result.isSuccess)

        val updatedWallet = repo.walletBalance.value
        assertEquals(initialAvailable - requestedAmount, updatedWallet.availableBalance)
        assertEquals(initialPending + requestedAmount, updatedWallet.pendingBalance)

        val latestWithdrawal = repo.withdrawalHistory.value.first()
        assertEquals(requestedAmount, latestWithdrawal.amount)
        assertEquals(WithdrawalStatus.WAITING, latestWithdrawal.status)
        assertEquals("**** **** 2831", latestWithdrawal.accountNumberMasked)
    }

    @Test
    fun wallet_adminApproval_realtimeTransitionToSuccess() = runBlocking {
        val repo = createSeededRepository()
        val requestedAmount = 150000L
        val result = repo.requestWithdrawal(requestedAmount)
        assertTrue(result.isSuccess)
        val withdrawal = result.getOrThrow()

        val approved = repo.simulateAdminApproval(withdrawal.id)
        assertTrue(approved)

        val updatedItem = repo.withdrawalHistory.value.find { it.id == withdrawal.id }
        assertEquals(WithdrawalStatus.SUCCESS, updatedItem?.status)
        assertEquals(145000L, repo.walletBalance.value.pendingBalance)
        assertEquals(2450000L + requestedAmount, repo.walletBalance.value.withdrawnBalance)
    }

    @Test
    fun profile_vehicleAndEditUpdatesWorkCorrectly() {
        val repo = createSeededRepository()
        val courier = repo.courier.value
        assertEquals("101", courier.id)
        assertEquals("budi@kube.com", courier.email)
        assertEquals("Truk Box Pendingin (Freezer)", courier.vehicleType)

        repo.updateBiometricEnabled(false)
        assertFalse(repo.courier.value.isBiometricEnabled)

        repo.updateCourierProfile("+62 821 7788 9900", "Jl. Patriot No. 10, Purwokerto")
        assertEquals("+62 821 7788 9900", repo.courier.value.phone)
        assertEquals("Jl. Patriot No. 10, Purwokerto", repo.courier.value.address)
        assertEquals("budi@kube.com", repo.courier.value.email)
    }

    @Test
    fun proofOfDelivery_submissionCreditsCommissionToPendingBalance() {
        val repo = createSeededRepository()
        val initialPending = repo.walletBalance.value.pendingBalance

        repo.submitProofOfDelivery(
            orderId = "ESB-00128",
            recipientName = "Bpk. Bambang",
            photoUri = "https://wopqozbstvtuhxkuciun.supabase.co/storage/v1/object/public/pod-photos/test.jpg",
            confirmedQuantity = 10,
            notes = "Selesai"
        )

        assertEquals(initialPending + 50000L, repo.walletBalance.value.pendingBalance)
        val latestCommission = repo.commissionHistory.value.first()
        assertEquals("ESB-00128", latestCommission.orderId)
        assertEquals(50000L, latestCommission.amount)
    }

    @Test
    fun supabaseService_calculateTodaySummary_isConsistent() {
        val service = CourierSupabaseService.forRepository(createSeededRepository())
        val summary = service.calculateTodaySummary()
        assertEquals("Hari Ini", summary.periodLabel)
        assertTrue(summary.isMathematicallyConsistent)
    }

    @Test
    fun orderPaymentMeta_isCodOrderDetectsCodAndTunaiAccurately() {
        val codByCode = OrderPaymentMeta(
            orderDbId = 1,
            orderCode = "ORD-COD-1",
            paymentMethodCode = "cod",
            paymentMethodName = "Bayar di Tempat",
            totalAmount = 150000L,
            unitPrice = 15000L,
            commissionAmount = 5000L,
            isPaid = false
        )
        assertTrue(codByCode.isCodOrder)

        val codByName = OrderPaymentMeta(
            orderDbId = 2,
            orderCode = "ORD-COD-2",
            paymentMethodCode = "CASH",
            paymentMethodName = "Tunai ke Kurir",
            totalAmount = 90000L,
            unitPrice = 15000L,
            commissionAmount = 3000L,
            isPaid = false
        )
        assertTrue(codByName.isCodOrder)

        val bcaTransfer = OrderPaymentMeta(
            orderDbId = 3,
            orderCode = "ORD-BCA-1",
            paymentMethodCode = "BCA",
            paymentMethodName = "Transfer BCA",
            totalAmount = 120000L,
            unitPrice = 15000L,
            commissionAmount = 4000L,
            isPaid = true
        )
        assertFalse(bcaTransfer.isCodOrder)

        val qrisOrder = OrderPaymentMeta(
            orderDbId = 4,
            orderCode = "ORD-QRIS-1",
            paymentMethodCode = "QRIS",
            paymentMethodName = "QRIS Instan",
            totalAmount = 75000L,
            unitPrice = 15000L,
            commissionAmount = 2500L,
            isPaid = true
        )
        assertFalse(qrisOrder.isCodOrder)
    }

    @Test
    fun supabaseService_getOrderMetaAndSupportRecords_workCorrectly() {
        val repo = OrderRepository()
        val service = CourierSupabaseService.forRepository(repo)

        assertNull(service.getOrderMeta("ORD-NONE"))

        val meta = OrderPaymentMeta(
            orderDbId = 10,
            orderCode = "ORD-COD-10",
            paymentMethodCode = "cod",
            paymentMethodName = "Tunai",
            totalAmount = 200000L,
            unitPrice = 20000L,
            commissionAmount = 10000L,
            isPaid = false
        )
        service.cacheOrderMetaForTest(meta)

        val fetched = service.getOrderMeta("ORD-COD-10")
        assertNotNull(fetched)
        assertTrue(fetched?.isCodOrder == true)
        assertEquals(200000L, fetched?.totalAmount)
        assertEquals(20000L, fetched?.unitPrice)

        val supportContact = SupportContactRecord(
            id = 1,
            warehouseId = 1,
            serviceType = "WhatsApp Dispatcher",
            contactName = "Admin Dispatcher Kube",
            phoneNumber = "0811-2345-6789",
            serviceHours = "24 Jam"
        )
        val bankRecord = CourierBankAccountRecord(
            id = 5,
            courierId = 12,
            bankName = "BCA",
            accountNumber = "8830192831",
            accountHolder = "ANDI KURIR",
            isPrimary = true,
            verificationStatus = "verified"
        )
        service.setSupportContactsForTest(listOf(supportContact))
        assertEquals(1, service.supportContacts.value.size)
        assertEquals("Admin Dispatcher Kube", service.supportContacts.value.first().contactName)
        assertTrue(bankRecord.isPrimary)

        service.logoutCourier()
        assertNull(service.getOrderMeta("ORD-COD-10"))
        assertTrue(service.supportContacts.value.isEmpty())
    }

    @Test
    fun supabaseService_realtimeStreams_reconcileWithdrawalsAndWalletTransactions() = runBlocking {
        val repo = createSeededRepository()
        val initialPending = repo.walletBalance.value.pendingBalance
        val initialWithdrawn = repo.walletBalance.value.withdrawnBalance

        repo.syncWithdrawalsFromStream(
            listOf(
                WithdrawalHistoryItem(
                    id = "WD-99401",
                    bankName = "BCA",
                    accountNumberMasked = "**** **** 2831",
                    recipientName = "BUDI SANTOSO",
                    amount = 500000L,
                    date = "28 Okt 2026, 09:30",
                    status = WithdrawalStatus.SUCCESS,
                    rejectionReason = null
                )
            )
        )

        val syncedItem = repo.withdrawalHistory.value.find { it.id == "WD-99401" }
        assertNotNull(syncedItem)
        assertEquals(WithdrawalStatus.SUCCESS, syncedItem?.status)
        assertEquals((initialPending - 500000L).coerceAtLeast(0L), repo.walletBalance.value.pendingBalance)
        assertEquals(initialWithdrawn + 500000L, repo.walletBalance.value.withdrawnBalance)

        repo.syncCommissionHistoryFromStream(
            listOf(
                CommissionHistoryItem(
                    id = "COM-STREAM-901",
                    orderId = "#ESB-00130",
                    title = "Komisi 15 balok es - Rp75.000",
                    subtitle = "Resto Seafood Bahari (#ESB-00130) · Baru saja",
                    amount = 75000L,
                    isTip = false,
                    timestamp = "Baru saja"
                )
            )
        )

        val syncedTx = repo.commissionHistory.value.find { it.id == "COM-STREAM-901" }
        assertNotNull(syncedTx)
        assertEquals(75000L, syncedTx?.amount)
    }

    @Test
    fun deliveryOrder_codDefaultsAndPriceCalculations_workCorrectly() {
        val defaultOrder = DeliveryOrder(
            id = "ORD-DEF-1",
            customerName = "Warung A",
            address = "Jl. Test 1",
            itemDescription = "5 balok es"
        )
        assertEquals("QRIS", defaultOrder.paymentMethod)
        assertEquals(0L, defaultOrder.totalAmount)
        assertEquals(0L, defaultOrder.unitPrice)
        assertFalse(defaultOrder.isCod)

        val codOrderWithoutExplicitUnitPrice = DeliveryOrder(
            id = "ORD-COD-1",
            customerName = "Warung B",
            address = "Jl. Test 2",
            itemDescription = "10 balok es",
            paymentMethod = "COD",
            totalAmount = 150000L
        )
        assertTrue(codOrderWithoutExplicitUnitPrice.isCod)
        assertEquals(15000L, codOrderWithoutExplicitUnitPrice.effectiveUnitPrice)
        assertEquals(150000L, codOrderWithoutExplicitUnitPrice.effectiveTotalAmount)
    }

    @Test
    fun proofOfDelivery_codOrder_marksPaymentStatusPaidWhenConfirmed() {
        val repo = createSeededRepository()

        val initialOrder = repo.getOrderById("ESB-00128")
        assertNotNull(initialOrder)
        assertTrue(initialOrder?.isCod == true)
        assertEquals("Belum dibayar", initialOrder?.paymentStatus)

        repo.submitProofOfDelivery(
            orderId = "ESB-00128",
            recipientName = "Ibu Sinta",
            photoUri = null,
            confirmedQuantity = 10,
            notes = null,
            isCodPaymentConfirmed = false
        )
        assertEquals("Belum dibayar", repo.getOrderById("ESB-00128")?.paymentStatus)

        val repo2 = createSeededRepository()
        repo2.submitProofOfDelivery(
            orderId = "ESB-00128",
            recipientName = "Ibu Sinta",
            photoUri = "content://pod/photo.jpg",
            confirmedQuantity = 10,
            notes = "Tunai diterima pas",
            isCodPaymentConfirmed = true
        )
        val updated = repo2.getOrderById("ESB-00128")
        assertEquals(DeliveryStatus.DELIVERED, updated?.status)
        assertEquals("Lunas", updated?.paymentStatus)
    }
}
