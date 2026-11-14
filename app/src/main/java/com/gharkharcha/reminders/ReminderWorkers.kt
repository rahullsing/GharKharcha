package com.gharkharcha.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.gharkharcha.MainActivity
import com.gharkharcha.R
import com.gharkharcha.data.dao.AttendanceDao
import com.gharkharcha.data.dao.PaymentDao
import com.gharkharcha.data.dao.ProjectDao
import com.gharkharcha.util.Money
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

const val CHANNEL_REMINDERS = "gharkharcha_reminders"
const val WORK_DAILY_ATTENDANCE = "work_daily_attendance"
const val WORK_WEEKLY_PAYDAY = "work_weekly_payday"
const val NOTIF_ID_ATTENDANCE = 1001
const val NOTIF_ID_PAYDAY = 1002

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReminderEntryPoint {
    fun attendanceDao(): AttendanceDao
    fun paymentDao(): PaymentDao
    fun projectDao(): ProjectDao
}

fun ensureReminderNotificationChannel(context: Context) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channel = NotificationChannel(
        CHANNEL_REMINDERS,
        "Reminders & Alerts",
        NotificationManager.IMPORTANCE_DEFAULT,
    ).apply {
        description = "Daily attendance reminders and weekly payday alerts"
    }
    manager.createNotificationChannel(channel)
}

class DailyAttendanceWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        ensureReminderNotificationChannel(applicationContext)

        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            ReminderEntryPoint::class.java,
        )
        val projectDao = entryPoint.projectDao()
        val attendanceDao = entryPoint.attendanceDao()

        val projects = projectDao.observeAll().first()
        val today = LocalDate.now()
        var hasAttendanceToday = false

        for (project in projects) {
            val cost = attendanceDao.observeDayCost(project.id, today).first()
            if (cost > 0) {
                hasAttendanceToday = true
                break
            }
        }

        if (!hasAttendanceToday) {
            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                NOTIF_ID_ATTENDANCE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            val notif = NotificationCompat.Builder(applicationContext, CHANNEL_REMINDERS)
                .setSmallIcon(R.drawable.ic_calendar)
                .setContentTitle("Mark today's attendance")
                .setContentText("Don't forget to mark labour attendance for today.")
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIF_ID_ATTENDANCE, notif)
        }

        return Result.success()
    }
}

class WeeklyPaydayWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        ensureReminderNotificationChannel(applicationContext)

        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            ReminderEntryPoint::class.java,
        )
        val attendanceDao = entryPoint.attendanceDao()
        val paymentDao = entryPoint.paymentDao()

        val attTotals = attendanceDao.observeTotalsByLabourer().first()
        val payTotals = paymentDao.observeTotalsByLabourer().first()

        val earnedMap = attTotals.associate { it.labourerId to it.earnedPaise }
        val paidMap = payTotals.associate { it.labourerId to (it.wagePaise + it.advancePaise) }

        val allLabourerIds = (earnedMap.keys + paidMap.keys).toSet()
        var totalPendingDues = 0L

        allLabourerIds.forEach { id ->
            val earned = earnedMap[id] ?: 0L
            val paid = paidMap[id] ?: 0L
            val due = earned - paid
            if (due > 0) {
                totalPendingDues += due
            }
        }

        if (totalPendingDues > 0) {
            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                NOTIF_ID_PAYDAY,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            val notif = NotificationCompat.Builder(applicationContext, CHANNEL_REMINDERS)
                .setSmallIcon(R.drawable.ic_wallet)
                .setContentTitle("Weekly Payday Reminder")
                .setContentText("Pending labour dues: ${Money.format(totalPendingDues)}. Tap to review and settle.")
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIF_ID_PAYDAY, notif)
        }

        return Result.success()
    }
}

object ReminderScheduler {

    fun scheduleDailyAttendance(context: Context, hour: Int, minute: Int) {
        val now = LocalDateTime.now()
        var target = now.with(LocalTime.of(hour, minute, 0))
        if (!target.isAfter(now)) {
            target = target.plusDays(1)
        }
        val initialDelayMinutes = Duration.between(now, target).toMinutes()

        val workRequest = PeriodicWorkRequestBuilder<DailyAttendanceWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_DAILY_ATTENDANCE,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest,
        )
    }

    fun cancelDailyAttendance(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_DAILY_ATTENDANCE)
    }

    fun scheduleWeeklyPayday(context: Context) {
        // Scheduled every 7 days
        val workRequest = PeriodicWorkRequestBuilder<WeeklyPaydayWorker>(7, TimeUnit.DAYS)
            .setInitialDelay(1, TimeUnit.DAYS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_WEEKLY_PAYDAY,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest,
        )
    }

    fun cancelWeeklyPayday(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_WEEKLY_PAYDAY)
    }
}
