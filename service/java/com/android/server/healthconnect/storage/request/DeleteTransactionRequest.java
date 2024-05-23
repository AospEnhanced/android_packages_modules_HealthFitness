/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.server.healthconnect.storage.request;

import static android.health.connect.Constants.DEFAULT_LONG;

import android.health.connect.Constants;
import android.health.connect.RecordIdFilter;
import android.health.connect.aidl.DeleteUsingFiltersRequestParcel;
import android.health.connect.internal.datatypes.utils.RecordMapper;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;

import com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.RecordHelper;
import com.android.server.healthconnect.storage.utils.RecordHelperProvider;
import com.android.server.healthconnect.storage.utils.StorageUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** @hide */
public final class DeleteTransactionRequest {
    private static final String TAG = "HealthConnectDelete";
    private final List<DeleteTableRequest> mDeleteTableRequests;
    private final long mRequestingPackageNameId;
    private boolean mHasHealthDataManagementPermission;

    @SuppressWarnings("NullAway.Init") // TODO(b/317029272): fix this suppression
    public DeleteTransactionRequest(String packageName, DeleteUsingFiltersRequestParcel request) {
        Objects.requireNonNull(packageName);
        mDeleteTableRequests = new ArrayList<>(request.getRecordTypeFilters().size());
        mRequestingPackageNameId = AppInfoHelper.getInstance().getAppInfoId(packageName);
        if (request.usesIdFilters()) {
            List<RecordIdFilter> recordIds =
                    request.getRecordIdFiltersParcel().getRecordIdFilters();
            Set<UUID> uuidSet = new ArraySet<>();
            Map<RecordHelper<?>, List<UUID>> recordTypeToUuids = new ArrayMap<>();
            for (RecordIdFilter recordId : recordIds) {
                RecordHelper<?> recordHelper =
                        RecordHelperProvider.getRecordHelper(
                                RecordMapper.getInstance().getRecordType(recordId.getRecordType()));
                UUID uuid = StorageUtils.getUUIDFor(recordId, packageName);
                if (uuidSet.contains(uuid)) {
                    // id has been already been processed;
                    continue;
                }
                recordTypeToUuids.putIfAbsent(recordHelper, new ArrayList<>());
                Objects.requireNonNull(recordTypeToUuids.get(recordHelper)).add(uuid);
                uuidSet.add(uuid);
            }

            recordTypeToUuids.forEach(
                    (recordHelper, uuids) ->
                            mDeleteTableRequests.add(recordHelper.getDeleteTableRequest(uuids)));

            // We currently only support either using filters or ids, so if we are deleting using
            // ids no need to proceed further.
            return;
        }

        // No ids are present, so we are good to go to use filters to process our request
        List<Integer> recordTypeFilters = request.getRecordTypeFilters();
        if (recordTypeFilters == null || recordTypeFilters.isEmpty()) {
            recordTypeFilters =
                    new ArrayList<>(
                            RecordMapper.getInstance()
                                    .getRecordIdToExternalRecordClassMap()
                                    .keySet());
        }

        recordTypeFilters.forEach(
                (recordType) -> {
                    RecordHelper<?> recordHelper = RecordHelperProvider.getRecordHelper(recordType);
                    Objects.requireNonNull(recordHelper);

                    mDeleteTableRequests.add(
                            recordHelper.getDeleteTableRequest(
                                    request.getPackageNameFilters(),
                                    request.getStartTime(),
                                    request.getEndTime(),
                                    request.isLocalTimeFilter()));
                });
    }

    // Used for auto delete only
    public DeleteTransactionRequest(List<DeleteTableRequest> deleteTableRequests) {
        mDeleteTableRequests = List.copyOf(deleteTableRequests);
        mHasHealthDataManagementPermission = true;
        mRequestingPackageNameId = DEFAULT_LONG;
    }

    public List<DeleteTableRequest> getDeleteTableRequests() {
        if (Constants.DEBUG) {
            Slog.d(TAG, "num of delete requests: " + mDeleteTableRequests.size());
        }
        return mDeleteTableRequests;
    }

    public void enforcePackageCheck(UUID uuid, long appInfoId) {
        if (mHasHealthDataManagementPermission) {
            // Skip this check if the caller has data management permission
            return;
        }

        if (mRequestingPackageNameId != appInfoId) {
            throw new IllegalArgumentException(
                    mRequestingPackageNameId + " is not the owner for " + uuid);
        }
    }

    public DeleteTransactionRequest setHasManageHealthDataPermission(
            boolean hasHealthDataManagementPermission) {
        mHasHealthDataManagementPermission = hasHealthDataManagementPermission;

        return this;
    }
}
