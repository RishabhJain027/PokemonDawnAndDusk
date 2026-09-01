package com.dawnanddusk.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dawnanddusk.data.local.catalog.StaticCreatureCatalog
import com.dawnanddusk.data.local.dao.*
import com.dawnanddusk.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        PlayerEntity::class,
        CreatureEntity::class,
        CaptureEntity::class,
        SpawnEntity::class,
        SessionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao
    abstract fun creatureDao(): CreatureDao
    abstract fun spawnDao(): SpawnDao
    abstract fun captureDao(): CaptureDao
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pokemon_dawn_and_dusk.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Seed the static 151 creature catalog upon initial DB creation
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.creatureDao()?.insertAll(StaticCreatureCatalog.allCreatures)
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
