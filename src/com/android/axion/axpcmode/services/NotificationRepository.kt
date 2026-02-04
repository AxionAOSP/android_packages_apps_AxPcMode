package com.android.axion.axpcmode.services

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.runtime.mutableStateListOf

data class NotificationGroup(
    val groupKey: String,
    val packageName: String,
    val summary: StatusBarNotification?,
    val children: List<StatusBarNotification>,
)

object NotificationRepository {
    val activeNotifications = mutableStateListOf<StatusBarNotification>()

    fun addOrUpdate(sbn: StatusBarNotification) {
        if (!shouldShowNotification(sbn)) {
            activeNotifications.removeAll { it.key == sbn.key }
            return
        }

        activeNotifications.removeAll { it.key == sbn.key }
        activeNotifications.add(0, sbn)
    }

    private fun shouldShowNotification(sbn: StatusBarNotification): Boolean {
        val n = sbn.notification

        if (
            n.category == Notification.CATEGORY_TRANSPORT ||
                n.extras.containsKey(Notification.EXTRA_MEDIA_SESSION) ||
                n.extras.containsKey("android.mediaSession")
        ) {
            return false
        }

        return true
    }

    fun getGroupedNotifications(): List<NotificationGroup> {
        val grouped = mutableMapOf<String, MutableList<StatusBarNotification>>()
        val summaries = mutableMapOf<String, StatusBarNotification>()

        for (sbn in activeNotifications) {
            val groupKey = sbn.groupKey ?: sbn.packageName
            val isGroupSummary = (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0

            if (isGroupSummary) {
                summaries[groupKey] = sbn
            } else {
                grouped.getOrPut(groupKey) { mutableListOf() }.add(sbn)
            }
        }

        return grouped
            .map { (groupKey, children) ->
                NotificationGroup(
                    groupKey = groupKey,
                    packageName = children.firstOrNull()?.packageName ?: "",
                    summary = summaries[groupKey],
                    children = children.sortedByDescending { it.postTime },
                )
            }
            .sortedByDescending { it.children.firstOrNull()?.postTime ?: 0L }
    }

    var listener: NotificationListenerService? = null

    fun dismiss(sbn: StatusBarNotification) {

        if (sbn.isClearable) {
            activeNotifications.removeAll { it.key == sbn.key }
            listener?.cancelNotification(sbn.key)
        }
    }

    fun remove(sbn: StatusBarNotification) {
        activeNotifications.removeAll { it.key == sbn.key }
    }

    fun clearAll() {

        val clearable = activeNotifications.filter { it.isClearable }
        clearable.forEach { sbn -> listener?.cancelNotification(sbn.key) }
        activeNotifications.removeAll { it.isClearable }
    }

    fun dismissGroup(groupKey: String) {

        val toRemove =
            activeNotifications.filter {
                (it.groupKey ?: it.packageName) == groupKey && it.isClearable
            }
        toRemove.forEach { sbn -> listener?.cancelNotification(sbn.key) }
        activeNotifications.removeAll {
            (it.groupKey ?: it.packageName) == groupKey && it.isClearable
        }
    }
}
