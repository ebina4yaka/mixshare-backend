package models

import java.time.ZonedDateTime

case class User(
    id: Option[Long] = None,
    username: String,
    email: String,
    passwordHash: String,
    createdAt: ZonedDateTime = ZonedDateTime.now(),
    updatedAt: ZonedDateTime = ZonedDateTime.now()
)
