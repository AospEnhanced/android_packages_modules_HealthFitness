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

package android.healthconnect.datatypes;

import android.annotation.NonNull;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;

/** A record that contains a measurement recorded as an instance . */
public abstract class InstantRecord extends Record {
    private final Instant mTime;
    private final ZoneOffset mZoneOffset;

    /**
     * @param metadata Metadata to be associated with the record. See {@link Metadata}
     * @param time Time of this activity
     * @param zoneOffset Zone offset of the user when the activity happened
     */
    InstantRecord(
            @NonNull Metadata metadata, @NonNull Instant time, @NonNull ZoneOffset zoneOffset) {
        super(metadata);
        Objects.requireNonNull(time);
        Objects.requireNonNull(zoneOffset);
        mTime = time;
        mZoneOffset = zoneOffset;
    }

    /**
     * @return Time of the activity
     */
    @NonNull
    public Instant getTime() {
        return mTime;
    }

    /**
     * @return Zone offset of the user when the activity happened
     */
    @NonNull
    public ZoneOffset getZoneOffset() {
        return mZoneOffset;
    }
}
