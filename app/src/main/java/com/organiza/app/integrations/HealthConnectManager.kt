package com.organiza.app.integrations

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant

class HealthConnectManager(private val context: Context) {
    val permissions: Set<String> = setOf(HealthPermission.getReadPermission(SleepSessionRecord::class))

    fun availability(): Int = HealthConnectClient.getSdkStatus(context)

    fun isAvailable(): Boolean = availability() == HealthConnectClient.SDK_AVAILABLE

    suspend fun hasPermissions(): Boolean {
        if (!isAvailable()) return false
        val granted = HealthConnectClient.getOrCreate(context).permissionController.getGrantedPermissions()
        return granted.containsAll(permissions)
    }

    suspend fun readLatestSleep(): SleepSummary? {
        if (!hasPermissions()) return null
        val client = HealthConnectClient.getOrCreate(context)
        val now = Instant.now()
        val response = client.readRecords(
            ReadRecordsRequest<SleepSessionRecord>(
                timeRangeFilter = TimeRangeFilter.between(now.minus(Duration.ofHours(48)), now),
                ascendingOrder = false,
                pageSize = 50
            )
        )
        val latest = response.records.maxByOrNull { it.endTime } ?: return null
        val hours = Duration.between(latest.startTime, latest.endTime).toMinutes() / 60.0
        return SleepSummary(hours = hours, endTime = latest.endTime.toString())
    }

    data class SleepSummary(val hours: Double, val endTime: String)
}
