package com.example.core.repository

import com.example.core.model.DeliveryOrder
import com.example.core.model.DeliveryStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * DailyStatsState represents the Single Source of Truth for all daily statistics.
 *
 * Invariant:
 * totalOrders == completedOrders + inDeliveryOrders + newTasksOrders + failedReturnOrders
 *
 * e.g. 20 == 12 Selesai + 5 Diantar + 3 Menunggu + 0 Retur
 */
data class DailyStatsState(
    val periodLabel: String = "Hari Ini",
    val dateText: String = "Hari Ini",
    val totalOrders: Int = 0,
    val completedOrders: Int = 0,
    val inDeliveryOrders: Int = 0,
    val newTasksOrders: Int = 0,
    val failedReturnOrders: Int = 0
) {
    /**
     * Mathematical check:
     * Guarantees total is strictly equal to the sum of all categories
     */
    val isMathematicallyConsistent: Boolean
        get() = totalOrders == (completedOrders + inDeliveryOrders + newTasksOrders + failedReturnOrders)

    val progressFraction: Float
        get() = if (totalOrders > 0) completedOrders.toFloat() / totalOrders.toFloat() else 0f

    val formulaDescription: String
        get() = "$completedOrders Selesai + $inDeliveryOrders Diantar + $newTasksOrders Menunggu${if (failedReturnOrders > 0) " + $failedReturnOrders Retur" else ""} = $totalOrders Total"
}

/**
 * DailyStatsProvider:
 * Unified State Management provider (Single Source of Truth) controlling
 * all metrics displayed on the Home Dashboard.
 */
class DailyStatsProvider private constructor(
    private val repository: OrderRepository
) {
    private val _stats = MutableStateFlow(computeStats(repository.orders.value))
    val stats: StateFlow<DailyStatsState> = _stats.asStateFlow()

    init {
        updateFromOrders(repository.orders.value)
    }

    fun updateFromOrders(orders: List<DeliveryOrder>) {
        _stats.value = computeStats(orders)
    }

    fun computeStats(orders: List<DeliveryOrder>): DailyStatsState {
        val completed = orders.count { it.status == DeliveryStatus.DELIVERED }
        val inDelivery = orders.count {
            it.status == DeliveryStatus.IN_TRANSIT ||
            it.status == DeliveryStatus.ARRIVED ||
            it.status == DeliveryStatus.PENDING_VERIFICATION
        }
        val newTasks = orders.count { it.status == DeliveryStatus.NEW }
        val failed = orders.count { it.status == DeliveryStatus.FAILED }
        val total = completed + inDelivery + newTasks + failed

        return DailyStatsState(
            periodLabel = "Hari Ini",
            dateText = "Hari Ini • 28 Okt 2026",
            totalOrders = total,
            completedOrders = completed,
            inDeliveryOrders = inDelivery,
            newTasksOrders = newTasks,
            failedReturnOrders = failed
        )
    }

    companion object {
        @Volatile
        private var instance: DailyStatsProvider? = null

        fun getInstance(repository: OrderRepository = OrderRepository.instance): DailyStatsProvider {
            return instance ?: synchronized(this) {
                instance ?: DailyStatsProvider(repository).also { instance = it }
            }
        }
    }
}
