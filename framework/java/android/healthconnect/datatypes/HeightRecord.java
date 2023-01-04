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

import static android.healthconnect.datatypes.AggregationType.AggregationTypeIdentifier.HEIGHT_RECORD_HEIGHT_AVG;
import static android.healthconnect.datatypes.AggregationType.AggregationTypeIdentifier.HEIGHT_RECORD_HEIGHT_MAX;
import static android.healthconnect.datatypes.AggregationType.AggregationTypeIdentifier.HEIGHT_RECORD_HEIGHT_MIN;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_HEIGHT;

import android.annotation.NonNull;
import android.healthconnect.HealthConnectManager;
import android.healthconnect.datatypes.units.Length;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;

/** Captures the user's height. */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_HEIGHT)
public final class HeightRecord extends InstantRecord {

    private final Length mHeight;

    /**
     * Metric identifier to get average height using aggregate APIs in {@link HealthConnectManager}
     */
    @android.annotation.NonNull
    public static final AggregationType<Length> HEIGHT_AVG =
            new AggregationType<>(
                    HEIGHT_RECORD_HEIGHT_AVG,
                    AggregationType.AVG,
                    RECORD_TYPE_HEIGHT,
                    Length.class);

    /**
     * Metric identifier to get minimum height using aggregate APIs in {@link HealthConnectManager}
     */
    @android.annotation.NonNull
    public static final AggregationType<Length> HEIGHT_MIN =
            new AggregationType<>(
                    HEIGHT_RECORD_HEIGHT_MIN,
                    AggregationType.MIN,
                    RECORD_TYPE_HEIGHT,
                    Length.class);

    /**
     * Metric identifier to get maximum height using aggregate APIs in {@link HealthConnectManager}
     */
    @android.annotation.NonNull
    public static final AggregationType<Length> HEIGHT_MAX =
            new AggregationType<>(
                    HEIGHT_RECORD_HEIGHT_MAX,
                    AggregationType.MAX,
                    RECORD_TYPE_HEIGHT,
                    Length.class);

    /**
     * @param metadata Metadata to be associated with the record. See {@link Metadata}.
     * @param time Start time of this activity
     * @param zoneOffset Zone offset of the user when the activity started
     * @param height Height of this activity
     */
    private HeightRecord(
            @NonNull Metadata metadata,
            @NonNull Instant time,
            @NonNull ZoneOffset zoneOffset,
            @NonNull Length height) {
        super(metadata, time, zoneOffset);
        Objects.requireNonNull(metadata);
        Objects.requireNonNull(time);
        Objects.requireNonNull(zoneOffset);
        Objects.requireNonNull(height);
        mHeight = height;
    }
    /**
     * @return height in {@link Length} unit.
     */
    @NonNull
    public Length getHeight() {
        return mHeight;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param o the reference object with which to compare.
     * @return {@code true} if this object is the same as the obj
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!super.equals(o)) return false;
        HeightRecord that = (HeightRecord) o;
        return getHeight().equals(that.getHeight());
    }

    /** Returns a hash code value for the object. */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getHeight());
    }

    /** Builder class for {@link HeightRecord} */
    public static final class Builder {
        private final Metadata mMetadata;
        private final Instant mTime;
        private ZoneOffset mZoneOffset;
        private final Length mHeight;

        /**
         * @param metadata Metadata to be associated with the record. See {@link Metadata}.
         * @param time Start time of this activity
         * @param height Height in {@link Length} unit. Required field. Valid range: 0-3 meters.
         */
        public Builder(@NonNull Metadata metadata, @NonNull Instant time, @NonNull Length height) {
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(time);
            Objects.requireNonNull(height);
            mMetadata = metadata;
            mTime = time;
            mZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
            mHeight = height;
        }

        /** Sets the zone offset of the user when the activity happened */
        @NonNull
        public Builder setZoneOffset(@NonNull ZoneOffset zoneOffset) {
            Objects.requireNonNull(zoneOffset);
            mZoneOffset = zoneOffset;
            return this;
        }

        /**
         * @return Object of {@link HeightRecord}
         */
        @NonNull
        public HeightRecord build() {
            return new HeightRecord(mMetadata, mTime, mZoneOffset, mHeight);
        }
    }
}
