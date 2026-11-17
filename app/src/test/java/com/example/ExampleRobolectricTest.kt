package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.model.DeliveryOrder
import com.example.core.model.DeliveryStatus
import com.example.core.network.CourierSupabaseService
import com.example.core.network.OrderPaymentMeta
import com.example.core.network.SupportContactRecord
import com.example.core.repository.OrderRepository
import com.example.features.profile.normalizeWhatsappDigits
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

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
        isPaid = false
      )
    )

    val res = service.completeDeliveryWithPoD(
      orderId = "ESB-00128",
      recipientName = "Bpk. Hendra",
      photoUri = "https://wopqozbstvtuhxkuciun.supabase.co/storage/v1/object/public/pod-photos/sample.jpg",
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
}
