package com.gharkharcha.data.db

import androidx.room.TypeConverter
import java.time.LocalDate

/** Dates are stored as epoch-day (Long) so range queries and sorting are plain integer ops. */
class Converters {
    @TypeConverter
    fun localDateToEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(epochDay: Long?): LocalDate? = epochDay?.let(LocalDate::ofEpochDay)
}
