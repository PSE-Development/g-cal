package com.gcal.app.model.modelData

import android.util.Log
import com.gcal.app.model.asEventCompletion
import com.gcal.app.model.modelData.data.system.EventCD
import com.gcal.app.model.modelData.data.system.UserAppointment
import com.gcal.app.model.modelData.data.system.UserSharedEvent
import com.gcal.app.model.modelData.data.system.UserToDo
import com.gcal.app.model.modelData.data.system.XP
import com.gcal.app.model.modelData.repo.EventRepo
import com.gcal.app.model.modelData.repo.UserRepo
import com.gcal.app.model.modelFacade.general.Appointment
import com.gcal.app.model.modelFacade.general.SharedEvent
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.floor
import kotlin.random.Random


class XpDistributor(val userRepo: UserRepo, val eventRepo: EventRepo) {
    val rng: Random = Random.Default
    val maxValue = 128.0

    suspend fun distributeXp() {
        val now = LocalDateTime.now()
        val startOfDay = now.toLocalDate().atStartOfDay()
        val endOfDay = now.toLocalDate().atTime(LocalTime.MAX)
        val tomorrow = now.plusDays(1).toLocalDate()
        val startOfTomorrow = tomorrow.atStartOfDay()
        val endOfTomorrow = tomorrow.atTime(LocalTime.MAX)
        if (checkToday(startOfDay, endOfDay)) {
            Log.i("XpDistributor.distributeXp()", "Distributing Xp for Today.")
            val appointments = eventRepo.getAppointmentsIn(startOfDay, endOfDay).first()
            val distributed = calculateAppointments(appointments)
            for (event in distributed)
                eventRepo.updateAppointment(event)
        }
        if (checkTomorrow(startOfTomorrow, endOfTomorrow)) {
            Log.i("XpDistributor.distributeXp()", "Distributing Xp for Tomorrow.")
            val appointments = eventRepo.getAppointmentsIn(startOfTomorrow, endOfTomorrow).first()
            val distributed = calculateAppointments(appointments)
            for (event in distributed)
                eventRepo.updateAppointment(event)
        }
    }

    private fun calculateAppointments(appointments: List<Appointment>): List<UserAppointment> {
        val totalMinutes = appointments.sumOf {
            Duration.between(it.start(), it.end()).toMinutes()
        }
        if (totalMinutes == 0L) return emptyList()
        val basisFactor = (maxValue / totalMinutes).coerceAtMost(0.275)
        return appointments.map { appointment ->
            val durationMinutes =
                Duration.between(appointment.start(), appointment.end()).toMinutes()
            val randomFactor = rng.nextDouble(0.8, 1.0) * basisFactor
            val xp = floor(durationMinutes * randomFactor).toInt()
            UserAppointment(
                appointment.eventID(),
                appointment.eventName(),
                appointment.description(),
                appointment.start(),
                appointment.end(),
                appointment.group(),
                XP.from(xp),
                appointment.checkCompletion().asEventCompletion()
            )
        }
    }

    private suspend fun checkToday(startOfDay: LocalDateTime, endOfDay: LocalDateTime): Boolean {
        val eventsToday = eventRepo.getAppointmentsIn(startOfDay, endOfDay).first()
        if (eventsToday.isEmpty()) return false
        return eventsToday.all { it.experiencePoints().value() == 0 }
    }

    private suspend fun checkTomorrow(
        startOfTomorrow: LocalDateTime,
        endOfTomorrow: LocalDateTime
    ): Boolean {
        val appointments = eventRepo.getAppointmentsIn(startOfTomorrow, endOfTomorrow).first()
        if (appointments.isEmpty()) return false
        return !appointments.all { it.experiencePoints().value() != 0 }

    }

    suspend fun calculateTodo(todo: UserToDo): UserToDo {
        var xpToday = xpToday()
        if (xpToday < 50) {
            xpToday = 50
        }
        val rarity = rng.nextDouble(1.0)
        val factor: Double = if (rarity < 0.3) {
            0.1
        } else if (rarity < 0.6) {
            0.15
        } else if (rarity < 0.9) {
            0.2
        } else {
            0.25
        }
        return UserToDo(
            todo.eventID(),
            todo.eventName(),
            todo.description(),
            todo.end(),
            XP.from(floor(xpToday * factor).toInt()),
            EventCD(todo.eventID(), true, LocalDateTime.now())
        )
    }

    companion object {
        fun calculateSharedEvent(sharedEvent: SharedEvent): UserSharedEvent {
            val durationMinutes =
                Duration.between(sharedEvent.start(), sharedEvent.end()).toMinutes()
            return UserSharedEvent(
                sharedEvent.eventID(),
                sharedEvent.eventName(),
                sharedEvent.description(),
                sharedEvent.start(),
                sharedEvent.end(),
                sharedEvent.group(),
                XP.from(floor(durationMinutes * 0.3).toInt()),
                sharedEvent.checkCompletion().asEventCompletion()
            )
        }
    }

    private suspend fun xpToday(): Int {
        val today = LocalDateTime.now()
        val startOfDay = today.toLocalDate().atStartOfDay()
        val endOfDay = today.plusDays(1).toLocalDate().atStartOfDay()
        val appointments = eventRepo.getAppointmentsIn(startOfDay, endOfDay).first()
        return appointments.filter { it.checkCompletion().completed() }
            .sumOf { it.experiencePoints().value() }
    }
}