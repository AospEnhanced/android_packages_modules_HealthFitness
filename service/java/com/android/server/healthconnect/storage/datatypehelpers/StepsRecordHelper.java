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
package com.android.server.healthconnect.storage.datatypehelpers;

import static com.android.server.healthconnect.storage.utils.StorageUtils.INTEGER;

import android.annotation.NonNull;
import android.content.ContentValues;
import android.healthconnect.datatypes.RecordTypeIdentifier;
import android.healthconnect.internal.datatypes.StepsRecordInternal;
import android.util.Pair;

import java.util.Collections;
import java.util.List;

/**
 * Helper class for StepsRecord.
 *
 * @hide
 */
@HelperFor(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_STEPS)
public final class StepsRecordHelper extends IntervalRecordHelper<StepsRecordInternal> {
    private static final String STEPS_TABLE_NAME = "steps_record_table";
    private static final String COUNT_COLUMN_NAME = "count";

    @Override
    @NonNull
    String getMainTableName() {
        return STEPS_TABLE_NAME;
    }

    @Override
    void populateSpecificContentValues(
            @NonNull ContentValues contentValues, @NonNull StepsRecordInternal stepsRecord) {
        contentValues.put(COUNT_COLUMN_NAME, stepsRecord.getCount());
    }

    @Override
    @NonNull
    protected List<Pair<String, String>> getIntervalRecordColumnInfo() {
        return Collections.singletonList(new Pair<>(COUNT_COLUMN_NAME, INTEGER));
    }
}
