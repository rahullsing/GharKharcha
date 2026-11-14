package com.gharkharcha.data.db

import androidx.room.migration.Migration

/**
 * Every schema change bumps [GharKharchaDatabase] version and adds a Migration here
 * (or an @AutoMigration on the database). Destructive fallback is never enabled.
 * Exported schemas live in app/schemas/ and are verified by MigrationTest.
 */
object Migrations {
    val ALL: Array<Migration> = emptyArray()
}
