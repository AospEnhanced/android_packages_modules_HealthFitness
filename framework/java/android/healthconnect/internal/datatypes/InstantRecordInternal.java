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

package android.healthconnect.internal.datatypes;

import android.annotation.NonNull;
import android.healthconnect.datatypes.InstantRecord;
import android.os.Parcel;

import java.time.Instant;
import java.time.ZoneOffset;

/**
 * Base class for all health connect datatype records that require a time and a zone offseet.
 *
 * @param <T> instant record
 * @hide
 */
public abstract class InstantRecordInternal<T extends InstantRecord> extends RecordInternal<T> {
    private long mTime;
    private int mZoneOffset;

    public long getTimeInMillis() {
        return mTime;
    }

    /**
     * @param time time to update this object with
     * @return this object
     */
    @NonNull
    public InstantRecordInternal<T> setTime(long time) {
        mTime = time;
        return this;
    }

    public int getZoneOffsetInSeconds() {
        return mZoneOffset;
    }

    /**
     * @param zoneOffset zoneOffset to update this object with
     * @return this object
     */
    @NonNull
    public InstantRecordInternal<T> setZoneOffset(int zoneOffset) {
        mZoneOffset = zoneOffset;
        return this;
    }

    @Override
    void populateRecordFrom(@NonNull Parcel parcel) {
        mTime = parcel.readLong();
        mZoneOffset = parcel.readInt();

        populateInstantRecordFrom(parcel);
    }

    @Override
    void populateRecordFrom(@NonNull T instantRecord) {
        mTime = instantRecord.getTime().toEpochMilli();
        mZoneOffset = instantRecord.getZoneOffset().getTotalSeconds();

        populateInstantRecordFrom(instantRecord);
    }

    @Override
    void populateRecordTo(@NonNull Parcel parcel) {
        parcel.writeLong(mTime);
        parcel.writeInt(mZoneOffset);

        populateInstantRecordTo(parcel);
    }

    Instant getTime() {
        return Instant.ofEpochMilli(mTime);
    }

    ZoneOffset getZoneOffset() {
        return ZoneOffset.ofTotalSeconds(mZoneOffset);
    }

    /**
     * Child class must implement this method and populates itself with the data present in {@code
     * bundle}. Reads should be in the same order as write
     */
    abstract void populateInstantRecordFrom(@NonNull Parcel parcel);

    /**
     * Child class must implement this method and populates itself with the data present in {@code
     * instantRecord}
     */
    abstract void populateInstantRecordFrom(@NonNull T instantRecord);

    /**
     * Populate {@code bundle} with the data required to un-bundle self. This is used during IPC
     * transmissions
     */
    abstract void populateInstantRecordTo(@NonNull Parcel parcel);
}
