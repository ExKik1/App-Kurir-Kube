package com.example.core.network

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.core.components.KubeFeedbackManager
import com.example.core.model.CommissionHistoryItem
import com.example.core.model.Courier
import com.example.core.model.DeliveryOrder
import com.example.core.model.DeliveryStatus
import com.example.core.model.ProofOfDelivery
import com.example.core.model.WalletBalance
import com.example.core.model.WithdrawalHistoryItem
import com.example.core.model.WithdrawalStatus
import com.example.core.model.maskAccountNumber
import com.example.core.repository.DailyStatsProvider
import com.example.core.repository.DailyStatsState
import com.example.core.repository.OrderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

data class CourierAuthSession(
    val userId: String,
    val courierId: String,
    val email: String,
    val role: String,
    val accountStatus: String,
    val accessToken: String? = null
)

data class CourierNotificationRecord(
    val id: String,
    val title: String,
    val message: String,
    val time: String,
    val category: String,
    val isUnread: Boolean = false
)

data class WarehouseInfo(
    val id: String = "1",
    val name: String = "Pabrik Es Kube Banyumas",
    val address: String = "Depot Utama Kube",
    val operationalHours: String = "24 Jam Non-Stop",
    val facilities: String = "Cold Storage & Loading Dock Es",
    val latitude: Double = -7.4245,
    val longitude: Double = 109.2300
)

data class SupportContactRecord(
    val id: Int,
    val warehouseId: Int?,
    val serviceType: String,
    val contactName: String,
    val phoneNumber: String,
    val serviceHours: String
)

data class CourierBankAccountRecord(
    val id: Int,
    val courierId: Int,
    val bankName: String,
    val accountNumber: String,
    val accountHolder: String,
    val isPrimary: Boolean,
    val verificationStatus: String
)

data class OrderPaymentMeta(
    val orderDbId: Int,
    val orderCode: String,
    val paymentMethodCode: String, // e.g., "cod", "BCA", "QRIS"
    val paymentMethodName: String, // e.g., "Tunai", "Transfer BCA", "QRIS Instan"
    val totalAmount: Long,
    val unitPrice: Long,
    val commissionAmount: Long,
    val isPaid: Boolean,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val recipientName: String = "",
    val podPhotoUrl: String? = null,
    val codCashPhotoUrl: String? = null,
    val podStatus: String = "approved",
    val deliveredAtFormatted: String = "",
    val deliveryNotes: String? = null
) {
    val isCodOrder: Boolean
        get() = paymentMethodCode.equals("cod", ignoreCase = true) ||
                paymentMethodName.contains("tunai", ignoreCase = true)
}

data class ChatMessageRecord(
    val id: Int,
    val roomType: String, // "courier_admin" atau "order_chat"
    val orderId: Int?,
    val courierUserId: String,
    val senderId: String,
    val senderRole: String, // "courier", "admin", "consumer"
    val senderName: String,
    val message: String,
    val isRead: Boolean,
    val timeLabel: String,
    val isFromMe: Boolean
)

