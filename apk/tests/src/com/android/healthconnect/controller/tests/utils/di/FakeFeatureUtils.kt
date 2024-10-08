package com.android.healthconnect.controller.tests.utils.di

import com.android.healthconnect.controller.utils.FeatureUtils
import com.android.healthconnect.controller.utils.FeaturesModule
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

class FakeFeatureUtils : FeatureUtils {

    private var isSessionTypesEnabled = true
    private var isExerciseRoutesReadAllEnabled = true
    private var isEntryPointsEnabled = true
    private var isNewAppPriorityEnabled = false
    private var isNewInformationArchitectureEnabled = false
    private var isBackgroundReadEnabled = false
    private var isHistoryReadEnabled = false
    private var isSkinTemperatureEnabled = false
    private var isPlannedExerciseEnabled = false
    private var isPersonalHealthRecordEnabled = false

    fun setIsSessionTypesEnabled(boolean: Boolean) {
        isSessionTypesEnabled = boolean
    }

    fun setIsExerciseRoutesReadAllEnabled(boolean: Boolean) {
        isExerciseRoutesReadAllEnabled = boolean
    }

    fun setIsEntryPointsEnabled(boolean: Boolean) {
        isEntryPointsEnabled = boolean
    }

    fun setIsNewAppPriorityEnabled(boolean: Boolean) {
        isNewAppPriorityEnabled = boolean
    }

    fun setIsNewInformationArchitectureEnabled(boolean: Boolean) {
        isNewInformationArchitectureEnabled = boolean
    }

    fun setIsBackgroundReadEnabled(isBackgroundReadEnabled: Boolean) {
        this.isBackgroundReadEnabled = isBackgroundReadEnabled
    }

    fun setIsHistoryReadEnabled(isHistoryReadEnabled: Boolean) {
        this.isHistoryReadEnabled = isHistoryReadEnabled
    }

    fun setIsSkinTemperatureEnabled(isSkinTemperatureEnabled: Boolean) {
        this.isSkinTemperatureEnabled = isSkinTemperatureEnabled
    }

    fun setIsPlannedExerciseEnabled(isPlannedExerciseEnabled: Boolean) {
        this.isPlannedExerciseEnabled = isPlannedExerciseEnabled
    }

    fun setIsPersonalHealthRecordEnabled(isPersonalHealthRecordEnabled: Boolean) {
        this.isPersonalHealthRecordEnabled = isPersonalHealthRecordEnabled
    }

    override fun isNewAppPriorityEnabled(): Boolean {
        return isNewAppPriorityEnabled
    }

    override fun isNewInformationArchitectureEnabled(): Boolean {
        return isNewInformationArchitectureEnabled
    }

    override fun isSessionTypesEnabled(): Boolean {
        return isSessionTypesEnabled
    }

    override fun isExerciseRouteReadAllEnabled(): Boolean {
        return isExerciseRoutesReadAllEnabled
    }

    override fun isEntryPointsEnabled(): Boolean {
        return isEntryPointsEnabled
    }

    override fun isBackgroundReadEnabled(): Boolean {
        return isBackgroundReadEnabled
    }

    override fun isHistoryReadEnabled(): Boolean {
        return isHistoryReadEnabled
    }

    override fun isPlannedExerciseEnabled(): Boolean {
        return isPlannedExerciseEnabled
    }

    override fun isSkinTemperatureEnabled(): Boolean {
        return isSkinTemperatureEnabled
    }

    override fun isPersonalHealthRecordEnabled(): Boolean {
        return isPersonalHealthRecordEnabled
    }
}

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [FeaturesModule::class])
object FakeFeaturesUtilsModule {
    @Provides @Singleton fun providesFeaturesUtils(): FeatureUtils = FakeFeatureUtils()
}
