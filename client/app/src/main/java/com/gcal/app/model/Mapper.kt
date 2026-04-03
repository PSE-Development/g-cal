package com.gcal.app.model

import com.gcal.app.model.localData.entity.*
import com.gcal.app.model.localData.relation.*
import com.gcal.app.model.modelData.data.achievements.AchievementType
import com.gcal.app.model.modelData.data.achievements.UserAchievement
import com.gcal.app.model.modelData.data.system.*
import com.gcal.app.model.modelData.data.system.Group
import com.gcal.app.model.modelData.data.system.UserGroup
import com.gcal.app.model.modelFacade.general.*
import java.time.LocalDateTime

fun AchievementEntity.toDomainModel(): UserAchievement = UserAchievement(
    type = AchievementType.fromDisplayName(name),
    isCompleted,
    achievedAt = completedAtTimestamp
)


/**
 * Maps [AppointmentRelation] to the [Appointment] facade, creating a [UserAppointment].
 */
fun AppointmentRelation.asDomainModel(): Appointment = UserAppointment(
    id = appointment.eventId,
    name = appointment.name,
    description = appointment.description,
    startAt = appointment.startTimeTimestamp,
    endAt = appointment.endTimeTimestamp,
    group = group?.asDomainModel() ?: NoGroup,
    xpValue = XP.from(appointment.xpValue),
    detail = completion.asDomainModel()
)

/**
 * Maps [SharedEventRelation] to the [SharedEvent] facade, creating a [UserSharedEvent].
 */
fun SharedEventRelation.asDomainModel(): SharedEvent = UserSharedEvent(
    id = sharedEvent.eventId,
    name = sharedEvent.name,
    description = sharedEvent.description,
    startAt = sharedEvent.startTimeTimestamp,
    endAt = sharedEvent.endTimeTimestamp,
    group = group?.asDomainModel() ?: NoGroup,
    xpValue = XP.from(sharedEvent.xpValue),
    detail = completion.asDomainModel()
)

/**
 * Maps [ToDoRelation] to the [ToDo] facade, creating a [UserToDo].
 */
fun ToDoRelation.asDomainModel(): ToDo = UserToDo(
    id = toDo.eventId,
    name = toDo.name,
    description = toDo.description,
    deadline = toDo.deadlineTimestamp,
    xpValue = XP.from(toDo.xpValue),
    detail = completion.asDomainModel()
)

/**
 * Maps [EventCompletionEntity] to the domain-specific [EventCD] (Completion Data).
 */
fun EventCompletionEntity.asDomainModel(): EventCD = EventCD(
    eventId = eventId,
    isCompleted = isCompleted,
    completionTime = completedAtTimestamp
)

/**
 * Maps [FriendEntity] to a [RegularUser].
 */
fun FriendEntity.asDomainModel(): RegularUser = RegularUser(
    username = username,
    name = name,
    totalXp = XP.from(xpTotal)
)

/**
 * Maps [GroupEntity] to a [UserGroup].
 */
fun GroupEntity.asDomainModel(): Group = Group.from(
    id = id,
    name = name,
    color = colour
)

/**
 * Maps [LeaderboardEntity] to a [LeaderboardEntry].
 */
fun LeaderboardEntity.asDomainModel(): LeaderboardEntry = LeaderboardEntry(
    rank = rank,
    username = username,
    name = name,
    totalXp = XP.from(xpTotal)
)

/**
 * Maps the local user profile [ProfileEntity] to a [PersonalUser].
 */
fun ProfileEntity?.asDomainModel(): PersonalUser {
    return PersonalUser(
        username = this?.username ?: "Guest",
        name = this?.name ?: "Guest",
        totalXp = XP.from(this?.xpTotal ?: 0),
        dailyXp = XP.from(this?.xpToday ?: 0)
    )
}

