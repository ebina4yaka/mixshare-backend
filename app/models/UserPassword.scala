package models

import java.time.ZonedDateTime

case class UserPassword(
    userId: Long,
    passwordHash: String,
    createdAt: ZonedDateTime = ZonedDateTime.now(),
    updatedAt: ZonedDateTime = ZonedDateTime.now()
)
