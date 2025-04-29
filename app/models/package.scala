package models

import java.sql.Timestamp
import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId, ZoneOffset, ZonedDateTime}
import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}

import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.json._
import slick.ast.BaseTypedType
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._

object ZonedDateTimeUtils {
  // Custom formatter that doesn't include zone ID (like [Asia/Tokyo])
  private val formatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSX")

  // Store ZonedDateTime as Timestamp for PostgreSQL
  implicit val zonedDateTimeColumnType: BaseColumnType[ZonedDateTime] =
    MappedColumnType.base[ZonedDateTime, Timestamp](
      // To database: Convert ZonedDateTime to SQL Timestamp preserving the instant
      zonedDateTime => Timestamp.from(zonedDateTime.toInstant),
      // From database: Convert SQL Timestamp to ZonedDateTime using fixed offset
      timestamp => {
        val instant = timestamp.toInstant
        // Use ZoneOffset.ofHours(9) for Japan timezone without zone ID
        val zdt = ZonedDateTime.ofInstant(instant, ZoneOffset.ofHours(9))
        // Format and parse to remove zone ID
        ZonedDateTime.parse(zdt.format(formatter), formatter)
      }
    )
}
package object models {
  // Use the same formatter pattern as in ZonedDateTimeUtils
  private val zonedDateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSX")

  // JSON formatter for ZonedDateTime that omits zone ID information
  implicit val zonedDateTimeFormat: Format[ZonedDateTime] =
    new Format[ZonedDateTime] {
      def reads(json: JsValue): JsResult[ZonedDateTime] = {
        try {
          // Extract string value
          val dateStr = json.as[String]

          // Handle the specific problematic format "2025-04-13 23:16:59.628606+09"
          if (dateStr.contains(" ") && dateStr.matches(".*\\+\\d{2}$")) {
            try {
              // Extract components manually
              val regex =
                "(\\d{4}-\\d{2}-\\d{2}) (\\d{2}:\\d{2}:\\d{2}\\.\\d+)\\+(\\d{2})".r
              dateStr match {
                case regex(date, time, zone) =>
                  // Parse the local date-time part
                  val formatter =
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
                  val localDateTime =
                    LocalDateTime.parse(s"$date $time", formatter)
                  // Convert zone offset "+09" to hours
                  val zoneOffsetHours = zone.toInt
                  // Create ZonedDateTime with the offset but no zone ID
                  val result = ZonedDateTime.of(
                    localDateTime,
                    ZoneOffset.ofHours(zoneOffsetHours)
                  )
                  // Format and parse to remove any zone ID
                  val cleanResult = ZonedDateTime.parse(
                    result.format(zonedDateTimeFormatter),
                    zonedDateTimeFormatter
                  )
                  JsSuccess(cleanResult)
                case _ =>
                  JsError(s"Could not match datetime pattern in '$dateStr'")
              }
            } catch {
              case e: Exception =>
                JsError(
                  s"Failed to manually parse datetime '$dateStr': ${e.getMessage}"
                )
            }
          } else if (dateStr.contains("[")) {
            // Handle strings with zone ID like "2025-04-13T23:16:59.628606+09:00[Asia/Tokyo]"
            try {
              // Parse with standard parser, then reformat to remove zone ID
              val zdt = ZonedDateTime.parse(dateStr)
              val cleanZdt = ZonedDateTime.parse(
                zdt.format(zonedDateTimeFormatter),
                zonedDateTimeFormatter
              )
              JsSuccess(cleanZdt)
            } catch {
              case e: Exception =>
                JsError(
                  s"Failed to parse datetime with zone ID: ${e.getMessage}"
                )
            }
          } else {
            // Standard format without zone ID
            try {
              JsSuccess(ZonedDateTime.parse(dateStr))
            } catch {
              case e: Exception =>
                JsError(s"Failed to parse datetime: ${e.getMessage}")
            }
          }
        } catch {
          case e: Exception =>
            JsError(
              s"Failed to extract string value from JSON: ${e.getMessage}"
            )
        }
      }

      // When serializing to JSON, use our custom formatter without zone ID
      def writes(dateTime: ZonedDateTime): JsValue = {
        // Format and parse to remove any zone ID information
        val cleanDateTime = ZonedDateTime.parse(
          dateTime.format(zonedDateTimeFormatter),
          zonedDateTimeFormatter
        )
        JsString(cleanDateTime.format(zonedDateTimeFormatter))
      }
    }
}