fun Appointment.asDatabaseEntity(): AppointmentEntity = AppointmentEntity(
    eventId = eventID(),
    name = eventName(),
    description = description(),
    startTimeTimestamp = start(),
    endTimeTimestamp = end(),
    groupId = if (group().groupId() == 0L) null else group().groupId(),
    xpValue = experiencePoints().value()
)


/**
 * Converts a [ToDo] facade into a [ToDoEntity] for database storage.
 */
fun ToDo.asDatabaseEntity(): ToDoEntity = ToDoEntity(
    eventId = eventID(),
    name = eventName(),
    description = description(),
    deadlineTimestamp = end(),
    xpValue = experiencePoints().value()
)

/**
 * Converts a [SharedEvent] facade into a [SharedEventEntity] for local storage.
 */
fun SharedEvent.asDatabaseEntity(): SharedEventEntity = SharedEventEntity(
    eventId = eventID(),
    name = eventName(),
    description = description(),
    groupId = if (group().groupId() == 0L) null else group().groupId(),
    startTimeTimestamp = start(),
    endTimeTimestamp = end(),
    xpValue = experiencePoints().value()
)

/**
 * Converts the [PersonalUser] domain model into a [ProfileEntity] for local storage.
 */
fun PersonalUser.asDatabaseEntity(): ProfileEntity = ProfileEntity(
    username = username,
    name = name,
    xpTotal = totalXp.value(),
    xpToday = dailyXp.value()
)


/**
 * Converts a domain [LeaderboardEntry] into a [LeaderboardEntity] for persistence.
 */
fun LeaderboardEntry.asDatabaseEntity(): LeaderboardEntity =
    LeaderboardEntity(
        rank = rank,
        username = username,
        name = name,
        xpTotal = totalXp.value()
    )


/**
 * Converts a [Group] domain model into a [GroupEntity] for persistence.
 */
fun Group.asDatabaseEntity(): GroupEntity = GroupEntity(
    id = groupId(),
    name = groupName(),
    colour = groupColour()
)


/**
 * Converts a [User] facade into a [FriendEntity] for local storage.
 */
fun User.asDatabaseEntity(): FriendEntity = FriendEntity(
    username = username(),
    name = name(),
    xpTotal = experiencePoints().value()
)

fun CompletionDetail.asDatabaseEntity(): EventCompletionEntity = EventCompletionEntity(
    eventId = identifier(),
    isCompleted = completed(),
    completedAtTimestamp = completionDate()
)

fun Achievement.asCompletedDatabaseEntity(): AchievementEntity = AchievementEntity(
    name = achievementName(),
    isCompleted = isCompleted(),
    completedAtTimestamp = LocalDateTime.now()
)

fun UserAppointment.asCompletedDomainModel(): UserAppointment = UserAppointment(
    eventID(),
    eventName(),
    description(),
    start(),
    end(),
    group(),
    experiencePoints(),
    EventCD(eventID(), true, LocalDateTime.now())
)

fun UserSharedEvent.asCompletedDomainModel(): UserSharedEvent = UserSharedEvent(
    eventID(),
    eventName(),
    description(),
    start(),
    end(),
    group(),
    experiencePoints(),
    EventCD(eventID(), true, LocalDateTime.now())
)

// Interface -> DomainModel

fun CompletionDetail.asEventCompletion(): EventCD = EventCD(
    identifier(),
    completed(),
    completionDate()
)


fun Appointment.asSharedEvent(): UserSharedEvent = UserSharedEvent(
    eventID(),
    eventName(),
    description(),
    start(),
    end(),
    group(),
    experiencePoints(),
    checkCompletion().asEventCompletion()
)

fun User.asRegularUser(): RegularUser = RegularUser(
    username(), name(), experiencePoints()
)

fun PersonalUser.asUpdatedDomainModel(xpUpdate: XP): PersonalUser = PersonalUser(
    username,
    name,
    XP.from(experiencePoints().value() + xpUpdate.value()),
    XP.from(dailyProgress().value() + xpUpdate.value())
)
