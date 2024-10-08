/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.healthconnect.controller.tests.permissions.connectedapps

import android.health.connect.TimeInstantRangeFilter
import com.android.healthconnect.controller.deletion.DeletionType
import com.android.healthconnect.controller.deletion.api.DeleteAppDataUseCase
import com.android.healthconnect.controller.permissions.additionalaccess.ExerciseRouteState
import com.android.healthconnect.controller.permissions.additionalaccess.PermissionUiState
import com.android.healthconnect.controller.permissions.api.GrantHealthPermissionUseCase
import com.android.healthconnect.controller.permissions.api.LoadAccessDateUseCase
import com.android.healthconnect.controller.permissions.api.RevokeAllHealthPermissionsUseCase
import com.android.healthconnect.controller.permissions.api.RevokeHealthPermissionUseCase
import com.android.healthconnect.controller.permissions.app.AppPermissionViewModel
import com.android.healthconnect.controller.permissions.app.LoadAppPermissionsStatusUseCase
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.FitnessPermission
import com.android.healthconnect.controller.permissions.data.HealthPermissionType
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.tests.utils.InstantTaskExecutorRule
import com.android.healthconnect.controller.tests.utils.NOW
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.TestObserver
import com.android.healthconnect.controller.tests.utils.di.FakeGetGrantedHealthPermissionsUseCase
import com.android.healthconnect.controller.tests.utils.di.FakeLoadExerciseRoute
import com.android.healthconnect.controller.utils.FeatureUtils
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
class AppPermissionViewModelTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()
    private val testDispatcher = TestCoroutineDispatcher()

    private val healthPermissionReader: HealthPermissionReader = mock()
    private val getGrantedHealthPermissionsUseCase = FakeGetGrantedHealthPermissionsUseCase()
    private val loadAccessDateUseCase: LoadAccessDateUseCase = mock()
    private val deleteAppDateUseCase: DeleteAppDataUseCase = mock()
    private val revokeAllHealthPermissionsUseCase: RevokeAllHealthPermissionsUseCase = mock()
    private val revokePermissionStatusUseCase: RevokeHealthPermissionUseCase = mock()
    private val grantPermissionsUseCase: GrantHealthPermissionUseCase = mock()
    private val loadExerciseRoutePermissionUseCase = FakeLoadExerciseRoute()

    private lateinit var loadAppPermissionsStatusUseCase: LoadAppPermissionsStatusUseCase
    private lateinit var appPermissionViewModel: AppPermissionViewModel
    @Inject lateinit var appInfoReader: AppInfoReader
    @Inject lateinit var featureUtils: FeatureUtils

    private val readExercisePermission =
        FitnessPermission(HealthPermissionType.EXERCISE, PermissionsAccessType.READ)
    private val readNutritionPermission =
        FitnessPermission(HealthPermissionType.NUTRITION, PermissionsAccessType.READ)
    private val readExerciseRoutesPermission =
        HealthPermission.AdditionalPermission.READ_EXERCISE_ROUTES
    private val readHistoryDataPermission =
        HealthPermission.AdditionalPermission.READ_HEALTH_DATA_HISTORY
    private val readDataInBackgroundPermission =
        HealthPermission.AdditionalPermission.READ_HEALTH_DATA_IN_BACKGROUND
    private val writeSleepPermission =
        FitnessPermission(HealthPermissionType.SLEEP, PermissionsAccessType.WRITE)
    private val writeDistancePermission =
        FitnessPermission(HealthPermissionType.DISTANCE, PermissionsAccessType.WRITE)

    @Captor lateinit var appDataCaptor: ArgumentCaptor<DeletionType.DeletionTypeAppData>
    @Captor lateinit var timeFilterCaptor: ArgumentCaptor<TimeInstantRangeFilter>

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        hiltRule.inject()
        Dispatchers.setMain(testDispatcher)
        loadAppPermissionsStatusUseCase =
            LoadAppPermissionsStatusUseCase(
                getGrantedHealthPermissionsUseCase, healthPermissionReader, Dispatchers.Main)
        appPermissionViewModel =
            AppPermissionViewModel(
                appInfoReader,
                loadAppPermissionsStatusUseCase,
                grantPermissionsUseCase,
                revokePermissionStatusUseCase,
                revokeAllHealthPermissionsUseCase,
                deleteAppDateUseCase,
                loadAccessDateUseCase,
                getGrantedHealthPermissionsUseCase,
                loadExerciseRoutePermissionUseCase,
                healthPermissionReader,
                featureUtils,
                Dispatchers.Main)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun loadsCorrectAccessDate() {
        val accessDate = NOW
        whenever(loadAccessDateUseCase.invoke(TEST_APP_PACKAGE_NAME)).thenReturn(accessDate)
        val result = appPermissionViewModel.loadAccessDate(TEST_APP_PACKAGE_NAME)
        assertThat(result).isEqualTo(accessDate)
    }

    @Test
    fun whenPackageSupported_loadAllPermissions() = runTest {
        whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
        whenever(healthPermissionReader.getValidHealthPermissions(any()))
            .thenReturn(
                listOf(
                    readExercisePermission,
                    readNutritionPermission,
                    writeSleepPermission,
                    writeDistancePermission))
        getGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME, listOf(readExercisePermission.toString()))

        val appPermissionsObserver = TestObserver<List<FitnessPermission>>()
        val grantedPermissionsObserver = TestObserver<Set<FitnessPermission>>()
        appPermissionViewModel.appPermissions.observeForever(appPermissionsObserver)
        appPermissionViewModel.grantedPermissions.observeForever(grantedPermissionsObserver)

        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val appPermissionsResult = appPermissionsObserver.getLastValue()
        val grantedPermissionsResult = grantedPermissionsObserver.getLastValue()

        assertThat(appPermissionsResult)
            .containsExactlyElementsIn(
                listOf(
                    readExercisePermission,
                    readNutritionPermission,
                    writeSleepPermission,
                    writeDistancePermission))
        assertThat(grantedPermissionsResult)
            .containsExactlyElementsIn(setOf(readExercisePermission))
    }

    @Test
    fun whenPackageNotSupported_loadOnlyGrantedPermissions() = runTest {
        whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(false)
        whenever(healthPermissionReader.getValidHealthPermissions(any()))
            .thenReturn(
                listOf(
                    readExercisePermission,
                    readNutritionPermission,
                    writeSleepPermission,
                    writeDistancePermission))
        getGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME, listOf(readExercisePermission.toString()))

        val appPermissionsObserver = TestObserver<List<FitnessPermission>>()
        val grantedPermissionsObserver = TestObserver<Set<FitnessPermission>>()
        appPermissionViewModel.appPermissions.observeForever(appPermissionsObserver)
        appPermissionViewModel.grantedPermissions.observeForever(grantedPermissionsObserver)

        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val appPermissionsResult = appPermissionsObserver.getLastValue()
        val grantedPermissionsResult = grantedPermissionsObserver.getLastValue()

        assertThat(appPermissionsResult).containsExactlyElementsIn(listOf(readExercisePermission))
        assertThat(grantedPermissionsResult)
            .containsExactlyElementsIn(setOf(readExercisePermission))
    }

    @Test
    fun updatePermissions_grant_whenSuccessful_returnsTrue() = runTest {
        val grantedPermissionsObserver = TestObserver<Set<FitnessPermission>>()
        val readExercisePermission =
            FitnessPermission(HealthPermissionType.EXERCISE, PermissionsAccessType.READ)
        appPermissionViewModel.grantedPermissions.observeForever(grantedPermissionsObserver)

        val result =
            appPermissionViewModel.updatePermission(
                TEST_APP_PACKAGE_NAME, readExercisePermission, true)
        advanceUntilIdle()

        assertThat(grantedPermissionsObserver.getLastValue())
            .containsExactlyElementsIn(setOf(readExercisePermission))
        assertThat(result).isTrue()
    }

    @Test
    fun updatePermissions_grant_whenUnsuccessful_returnsFalse() = runTest {
        val grantedPermissionsObserver = TestObserver<Set<FitnessPermission>>()
        val readExercisePermission =
            FitnessPermission(HealthPermissionType.EXERCISE, PermissionsAccessType.READ)
        whenever(grantPermissionsUseCase.invoke(any(), any())).thenThrow(RuntimeException("Error!"))
        appPermissionViewModel.grantedPermissions.observeForever(grantedPermissionsObserver)

        val result =
            appPermissionViewModel.updatePermission(
                TEST_APP_PACKAGE_NAME, readExercisePermission, true)
        advanceUntilIdle()

        assertThat(grantedPermissionsObserver.getLastValue()).isEmpty()
        assertThat(result).isFalse()
    }

    @Test
    fun updatePermissions_deny_whenSuccessful_returnsTrue() = runTest {
        setupDeclaredAndGrantedPermissions()
        val appPermissionsObserver = TestObserver<List<FitnessPermission>>()
        val grantedPermissionsObserver = TestObserver<Set<FitnessPermission>>()
        appPermissionViewModel.appPermissions.observeForever(appPermissionsObserver)
        appPermissionViewModel.grantedPermissions.observeForever(grantedPermissionsObserver)

        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val appPermissionsResult = appPermissionsObserver.getLastValue()
        val grantedPermissionsResult = grantedPermissionsObserver.getLastValue()

        assertThat(appPermissionsResult)
            .containsExactlyElementsIn(
                listOf(
                    readExercisePermission,
                    readNutritionPermission,
                    writeSleepPermission,
                    writeDistancePermission))
        assertThat(grantedPermissionsResult)
            .containsExactlyElementsIn(setOf(readExercisePermission, writeDistancePermission))

        val result =
            appPermissionViewModel.updatePermission(
                TEST_APP_PACKAGE_NAME, writeDistancePermission, false)
        advanceUntilIdle()

        assertThat(grantedPermissionsObserver.getLastValue())
            .containsExactlyElementsIn(setOf(readExercisePermission))
        assertThat(result).isTrue()
    }

    @Test
    fun updatePermissions_denyLastReadPermission_updatesAdditionalPermissions() = runTest {
        whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
        whenever(healthPermissionReader.getValidHealthPermissions(any()))
            .thenReturn(
                listOf(
                    readExercisePermission,
                    readNutritionPermission,
                    readExerciseRoutesPermission,
                    readDataInBackgroundPermission,
                    readHistoryDataPermission,
                    writeSleepPermission,
                    writeDistancePermission,
                ))
        getGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME,
            listOf(
                readNutritionPermission.toString(),
                writeDistancePermission.toString(),
                readExerciseRoutesPermission.additionalPermission,
                readHistoryDataPermission.additionalPermission,
                readDataInBackgroundPermission.additionalPermission))

        loadExerciseRoutePermissionUseCase.setExerciseRouteState(
            ExerciseRouteState(
                exerciseRoutePermissionState = PermissionUiState.ALWAYS_ALLOW,
                exercisePermissionState = PermissionUiState.ALWAYS_ALLOW))

        val appPermissionsObserver = TestObserver<List<FitnessPermission>>()
        val grantedPermissionsObserver = TestObserver<Set<FitnessPermission>>()
        appPermissionViewModel.appPermissions.observeForever(appPermissionsObserver)
        appPermissionViewModel.grantedPermissions.observeForever(grantedPermissionsObserver)

        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val appPermissionsResult = appPermissionsObserver.getLastValue()
        val grantedPermissionsResult = grantedPermissionsObserver.getLastValue()

        assertThat(appPermissionsResult)
            .containsExactlyElementsIn(
                listOf(
                    readExercisePermission,
                    readNutritionPermission,
                    writeSleepPermission,
                    writeDistancePermission))
        assertThat(grantedPermissionsResult)
            .containsExactlyElementsIn(setOf(readNutritionPermission, writeDistancePermission))

        val result =
            appPermissionViewModel.updatePermission(
                TEST_APP_PACKAGE_NAME, readNutritionPermission, false)
        advanceUntilIdle()

        assertThat(grantedPermissionsObserver.getLastValue())
            .containsExactlyElementsIn(setOf(writeDistancePermission))
        assertThat(result).isTrue()
        verify(revokePermissionStatusUseCase)
            .invoke(TEST_APP_PACKAGE_NAME, readExerciseRoutesPermission.additionalPermission)
        verify(revokePermissionStatusUseCase)
            .invoke(TEST_APP_PACKAGE_NAME, readDataInBackgroundPermission.additionalPermission)
        verify(revokePermissionStatusUseCase)
            .invoke(TEST_APP_PACKAGE_NAME, readHistoryDataPermission.additionalPermission)
    }

    @Test
    fun updatePermissions_denyLastReadPermission_skipsERIfAlreadyAskEveryTime() = runTest {
        whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
        whenever(healthPermissionReader.getValidHealthPermissions(any()))
            .thenReturn(
                listOf(
                    readExercisePermission,
                    readNutritionPermission,
                    readExerciseRoutesPermission,
                    readDataInBackgroundPermission,
                    readHistoryDataPermission,
                    writeSleepPermission,
                    writeDistancePermission,
                ))
        getGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME,
            listOf(
                readExercisePermission.toString(),
                writeDistancePermission.toString(),
                readHistoryDataPermission.additionalPermission,
                readDataInBackgroundPermission.additionalPermission))

        loadExerciseRoutePermissionUseCase.setExerciseRouteState(
            ExerciseRouteState(
                exerciseRoutePermissionState = PermissionUiState.ASK_EVERY_TIME,
                exercisePermissionState = PermissionUiState.ALWAYS_ALLOW))

        val appPermissionsObserver = TestObserver<List<FitnessPermission>>()
        val grantedPermissionsObserver = TestObserver<Set<FitnessPermission>>()
        appPermissionViewModel.appPermissions.observeForever(appPermissionsObserver)
        appPermissionViewModel.grantedPermissions.observeForever(grantedPermissionsObserver)

        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val appPermissionsResult = appPermissionsObserver.getLastValue()
        val grantedPermissionsResult = grantedPermissionsObserver.getLastValue()

        assertThat(appPermissionsResult)
            .containsExactlyElementsIn(
                listOf(
                    readExercisePermission,
                    readNutritionPermission,
                    writeSleepPermission,
                    writeDistancePermission))
        assertThat(grantedPermissionsResult)
            .containsExactlyElementsIn(setOf(readExercisePermission, writeDistancePermission))

        val result =
            appPermissionViewModel.updatePermission(
                TEST_APP_PACKAGE_NAME, readExercisePermission, false)
        advanceUntilIdle()

        assertThat(grantedPermissionsObserver.getLastValue())
            .containsExactlyElementsIn(setOf(writeDistancePermission))
        assertThat(result).isTrue()
        verify(revokePermissionStatusUseCase, times(0))
            .invoke(TEST_APP_PACKAGE_NAME, readExerciseRoutesPermission.additionalPermission)
        verify(revokePermissionStatusUseCase)
            .invoke(TEST_APP_PACKAGE_NAME, readDataInBackgroundPermission.additionalPermission)
        verify(revokePermissionStatusUseCase)
            .invoke(TEST_APP_PACKAGE_NAME, readHistoryDataPermission.additionalPermission)
    }

    @Test
    fun updatePermissions_deny_whenUnsuccessful_returnsFalse() = runTest {
        setupDeclaredAndGrantedPermissions()

        val appPermissionsObserver = TestObserver<List<FitnessPermission>>()
        val grantedPermissionsObserver = TestObserver<Set<FitnessPermission>>()
        appPermissionViewModel.appPermissions.observeForever(appPermissionsObserver)
        appPermissionViewModel.grantedPermissions.observeForever(grantedPermissionsObserver)

        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val appPermissionsResult = appPermissionsObserver.getLastValue()
        val grantedPermissionsResult = grantedPermissionsObserver.getLastValue()

        assertThat(appPermissionsResult)
            .containsExactlyElementsIn(
                listOf(
                    readExercisePermission,
                    readNutritionPermission,
                    writeSleepPermission,
                    writeDistancePermission))
        assertThat(grantedPermissionsResult)
            .containsExactlyElementsIn(setOf(readExercisePermission, writeDistancePermission))

        whenever(grantPermissionsUseCase.invoke(any(), any())).thenThrow(RuntimeException("Error!"))
        val result =
            appPermissionViewModel.updatePermission(
                TEST_APP_PACKAGE_NAME, readExercisePermission, true)
        advanceUntilIdle()

        assertThat(grantedPermissionsObserver.getLastValue())
            .containsExactlyElementsIn(setOf(readExercisePermission, writeDistancePermission))
        assertThat(result).isFalse()
    }

    @Test
    fun grantAllPermissions_whenSuccessful_returnsTrue() = runTest {
        setupDeclaredAndGrantedPermissions()

        val appPermissionsObserver = TestObserver<List<FitnessPermission>>()
        val grantedPermissionsObserver = TestObserver<Set<FitnessPermission>>()
        appPermissionViewModel.appPermissions.observeForever(appPermissionsObserver)
        appPermissionViewModel.grantedPermissions.observeForever(grantedPermissionsObserver)

        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val appPermissionsResult = appPermissionsObserver.getLastValue()
        val grantedPermissionsResult = grantedPermissionsObserver.getLastValue()

        assertThat(appPermissionsResult)
            .containsExactlyElementsIn(
                listOf(
                    readExercisePermission,
                    readNutritionPermission,
                    writeSleepPermission,
                    writeDistancePermission))
        assertThat(grantedPermissionsResult)
            .containsExactlyElementsIn(setOf(readExercisePermission, writeDistancePermission))

        val result = appPermissionViewModel.grantAllPermissions(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        assertThat(grantedPermissionsObserver.getLastValue())
            .containsExactlyElementsIn(
                setOf(
                    readExercisePermission,
                    readNutritionPermission,
                    writeSleepPermission,
                    writeDistancePermission))
        assertThat(result).isTrue()
    }

    @Test
    fun grantAllPermissions_whenUnsuccessful_returnsFalse() = runTest {
        setupDeclaredAndGrantedPermissions()

        val appPermissionsObserver = TestObserver<List<FitnessPermission>>()
        val grantedPermissionsObserver = TestObserver<Set<FitnessPermission>>()
        appPermissionViewModel.appPermissions.observeForever(appPermissionsObserver)
        appPermissionViewModel.grantedPermissions.observeForever(grantedPermissionsObserver)

        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val appPermissionsResult = appPermissionsObserver.getLastValue()
        val grantedPermissionsResult = grantedPermissionsObserver.getLastValue()

        assertThat(appPermissionsResult)
            .containsExactlyElementsIn(
                listOf(
                    readExercisePermission,
                    readNutritionPermission,
                    writeSleepPermission,
                    writeDistancePermission))
        assertThat(grantedPermissionsResult)
            .containsExactlyElementsIn(setOf(readExercisePermission, writeDistancePermission))

        whenever(grantPermissionsUseCase.invoke(any(), any())).thenThrow(RuntimeException("Error!"))
        val result = appPermissionViewModel.grantAllPermissions(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        assertThat(grantedPermissionsObserver.getLastValue())
            .containsExactlyElementsIn(setOf(readExercisePermission, writeDistancePermission))
        assertThat(result).isFalse()
    }

    @Test
    fun revokeAllPermissions_whenSuccessful_returnsTrue() = runTest {
        setupDeclaredAndGrantedPermissions()

        val appPermissionsObserver = TestObserver<List<FitnessPermission>>()
        val grantedPermissionsObserver = TestObserver<Set<FitnessPermission>>()
        appPermissionViewModel.appPermissions.observeForever(appPermissionsObserver)
        appPermissionViewModel.grantedPermissions.observeForever(grantedPermissionsObserver)

        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val appPermissionsResult = appPermissionsObserver.getLastValue()
        val grantedPermissionsResult = grantedPermissionsObserver.getLastValue()

        assertThat(appPermissionsResult)
            .containsExactlyElementsIn(
                listOf(
                    readExercisePermission,
                    readNutritionPermission,
                    writeSleepPermission,
                    writeDistancePermission))
        assertThat(grantedPermissionsResult)
            .containsExactlyElementsIn(setOf(readExercisePermission, writeDistancePermission))

        val result = appPermissionViewModel.revokeAllPermissions(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()
        assertThat(grantedPermissionsObserver.getLastValue()).isEmpty()
        assertThat(result).isTrue()
    }

    // TODO (b/324247426) unignore when we can mock suspend functions
    @Test
    @Ignore
    fun deleteAppData_invokesUseCaseWithCorrectFilter() = runTest {
        appPermissionViewModel.deleteAppData(TEST_APP_PACKAGE_NAME, TEST_APP_NAME)
        advanceUntilIdle()

        verify(deleteAppDateUseCase).invoke(appDataCaptor.capture(), timeFilterCaptor.capture())
    }

    @Test
    fun shouldNavigateToFragment_whenPackageNameSupported_returnsTrue() = runTest {
        whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
        whenever(healthPermissionReader.getValidHealthPermissions(any()))
            .thenReturn(
                listOf(
                    readExercisePermission,
                    readNutritionPermission,
                    writeSleepPermission,
                    writeDistancePermission))
        getGrantedHealthPermissionsUseCase.updateData(TEST_APP_PACKAGE_NAME, listOf())

        advanceUntilIdle()

        assertThat(
                appPermissionViewModel.shouldNavigateToAppPermissionsFragment(
                    TEST_APP_PACKAGE_NAME))
            .isTrue()
    }

    @Test
    fun shouldNavigateToFragment_whenAnyPermissionGranted_returnsTrue() = runTest {
        whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(false)
        whenever(healthPermissionReader.getValidHealthPermissions(any()))
            .thenReturn(
                listOf(
                    readExercisePermission,
                    readNutritionPermission,
                    writeSleepPermission,
                    writeDistancePermission))
        getGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME, listOf(writeSleepPermission.toString()))

        advanceUntilIdle()

        assertThat(
                appPermissionViewModel.shouldNavigateToAppPermissionsFragment(
                    TEST_APP_PACKAGE_NAME))
            .isTrue()
    }

    @Test
    fun shouldNavigateToFragment_whenPackageNotSupported_andNoPermissionsGranted_returnsFalse() =
        runTest {
            whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(false)
            whenever(healthPermissionReader.getValidHealthPermissions(any()))
                .thenReturn(
                    listOf(
                        readExercisePermission,
                        readNutritionPermission,
                        writeSleepPermission,
                        writeDistancePermission))
            getGrantedHealthPermissionsUseCase.updateData(TEST_APP_PACKAGE_NAME, listOf())

            advanceUntilIdle()

            assertThat(
                    appPermissionViewModel.shouldNavigateToAppPermissionsFragment(
                        TEST_APP_PACKAGE_NAME))
                .isFalse()
        }

    @Test
    fun isPackageSupported_callsCorrectMethod() {
        appPermissionViewModel.isPackageSupported(TEST_APP_PACKAGE_NAME)
        verify(healthPermissionReader).isRationaleIntentDeclared(TEST_APP_PACKAGE_NAME)
    }

    private fun setupDeclaredAndGrantedPermissions() {
        whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
        whenever(healthPermissionReader.getValidHealthPermissions(any()))
            .thenReturn(
                listOf(
                    readExercisePermission,
                    readNutritionPermission,
                    writeSleepPermission,
                    writeDistancePermission))
        getGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME,
            listOf(readExercisePermission.toString(), writeDistancePermission.toString()))
    }
}
