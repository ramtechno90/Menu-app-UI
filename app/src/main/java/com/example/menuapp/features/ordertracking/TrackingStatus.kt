
package com.example.menuapp.features.ordertracking

enum class TrackingStatus {
    PLACED, CONFIRMED, PREPARING, COMPLETED, OUT_FOR_DELIVERY, DELIVERED
}

fun String?.toTrackingStatus(): TrackingStatus = when (this) {
    "PENDING" -> TrackingStatus.PLACED
    "ACCEPTED" -> TrackingStatus.CONFIRMED
    "PREPARING" -> TrackingStatus.PREPARING
    "COMPLETED", "READY_FOR_DELIVERY" -> TrackingStatus.COMPLETED
    "OUT_FOR_DELIVERY", "PICKED_UP" -> TrackingStatus.OUT_FOR_DELIVERY
    "DELIVERED" -> TrackingStatus.DELIVERED
    else -> TrackingStatus.PLACED
}
