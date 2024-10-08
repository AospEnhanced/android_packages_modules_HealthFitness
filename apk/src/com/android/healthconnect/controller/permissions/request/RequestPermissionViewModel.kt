/*
 * Copyright (C) 2023 The Android Open Source Project
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
 *
 *
 */

package com.android.healthconnect.controller.permissions.request

import android.content.pm.PackageManager
import android.health.connect.HealthPermissions
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.healthconnect.controller.permissions.api.GetGrantedHealthPermissionsUseCase
import com.android.healthconnect.controller.permissions.api.GetHealthPermissionsFlagsUseCase
import com.android.healthconnect.controller.permissions.api.GrantHealthPermissionUseCase
import com.android.healthconnect.controller.permissions.api.LoadAccessDateUseCase
import com.android.healthconnect.controller.permissions.api.RevokeHealthPermissionUseCase
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.AdditionalPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.FitnessPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.MedicalPermission
import com.android.healthconnect.controller.permissions.data.PermissionState
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.app.AppMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/** View model for {@link PermissionsFragment} . */
@HiltViewModel
class RequestPermissionViewModel
@Inject
constructor(
    private val appInfoReader: AppInfoReader,
    private val grantHealthPermissionUseCase: GrantHealthPermissionUseCase,
    private val revokeHealthPermissionUseCase: RevokeHealthPermissionUseCase,
    private val getGrantedHealthPermissionsUseCase: GetGrantedHealthPermissionsUseCase,
    private val getHealthPermissionsFlagsUseCase: GetHealthPermissionsFlagsUseCase,
    private val loadAccessDateUseCase: LoadAccessDateUseCase,
    private val healthPermissionReader: HealthPermissionReader
) : ViewModel() {

    companion object {
        private const val TAG = "RequestPermissionViewMo"
    }

    private val _appMetaData = MutableLiveData<AppMetadata>()
    val appMetadata: LiveData<AppMetadata>
        get() = _appMetaData

    /** List of grantable [FitnessPermission]s */
    private val _fitnessPermissionsList = MutableLiveData<List<FitnessPermission>>()
    val fitnessPermissionsList: LiveData<List<FitnessPermission>>
        get() = _fitnessPermissionsList

    /** List of grantable [MedicalPermission]s */
    private val _medicalPermissionsList = MutableLiveData<List<MedicalPermission>>()
    val medicalPermissionsList: LiveData<List<MedicalPermission>>
        get() = _medicalPermissionsList

    /** List of grantable [AdditionalPermission]s */
    private val _additionalPermissionsList = MutableLiveData<List<AdditionalPermission>>()
    val additionalPermissionsList: LiveData<List<AdditionalPermission>>
        get() = _additionalPermissionsList

    /** List of grantable [HealthPermissions]s */
    private val _healthPermissionsList = MutableLiveData<List<HealthPermission>>()
    val healthPermissionsList: LiveData<List<HealthPermission>>
        get() = _healthPermissionsList

    /** [FitnessPermission]s that have been granted locally via a toggle, but not yet requested */
    private val _grantedFitnessPermissions = MutableLiveData<Set<FitnessPermission>>(emptySet())
    val grantedFitnessPermissions: LiveData<Set<FitnessPermission>>
        get() = _grantedFitnessPermissions

    /** [FitnessPermission]s that have been granted locally via a toggle, but not yet requested */
    private val _grantedMedicalPermissions = MutableLiveData<Set<MedicalPermission>>(emptySet())
    val grantedMedicalPermissions: LiveData<Set<MedicalPermission>>
        get() = _grantedMedicalPermissions

    /**
     * [AdditionalPermission]s that have been granted locally via a toggle, but not yet requested
     */
    private val _grantedAdditionalPermissions =
        MutableLiveData<Set<AdditionalPermission>>(emptySet())
    val grantedAdditionalPermissions: LiveData<Set<AdditionalPermission>>
        get() = _grantedAdditionalPermissions

    /** Used to control the enabled state of the Allow all switch */
    private val _allFitnessPermissionsGranted =
        MediatorLiveData(false).apply {
            addSource(_fitnessPermissionsList) {
                postValue(
                    areAllPermissionsGranted(fitnessPermissionsList, grantedFitnessPermissions))
            }
            addSource(_grantedFitnessPermissions) {
                postValue(
                    areAllPermissionsGranted(fitnessPermissionsList, grantedFitnessPermissions))
            }
        }
    val allFitnessPermissionsGranted: LiveData<Boolean>
        get() = _allFitnessPermissionsGranted

    /** Used to control the enabled state of the Allow all switch */
    private val _allMedicalPermissionsGranted =
        MediatorLiveData(false).apply {
            addSource(_medicalPermissionsList) {
                postValue(
                    areAllPermissionsGranted(medicalPermissionsList, grantedMedicalPermissions))
            }
            addSource(_grantedFitnessPermissions) {
                postValue(
                    areAllPermissionsGranted(medicalPermissionsList, grantedMedicalPermissions))
            }
        }
    val allMedicalPermissionsGranted: LiveData<Boolean>
        get() = _allMedicalPermissionsGranted

    /**
     * MediatorLiveData to hold the caller app info and the requested additional permissions needed
     * for the [AdditionalPermissionsRequestFragment]
     */
    private val _additionalPermissionsInfo =
        MediatorLiveData<AdditionalPermissionsInfo>().apply {
            addSource(_additionalPermissionsList) { additionalPermissionsList ->
                this.postValue(
                    AdditionalPermissionsInfo(additionalPermissionsList, _appMetaData.value))
            }
            addSource(_appMetaData) { appMetadata ->
                this.postValue(
                    AdditionalPermissionsInfo(_additionalPermissionsList.value, appMetadata))
            }
        }

    val additionalPermissionsInfo: LiveData<AdditionalPermissionsInfo>
        get() = _additionalPermissionsInfo

    /** Retains the originally requested permissions and their state. */
    private var requestedPermissions: MutableMap<HealthPermission, PermissionState> = mutableMapOf()

    /**
     * A map of permissions that have been requested and their state. The union of this and
     * [requestedPermissions] will be returned to the caller as an intent extra.
     */
    private var grants: MutableMap<HealthPermission, PermissionState> = mutableMapOf()

    /** Indicates whether the fitness data type request has been concluded. */
    private var fitnessPermissionsConcluded = false

    fun isFitnessPermissionRequestConcluded(): Boolean = fitnessPermissionsConcluded

    fun setFitnessPermissionRequestConcluded(boolean: Boolean) {
        fitnessPermissionsConcluded = boolean
    }

    /** Indicates whether the medical data type request has been concluded. */
    private var medicalPermissionsConcluded = false

    fun isMedicalPermissionRequestConcluded(): Boolean = medicalPermissionsConcluded

    fun setMedicalPermissionRequestConcluded(boolean: Boolean) {
        medicalPermissionsConcluded = boolean
    }

    /**
     * If no read permissions granted, the AdditionalPermissions request screen will not be shown
     */
    private var anyReadPermissionsGranted: Boolean = false

    fun isAnyReadPermissionGranted(): Boolean = anyReadPermissionsGranted

    /** Whether to modify the historic access text on the [FitnessPermissionsFragment] */
    private var historyAccessGranted: Boolean = false

    fun isHistoryAccessGranted(): Boolean = historyAccessGranted

    fun loadAccessDate(packageName: String) = loadAccessDateUseCase.invoke(packageName)

    fun init(packageName: String, permissions: Array<out String>) {
        loadAppInfo(packageName)
        loadPermissions(packageName, permissions)
    }

    /** Whether the user has enabled this permission in the Permission Request screen. */
    fun isPermissionLocallyGranted(permission: HealthPermission): Boolean {
        return when (permission) {
            is FitnessPermission -> {
                _grantedFitnessPermissions.value.orEmpty().contains(permission)
            }
            is MedicalPermission -> {
                _grantedMedicalPermissions.value.orEmpty().contains(permission)
            }
            else -> {
                _grantedAdditionalPermissions.value.orEmpty().contains(permission)
            }
        }
    }

    /** Returns true if any of the requested permissions is USER_FIXED, false otherwise. */
    fun isAnyPermissionUserFixed(packageName: String, permissions: Array<out String>): Boolean {
        return getHealthPermissionsFlagsUseCase.invoke(packageName, permissions.toList()).any {
            (_, flags) ->
            flags.and(PackageManager.FLAG_PERMISSION_USER_FIXED) != 0
        }
    }

    /** Mark a permission as locally granted */
    fun updateHealthPermission(permission: HealthPermission, grant: Boolean) {
        if (permission is FitnessPermission) {
            updateFitnessPermission(permission, grant)
        } else if (permission is MedicalPermission) {
            updateMedicalPermission(permission, grant)
        } else if (permission is AdditionalPermission) {
            updateAdditionalPermission(permission, grant)
        }
    }

    /** Mark all [FitnessPermission]s as locally granted */
    fun updateFitnessPermissions(grant: Boolean) {
        if (grant) {
            _grantedFitnessPermissions.setValue(_fitnessPermissionsList.value.orEmpty().toSet())
        } else {
            _grantedFitnessPermissions.setValue(emptySet())
        }
    }

    /** Mark all [MedicalPermission]s as locally granted */
    fun updateMedicalPermissions(grant: Boolean) {
        if (grant) {
            _grantedMedicalPermissions.setValue(_medicalPermissionsList.value.orEmpty().toSet())
        } else {
            _grantedMedicalPermissions.setValue(emptySet())
        }
    }

    /** Mark all [AdditionalPermission]s as locally granted */
    fun updateAdditionalPermissions(grant: Boolean) {
        if (grant) {
            _grantedAdditionalPermissions.value = _additionalPermissionsList.value.orEmpty().toSet()
        } else {
            _grantedAdditionalPermissions.value = emptySet()
        }
    }

    /** Grants/Revokes all the [FitnessPermission]s sent by the caller. */
    fun requestFitnessPermissions(packageName: String) {
        requestedPermissions
            .filterKeys { it is FitnessPermission }
            .forEach { (permission, permissionState) ->
                internalGrantOrRevokePermission(packageName, permission, permissionState)
            }
    }

    /** Grants/Revokes all the [MedicalPermission]s sent by the caller. */
    fun requestMedicalPermissions(packageName: String) {
        requestedPermissions
            .filterKeys { it is MedicalPermission }
            .forEach { (permission, permissionState) ->
                internalGrantOrRevokePermission(packageName, permission, permissionState)
            }
    }

    /** Grants/Revokes all the [AdditionalPermission]s sent by the caller. */
    fun requestAdditionalPermissions(packageName: String) {
        requestedPermissions
            .filterKeys { it is AdditionalPermission }
            .filterKeys { it != AdditionalPermission.READ_EXERCISE_ROUTES }
            .forEach { (permission, permissionState) ->
                internalGrantOrRevokePermission(packageName, permission, permissionState)
            }
    }

    /** Grants/Revokes all the [HealthPermission]s sent by the caller. */
    fun requestHealthPermissions(packageName: String) {
        requestedPermissions.forEach { (permission, permissionState) ->
            internalGrantOrRevokePermission(packageName, permission, permissionState)
        }
    }

    /**
     * Returns a map of all [HealthPermission]s that have been requested by the caller and their
     * current grant state. A permission may be granted if it was already granted when the request
     * was made, or if it was granted during this permission request. Similarly for not granted
     * permissions.
     */
    fun getPermissionGrants(): MutableMap<HealthPermission, PermissionState> {
        val permissionGrants = requestedPermissions.toMutableMap()
        permissionGrants.putAll(grants)
        return permissionGrants
    }

    private fun <T> areAllPermissionsGranted(
        permissionsListLiveData: LiveData<List<T>>,
        grantedPermissionsLiveData: LiveData<Set<T>>
    ): Boolean {
        val permissionsList = permissionsListLiveData.value.orEmpty()
        val grantedPermissions = grantedPermissionsLiveData.value.orEmpty()
        return if (permissionsList.isEmpty() || grantedPermissions.isEmpty()) {
            false
        } else {
            permissionsList.size == grantedPermissions.size
        }
    }

    private fun isDataTypeReadPermission(permission: String): Boolean {
        val healthPermission = HealthPermission.fromPermissionString(permission)
        return ((healthPermission is FitnessPermission) &&
            healthPermission.permissionsAccessType == PermissionsAccessType.READ)
    }

    private fun isHistoryReadPermission(permission: String): Boolean {
        return permission == HealthPermissions.READ_HEALTH_DATA_HISTORY
    }

    private fun loadPermissions(packageName: String, permissions: Array<out String>) {
        val grantedPermissions = getGrantedHealthPermissionsUseCase.invoke(packageName)

        anyReadPermissionsGranted =
            // TODO(): Decision whether background read should apply to medical permission too.
            grantedPermissions.any { permission -> isDataTypeReadPermission(permission) }
        historyAccessGranted =
            grantedPermissions.any { permission -> isHistoryReadPermission(permission) }
        val declaredPermissions = healthPermissionReader.getDeclaredHealthPermissions(packageName)

        val filteredPermissions =
            permissions
                // Do not show hidden permissions
                .filterNot { permission -> healthPermissionReader.shouldHidePermission(permission) }
                // Do not show undeclared permissions
                .filter { permission -> declaredPermissions.contains(permission) }
                // Filter invalid health permissions
                // This will also transform each permission into DataType or Medical or Additional
                .mapNotNull { permissionString ->
                    try {
                        HealthPermission.fromPermissionString(permissionString)
                    } catch (exception: IllegalArgumentException) {
                        Log.e(TAG, "Unrecognized health exception!", exception)
                        null
                    }
                }
                // Add the requested permissions and their states to requestedPermissions
                .onEach { permission -> addToRequestedPermissions(grantedPermissions, permission) }
                // Finally, filter out the granted permissions
                .filterNot { permission -> grantedPermissions.contains(permission.toString()) }

        val dataTypeNotGrantedPermissions =
            filteredPermissions
                .filter { permission ->
                    healthPermissionReader.isFitnessPermission(permission.toString())
                }
                .map { permission -> permission as FitnessPermission }

        val medicalNotGrantedPermissions =
            filteredPermissions
                .filter { permission ->
                    healthPermissionReader.isMedicalPermission(permission.toString())
                }
                .map { permission -> permission as MedicalPermission }

        val additionalNotGrantedPermissions =
            filteredPermissions
                .filter { permission ->
                    healthPermissionReader.isAdditionalPermission(permission.toString())
                }
                .filterNot { permission ->
                    permission.toString() == HealthPermissions.READ_EXERCISE_ROUTES
                }
                .map { permission -> permission as AdditionalPermission }

        _fitnessPermissionsList.value = dataTypeNotGrantedPermissions
        _medicalPermissionsList.value = medicalNotGrantedPermissions
        _additionalPermissionsList.value = additionalNotGrantedPermissions
        _healthPermissionsList.value =
            dataTypeNotGrantedPermissions +
                medicalNotGrantedPermissions +
                additionalNotGrantedPermissions
    }

    /** Adds a permission to the [requestedPermissions] map with its original granted state */
    private fun addToRequestedPermissions(
        grantedPermissions: List<String>,
        permission: HealthPermission
    ) {
        val isPermissionGranted = grantedPermissions.contains(permission.toString())
        if (isPermissionGranted) {
            requestedPermissions[permission] = PermissionState.GRANTED
        } else {
            requestedPermissions[permission] = PermissionState.NOT_GRANTED
        }
    }

    private fun updateFitnessPermission(permission: FitnessPermission, grant: Boolean) {
        val updatedGrantedPermissions = _grantedFitnessPermissions.value.orEmpty().toMutableSet()

        if (grant) {
            updatedGrantedPermissions.add(permission)
        } else {
            updatedGrantedPermissions.remove(permission)
        }
        _grantedFitnessPermissions.postValue(updatedGrantedPermissions)
    }

    private fun updateMedicalPermission(permission: MedicalPermission, grant: Boolean) {
        val updatedGrantedPermissions = _grantedMedicalPermissions.value.orEmpty().toMutableSet()

        if (grant) {
            updatedGrantedPermissions.add(permission)
        } else {
            updatedGrantedPermissions.remove(permission)
        }
        _grantedMedicalPermissions.postValue(updatedGrantedPermissions)
    }

    private fun updateAdditionalPermission(permission: AdditionalPermission, grant: Boolean) {
        val updatedGrantedPermissions = _grantedAdditionalPermissions.value.orEmpty().toMutableSet()
        if (grant) {
            updatedGrantedPermissions.add(permission)
        } else {
            updatedGrantedPermissions.remove(permission)
        }
        _grantedAdditionalPermissions.postValue(updatedGrantedPermissions)
    }

    private fun loadAppInfo(packageName: String) {
        viewModelScope.launch { _appMetaData.postValue(appInfoReader.getAppMetadata(packageName)) }
    }

    private fun internalGrantOrRevokePermission(
        packageName: String,
        permission: HealthPermission,
        permissionState: PermissionState
    ) {
        val granted =
            isPermissionLocallyGranted(permission) || permissionState == PermissionState.GRANTED

        try {
            if (granted) {
                grantHealthPermissionUseCase.invoke(packageName, permission.toString())
                grants[permission] = PermissionState.GRANTED
            } else {
                revokeHealthPermissionUseCase.invoke(packageName, permission.toString())
                grants[permission] = PermissionState.NOT_GRANTED
            }
        } catch (e: SecurityException) {
            grants[permission] = PermissionState.NOT_GRANTED
        } catch (e: Exception) {
            grants[permission] = PermissionState.ERROR
        }
    }
}

data class AdditionalPermissionsInfo(
    val additionalPermissions: List<AdditionalPermission>?,
    val appInfo: AppMetadata?
)