class CourierSupabaseService private constructor(
    private val repository: OrderRepository = OrderRepository.instance
) {

    companion object {
        private const val TAG = "CourierSupabaseService"
        const val SUPABASE_URL = "https://wopqozbstvtuhxkuciun.supabase.co"
        const val SUPABASE_ANON_KEY = "sb_publishable_0mVVCciTLSOAi0j5FPdePQ_kc8UvQBC"
        const val POD_STORAGE_BUCKET = "pod-photos"
        private const val REALTIME_POLL_INTERVAL_MS = 4000L

        private val sharedOkHttpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .build()
        }

        val instance: CourierSupabaseService by lazy { CourierSupabaseService() }

        fun forRepository(repository: OrderRepository): CourierSupabaseService {
            return if (repository === OrderRepository.instance) {
                instance
            } else {
                CourierSupabaseService(repository)
            }
        }
    }

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val okHttpClient: OkHttpClient get() = sharedOkHttpClient

    @Volatile private var appContext: Context? = null
    @Volatile private var currentAccessToken: String? = null
    @Volatile private var currentUserUuid: String = ""
    @Volatile private var currentCourierProfileId: Int = 0

    private val notifiedIds: MutableSet<String> = ConcurrentHashMap.newKeySet()

    private val _activeCourierProfileId = MutableStateFlow<String?>(null)
    val activeCourierProfileId: StateFlow<String?> = _activeCourierProfileId.asStateFlow()

    private val realtimeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val isRealtimeSyncStarted = AtomicBoolean(false)

    // Cache metadata order (kode order -> detail DB, harga, metode COD, komisi)
    private val orderMetaByCode = ConcurrentHashMap<String, OrderPaymentMeta>()
    private val deliveryIdByOrderDbId = ConcurrentHashMap<Int, Int>()

    private val _warehouseInfo = MutableStateFlow(WarehouseInfo())
    val warehouseInfo: StateFlow<WarehouseInfo> = _warehouseInfo.asStateFlow()

    private val _notifications = MutableStateFlow<List<CourierNotificationRecord>>(emptyList())
    val notifications: StateFlow<List<CourierNotificationRecord>> = _notifications.asStateFlow()

    fun initAppContext(context: Context) {
        appContext = context.applicationContext
    }

    private val _supportContacts = MutableStateFlow<List<SupportContactRecord>>(emptyList())
    val supportContacts: StateFlow<List<SupportContactRecord>> = _supportContacts.asStateFlow()

    private val _bankAccounts = MutableStateFlow<List<CourierBankAccountRecord>>(emptyList())
    val bankAccounts: StateFlow<List<CourierBankAccountRecord>> = _bankAccounts.asStateFlow()

    private val _adminChatMessages = MutableStateFlow<List<ChatMessageRecord>>(emptyList())
    val adminChatMessages: StateFlow<List<ChatMessageRecord>> = _adminChatMessages.asStateFlow()

    private val _orderChatMessages = MutableStateFlow<Map<Int, List<ChatMessageRecord>>>(emptyMap())
    val orderChatMessages: StateFlow<Map<Int, List<ChatMessageRecord>>> = _orderChatMessages.asStateFlow()

    fun getOrderMeta(orderCode: String): OrderPaymentMeta? = orderMetaByCode[orderCode]

    fun getOrderDbId(orderCode: String): Int? = orderMetaByCode[orderCode]?.orderDbId

    internal fun cacheOrderMetaForTest(meta: OrderPaymentMeta) {
        orderMetaByCode[meta.orderCode] = meta
    }

    internal fun setSupportContactsForTest(contacts: List<SupportContactRecord>) {
        _supportContacts.value = contacts
    }

    internal fun setCurrentUserUuidForTest(userUuid: String) {
        currentUserUuid = userUuid
    }

    fun logoutCourier(context: Context? = null) {
        if (context != null) {
            CourierSessionStorage.clearSession(context)
        }
        currentAccessToken = null
        currentUserUuid = ""
        currentCourierProfileId = 0
        _activeCourierProfileId.value = null
        orderMetaByCode.clear()
        deliveryIdByOrderDbId.clear()
        notifiedIds.clear()
        _notifications.value = emptyList()
        _supportContacts.value = emptyList()
        _bankAccounts.value = emptyList()
        _adminChatMessages.value = emptyList()
        _orderChatMessages.value = emptyMap()
        _warehouseInfo.value = WarehouseInfo()
        repository.resetAllData()
    }

    private fun ensureRealtimeSyncStarted() {
        if (!isRealtimeSyncStarted.compareAndSet(false, true)) return
        realtimeScope.launch {
            while (isActive) {
                if (currentUserUuid.isNotBlank() || currentCourierProfileId > 0) {
                    runCatching {
                        syncAllTablesOnce()
                    }.onFailure { err ->
                        Log.w(TAG, "Realtime sync cycle warning: ${err.message}")
                    }
                }
                delay(REALTIME_POLL_INTERVAL_MS)
            }
        }
    }

    suspend fun syncAllTablesOnce() = withContext(Dispatchers.IO) {
        if (currentUserUuid.isNotBlank()) {
            fetchAndSyncCourierProfileByUuid(currentUserUuid)
            fetchAndSyncOrdersAndDeliveries(currentUserUuid, currentCourierProfileId)
        }
        if (currentCourierProfileId > 0) {
            fetchAndSyncBankAccounts(currentCourierProfileId)
            fetchAndSyncWalletAndTransactions(currentCourierProfileId)
            fetchAndSyncWithdrawals(currentCourierProfileId)
            fetchAndSyncNotifications(currentCourierProfileId)
        }
        fetchAndSyncSupportContacts()
    }

    // =========================================================================
    // 1. AUTENTIKASI & VALIDASI ROLE KURIR (users + courier_profiles)
    // =========================================================================

    suspend fun loginCourier(
        email: String,
        password: String,
        rememberMe: Boolean = false,
        context: Context? = null
    ): Result<CourierAuthSession> =
        withContext(Dispatchers.IO) {
            val cleanEmail = email.trim()
            if (cleanEmail.isEmpty() || !cleanEmail.contains("@")) {
                return@withContext Result.failure(IllegalArgumentException("Format email tidak valid."))
            }
            if (password.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Kata sandi wajib diisi."))
            }

            try {
                val authPayload = JSONObject().apply {
                    put("email", cleanEmail)
                    put("password", password)
                }.toString()

                val authRequest = Request.Builder()
                    .url("$SUPABASE_URL/auth/v1/token?grant_type=password")
                    .addHeader("apikey", SUPABASE_ANON_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(authPayload.toRequestBody(jsonMediaType))
                    .build()

                okHttpClient.newCall(authRequest).execute().use { authResponse ->
                    val authBody = authResponse.body?.string().orEmpty()
                    if (!authResponse.isSuccessful || authBody.isBlank()) {
                        val encodedEmail = Uri.encode(cleanEmail)
                        val usersArr = runCatching {
                            executeGetJsonArray("$SUPABASE_URL/rest/v1/users?select=id,role_id,account_status&email=eq.$encodedEmail")
                        }.getOrNull()

                        val (errTitle, errMessage) = when {
                            usersArr == null || usersArr.length() == 0 -> {
                                "Email Tidak Terdaftar" to "Email '$cleanEmail' belum terdaftar di sistem kurir Kube."
                            }
                            else -> {
                                val uRow = usersArr.optJSONObject(0)
                                val roleId = uRow?.optInt("role_id", -1) ?: -1
                                val accStatus = uRow?.optString("account_status", "active").orEmpty()
                                when {
                                    roleId != 3 -> "Akses Akun Ditolak" to "Akun ini bukan akun kurir resmi Kube."
                                    !accStatus.equals("active", ignoreCase = true) ->
                                        "Akun Dinonaktifkan" to "Status akun Anda saat ini '$accStatus'. Hubungi Admin Kube."
                                    else -> "Kata Sandi Salah" to "Kata sandi yang Anda masukkan tidak sesuai. Periksa kembali lalu coba lagi."
                                }
                            }
                        }

                        KubeFeedbackManager.showError(errTitle, errMessage)
                        return@withContext Result.failure(IllegalStateException(errMessage))
                    }

                    val json = JSONObject(authBody)
                    val accessToken = json.optString("access_token")
                    val refreshToken = json.optString("refresh_token")
                    val authUserId = json.optJSONObject("user")?.optString("id").orEmpty()
                    currentAccessToken = accessToken

                    val sessionResult = fetchAndValidateCourierProfile(
                        email = cleanEmail,
                        authUserId = authUserId,
                        accessToken = accessToken
                    )

                    sessionResult.onSuccess { session ->
                        if (context != null) {
                            CourierSessionStorage.saveSession(
                                context = context,
                                rememberMe = rememberMe,
                                email = cleanEmail,
                                accessToken = accessToken,
                                refreshToken = refreshToken,
                                userId = session.userId,
                                courierId = session.courierId
                            )
                        }
                        ensureRealtimeSyncStarted()
                        realtimeScope.launch { syncAllTablesOnce() }
                    }
                    return@withContext sessionResult
                }
            } catch (e: Exception) {
                Log.e(TAG, "loginCourier exception", e)
                return@withContext Result.failure(
                    IllegalStateException("Gagal terhubung ke Supabase: ${e.message}")
                )
            }
        }

    /**
     * Dipanggil saat SplashScreen dibuka:
     * Jika Remember Me sebelumnya dicentang, memperbarui token via Supabase `refresh_token`
     * dan langsung masuk ke Dashboard tanpa perlu input email & password ulang.
     */
    suspend fun restoreSessionIfRemembered(context: Context): Result<CourierAuthSession> =
        withContext(Dispatchers.IO) {
            val saved = CourierSessionStorage.getSavedSession(context)
                ?: return@withContext Result.failure(IllegalStateException("Tidak ada sesi tersimpan."))

            try {
                val refreshPayload = JSONObject().apply {
                    put("refresh_token", saved.refreshToken)
                }.toString()

                val req = Request.Builder()
                    .url("$SUPABASE_URL/auth/v1/token?grant_type=refresh_token")
                    .addHeader("apikey", SUPABASE_ANON_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(refreshPayload.toRequestBody(jsonMediaType))
                    .build()

                okHttpClient.newCall(req).execute().use { res ->
                    val body = res.body?.string().orEmpty()
                    if (!res.isSuccessful || body.isBlank()) {
                        CourierSessionStorage.clearSession(context)
                        return@withContext Result.failure(IllegalStateException("Sesi berakhir, silakan login ulang."))
                    }

                    val json = JSONObject(body)
                    val newAccessToken = json.optString("access_token")
                    val newRefreshToken = json.optString("refresh_token").ifBlank { saved.refreshToken }
                    val authUserId = json.optJSONObject("user")?.optString("id").orEmpty().ifBlank { saved.userId }
                    currentAccessToken = newAccessToken

                    val sessionResult = fetchAndValidateCourierProfile(
                        email = saved.email,
                        authUserId = authUserId,
                        accessToken = newAccessToken
                    )

                    sessionResult.onSuccess { session ->
                        CourierSessionStorage.saveSession(
                            context = context,
                            rememberMe = true,
                            email = saved.email,
                            accessToken = newAccessToken,
                            refreshToken = newRefreshToken,
                            userId = session.userId,
                            courierId = session.courierId
                        )
                        ensureRealtimeSyncStarted()
                        realtimeScope.launch { syncAllTablesOnce() }
                    }.onFailure {
                        CourierSessionStorage.clearSession(context)
                    }

                    return@withContext sessionResult
                }
            } catch (e: Exception) {
                Log.w(TAG, "restoreSessionIfRemembered error: ${e.message}")
                return@withContext Result.failure(e)
            }
        }

    private fun fetchAndValidateCourierProfile(
        email: String,
        authUserId: String,
        accessToken: String
    ): Result<CourierAuthSession> {
        return try {
            var usersArr = if (authUserId.isNotBlank()) {
                executeGetJsonArray("$SUPABASE_URL/rest/v1/users?select=*,roles(name)&id=eq.$authUserId")
            } else null

            if (usersArr == null || usersArr.length() == 0) {
                usersArr = executeGetJsonArray("$SUPABASE_URL/rest/v1/users?select=*,roles(name)&email=eq.$email")
            }

            if (usersArr == null || usersArr.length() == 0) {
                return Result.failure(SecurityException("Akun tidak ditemukan di tabel users."))
            }

            val userRow = usersArr.getJSONObject(0)
            val dbUserId = userRow.optString("id", authUserId)
            val roleId = userRow.optInt("role_id", -1)
            val roleName = userRow.optJSONObject("roles")?.optString("name").orEmpty()
            val accountStatus = userRow.optString("account_status", "active").ifBlank { "active" }
            val userName = userRow.optString("name", "Kurir Kube")
            val userPhone = userRow.optString("phone", "")

            if (roleId != 3 && !roleName.equals("courier", ignoreCase = true)) {
                return Result.failure(
                    SecurityException("Akses ditolak: akun Anda bukan kurir (role_id: $roleId).")
                )
            }

            if (!accountStatus.equals("active", ignoreCase = true)) {
                return Result.failure(
                    SecurityException("Akun dinonaktifkan (status: $accountStatus). Hubungi Admin.")
                )
            }

            currentUserUuid = dbUserId

            val profileArr = executeGetJsonArray(
                "$SUPABASE_URL/rest/v1/courier_profiles?select=*&user_id=eq.$dbUserId"
            )

            var profileId = 0
            if (profileArr != null && profileArr.length() > 0) {
                val pRow = profileArr.getJSONObject(0)
                profileId = pRow.optInt("id", 0)
                currentCourierProfileId = profileId
                _activeCourierProfileId.value = if (profileId > 0) profileId.toString() else dbUserId
                syncCourierProfileRowToRepo(pRow, userName, email, userPhone)
                syncWarehouseFromProfileRow(pRow)
            } else {
                _activeCourierProfileId.value = dbUserId
                val base = repository.courier.value
                repository.syncCourierFromStream(
                    base.copy(id = dbUserId, name = userName, email = email, phone = userPhone)
                )
            }

            Result.success(
                CourierAuthSession(
                    userId = dbUserId,
                    courierId = if (profileId > 0) profileId.toString() else dbUserId,
                    email = email,
                    role = "courier",
                    accountStatus = accountStatus,
                    accessToken = accessToken
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun fetchAndSyncCourierProfileByUuid(userUuid: String) {
        val usersArr = executeGetJsonArray("$SUPABASE_URL/rest/v1/users?select=*&id=eq.$userUuid") ?: return
        if (usersArr.length() == 0) return
        val uRow = usersArr.getJSONObject(0)
        val userName = uRow.optString("name", "Kurir Kube")
        val userEmail = uRow.optString("email", "")
        val userPhone = uRow.optString("phone", "")

        val profileArr = executeGetJsonArray(
            "$SUPABASE_URL/rest/v1/courier_profiles?select=*&user_id=eq.$userUuid"
        ) ?: return
        if (profileArr.length() > 0) {
            val pRow = profileArr.getJSONObject(0)
            val parsedId = pRow.optInt("id", currentCourierProfileId)
            currentCourierProfileId = parsedId
            _activeCourierProfileId.value = if (parsedId > 0) parsedId.toString() else userUuid
            syncCourierProfileRowToRepo(pRow, userName, userEmail, userPhone)
            syncWarehouseFromProfileRow(pRow)
        }
    }

    private fun syncWarehouseFromProfileRow(pRow: JSONObject) {
        val warehouseId = pRow.optInt("warehouse_id", 0)
        if (warehouseId <= 0) return
        val wArr = executeGetJsonArray("$SUPABASE_URL/rest/v1/warehouses?select=*&id=eq.$warehouseId")
            ?: return
        if (wArr.length() == 0) return
        val wRow = wArr.optJSONObject(0) ?: return
        _warehouseInfo.value = WarehouseInfo(
            id = wRow.optInt("id", warehouseId).toString(),
            name = wRow.optString("name").takeIf { it.isNotBlank() && it != "null" }
                ?: _warehouseInfo.value.name,
            address = wRow.optString("address").takeIf { it.isNotBlank() && it != "null" }
                ?: _warehouseInfo.value.address,
            operationalHours = wRow.optString("operational_hours").takeIf { it.isNotBlank() && it != "null" }
                ?: _warehouseInfo.value.operationalHours,
            facilities = wRow.optString("facilities").takeIf { it.isNotBlank() && it != "null" }
                ?: _warehouseInfo.value.facilities,
            latitude = wRow.optDouble("latitude", _warehouseInfo.value.latitude)
                .takeIf { !it.isNaN() } ?: _warehouseInfo.value.latitude,
            longitude = wRow.optDouble("longitude", _warehouseInfo.value.longitude)
                .takeIf { !it.isNaN() } ?: _warehouseInfo.value.longitude
        )
    }

    private fun syncCourierProfileRowToRepo(
        row: JSONObject,
        fallbackName: String,
        fallbackEmail: String,
        fallbackPhone: String
    ) {
        val base = repository.courier.value
        val profileIdStr = row.optInt("id", 0).let { if (it > 0) it.toString() else base.id }
        val fullName = row.optString("full_name").takeIf { it.isNotBlank() && it != "null" } ?: fallbackName
        val phone = row.optString("phone_whatsapp").takeIf { it.isNotBlank() && it != "null" } ?: fallbackPhone
        val address = row.optString("address").takeIf { it.isNotBlank() && it != "null" }
            ?: row.optString("work_area").takeIf { it.isNotBlank() && it != "null" }
            ?: "Area Operasional Kube"
        val plate = row.optString("license_plate").takeIf { it.isNotBlank() && it != "null" }
            ?: row.optString("vehicle_plate").takeIf { it.isNotBlank() && it != "null" }
            ?: "-"
        val capacity = row.optString("vehicle_capacity").takeIf { it.isNotBlank() && it != "null" }
            ?: "Standar Armada"
        val statusRaw = row.optString("status", "available")
        val statusLabel = if (statusRaw.equals("available", ignoreCase = true)) "Siap Bertugas" else statusRaw

        val primaryBank = _bankAccounts.value.firstOrNull { it.isPrimary } ?: _bankAccounts.value.firstOrNull()

        repository.syncCourierFromStream(
            base.copy(
                id = profileIdStr,
                name = fullName,
                email = fallbackEmail.ifBlank { base.email },
                phone = phone,
                address = address,
                isActiveDuty = row.optBoolean("is_on_duty", true),
                vehicleType = row.optString("vehicle_type", "-"),
                vehiclePlateNumber = plate,
                vehicleCapacity = capacity,
                vehicleStatus = statusLabel,
                bankName = primaryBank?.bankName ?: base.bankName,
                bankAccountNumber = primaryBank?.accountNumber ?: base.bankAccountNumber,
                bankAccountHolder = primaryBank?.accountHolder ?: fullName
            )
        )
    }

    // =========================================================================
    // 2. TOGGLE ON-DUTY & UPDATE PROFIL
    // =========================================================================

    suspend fun toggleOnDuty(
        courierId: String = repository.courier.value.id,
        isOnDuty: Boolean
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            repository.updateCourierDuty(isOnDuty)
            val profileId = courierId.toIntOrNull() ?: currentCourierProfileId
            if (profileId > 0) {
                val payload = JSONObject().apply {
                    put("is_on_duty", isOnDuty)
                    put("status", if (isOnDuty) "available" else "offline")
                }.toString()
                executePatch("$SUPABASE_URL/rest/v1/courier_profiles?id=eq.$profileId", payload)
            }
            if (isOnDuty) {
                realtimeScope.launch {
                    runCatching { syncAllTablesOnce() }
                }
                KubeFeedbackManager.showSuccess(
                    "Status Aktif (On-Duty)",
                    "Anda kini siap menerima dan memproses tugas pengantaran."
                )
            } else {
                KubeFeedbackManager.showWarning(
                    "Mode Istirahat (Off-Duty)",
                    "Penerimaan tugas baru dan aksi pengantaran dikunci sementara."
                )
            }
            Result.success(isOnDuty)
        } catch (e: Exception) {
            Log.e(TAG, "toggleOnDuty error", e)
            Result.failure(e)
        }
    }

    suspend fun updateCourierProfile(
        courierId: String = repository.courier.value.id,
        phone: String,
        address: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            repository.updateCourierProfile(phone, address)
            val profileId = courierId.toIntOrNull() ?: currentCourierProfileId
            if (profileId > 0) {
                val profilePayload = JSONObject().apply {
                    put("phone_whatsapp", phone.trim())
                    put("address", address.trim())
                }.toString()
                executePatch("$SUPABASE_URL/rest/v1/courier_profiles?id=eq.$profileId", profilePayload)
            }
            if (currentUserUuid.isNotBlank()) {
                val userPayload = JSONObject().apply {
                    put("phone", phone.trim())
                }.toString()
                executePatch("$SUPABASE_URL/rest/v1/users?id=eq.$currentUserUuid", userPayload)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "updateCourierProfile error", e)
            Result.failure(e)
        }
    }

    // =========================================================================
    // 3. DAFTAR TUGAS PENGIRIMAN (orders + order_items + products + payment_methods + consumer_profiles + deliveries)
    // =========================================================================

    suspend fun getMyDeliveries(
        courierId: String = repository.courier.value.id
    ): Result<List<DeliveryOrder>> = withContext(Dispatchers.IO) {
        try {
            if (currentUserUuid.isNotBlank()) {
                fetchAndSyncOrdersAndDeliveries(currentUserUuid, currentCourierProfileId)
            }
            Result.success(repository.orders.value)
        } catch (e: Exception) {
            Log.e(TAG, "getMyDeliveries error", e)
            Result.success(repository.orders.value)
        }
    }

    private fun fetchAndSyncOrdersAndDeliveries(userUuid: String, profileId: Int) {
        val ordersUrl = "$SUPABASE_URL/rest/v1/orders" +
            "?select=*,payment_methods(id,name,code),order_items(id,quantity,price_per_unit,subtotal,products(id,name,courier_commission)),deliveries(*)" +
            "&courier_id=eq.$userUuid" +
            "&order=created_at.desc"

        val ordersArr = executeGetJsonArray(ordersUrl) ?: return
        if (ordersArr.length() == 0) {
            repository.syncOrdersFromStream(emptyList())
            return
        }

        // Kumpulkan consumer_id untuk mengambil nama dari `users` & alamat/nama kafe dari `consumer_profiles`
        val consumerIds = mutableSetOf<String>()
        for (i in 0 until ordersArr.length()) {
            val cId = ordersArr.optJSONObject(i)?.optString("consumer_id").orEmpty()
            if (cId.isNotBlank() && cId != "null") consumerIds.add(cId)
        }

        val consumerProfileMap = mutableMapOf<String, JSONObject>()
        val consumerUserMap = mutableMapOf<String, JSONObject>()

        if (consumerIds.isNotEmpty()) {
            val inFilter = consumerIds.joinToString(",")
            executeGetJsonArray("$SUPABASE_URL/rest/v1/consumer_profiles?select=*&user_id=in.($inFilter)")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val uId = obj.optString("user_id")
                    if (uId.isNotBlank()) consumerProfileMap[uId] = obj
                }
            }
            executeGetJsonArray("$SUPABASE_URL/rest/v1/users?select=id,name,phone&id=in.($inFilter)")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val uId = obj.optString("id")
                    if (uId.isNotBlank()) consumerUserMap[uId] = obj
                }
            }
        }

        val mappedOrders = mutableListOf<DeliveryOrder>()

        for (i in 0 until ordersArr.length()) {
            val row = ordersArr.optJSONObject(i) ?: continue
            val orderDbId = row.optInt("id", 0)
            val orderCode = row.optString("order_code", "ORD-$orderDbId")
            val consumerId = row.optString("consumer_id")

            // 1. Resolusi Pelanggan & Alamat
            val cProfile = consumerProfileMap[consumerId]
            val cUser = consumerUserMap[consumerId]
            val cafeName = cProfile?.optString("cafe_name").takeIf { !it.isNullOrBlank() && it != "null" }
            val userFullName = cUser?.optString("name").takeIf { !it.isNullOrBlank() && it != "null" }
            val customerName = when {
                cafeName != null && userFullName != null -> "$cafeName ($userFullName)"
                cafeName != null -> cafeName
                userFullName != null -> userFullName
                else -> "Pelanggan #$orderCode"
            }

            val rawOrderAddr = row.optString("delivery_address").trim()
            val profileAddr = cProfile?.optString("address").takeIf { !it.isNullOrBlank() && it != "null" }.orEmpty()
            val finalAddress = if (rawOrderAddr.isBlank() ||
                rawOrderAddr.equals("null", ignoreCase = true) ||
                rawOrderAddr.equals("Gagal memuat alamat", ignoreCase = true)
            ) {
                profileAddr.ifBlank { "Alamat menunggu konfirmasi pelanggan" }
            } else {
                rawOrderAddr
            }

            val customerPhone = cUser?.optString("phone").takeIf { !it.isNullOrBlank() && it != "null" }.orEmpty()

            // 2. Resolusi Item Barang, Harga Satuan & Komisi Kurir
            val itemsArr = row.optJSONArray("order_items")
            val itemSummaries = mutableListOf<String>()
            var totalCommission = 0L
            var firstUnitPrice = 0L

            if (itemsArr != null) {
                for (j in 0 until itemsArr.length()) {
                    val itemObj = itemsArr.optJSONObject(j) ?: continue
                    val qty = itemObj.optInt("quantity", 1)
                    val unitPrice = itemObj.optDouble("price_per_unit", 0.0).toLong()
                    if (firstUnitPrice == 0L && unitPrice > 0L) firstUnitPrice = unitPrice

                    val prodObj = itemObj.optJSONObject("products")
                    val prodName = prodObj?.optString("name").orEmpty().ifBlank { "Paket Es Kube" }
                    val unitComm = prodObj?.optDouble("courier_commission", 500.0)?.toLong() ?: 500L

                    totalCommission += (qty * unitComm)
                    itemSummaries.add("$qty x $prodName")
                }
            }
            val itemDescription = itemSummaries.joinToString(", ").ifBlank { "Paket Es Kube" }

            // 3. Resolusi Pembayaran, Deteksi COD, & Koordinat Pelanggan
            val pmObj = row.optJSONObject("payment_methods")
            val pmCode = pmObj?.optString("code").orEmpty().ifBlank { "QRIS" }
            val pmName = pmObj?.optString("name").orEmpty().ifBlank { "Non-Tunai" }
            val totalAmount = row.optDouble("total_amount", 0.0).toLong()
            val paymentStatusRaw = row.optString("payment_status", "unpaid")
            val isPaid = paymentStatusRaw.equals("paid", ignoreCase = true)
            val custLat = cProfile?.optString("latitude")
                ?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                ?.toDoubleOrNull()
            val custLng = cProfile?.optString("longitude")
                ?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                ?.toDoubleOrNull()

            val meta = OrderPaymentMeta(
                orderDbId = orderDbId,
                orderCode = orderCode,
                paymentMethodCode = pmCode,
                paymentMethodName = pmName,
                totalAmount = totalAmount,
                unitPrice = firstUnitPrice,
                commissionAmount = totalCommission.coerceAtLeast(1000L),
                isPaid = isPaid,
                latitude = custLat,
                longitude = custLng
            )
            orderMetaByCode[orderCode] = meta

            val paymentStatusLabel = when {
                meta.isCodOrder && !isPaid -> "COD - Tagih Rp ${"%,d".format(totalAmount).replace(',', '.')}"
                meta.isCodOrder && isPaid -> "COD - Lunas Tunai"
                isPaid -> "Lunas ($pmName)"
                else -> "Belum dibayar ($pmName)"
            }

            // 4. Resolusi Status & Data Pengiriman dari tabel `deliveries` (jika sudah ada barisnya)
            val deliveriesVal = row.opt("deliveries")
            val deliveryObj: JSONObject? = when (deliveriesVal) {
                is JSONArray -> if (deliveriesVal.length() > 0) deliveriesVal.optJSONObject(0) else null
                is JSONObject -> deliveriesVal
                else -> null
            }

            if (deliveryObj != null) {
                val dId = deliveryObj.optInt("id", 0)
                if (dId > 0) deliveryIdByOrderDbId[orderDbId] = dId
            }

            val rawStatus = deliveryObj?.optString("status").takeIf { !it.isNullOrBlank() && it != "null" }
                ?: row.optString("order_status", "pending")
            val mappedStatus = mapRemoteDeliveryStatus(rawStatus)

            val priorityRaw = deliveryObj?.optString("priority").takeIf { !it.isNullOrBlank() && it != "null" }
                ?: row.optString("delivery_type", "Normal")
            val priorityLabel = if (priorityRaw.equals("instant", ignoreCase = true) ||
                priorityRaw.equals("urgent", ignoreCase = true)
            ) "Prioritas" else "Normal"

            val recipient = deliveryObj?.optString("recipient_name").takeIf { !it.isNullOrBlank() && it != "null" }.orEmpty()
            val podUrl = deliveryObj?.optString("pod_photo_url").takeIf { !it.isNullOrBlank() && it != "null" }
            val rawNotes = deliveryObj?.optString("delivery_notes")
                ?.takeIf { it.isNotBlank() && it != "null" }
                ?: row.optString("notes")?.takeIf { it.isNotBlank() && it != "null" }

            // Ambil URL bukti uang COD dari kolom `proof_photo_path` atau dari teks catatan lama
            val proofPathCol = deliveryObj?.optString("proof_photo_path")
                ?.takeIf { it.isNotBlank() && it != "null" }
            val urlInNotesRegex = Regex("""Bukti Uang COD:\s*(https?://\S+)""")
            val extractedCodUrlFromNotes = rawNotes?.let { urlInNotesRegex.find(it)?.groupValues?.getOrNull(1) }

            val resolvedCodPhotoUrl = when {
                !extractedCodUrlFromNotes.isNullOrBlank() -> extractedCodUrlFromNotes
                !proofPathCol.isNullOrBlank() && proofPathCol != podUrl -> proofPathCol
                meta.isCodOrder && !proofPathCol.isNullOrBlank() -> proofPathCol
                else -> null
            }

            // Hapus teks mentah "Bukti Uang COD: https://..." dari catatan agar Catatan Pengantaran bersih
            val cleanedNotes = rawNotes
                ?.replace(Regex("""\|?\s*Bukti Uang COD:\s*https?://\S+"""), "")
                ?.trim()
                ?.takeIf { it.isNotBlank() }

            val deliveredAtStr = deliveryObj?.optString("delivered_at")
                ?.takeIf { it.isNotBlank() && it != "null" }
                ?.let { formatTimestampShort(it) }
                ?: formatTimestampShort(row.optString("updated_at"))

            val finalCommission = deliveryObj?.optDouble("commission_amount", 0.0)?.toLong()
                ?.takeIf { it > 0L } ?: totalCommission.coerceAtLeast(1000L)

            orderMetaByCode[orderCode] = meta.copy(
                commissionAmount = finalCommission,
                recipientName = recipient.ifBlank { customerName },
                podPhotoUrl = podUrl,
                codCashPhotoUrl = resolvedCodPhotoUrl,
                podStatus = deliveryObj?.optString("pod_status", "approved")
                    ?.takeIf { it.isNotBlank() && it != "null" } ?: "approved",
                deliveredAtFormatted = deliveredAtStr,
                deliveryNotes = cleanedNotes
            )

            val pod = if (recipient.isNotBlank() || !podUrl.isNullOrBlank()) {
                ProofOfDelivery(
                    recipientName = recipient.ifBlank { customerName },
                    photoUri = podUrl,
                    confirmedQuantity = itemsArr?.optJSONObject(0)?.optInt("quantity", 1) ?: 1,
                    notes = cleanedNotes
                )
            } else null

            mappedOrders.add(
                DeliveryOrder(
                    id = orderCode,
                    customerName = customerName,
                    address = finalAddress,
                    subDistrict = if (meta.isCodOrder) "Metode: COD (Tunai)" else "Metode: $pmName",
                    phoneNumber = customerPhone,
                    itemDescription = itemDescription,
                    paymentStatus = paymentStatusLabel,
                    priority = priorityLabel,
                    estimatedTimeMinutes = deliveryObj?.optInt("estimated_minutes", 15)?.takeIf { it > 0 } ?: 15,
                    distanceKm = deliveryObj?.optDouble("distance_km", 2.5)?.takeIf { !it.isNaN() && it > 0.0 } ?: 2.5,
                    status = mappedStatus,
                    assignedTime = formatTimestampShort(row.optString("created_at")),
                    inTransitTime = if (mappedStatus == DeliveryStatus.IN_TRANSIT || mappedStatus == DeliveryStatus.DELIVERED)
                        formatTimestampShort(row.optString("updated_at")) else null,
                    deliveredTime = deliveryObj?.optString("delivered_at").takeIf { !it.isNullOrBlank() && it != "null" }
                        ?.let { formatTimestampShort(it) },
                    notes = cleanedNotes,
                    proofOfDelivery = pod,
                    paymentMethod = meta.paymentMethodCode,
                    totalAmount = meta.totalAmount,
                    unitPrice = meta.unitPrice
                )
            )
        }

        repository.syncOrdersFromStream(mappedOrders)
    }

    private fun mapRemoteDeliveryStatus(statusRaw: String): DeliveryStatus {
        return when (statusRaw.trim().lowercase()) {
            "assigned", "new", "pending", "confirmed", "waiting" -> DeliveryStatus.NEW
            "picked_up", "in_transit", "delivering", "shipping", "on_delivery" -> DeliveryStatus.IN_TRANSIT
            "arrived", "at_location" -> DeliveryStatus.ARRIVED
            "delivered", "completed", "success" -> DeliveryStatus.DELIVERED
            "returned", "failed", "cancelled" -> DeliveryStatus.FAILED
            else -> DeliveryStatus.NEW
        }
    }

    // =========================================================================
    // 4. UPDATE PROGRES PERJALANAN & UPLOAD BUKTI PENGIRIMAN (PoD + STORAGE + KOMISI)
    // =========================================================================

    suspend fun updateDeliveryProgress(
        orderId: String,
        newStatus: DeliveryStatus
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!repository.courier.value.isActiveDuty) {
            return@withContext Result.failure(
                IllegalStateException("Status Anda sedang Off-Duty. Aktifkan On-Duty terlebih dahulu untuk memproses pengantaran.")
            )
        }
        try {
            repository.updateOrderStatus(orderId, newStatus)
            val meta = orderMetaByCode[orderId] ?: return@withContext Result.success(Unit)

            val orderStatusDb = when (newStatus) {
                DeliveryStatus.NEW -> "pending"
                DeliveryStatus.IN_TRANSIT, DeliveryStatus.ARRIVED -> "delivering"
                DeliveryStatus.PENDING_VERIFICATION, DeliveryStatus.DELIVERED -> "completed"
                DeliveryStatus.FAILED -> "cancelled"
            }

            val deliveryStatusDb = when (newStatus) {
                DeliveryStatus.NEW -> "assigned"
                DeliveryStatus.IN_TRANSIT -> "in_transit"
                DeliveryStatus.ARRIVED -> "arrived"
                DeliveryStatus.PENDING_VERIFICATION, DeliveryStatus.DELIVERED -> "delivered"
                DeliveryStatus.FAILED -> "returned"
            }

            // 1. Update tabel `orders`
            val orderPayload = JSONObject().apply {
                put("order_status", orderStatusDb)
                if (currentUserUuid.isNotBlank()) put("updated_by", currentUserUuid)
            }.toString()
            executePatch("$SUPABASE_URL/rest/v1/orders?id=eq.${meta.orderDbId}", orderPayload)

            // 2. Upsert tabel `deliveries` berdasarkan `order_id`
            val deliveryDbId = upsertDeliveryRow(
                orderDbId = meta.orderDbId,
                status = deliveryStatusDb,
                commissionAmount = meta.commissionAmount
            )

            // 3. Catat log ke `delivery_logs`
            if (deliveryDbId > 0) {
                insertDeliveryLog(
                    deliveryId = deliveryDbId,
                    status = deliveryStatusDb,
                    notes = "Status pengiriman diperbarui menjadi $deliveryStatusDb"
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "updateDeliveryProgress error", e)
            Result.failure(e)
        }
    }

    suspend fun completeDeliveryWithPoD(
        orderId: String,
        recipientName: String,
        photoUri: String?,
        codCashPhotoUri: String? = null,
        confirmedQuantity: Int,
        notes: String?,
        context: Context? = null,
        isCodCollected: Boolean = true
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!repository.courier.value.isActiveDuty) {
            return@withContext Result.failure(
                IllegalStateException("Status Anda sedang Off-Duty. Aktifkan On-Duty terlebih dahulu untuk menyelesaikan pengiriman.")
            )
        }
        try {
            val meta = orderMetaByCode[orderId]
            val orderDbId = meta?.orderDbId ?: 0
            val commissionAmt = meta?.commissionAmount ?: 1000L
            val isCod = meta?.isCodOrder ?: (repository.getOrderById(orderId)?.isCod == true)

            // 1. Upload foto bukti pengiriman ke Storage bucket `pod-photos`
            val uploadedPhotoUrl = if (!photoUri.isNullOrBlank()) {
                uploadPhotoToSupabaseStorage(
                    photoUriString = photoUri,
                    fileName = "pod_${orderId}_${System.currentTimeMillis()}.jpg",
                    context = context
                ) ?: photoUri
            } else ""

            // 2. Upload foto bukti uang COD (khusus pesanan COD)
            val uploadedCodCashUrl = if (isCod && !codCashPhotoUri.isNullOrBlank()) {
                uploadPhotoToSupabaseStorage(
                    photoUriString = codCashPhotoUri,
                    fileName = "cod_cash_${orderId}_${System.currentTimeMillis()}.jpg",
                    context = context
                ) ?: codCashPhotoUri
            } else ""

            repository.submitProofOfDelivery(
                orderId = orderId,
                recipientName = recipientName,
                photoUri = uploadedPhotoUrl.ifBlank { photoUri },
                confirmedQuantity = confirmedQuantity,
                notes = notes,
                isCodPaymentConfirmed = isCod && isCodCollected
            )

            if (orderDbId > 0) {
                // 3. Update `orders` -> completed (+ paid jika COD)
                val orderPayload = JSONObject().apply {
                    put("order_status", "completed")
                    if (isCod && isCodCollected) {
                        put("payment_status", "paid")
                    }
                    if (currentUserUuid.isNotBlank()) put("updated_by", currentUserUuid)
                }.toString()
                executePatch("$SUPABASE_URL/rest/v1/orders?id=eq.$orderDbId", orderPayload)

                val cleanNotes = notes?.trim()?.takeIf { it.isNotBlank() }

                // 4. Upsert `deliveries` (simpan foto bukti serah terima & bukti uang COD secara terpisah)
                val deliveryDbId = upsertDeliveryRow(
                    orderDbId = orderDbId,
                    status = "delivered",
                    commissionAmount = commissionAmt,
                    podPhotoUrl = uploadedPhotoUrl,
                    codCashPhotoUrl = uploadedCodCashUrl.ifBlank { null },
                    recipientName = recipientName.trim(),
                    deliveryNotes = cleanNotes
                )

                // 5. Log & Komisi langsung masuk ke Saldo Siap Cair (available)
                if (deliveryDbId > 0) {
                    insertDeliveryLog(
                        deliveryId = deliveryDbId,
                        status = "delivered",
                        notes = "Diterima oleh ${recipientName.trim()} ($confirmedQuantity unit). ${cleanNotes.orEmpty()}".trim()
                    )

                    if (currentCourierProfileId > 0 && commissionAmt > 0) {
                        val walletTxPayload = JSONObject().apply {
                            put("courier_id", currentCourierProfileId)
                            put("delivery_id", deliveryDbId)
                            put("type", "commission")
                            put("amount", commissionAmt)
                            put("balance_status", "available") // Langsung masuk Saldo Siap Cair tanpa approval admin
                            put("description", "Komisi pengantaran #$orderId")
                        }.toString()
                        executePost("$SUPABASE_URL/rest/v1/wallet_transactions", walletTxPayload)
                    }
                }
            }

            syncAllTablesOnce()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "completeDeliveryWithPoD error", e)
            Result.failure(e)
        }
    }

    private fun uploadPhotoToSupabaseStorage(
        photoUriString: String,
        fileName: String,
        context: Context?
    ): String? {
        return runCatching {
            if (photoUriString.startsWith("http://") || photoUriString.startsWith("https://")) {
                return photoUriString
            }

            // Konversi gambar format apa pun (JPG/PNG/WEBP/HEIC) menjadi file SVG (.svg)
            val svgBytes = com.example.core.components.KubeImageUtils.convertAnyImageToSvgBytes(
                context = context,
                photoUriString = photoUriString
            ) ?: return null

            val svgFileName = fileName.substringBeforeLast(".") + ".svg"

            val uploadReq = Request.Builder()
                .url("$SUPABASE_URL/storage/v1/object/$POD_STORAGE_BUCKET/$svgFileName")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer ${currentAccessToken ?: SUPABASE_ANON_KEY}")
                .addHeader("Content-Type", "image/svg+xml")
                .addHeader("x-upsert", "true")
                .post(svgBytes.toRequestBody("image/svg+xml".toMediaType()))
                .build()

            okHttpClient.newCall(uploadReq).execute().use { res ->
                if (res.isSuccessful) {
                    "$SUPABASE_URL/storage/v1/object/public/$POD_STORAGE_BUCKET/$svgFileName"
                } else {
                    Log.w(TAG, "SVG upload failed (${res.code}): ${res.body?.string()}")
                    null
                }
            }
        }.getOrNull()
    }

    private fun upsertDeliveryRow(
        orderDbId: Int,
        status: String,
        commissionAmount: Long,
        podPhotoUrl: String? = null,
        codCashPhotoUrl: String? = null,
        recipientName: String? = null,
        deliveryNotes: String? = null
    ): Int {
        return runCatching {
            val payload = JSONObject().apply {
                put("order_id", orderDbId)
                if (currentCourierProfileId > 0) put("courier_id", currentCourierProfileId)
                put("status", status)
                put("commission_amount", commissionAmount)
                if (!podPhotoUrl.isNullOrBlank()) {
                    put("pod_photo_url", podPhotoUrl)
                }
                if (!codCashPhotoUrl.isNullOrBlank()) {
                    put("proof_photo_path", codCashPhotoUrl)
                } else if (!podPhotoUrl.isNullOrBlank()) {
                    put("proof_photo_path", podPhotoUrl)
                }
                if (!recipientName.isNullOrBlank()) put("recipient_name", recipientName)
                if (!deliveryNotes.isNullOrBlank()) put("delivery_notes", deliveryNotes)
                if (status == "delivered") {
                    put("pod_status", "approved")
                    put("delivered_at", java.time.Instant.now().toString())
                }
            }.toString()

            val req = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/deliveries?on_conflict=order_id")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer ${currentAccessToken ?: SUPABASE_ANON_KEY}")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates,return=representation")
                .post(payload.toRequestBody(jsonMediaType))
                .build()

            okHttpClient.newCall(req).execute().use { res ->
                val body = res.body?.string().orEmpty().trim()
                if (res.isSuccessful && body.startsWith("[")) {
                    val id = JSONArray(body).optJSONObject(0)?.optInt("id", 0) ?: 0
                    if (id > 0) deliveryIdByOrderDbId[orderDbId] = id
                    id
                } else {
                    deliveryIdByOrderDbId[orderDbId] ?: 0
                }
            }
        }.getOrDefault(deliveryIdByOrderDbId[orderDbId] ?: 0)
    }

    private fun insertDeliveryLog(deliveryId: Int, status: String, notes: String) {
        val payload = JSONObject().apply {
            put("delivery_id", deliveryId)
            if (currentUserUuid.isNotBlank()) put("actor_id", currentUserUuid)
            put("actor_role", "courier")
            put("status", status)
            put("notes", notes)
        }.toString()
        executePost("$SUPABASE_URL/rest/v1/delivery_logs", payload)
    }

    // =========================================================================
    // 5. DOMPET, MUTASI KOMISI, REKENING BANK & PENARIKAN DANA (withdrawals)
    // =========================================================================

    private fun fetchAndSyncBankAccounts(courierProfileId: Int) {
        val arr = executeGetJsonArray(
            "$SUPABASE_URL/rest/v1/courier_bank_accounts?select=*&courier_id=eq.$courierProfileId&order=is_primary.desc"
        ) ?: return

        val accounts = mutableListOf<CourierBankAccountRecord>()
        for (i in 0 until arr.length()) {
            val row = arr.optJSONObject(i) ?: continue
            accounts.add(
                CourierBankAccountRecord(
                    id = row.optInt("id", 0),
                    courierId = row.optInt("courier_id", courierProfileId),
                    bankName = row.optString("bank_name", "-"),
                    accountNumber = row.optString("account_number", "-"),
                    accountHolder = row.optString("account_holder", "-"),
                    isPrimary = row.optBoolean("is_primary", false),
                    verificationStatus = row.optString("verification_status", "pending")
                )
            )
        }
        _bankAccounts.value = accounts

        val primary = accounts.firstOrNull { it.isPrimary } ?: accounts.firstOrNull()
        if (primary != null) {
            val base = repository.courier.value
            repository.syncCourierFromStream(
                base.copy(
                    bankName = primary.bankName,
                    bankAccountNumber = primary.accountNumber,
                    bankAccountHolder = primary.accountHolder
                )
            )
        }
    }

    /**
     * Mengirim pengajuan rekening baru / perubahan rekening kurir ke tabel `public.courier_bank_accounts`.
     * Mendukung unggah foto buku tabungan opsional ke Supabase Storage.
     */
    suspend fun submitBankAccountRequest(
        bankName: String,
        accountNumber: String,
        accountHolder: String,
        passbookPhotoUri: String? = null,
        context: Context? = null
    ): Result<CourierBankAccountRecord> = withContext(Dispatchers.IO) {
        val cleanBank = bankName.trim()
        val cleanNumber = accountNumber.trim()
        val cleanHolder = accountHolder.trim()

        if (cleanBank.isEmpty() || cleanNumber.isEmpty() || cleanHolder.isEmpty()) {
            return@withContext Result.failure(
                IllegalArgumentException("Nama bank, nomor rekening, dan nama pemilik wajib diisi.")
            )
        }

        if (currentCourierProfileId <= 0 && currentUserUuid.isNotBlank()) {
            fetchAndSyncCourierProfileByUuid(currentUserUuid)
        }
        if (currentCourierProfileId <= 0) {
            return@withContext Result.failure(
                IllegalStateException("Profil kurir belum terhubung. Pastikan akun terdaftar di courier_profiles.")
            )
        }

        try {
            // 1. Upload foto buku tabungan jika kurir melampirkannya
            val uploadedPassbookUrl = if (!passbookPhotoUri.isNullOrBlank()) {
                uploadPhotoToSupabaseStorage(
                    photoUriString = passbookPhotoUri,
                    fileName = "passbook_${currentCourierProfileId}_${System.currentTimeMillis()}.jpg",
                    context = context
                ) ?: passbookPhotoUri
            } else {
                null
            }

            // 2. Nonaktifkan status `is_primary` pada rekening lama milik kurir ini
            val resetPrimaryPayload = JSONObject().apply {
                put("is_primary", false)
            }.toString()
            executePatch(
                "$SUPABASE_URL/rest/v1/courier_bank_accounts?courier_id=eq.$currentCourierProfileId",
                resetPrimaryPayload
            )

            // 3. Insert pengajuan rekening baru ke `public.courier_bank_accounts`
            val insertPayload = JSONObject().apply {
                put("courier_id", currentCourierProfileId)
                put("bank_name", cleanBank)
                put("account_number", cleanNumber)
                put("account_holder", cleanHolder)
                if (!uploadedPassbookUrl.isNullOrBlank()) {
                    put("passbook_photo_url", uploadedPassbookUrl)
                }
                put("is_primary", true)
                put("verification_status", "pending")
            }.toString()

            val req = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/courier_bank_accounts")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer ${currentAccessToken ?: SUPABASE_ANON_KEY}")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .post(insertPayload.toRequestBody(jsonMediaType))
                .build()

            okHttpClient.newCall(req).execute().use { res ->
                val body = res.body?.string().orEmpty().trim()
                if (!res.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("Gagal menyimpan rekening ke database (${res.code}): $body")
                    )
                }

                // Segarkan daftar rekening di state lokal & UI Profile/Wallet
                fetchAndSyncBankAccounts(currentCourierProfileId)

                val created = _bankAccounts.value.firstOrNull { it.accountNumber == cleanNumber }
                    ?: CourierBankAccountRecord(
                        id = 0,
                        courierId = currentCourierProfileId,
                        bankName = cleanBank,
                        accountNumber = cleanNumber,
                        accountHolder = cleanHolder,
                        isPrimary = true,
                        verificationStatus = "pending"
                    )
                Result.success(created)
            }
        } catch (e: Exception) {
            Log.e(TAG, "submitBankAccountRequest error", e)
            Result.failure(e)
        }
    }

    private fun fetchAndSyncWalletAndTransactions(courierProfileId: Int) {
        val txArr = executeGetJsonArray(
            "$SUPABASE_URL/rest/v1/wallet_transactions?select=*&courier_id=eq.$courierProfileId&order=created_at.desc"
        ) ?: return

        var pendingBal = 0L
        var availableBal = 0L
        val historyList = mutableListOf<CommissionHistoryItem>()

        for (i in 0 until txArr.length()) {
            val row = txArr.optJSONObject(i) ?: continue
            val id = row.optInt("id", 0).toString()
            val type = row.optString("type", "commission").lowercase()
            val amount = row.optDouble("amount", 0.0).toLong()
            val status = row.optString("balance_status", "pending").lowercase()
            val deliveryId = row.opt("delivery_id")?.toString()?.takeIf { it != "null" } ?: "-"
            val desc = row.optString("description").takeIf { it.isNotBlank() && it != "null" }
                ?: if (type == "tip") "Tip Pelanggan" else "Komisi Pengantaran"

            if (status == "pending") {
                pendingBal += amount
            } else if (status == "available") {
                if (type == "withdrawal") {
                    availableBal -= amount
                } else {
                    availableBal += amount
                }
            }

            if (type != "withdrawal") {
                historyList.add(
                    CommissionHistoryItem(
                        id = id,
                        orderId = deliveryId,
                        title = desc,
                        subtitle = if (status == "available") "Siap Cair" else "Menunggu Verifikasi PoD",
                        amount = amount,
                        isTip = type == "tip",
                        timestamp = formatTimestampShort(row.optString("created_at"))
                    )
                )
            }
        }

        repository.syncCommissionHistoryFromStream(historyList)

        val currentWithdrawn = repository.walletBalance.value.withdrawnBalance
        repository.syncWalletBalanceFromStream(
            WalletBalance(
                pendingBalance = pendingBal.coerceAtLeast(0L),
                availableBalance = availableBal.coerceAtLeast(0L),
                withdrawnBalance = currentWithdrawn
            )
        )
    }

    private fun fetchAndSyncWithdrawals(courierProfileId: Int) {
        val wdArr = executeGetJsonArray(
            "$SUPABASE_URL/rest/v1/withdrawals" +
                "?select=*,courier_bank_accounts(bank_name,account_number,account_holder),users(name)" +
                "&courier_id=eq.$courierProfileId&order=created_at.desc"
        ) ?: return

        var totalWithdrawnSuccess = 0L
        val list = mutableListOf<WithdrawalHistoryItem>()

        for (i in 0 until wdArr.length()) {
            val row = wdArr.optJSONObject(i) ?: continue
            val code = row.optString("withdrawal_code", "WD-${row.optInt("id")}")
            val amount = row.optDouble("amount", 0.0).toLong()
            val statusRaw = row.optString("status", "pending").lowercase()
            val proofUrl = row.optString("transfer_proof_url")
                .takeIf { it.isNotBlank() && it != "null" }

            val mappedStatus = when (statusRaw) {
                "processing", "transferred", "sent", "waiting_confirmation" -> WithdrawalStatus.PROCESSING
                "success", "approved", "completed", "paid" -> {
                    totalWithdrawnSuccess += amount
                    WithdrawalStatus.SUCCESS
                }
                "rejected", "failed" -> WithdrawalStatus.REJECTED
                else -> WithdrawalStatus.WAITING
            }

            val bankObj = row.optJSONObject("courier_bank_accounts")
            val bankName = bankObj?.optString("bank_name").orEmpty().ifBlank { repository.courier.value.bankName }
            val accNum = bankObj?.optString("account_number").orEmpty().ifBlank { repository.courier.value.bankAccountNumber }
            val accHolder = bankObj?.optString("account_holder").orEmpty().ifBlank { repository.courier.value.bankAccountHolder }
            val adminName = row.optJSONObject("users")?.optString("name")
                ?.takeIf { it.isNotBlank() && it != "null" }

            val processedAtFormatted = row.optString("processed_at")
                .takeIf { it.isNotBlank() && it != "null" }
                ?.let { formatTimestampShort(it) }

            val rejectionReason = row.optString("rejection_reason")
                .takeIf { it.isNotBlank() && it != "null" }

            list.add(
                WithdrawalHistoryItem(
                    id = code,
                    bankName = bankName,
                    accountNumberMasked = maskAccountNumber(accNum),
                    recipientName = accHolder,
                    amount = amount,
                    date = formatTimestampShort(row.optString("created_at")),
                    status = mappedStatus,
                    rejectionReason = rejectionReason,
                    transferProofUrl = proofUrl,
                    processedAt = processedAtFormatted,
                    processedByName = adminName
                )
            )
        }

        repository.syncWithdrawalsFromStream(list)
        val curWallet = repository.walletBalance.value
        repository.syncWalletBalanceFromStream(curWallet.copy(withdrawnBalance = totalWithdrawnSuccess))
    }

    /**
     * Dipanggil saat kurir menekan tombol "Konfirmasi Uang Sudah Diterima"
     * setelah memeriksa bukti transfer dari Admin.
     */
    suspend fun confirmWithdrawalReceived(withdrawalCode: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                if (withdrawalCode.isBlank()) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Kode penarikan tidak valid.")
                    )
                }
                val payload = JSONObject().apply {
                    put("status", "completed")
                }.toString()

                val updated = executePatch(
                    "$SUPABASE_URL/rest/v1/withdrawals?withdrawal_code=eq.$withdrawalCode",
                    payload
                )

                if (!updated) {
                    return@withContext Result.failure(
                        IllegalStateException("Gagal memverifikasi pencairan dana ke database.")
                    )
                }

                syncAllTablesOnce()
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "confirmWithdrawalReceived error", e)
                Result.failure(e)
            }
        }

    suspend fun getWalletSummary(
        courierId: String = repository.courier.value.id
    ): Result<WalletBalance> = withContext(Dispatchers.IO) {
        val profileId = courierId.toIntOrNull() ?: currentCourierProfileId
        if (profileId > 0) {
            fetchAndSyncWalletAndTransactions(profileId)
            fetchAndSyncWithdrawals(profileId)
        }
        Result.success(repository.walletBalance.value)
    }

    suspend fun requestWithdrawal(
        courierId: String = repository.courier.value.id,
        amount: Long,
        bankAccount: String = repository.courier.value.bankAccountNumber
    ): Result<WithdrawalHistoryItem> = withContext(Dispatchers.IO) {
        try {
            val profileId = courierId.toIntOrNull() ?: currentCourierProfileId
            if (profileId <= 0) {
                return@withContext Result.failure(IllegalStateException("Profil kurir belum tersinkronisasi."))
            }

            if (_bankAccounts.value.isEmpty()) {
                fetchAndSyncBankAccounts(profileId)
            }
            val primaryBank = _bankAccounts.value.firstOrNull { it.isPrimary }
                ?: _bankAccounts.value.firstOrNull()
                ?: return@withContext Result.failure(
                    IllegalStateException("Rekening bank belum terdaftar. Hubungi Admin untuk mendaftarkan rekening.")
                )

            val wdCode = "WD-${System.currentTimeMillis().toString().takeLast(6)}"

            val wdPayload = JSONObject().apply {
                put("withdrawal_code", wdCode)
                put("courier_id", profileId)
                put("bank_account_id", primaryBank.id)
                put("amount", amount)
                put("status", "pending")
            }.toString()
            executePost("$SUPABASE_URL/rest/v1/withdrawals", wdPayload)

            val txPayload = JSONObject().apply {
                put("courier_id", profileId)
                put("type", "withdrawal")
                put("amount", amount)
                put("balance_status", "available")
                put("description", "Pengajuan tarik dana ($wdCode)")
            }.toString()
            executePost("$SUPABASE_URL/rest/v1/wallet_transactions", txPayload)

            syncAllTablesOnce()

            val created = repository.withdrawalHistory.value.firstOrNull { it.id == wdCode }
                ?: WithdrawalHistoryItem(
                    id = wdCode,
                    bankName = primaryBank.bankName,
                    accountNumberMasked = maskAccountNumber(primaryBank.accountNumber),
                    recipientName = primaryBank.accountHolder,
                    amount = amount,
                    date = "Baru saja",
                    status = WithdrawalStatus.WAITING
                )
            Result.success(created)
        } catch (e: Exception) {
            Log.e(TAG, "requestWithdrawal error", e)
            Result.failure(e)
        }
    }

    // =========================================================================
    // 6. NOTIFIKASI & KONTAK BANTUAN ADMIN (notifications + support_contacts)
    // =========================================================================

    private fun fetchAndSyncNotifications(courierProfileId: Int) {
        val arr = executeGetJsonArray(
            "$SUPABASE_URL/rest/v1/notifications?select=*&courier_id=eq.$courierProfileId&order=created_at.desc"
        ) ?: return

        val list = mutableListOf<CourierNotificationRecord>()
        for (i in 0 until arr.length()) {
            val row = arr.optJSONObject(i) ?: continue
            list.add(
                CourierNotificationRecord(
                    id = row.optInt("id", i).toString(),
                    title = row.optString("title", "Notifikasi Kube"),
                    message = row.optString("message", ""),
                    time = formatTimestampShort(row.optString("created_at")),
                    category = row.optString("category", "order"),
                    isUnread = !row.optBoolean("is_read", false)
                )
            )
        }
        applySyncedNotifications(list)
    }

    internal fun applySyncedNotifications(list: List<CourierNotificationRecord>) {
        val ctx = appContext
        list.forEach { item ->
            if (item.isUnread && ctx != null && notifiedIds.add(item.id)) {
                CourierNotificationHelper.showSystemNotification(
                    context = ctx,
                    notificationId = item.id.hashCode(),
                    title = item.title,
                    message = item.message
                )
            }
        }
        _notifications.value = list
    }

    internal fun hasNotifiedForTest(notificationId: String): Boolean = notificationId in notifiedIds

    suspend fun markNotificationAsRead(notificationId: String) = withContext(Dispatchers.IO) {
        val idInt = notificationId.toIntOrNull() ?: return@withContext
        val payload = JSONObject().apply { put("is_read", true) }.toString()
        val ok = executePatch("$SUPABASE_URL/rest/v1/notifications?id=eq.$idInt", payload)
        if (ok) {
            _notifications.value = _notifications.value.map {
                if (it.id == notificationId) it.copy(isUnread = false) else it
            }
        }
        if (currentCourierProfileId > 0) {
            fetchAndSyncNotifications(currentCourierProfileId)
        }
    }

    private fun fetchAndSyncSupportContacts() {
        val arr = executeGetJsonArray("$SUPABASE_URL/rest/v1/support_contacts?select=*") ?: return
        val list = mutableListOf<SupportContactRecord>()
        for (i in 0 until arr.length()) {
            val row = arr.optJSONObject(i) ?: continue
            list.add(
                SupportContactRecord(
                    id = row.optInt("id", 0),
                    warehouseId = if (row.isNull("warehouse_id")) null else row.optInt("warehouse_id"),
                    serviceType = row.optString("service_type", "Admin Operasional"),
                    contactName = row.optString("contact_name", "Admin Kube"),
                    phoneNumber = row.optString("phone_number", ""),
                    serviceHours = row.optString("service_hours", "24 Jam")
                )
            )
        }
        _supportContacts.value = list
    }

    // =========================================================================
    // 6B. IN-APP REALTIME CHAT (chat_messages: courier_admin & order_chat)
    // =========================================================================

    /**
     * Mengambil riwayat chat Kurir <-> Admin berdasarkan UUID kurir yang sedang login.
     */
    suspend fun fetchAdminChatMessages(): List<ChatMessageRecord> = withContext(Dispatchers.IO) {
        if (currentUserUuid.isBlank()) return@withContext emptyList()
        val url = "$SUPABASE_URL/rest/v1/chat_messages" +
            "?select=*&room_type=eq.courier_admin&courier_user_id=eq.$currentUserUuid&order=created_at.asc"
        val arr = executeGetJsonArray(url) ?: return@withContext _adminChatMessages.value
        val parsed = parseChatMessagesJson(arr)
        _adminChatMessages.value = parsed
        parsed
    }

    /**
     * Mengirim pesan baru dari Kurir ke Admin di dalam aplikasi.
     */
    suspend fun sendAdminChatMessage(
        messageText: String,
        context: Context? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val cleanText = messageText.trim()
        if (cleanText.isEmpty() || currentUserUuid.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Pesan tidak boleh kosong."))
        }
        val payload = JSONObject().apply {
            put("room_type", "courier_admin")
            put("courier_user_id", currentUserUuid)
            put("sender_id", currentUserUuid)
            put("sender_role", "courier")
            put("sender_name", repository.courier.value.name)
            put("message", cleanText)
            put("is_read", false)
        }.toString()

        val postResult = executePostWithDetail(
            url = "$SUPABASE_URL/rest/v1/chat_messages",
            jsonPayload = payload,
            context = context
        )
        if (postResult.isSuccess) {
            fetchAdminChatMessages()
            Result.success(Unit)
        } else {
            postResult
        }
    }

    /**
     * Mengambil riwayat chat Kurir <-> Konsumen untuk pesanan spesifik (berdasarkan orderCode atau orderDbId).
     */
    suspend fun fetchOrderChatMessages(orderCode: String): List<ChatMessageRecord> = withContext(Dispatchers.IO) {
        val orderDbId = orderMetaByCode[orderCode]?.orderDbId ?: return@withContext emptyList()
        val url = "$SUPABASE_URL/rest/v1/chat_messages" +
            "?select=*&room_type=eq.order_chat&order_id=eq.$orderDbId&order=created_at.asc"
        val arr = executeGetJsonArray(url) ?: return@withContext (_orderChatMessages.value[orderDbId] ?: emptyList())
        val parsed = parseChatMessagesJson(arr)
        _orderChatMessages.value = _orderChatMessages.value + (orderDbId to parsed)
        parsed
    }

    /**
     * Mengirim pesan baru dari Kurir ke Konsumen pada pesanan yang sedang dikirim.
     */
    suspend fun sendOrderChatMessage(
        orderCode: String,
        messageText: String,
        context: Context? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val cleanText = messageText.trim()
        val orderDbId = orderMetaByCode[orderCode]?.orderDbId ?: 0
        if (cleanText.isEmpty() || currentUserUuid.isBlank() || orderDbId <= 0) {
            return@withContext Result.failure(IllegalArgumentException("Data pesanan atau pesan tidak valid."))
        }

        val payload = JSONObject().apply {
            put("room_type", "order_chat")
            put("order_id", orderDbId)
            put("courier_user_id", currentUserUuid)
            put("sender_id", currentUserUuid)
            put("sender_role", "courier")
            put("sender_name", repository.courier.value.name)
            put("message", cleanText)
            put("is_read", false)
        }.toString()

        val postResult = executePostWithDetail(
            url = "$SUPABASE_URL/rest/v1/chat_messages",
            jsonPayload = payload,
            context = context
        )
        if (postResult.isSuccess) {
            fetchOrderChatMessages(orderCode)
            Result.success(Unit)
        } else {
            postResult
        }
    }

    /**
     * Menandai pesan masuk dari Admin/Konsumen sudah dibaca saat ruang chat dibuka.
     */
    suspend fun markChatMessagesAsRead(roomType: String, orderCode: String? = null) = withContext(Dispatchers.IO) {
        if (currentUserUuid.isBlank()) return@withContext
        val filter = if (roomType == "order_chat" && orderCode != null) {
            val orderDbId = orderMetaByCode[orderCode]?.orderDbId ?: return@withContext
            "room_type=eq.order_chat&order_id=eq.$orderDbId&sender_id=neq.$currentUserUuid&is_read=eq.false"
        } else {
            "room_type=eq.courier_admin&courier_user_id=eq.$currentUserUuid&sender_id=neq.$currentUserUuid&is_read=eq.false"
        }
        val payload = JSONObject().apply { put("is_read", true) }.toString()
        executePatch("$SUPABASE_URL/rest/v1/chat_messages?$filter", payload)
    }

    internal fun parseChatMessagesJson(arr: JSONArray): List<ChatMessageRecord> {
        val list = mutableListOf<ChatMessageRecord>()
        for (i in 0 until arr.length()) {
            val row = arr.optJSONObject(i) ?: continue
            val senderId = row.optString("sender_id")
            val senderRole = row.optString("sender_role", "admin")
            val defaultName = when (senderRole.lowercase()) {
                "admin" -> "Admin Kube"
                "consumer" -> "Pelanggan"
                else -> "Kurir"
            }
            list.add(
                ChatMessageRecord(
                    id = row.optInt("id", 0),
                    roomType = row.optString("room_type", "courier_admin"),
                    orderId = if (row.isNull("order_id")) null else row.optInt("order_id"),
                    courierUserId = row.optString("courier_user_id"),
                    senderId = senderId,
                    senderRole = senderRole,
                    senderName = row.optString("sender_name").takeIf { it.isNotBlank() && it != "null" } ?: defaultName,
                    message = row.optString("message"),
                    isRead = row.optBoolean("is_read", false),
                    timeLabel = formatTimestampShort(row.optString("created_at")),
                    isFromMe = senderId == currentUserUuid
                )
            )
        }
        return list
    }

    // =========================================================================
    // 7. PUBLIC STREAMS & SUMMARY HELPERS
    // =========================================================================

    fun calculateTodaySummary(orders: List<DeliveryOrder> = repository.orders.value): DailyStatsState {
        return DailyStatsProvider.getInstance(repository).computeStats(orders)
    }

    fun streamMyDeliveries(courierId: String = repository.courier.value.id): Flow<List<DeliveryOrder>> {
        return repository.orders.onStart { ensureRealtimeSyncStarted() }
    }

    fun streamNotifications(courierId: String = repository.courier.value.id): Flow<List<CourierNotificationRecord>> {
        return notifications.onStart { ensureRealtimeSyncStarted() }
    }

    fun streamWalletSummary(courierId: String = repository.courier.value.id): Flow<WalletBalance> {
        return repository.walletBalance.onStart { ensureRealtimeSyncStarted() }
    }

    fun streamWalletTransactions(courierId: String = repository.courier.value.id): Flow<List<CommissionHistoryItem>> {
        return repository.commissionHistory.onStart { ensureRealtimeSyncStarted() }
    }

    fun streamWithdrawals(courierId: String = repository.courier.value.id): Flow<List<WithdrawalHistoryItem>> {
        return repository.withdrawalHistory.onStart { ensureRealtimeSyncStarted() }
    }

    fun streamCourierProfile(courierId: String = repository.courier.value.id): Flow<Courier> {
        return repository.courier.onStart { ensureRealtimeSyncStarted() }
    }

    fun streamWarehouseInfo(warehouseId: String = "1"): Flow<WarehouseInfo> {
        return warehouseInfo.onStart { ensureRealtimeSyncStarted() }
    }

    // =========================================================================
    // 8. HTTP REST HELPERS
    // =========================================================================

    private fun executeGetJsonArray(url: String): JSONArray? {
        return runCatching {
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer ${currentAccessToken ?: SUPABASE_ANON_KEY}")
                .get()
                .build()
            okHttpClient.newCall(request).execute().use { res ->
                val body = res.body?.string().orEmpty().trim()
                if (res.isSuccessful && body.startsWith("[")) JSONArray(body) else null
            }
        }.getOrNull()
    }

    private fun executePost(url: String, jsonPayload: String): Boolean {
        return runCatching {
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer ${currentAccessToken ?: SUPABASE_ANON_KEY}")
                .addHeader("Content-Type", "application/json")
                .post(jsonPayload.toRequestBody(jsonMediaType))
                .build()
            okHttpClient.newCall(request).execute().use { it.isSuccessful }
        }.getOrDefault(false)
    }

    suspend fun executePostWithDetail(
        url: String,
        jsonPayload: String,
        context: Context? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        fun performPost(): Pair<Int, String> {
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer ${currentAccessToken ?: SUPABASE_ANON_KEY}")
                .addHeader("Content-Type", "application/json")
                .post(jsonPayload.toRequestBody(jsonMediaType))
                .build()
            return okHttpClient.newCall(request).execute().use { res ->
                res.code to res.body?.string().orEmpty().trim()
            }
        }

        try {
            var (code, errorBody) = performPost()
            if (code == 401 && context != null) {
                val refreshed = restoreSessionIfRemembered(context)
                if (refreshed.isSuccess) {
                    val retry = performPost()
                    code = retry.first
                    errorBody = retry.second
                }
            }
            if (code in 200..299) {
                Result.success(Unit)
            } else {
                val detail = errorBody.ifBlank { "Tidak ada detail error dari server" }
                Result.failure(IllegalStateException("Gagal ($code): $detail"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "executePostWithDetail network exception", e)
            Result.failure(
                IllegalStateException("Gagal (network): ${e.message ?: "Kendala koneksi jaringan"}", e)
            )
        }
    }

    private fun executePatch(url: String, jsonPayload: String): Boolean {
        return runCatching {
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer ${currentAccessToken ?: SUPABASE_ANON_KEY}")
                .addHeader("Content-Type", "application/json")
                .patch(jsonPayload.toRequestBody(jsonMediaType))
                .build()
            okHttpClient.newCall(request).execute().use { it.isSuccessful }
        }.getOrDefault(false)
    }

    private fun formatTimestampShort(isoString: String?): String {
        if (isoString.isNullOrBlank() || isoString == "null") return "Hari ini"
        return if (isoString.length >= 16) {
            isoString.substring(0, 10) + " " + isoString.substring(11, 16)
        } else {
            isoString
        }
    }
}
