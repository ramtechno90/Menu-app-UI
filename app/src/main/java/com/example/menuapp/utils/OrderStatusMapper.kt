package com.example.menuapp.utils

object OrderStatusMapper {
    fun mapOrderStatus(status: String): String {
        return when (status) {
            "PENDING" -> "Order Placed"
            "ACCEPTED" -> "Order Confirmed"
            "PREPARING" -> "Preparing Food"
            "COMPLETED", "READY_FOR_DELIVERY" -> "Order Completed"
            "OUT_FOR_DELIVERY", "PICKED_UP" -> "Out for Delivery"
            "DELIVERED" -> "Delivered"
            else -> status
        }
    }
}