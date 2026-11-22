package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.graphics.Color
import com.example.core.components.DeliveryHistoryDetailContent
import com.example.core.components.FloatingGlassNavBar
import com.example.core.components.KubeAtmosphericBackground
import com.example.core.components.KubeFeedbackManager
import com.example.core.components.KubeGlassDialog
import com.example.core.components.KubeGlassFeedbackHost
import com.example.core.components.TopCourierBar
import com.example.core.components.WithdrawalDetailContent
import com.example.core.components.kubeLightTextFieldColors
import com.example.core.model.DeliveryOrder
import com.example.core.model.DeliveryStatus
import com.example.core.model.WithdrawalHistoryItem
import com.example.core.model.WithdrawalStatus
import com.example.core.network.CourierNotificationRecord
import com.example.core.network.CourierSessionStorage
import com.example.core.network.CourierSupabaseService
import com.example.core.network.OrderPaymentMeta
import com.example.core.network.SupportContactRecord
import com.example.core.repository.OrderRepository
import com.example.features.dashboard.DashboardScreen
import android.graphics.Bitmap
import com.example.core.components.KubeImageUtils
import com.example.features.delivery.ProofOfDeliveryBottomSheet
import com.example.features.profile.HistoryOrderItemCard
import com.example.features.profile.normalizeWhatsappDigits
import com.example.ui.theme.KubeTheme
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Kube", appName)
  }

  @Test
  fun `completeDeliveryWithPoD marks COD order Lunas when cash collected with Context`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = OrderRepository()
    repo.updateCourierDuty(true)
    repo.syncOrdersFromStream(
      listOf(
        DeliveryOrder(
          id = "ESB-00128",
          customerName = "Toko Es Segar",
          address = "Jl. Melati No. 12",
          itemDescription = "10 balok es",
          paymentStatus = "COD - Tagih Rp 150.000",
          status = DeliveryStatus.ARRIVED,
          paymentMethod = "cod",
          totalAmount = 150000L,
          unitPrice = 15000L
        )
      )
    )
    val service = CourierSupabaseService.forRepository(repo)
    service.cacheOrderMetaForTest(
      OrderPaymentMeta(
        orderDbId = 0, // 0 avoids live network PATCH in unit test while keeping local COD PoD verification active
        orderCode = "ESB-00128",
        paymentMethodCode = "cod",
        paymentMethodName = "Tunai",
        totalAmount = 150000L,
        unitPrice = 15000L,
        commissionAmount = 5000L,
        isPaid = false,
        latitude = -7.4243,
        longitude = 109.2391
      )
    )

    val cachedMeta = service.getOrderMeta("ESB-00128")
    assertEquals(-7.4243, cachedMeta?.latitude ?: 0.0, 0.0001)
    assertEquals(109.2391, cachedMeta?.longitude ?: 0.0, 0.0001)

    val res = service.completeDeliveryWithPoD(
      orderId = "ESB-00128",
      recipientName = "Bpk. Hendra",
      photoUri = "https://wopqozbstvtuhxkuciun.supabase.co/storage/v1/object/public/pod-photos/sample.jpg",
      codCashPhotoUri = "https://wopqozbstvtuhxkuciun.supabase.co/storage/v1/object/public/pod-photos/cod_cash_sample.jpg",
      confirmedQuantity = 10,
      notes = "Uang pas Rp 150.000 diterima",
      context = context,
      isCodCollected = true
    )

    assertTrue(res.isSuccess)
    val order = repo.getOrderById("ESB-00128")
    assertEquals(DeliveryStatus.DELIVERED, order?.status)
    assertEquals("Lunas", order?.paymentStatus)
    assertEquals("Bpk. Hendra", order?.proofOfDelivery?.recipientName)
    assertEquals(10, order?.proofOfDelivery?.confirmedQuantity)
    assertNotNull(order?.proofOfDelivery?.photoUri)
  }

  @Test
  fun `loginCourier rejects invalid email or empty password`() = runBlocking {
    val service = CourierSupabaseService.forRepository(OrderRepository())

    val invalidEmailResult = service.loginCourier("invalid-email", "secret123")
    assertTrue("Invalid email must fail", invalidEmailResult.isFailure)

    val emptyPassResult = service.loginCourier("kurir@kube.com", "")
    assertTrue("Empty password must fail", emptyPassResult.isFailure)
  }

  @Test
  fun `supportContacts StateFlow and WhatsApp number normalization work cleanly`() {
    val service = CourierSupabaseService.forRepository(OrderRepository())
    val contact = SupportContactRecord(
      id = 7,
      warehouseId = 1,
      serviceType = "WhatsApp Dispatcher",
      contactName = "Budi Dispatcher",
      phoneNumber = "0811-2345-6789",
      serviceHours = "07:00 - 21:00 WIB"
    )
    service.setSupportContactsForTest(listOf(contact))

    assertEquals(1, service.supportContacts.value.size)
    assertEquals("6281123456789", normalizeWhatsappDigits(contact.phoneNumber))
    assertEquals("6281299887766", normalizeWhatsappDigits("+62 812-9988-7766"))

    service.logoutCourier()
    assertTrue(service.supportContacts.value.isEmpty())
    assertNull(service.activeCourierProfileId.value)
    assertNotNull(service.warehouseInfo.value)
    assertFalse(service.warehouseInfo.value.name.isBlank())
  }

  @Test
  fun `parseChatMessagesJson and chat validation handle courier_admin and order_chat rooms`() = runBlocking {
    val service = CourierSupabaseService.forRepository(OrderRepository())
    val courierUuid = "49b44450-956e-4d87-b205-47e55755c1cd"
    service.setCurrentUserUuidForTest(courierUuid)

    val json = org.json.JSONArray().apply {
      put(
        org.json.JSONObject().apply {
          put("id", 101)
          put("room_type", "courier_admin")
          put("order_id", org.json.JSONObject.NULL)
          put("courier_user_id", courierUuid)
          put("sender_id", "admin-uuid-1")
          put("sender_role", "admin")
          put("sender_name", "null")
          put("message", "Halo kurir, mohon cek rute")
          put("is_read", false)
          put("created_at", "2026-11-18T09:30:00+07:00")
        }
      )
      put(
        org.json.JSONObject().apply {
          put("id", 102)
          put("room_type", "order_chat")
          put("order_id", 13)
          put("courier_user_id", courierUuid)
          put("sender_id", courierUuid)
          put("sender_role", "courier")
          put("sender_name", "Jono")
          put("message", "Saya sedang menuju lokasi Anda")
          put("is_read", true)
          put("created_at", "2026-11-18T09:35:00+07:00")
        }
      )
    }

    val parsed = service.parseChatMessagesJson(json)
    assertEquals(2, parsed.size)

    val adminMsg = parsed[0]
    assertEquals("courier_admin", adminMsg.roomType)
    assertNull(adminMsg.orderId)
    assertEquals("Admin Kube", adminMsg.senderName)
    assertFalse(adminMsg.isFromMe)
    assertEquals("2026-11-18 09:30", adminMsg.timeLabel)

    val courierMsg = parsed[1]
    assertEquals("order_chat", courierMsg.roomType)
    assertEquals(13, courierMsg.orderId)
    assertEquals("Jono", courierMsg.senderName)
    assertTrue(courierMsg.isFromMe)

    val emptyAdminSend = service.sendAdminChatMessage("   ")
    assertTrue(emptyAdminSend.isFailure)

    val missingOrderSend = service.sendOrderChatMessage("UNKNOWN-ORDER", "Halo")
    assertTrue(missingOrderSend.isFailure)

    service.logoutCourier()
    assertTrue(service.adminChatMessages.value.isEmpty())
    assertTrue(service.orderChatMessages.value.isEmpty())
  }

  @Test
  fun `CourierSessionStorage persists Remember Me session and clears on logout`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    CourierSessionStorage.clearSession(context)

    CourierSessionStorage.saveSession(
      context = context,
      rememberMe = true,
      email = "jono@kube.id",
      accessToken = "acc_123",
      refreshToken = "ref_456",
      userId = "uuid-999",
      courierId = "12"
    )

    val saved = CourierSessionStorage.getSavedSession(context)
    assertNotNull(saved)
    assertTrue(saved?.rememberMe == true)
    assertEquals("jono@kube.id", saved?.email)
    assertEquals("acc_123", saved?.accessToken)
    assertEquals("ref_456", saved?.refreshToken)
    assertEquals("uuid-999", saved?.userId)
    assertEquals("12", saved?.courierId)

    val service = CourierSupabaseService.forRepository(OrderRepository())
    service.logoutCourier(context)
    assertNull(CourierSessionStorage.getSavedSession(context))

    CourierSessionStorage.saveSession(
      context = context,
      rememberMe = false,
      email = "jono@kube.id",
      accessToken = "acc_123",
      refreshToken = "ref_456",
      userId = "uuid-999",
      courierId = "12"
    )
    assertNull(CourierSessionStorage.getSavedSession(context))
  }

  @Test
  fun `submitBankAccountRequest validates inputs and markNotificationAsRead ignores non-numeric id`() = runBlocking {
    val service = CourierSupabaseService.forRepository(OrderRepository())

    val blankRes = service.submitBankAccountRequest(
      bankName = "",
      accountNumber = "1234567890",
      accountHolder = "Jono"
    )
    assertTrue(blankRes.isFailure)

    val unlinkedRes = service.submitBankAccountRequest(
      bankName = "BCA",
      accountNumber = "1234567890",
      accountHolder = "Jono"
    )
    assertTrue(unlinkedRes.isFailure)

    // Non-numeric ID safely no-ops without throwing
    service.markNotificationAsRead("invalid-id")
  }

  @Test
  fun `executePostWithDetail returns diagnostic failure and chat send accepts Context`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val service = CourierSupabaseService.forRepository(OrderRepository())

    val unreachableResult = service.executePostWithDetail(
      url = "http://127.0.0.1:1/rest/v1/chat_messages",
      jsonPayload = "{}",
      context = context
    )
    assertTrue(unreachableResult.isFailure)
    assertTrue(unreachableResult.exceptionOrNull()?.message?.startsWith("Gagal (") == true)

    val blankAdmin = service.sendAdminChatMessage("   ", context = context)
    assertTrue(blankAdmin.isFailure)

    val invalidOrder = service.sendOrderChatMessage("NO-ORDER", "Halo", context = context)
    assertTrue(invalidOrder.isFailure)
  }

  @Test
  fun `offDuty courier is blocked from updateDeliveryProgress and completeDeliveryWithPoD`() = runBlocking {
    val repo = OrderRepository()
    repo.updateCourierDuty(false)
    repo.syncOrdersFromStream(
      listOf(
        DeliveryOrder(
          id = "ESB-OFFDUTY-1",
          customerName = "Warung Es Jaya",
          address = "Jl. Merdeka No. 5",
          itemDescription = "5 balok es",
          paymentStatus = "QRIS",
          status = DeliveryStatus.NEW
        )
      )
    )
    val service = CourierSupabaseService.forRepository(repo)

    val progressRes = service.updateDeliveryProgress("ESB-OFFDUTY-1", DeliveryStatus.IN_TRANSIT)
    assertTrue(progressRes.isFailure)
    assertTrue(progressRes.exceptionOrNull()?.message?.contains("Off-Duty") == true)

    val podRes = service.completeDeliveryWithPoD(
      orderId = "ESB-OFFDUTY-1",
      recipientName = "Ibu Ani",
      photoUri = "sample.jpg",
      confirmedQuantity = 5,
      notes = null
    )
    assertTrue(podRes.isFailure)
    assertTrue(podRes.exceptionOrNull()?.message?.contains("Off-Duty") == true)

    val toggled = service.toggleOnDuty(isOnDuty = true)
    assertTrue(toggled.isSuccess)
    assertTrue(repo.courier.value.isActiveDuty)
    assertEquals("Selamat Bertugas", repo.courier.value.statusText)

    val activeProgress = service.updateDeliveryProgress("ESB-OFFDUTY-1", DeliveryStatus.IN_TRANSIT)
    assertTrue(activeProgress.isSuccess)
  }

  @Test
  fun `applySyncedNotifications tracks unread IDs and clears on logout`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val service = CourierSupabaseService.forRepository(OrderRepository())
    service.initAppContext(context)

    val batch = listOf(
      CourierNotificationRecord(
        id = "101",
        title = "Tugas Pengiriman Baru",
        message = "Anda mendapat tugas antar ke Cafe Kopi Kenangan.",
        time = "Baru saja",
        category = "order",
        isUnread = true
      ),
      CourierNotificationRecord(
        id = "102",
        title = "Komisi Masuk",
        message = "Komisi Rp 15.000 telah cair.",
        time = "10:00",
        category = "wallet",
        isUnread = false
      )
    )

    service.applySyncedNotifications(batch)
    assertTrue(service.hasNotifiedForTest("101"))
    assertFalse(service.hasNotifiedForTest("102"))
    assertEquals(2, service.notifications.value.size)

    // Polling ulang tidak membuang ID yang sudah ternotifikasi
    service.applySyncedNotifications(batch)
    assertTrue(service.hasNotifiedForTest("101"))

    service.logoutCourier(context)
    assertFalse(service.hasNotifiedForTest("101"))
    assertTrue(service.notifications.value.isEmpty())
  }

  @Test
  fun `TopCourierBar hides search button and shows unread notification badge`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    CourierSupabaseService.instance.initAppContext(context)
    CourierSupabaseService.instance.applySyncedNotifications(
      listOf(
        CourierNotificationRecord(
          id = "201",
          title = "Tugas Baru",
          message = "Ambil order ESB-201",
          time = "Baru saja",
          category = "order",
          isUnread = true
        ),
        CourierNotificationRecord(
          id = "202",
          title = "ACC Rekening",
          message = "Rekening BCA Anda disetujui Admin",
          time = "Baru saja",
          category = "wallet",
          isUnread = true
        )
      )
    )

    composeTestRule.setContent {
      KubeTheme {
        TopCourierBar(name = "Budi Kurir")
      }
    }

    composeTestRule.onNodeWithTag("search_button").assertDoesNotExist()
    composeTestRule.onNodeWithTag("notification_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("notification_unread_badge")
      .assertIsDisplayed()
      .assertTextEquals("2")

    CourierSupabaseService.instance.logoutCourier(context)
  }

  @Test
  fun `withdrawal status labels and confirmWithdrawalReceived validation work correctly`() = runBlocking {
    assertEquals("Menunggu Admin", WithdrawalStatus.WAITING.label)
    assertEquals("Konfirmasi Dana", WithdrawalStatus.PROCESSING.label)
    assertEquals("Berhasil", WithdrawalStatus.SUCCESS.label)

    val service = CourierSupabaseService.forRepository(OrderRepository())
    val blankRes = service.confirmWithdrawalReceived("   ")
    assertTrue(blankRes.isFailure)
  }

  @Test
  fun `WithdrawalDetailContent displays transfer proof and triggers confirmation`() {
    var proofOpened = false
    var confirmedClicked = false
    val item = WithdrawalHistoryItem(
      id = "WD-940987",
      bankName = "BCA",
      accountNumberMasked = "**** **** 2831",
      recipientName = "Maryono",
      amount = 350000L,
      date = "19 Nov 2026",
      status = WithdrawalStatus.PROCESSING,
      transferProofUrl = "content://kube/proof/wd-940987.jpg",
      processedAt = "19 Nov 2026, 10:30",
      processedByName = "Admin Rina"
    )

    composeTestRule.setContent {
      KubeTheme {
        WithdrawalDetailContent(
          item = item,
          proofUrl = item.transferProofUrl,
          showConfirmButton = true,
          isConfirming = false,
          errorMessage = null,
          onOpenFullScreenProof = { proofOpened = true },
          onConfirmClick = { confirmedClicked = true }
        )
      }
    }

    composeTestRule.onNodeWithTag("detail_withdrawal_id").assertTextEquals("WD-940987")
    composeTestRule.onNodeWithText("Konfirmasi Dana").assertIsDisplayed()
    composeTestRule.onNodeWithText("Admin Rina").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("img_transfer_proof").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("img_transfer_proof").performClick()
    assertTrue(proofOpened)

    composeTestRule.onNodeWithTag("btn_confirm_withdrawal_received")
      .performScrollTo()
      .assertIsDisplayed()
      .performClick()
    assertTrue(confirmedClicked)
  }

  @Test
  fun `HistoryOrderItemCard displays complete metadata and triggers onClick`() {
    var clicked = false
    val order = DeliveryOrder(
      id = "KUBE-2026",
      customerName = "Kopi Senja Purwokerto",
      address = "Jl. Jenderal Sudirman No. 88, Purwokerto",
      itemDescription = "10x Es Kristal Tube 5kg, 2x Es Serut 10kg",
      paymentStatus = "Lunas",
      status = DeliveryStatus.DELIVERED,
      totalAmount = 240000L,
      paymentMethod = "Transfer BCA",
      assignedTime = "19 Nov 2026, 09:00"
    )
    val meta = OrderPaymentMeta(
      orderDbId = 2026,
      orderCode = "KUBE-2026",
      paymentMethodCode = "bank_transfer",
      paymentMethodName = "Transfer BCA",
      totalAmount = 240000L,
      unitPrice = 20000L,
      commissionAmount = 15000L,
      isPaid = true,
      recipientName = "Barista Andi",
      podPhotoUrl = "content://kube/pod/kube-2026.jpg",
      podStatus = "verified",
      deliveredAtFormatted = "19 Nov 2026, 14:25",
      deliveryNotes = "Taruh di freezer belakang"
    )

    composeTestRule.setContent {
      KubeTheme {
        HistoryOrderItemCard(
          order = order,
          meta = meta,
          onClick = { clicked = true }
        )
      }
    }

    composeTestRule.onNodeWithText("Kopi Senja Purwokerto").assertIsDisplayed()
    composeTestRule.onNodeWithText("#KUBE-2026").assertIsDisplayed()
    composeTestRule.onNodeWithText("Terkirim (Selesai)").assertIsDisplayed()
    composeTestRule.onNodeWithText("10x Es Kristal Tube 5kg, 2x Es Serut 10kg").assertIsDisplayed()
    composeTestRule.onNodeWithText("Transfer BCA").assertIsDisplayed()
    composeTestRule.onNodeWithText("Rp 240.000").assertIsDisplayed()
    composeTestRule.onNodeWithText("+ Rp 15.000").assertIsDisplayed()
    composeTestRule.onNodeWithText("Diterima: Barista Andi").assertIsDisplayed()
    composeTestRule.onNodeWithText("19 Nov 2026, 14:25").assertIsDisplayed()

    composeTestRule.onNodeWithTag("history_card_KUBE-2026").performClick()
    assertTrue(clicked)
  }

  @Test
  fun `DeliveryHistoryDetailContent displays delivery notes and PoD photo zoom trigger`() {
    var fullScreenOpened = false
    val order = DeliveryOrder(
      id = "KUBE-2027",
      customerName = "Cafe Ombak",
      address = "Jl. Gerilya No. 15",
      phoneNumber = "081299988877",
      itemDescription = "5x Es Batu Kristal 10kg",
      paymentStatus = "COD - Lunas",
      status = DeliveryStatus.DELIVERED,
      totalAmount = 100000L,
      paymentMethod = "cod",
      assignedTime = "19 Nov 2026, 11:00"
    )
    val meta = OrderPaymentMeta(
      orderDbId = 2027,
      orderCode = "KUBE-2027",
      paymentMethodCode = "cod",
      paymentMethodName = "COD Tunai",
      totalAmount = 100000L,
      unitPrice = 20000L,
      commissionAmount = 10000L,
      isPaid = true,
      recipientName = "Sari",
      podPhotoUrl = "content://kube/pod/kube-2027.jpg",
      podStatus = "verified",
      deliveredAtFormatted = "19 Nov 2026, 15:10",
      deliveryNotes = "Uang COD sudah diserahkan pas"
    )

    composeTestRule.setContent {
      KubeTheme {
        DeliveryHistoryDetailContent(
          order = order,
          meta = meta,
          onOpenFullScreenProof = { fullScreenOpened = true }
        )
      }
    }

    composeTestRule.onNodeWithTag("detail_history_customer").assertTextEquals("Cafe Ombak")
    composeTestRule.onNodeWithText("Uang COD sudah diserahkan pas").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("img_history_pod_photo").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("img_history_pod_photo").performClick()
    assertTrue(fullScreenOpened)
  }

  @Test
  fun `kubeLightTextFieldColors enforces dark readable input text even in dark theme`() {
    var capturedFocusedTextColor: Color? = null
    var capturedUnfocusedTextColor: Color? = null
    var capturedDisabledTextColor: Color? = null
    var capturedFocusedContainerColor: Color? = null
    var capturedUnfocusedContainerColor: Color? = null

    composeTestRule.setContent {
      KubeTheme(darkTheme = true) {
        val colors = kubeLightTextFieldColors()
        capturedFocusedTextColor = colors.focusedTextColor
        capturedUnfocusedTextColor = colors.unfocusedTextColor
        capturedDisabledTextColor = colors.disabledTextColor
        capturedFocusedContainerColor = colors.focusedContainerColor
        capturedUnfocusedContainerColor = colors.unfocusedContainerColor
        OutlinedTextField(
          value = "Test Input",
          onValueChange = {},
          colors = colors
        )
      }
    }

    assertEquals(Color(0xFF0F172A), capturedFocusedTextColor)
    assertEquals(Color(0xFF0F172A), capturedUnfocusedTextColor)
    assertEquals(Color(0xFF334155), capturedDisabledTextColor)
    assertEquals(Color.White, capturedFocusedContainerColor)
    assertEquals(Color(0xFFF8FAFC), capturedUnfocusedContainerColor)
  }

  @Test
  fun `FloatingGlassNavBar displays expanding white pill for active tab and handles selection`() {
    var selectedTabIndex = 0
    composeTestRule.setContent {
      KubeTheme {
        FloatingGlassNavBar(
          selectedIndex = selectedTabIndex,
          onSelectTab = { selectedTabIndex = it }
        )
      }
    }

    composeTestRule.onNodeWithTag("floating_glass_nav_bar").assertIsDisplayed()
    composeTestRule.onNodeWithText("Home").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_tab_delivery").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_tab_map").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_tab_wallet").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_tab_profile").assertIsDisplayed()

    composeTestRule.onNodeWithTag("nav_tab_wallet").performClick()
    assertEquals(3, selectedTabIndex)
  }

  @Test
  fun `DashboardScreen displays atmospheric glass cards and dynamic active delivery summary`() {
    val repo = OrderRepository()
    repo.syncOrdersFromStream(
      listOf(
        DeliveryOrder(
          id = "KUBE-101",
          customerName = "Kopi Hujan",
          address = "Jl. Kampus No. 10, Purwokerto",
          subDistrict = "Purwokerto Utara",
          itemDescription = "4x Es Kristal 5kg",
          paymentStatus = "Lunas",
          status = DeliveryStatus.IN_TRANSIT,
          estimatedTimeMinutes = 18
        ),
        DeliveryOrder(
          id = "KUBE-102",
          customerName = "Warung Es Jaya",
          address = "Jl. Bank No. 5",
          itemDescription = "2x Es Kristal 5kg",
          paymentStatus = "Lunas",
          status = DeliveryStatus.DELIVERED
        )
      )
    )

    composeTestRule.setContent {
      KubeTheme {
        KubeAtmosphericBackground {
          DashboardScreen(
            repository = repo,
            onNavigateToDelivery = {},
            onNavigateToProfile = {}
          )
        }
      }
    }

    composeTestRule.onNodeWithTag("jobdesk_card_tugas_baru").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("Menunggu diambil").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("jobdesk_card_sedang_diantar").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("4 paket • Est. 18 mnt").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("Purwokerto Utara").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("jobdesk_card_gagal_retur").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("0 tugas").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("performance_card").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("performance_summary_pill").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun `KubeGlassFeedbackHost displays and dismisses Liquid Glass banners from KubeFeedbackManager`() {
    KubeFeedbackManager.dismiss()
    composeTestRule.setContent {
      KubeTheme {
        KubeGlassFeedbackHost {
          androidx.compose.foundation.layout.Box {}
        }
      }
    }

    KubeFeedbackManager.showError(
      "Kata Sandi Salah",
      "Kata sandi yang Anda masukkan tidak sesuai. Periksa kembali lalu coba lagi."
    )
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("kube_feedback_banner").assertIsDisplayed()
    composeTestRule.onNodeWithText("Kata Sandi Salah").assertIsDisplayed()

    composeTestRule.onNodeWithTag("kube_feedback_dismiss").performClick()
    composeTestRule.waitForIdle()
    assertNull(KubeFeedbackManager.currentEvent.value)
  }

  @Test
  fun `KubeGlassDialog renders logout confirmation with high contrast and handles actions`() {
    var confirmed = false
    var dismissed = false

    composeTestRule.setContent {
      KubeTheme {
        KubeGlassDialog(
          onDismiss = { dismissed = true },
          icon = Icons.AutoMirrored.Rounded.Logout,
          iconTint = Color(0xFFDC2626),
          title = "Konfirmasi Keluar",
          message = "Apakah Anda yakin ingin keluar dari akun kurir? Pastikan seluruh pesanan aktif telah diperbarui.",
          confirmText = "Ya, Keluar",
          confirmButtonColor = Color(0xFFDC2626),
          onConfirm = { confirmed = true },
          dismissText = "Batal"
        )
      }
    }

    composeTestRule.onNodeWithTag("kube_glass_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithText("Konfirmasi Keluar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("kube_dialog_confirm").performClick()
    assertTrue(confirmed)

    composeTestRule.onNodeWithTag("kube_dialog_dismiss").performClick()
    assertTrue(dismissed)
  }

  @Test
  fun `KubeImageUtils creates FileProvider camera URI and converts raster bitmap to SVG bytes`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val cameraUri = KubeImageUtils.createTempCameraImageUri(context, prefix = "test_pod")
    assertEquals("content", cameraUri.scheme)
    assertTrue(cameraUri.authority?.endsWith(".fileprovider") == true)

    val tempImg = File(context.cacheDir, "sample_test_pod.png")
    val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
    bitmap.eraseColor(android.graphics.Color.CYAN)
    FileOutputStream(tempImg).use { out ->
      bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }

    val svgBytes = KubeImageUtils.convertAnyImageToSvgBytes(context, tempImg.absolutePath)
    assertNotNull(svgBytes)
    val svgText = svgBytes!!.toString(Charsets.UTF_8)
    assertTrue(svgText.contains("<svg"))
    assertTrue(svgText.contains("data:image/jpeg;base64,"))
    tempImg.delete()
  }

  @Test
  fun `DeliveryHistoryDetailContent separates COD cash proof card and strips raw COD URL from notes`() {
    var codFullscreenOpened = false
    val order = DeliveryOrder(
      id = "KUBE-3001",
      customerName = "Kedai Es Nusantara",
      address = "Jl. Jenderal Sudirman No. 88",
      itemDescription = "8x Es Kristal 5kg",
      paymentStatus = "COD - Lunas",
      status = DeliveryStatus.DELIVERED,
      totalAmount = 160000L,
      paymentMethod = "cod",
      assignedTime = "20 Nov 2026, 09:00"
    )
    val meta = OrderPaymentMeta(
      orderDbId = 3001,
      orderCode = "KUBE-3001",
      paymentMethodCode = "cod",
      paymentMethodName = "COD Tunai",
      totalAmount = 160000L,
      unitPrice = 20000L,
      commissionAmount = 12000L,
      isPaid = true,
      recipientName = "Budi",
      podPhotoUrl = "https://example.supabase.co/storage/v1/object/public/pod-photos/pod_3001.svg",
      codCashPhotoUrl = "https://example.supabase.co/storage/v1/object/public/pod-photos/cod_3001.svg",
      podStatus = "verified",
      deliveredAtFormatted = "20 Nov 2026, 09:30",
      deliveryNotes = "Bukti Uang COD: https://example.supabase.co/storage/v1/object/public/pod-photos/cod_3001.svg"
    )

    composeTestRule.setContent {
      KubeTheme {
        DeliveryHistoryDetailContent(
          order = order,
          meta = meta,
          onOpenFullScreenCodProof = { codFullscreenOpened = true }
        )
      }
    }

    composeTestRule.onNodeWithTag("detail_history_notes_card").performScrollTo()
    composeTestRule.onNodeWithText("Tidak ada catatan pengantaran").assertIsDisplayed()
    composeTestRule.onNodeWithTag("card_history_cod_cash").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("Bukti Uang Tunai (COD)").assertIsDisplayed()
    composeTestRule.onNodeWithText("Lunas Tunai").assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_zoom_cod_photo").performScrollTo().performClick()
    assertTrue(codFullscreenOpened)
  }

  @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
  @Test
  fun `ProofOfDeliveryBottomSheet displays Camera and Gallery buttons without sample button`() {
    val codOrder = DeliveryOrder(
      id = "KUBE-4001",
      customerName = "Warung Kopi Dingin",
      address = "Jl. Pemuda No. 45",
      itemDescription = "6x Es Batu Kristal 5kg",
      paymentStatus = "COD - Tagih Rp 120.000",
      status = DeliveryStatus.ARRIVED,
      totalAmount = 120000L,
      paymentMethod = "cod"
    )

    composeTestRule.setContent {
      KubeTheme {
        ProofOfDeliveryBottomSheet(
          order = codOrder,
          orderMeta = null,
          onDismissRequest = {},
          onSubmitProof = { _, _, _, _, _, _ -> }
        )
      }
    }

    composeTestRule.onNodeWithTag("btn_pod_camera").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_pod_gallery").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_cod_camera").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_cod_gallery").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("Gunakan Sampel").assertDoesNotExist()
  }
}
