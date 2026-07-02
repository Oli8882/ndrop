package com.olii.ndrop.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.olii.ndrop.data.db.*
import com.olii.ndrop.data.db.DatabaseProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * NDrop — Hilt AppModule
 * Signature: Olii-8882
 */

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ndrop_prefs")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NDropDatabase =
        DatabaseProvider.getInstance(context)

    @Provides fun provideDropDao(db: NDropDatabase): DropDao = db.dropDao()
    @Provides fun provideParkingDao(db: NDropDatabase): ParkingDao = db.parkingDao()
    @Provides fun provideTagDao(db: NDropDatabase): TagDao = db.tagDao()
    @Provides fun provideStreakDao(db: NDropDatabase): StreakDao = db.streakDao()
    @Provides fun provideScanPatternDao(db: NDropDatabase): ScanPatternDao = db.scanPatternDao()

    @Provides @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.dataStore
}
