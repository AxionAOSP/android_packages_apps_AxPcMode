/*
 * Copyright (C) 2025-2026 AxionOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.axion.axpcmode.di

import android.content.Context
import com.android.axion.axpcmode.services.AxPlatformRepository
import com.android.axion.axpcmode.services.CalendarWeatherRepository
import com.android.axion.axpcmode.services.DesktopWallpaperRepository
import com.android.axion.axpcmode.services.MediaRepository
import com.android.axion.axpcmode.services.QuickSettingsRepository
import com.android.axion.axpcmode.services.TaskbarNotificationHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlin.annotation.AnnotationRetention.BINARY
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Qualifier @Retention(BINARY) annotation class MainScope

@Qualifier @Retention(BINARY) annotation class IoScope

@Qualifier @Retention(BINARY) annotation class Background

@Qualifier @Retention(BINARY) annotation class Main

@Module
@InstallIn(SingletonComponent::class)
object AxPcModeModule {

    @Provides
    @Singleton
    @MainScope
    fun provideMainScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Provides
    @Singleton
    @IoScope
    fun provideIoScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Provides
    @Singleton
    fun provideAxPlatformRepository(
        @ApplicationContext context: Context
    ): AxPlatformRepository = AxPlatformRepository(context)

    @Provides
    @Singleton
    fun provideMediaRepository(@ApplicationContext context: Context): MediaRepository =
        MediaRepository(context)

    @Provides
    @Singleton
    fun provideCalendarWeatherRepository(
        @ApplicationContext context: Context
    ): CalendarWeatherRepository = CalendarWeatherRepository(context)

    @Provides
    @Singleton
    fun provideQuickSettingsRepository(
        @ApplicationContext context: Context,
        platformRepo: AxPlatformRepository,
    ): QuickSettingsRepository = QuickSettingsRepository(context, platformRepo)

    @Provides
    @Singleton
    fun provideTaskbarNotificationHelper(
        @ApplicationContext context: Context
    ): TaskbarNotificationHelper = TaskbarNotificationHelper(context)

    @Provides @Background fun provideBackgroundDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides @Main fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

    @Provides
    @Singleton
    fun provideDesktopWallpaperRepository(
        @ApplicationContext context: Context,
        @IoScope scope: CoroutineScope,
    ): DesktopWallpaperRepository = DesktopWallpaperRepository(context, scope)
}
