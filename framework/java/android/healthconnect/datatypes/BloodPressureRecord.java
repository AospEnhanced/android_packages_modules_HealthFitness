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

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.healthconnect.datatypes.units.Pressure;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * Captures the blood pressure of a user. Each record represents a single instantaneous blood
 * pressure reading.
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_BLOOD_PRESSURE)
public final class BloodPressureRecord extends InstantRecord {
    private final int mMeasurementLocation;
    private final Pressure mSystolic;
    private final Pressure mDiastolic;
    private final int mBodyPosition;

    /**
     * @param metadata Metadata to be associated with the record. See {@link Metadata}.
     * @param time Start time of this activity
     * @param zoneOffset Zone offset of the user when the activity started
     * @param measurementLocation MeasurementLocation of this activity
     * @param systolic Systolic of this activity
     * @param diastolic Diastolic of this activity
     * @param bodyPosition BodyPosition of this activity
     */
    private BloodPressureRecord(
            @NonNull Metadata metadata,
            @NonNull Instant time,
            @NonNull ZoneOffset zoneOffset,
            @BloodPressureMeasurementLocation.BloodPressureMeasurementLocations
                    int measurementLocation,
            @NonNull Pressure systolic,
            @NonNull Pressure diastolic,
            @BodyPosition.BodyPositionType int bodyPosition) {
        super(metadata, time, zoneOffset);
        Objects.requireNonNull(metadata);
        Objects.requireNonNull(time);
        Objects.requireNonNull(zoneOffset);
        Objects.requireNonNull(systolic);
        Objects.requireNonNull(diastolic);
        mMeasurementLocation = measurementLocation;
        mSystolic = systolic;
        mDiastolic = diastolic;
        mBodyPosition = bodyPosition;
    }
    /**
     * @return measurementLocation
     */
    @BloodPressureMeasurementLocation.BloodPressureMeasurementLocations
    public int getMeasurementLocation() {
        return mMeasurementLocation;
    }
    /**
     * @return systolic
     */
    @NonNull
    public Pressure getSystolic() {
        return mSystolic;
    }
    /**
     * @return diastolic
     */
    @NonNull
    public Pressure getDiastolic() {
        return mDiastolic;
    }
    /**
     * @return bodyPosition
     */
    @BodyPosition.BodyPositionType
    public int getBodyPosition() {
        return mBodyPosition;
    }

    /** Identifier for Blood Pressure Measurement Location */
    public static final class BloodPressureMeasurementLocation {

        public static final int BLOOD_PRESSURE_MEASUREMENT_LOCATION_UNKNOWN = 0;
        /** Blood pressure measurement location constant for the left wrist. */
        public static final int BLOOD_PRESSURE_MEASUREMENT_LOCATION_LEFT_WRIST = 1;
        /** Blood pressure measurement location constant for the right wrist. */
        public static final int BLOOD_PRESSURE_MEASUREMENT_LOCATION_RIGHT_WRIST = 2;
        /** Blood pressure measurement location constant for the left upper arm. */
        public static final int BLOOD_PRESSURE_MEASUREMENT_LOCATION_LEFT_UPPER_ARM = 3;
        /** Blood pressure measurement location constant for the right upper arm. */
        public static final int BLOOD_PRESSURE_MEASUREMENT_LOCATION_RIGHT_UPPER_ARM = 4;

        private BloodPressureMeasurementLocation() {}

        /** @hide */
        @IntDef({
            BLOOD_PRESSURE_MEASUREMENT_LOCATION_UNKNOWN,
            BLOOD_PRESSURE_MEASUREMENT_LOCATION_LEFT_WRIST,
            BLOOD_PRESSURE_MEASUREMENT_LOCATION_RIGHT_WRIST,
            BLOOD_PRESSURE_MEASUREMENT_LOCATION_LEFT_UPPER_ARM,
            BLOOD_PRESSURE_MEASUREMENT_LOCATION_RIGHT_UPPER_ARM
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface BloodPressureMeasurementLocations {}
    }

    /** Identifier for body position */
    public static final class BodyPosition {

        /** Body position unknown / not identified. */
        public static final int BODY_POSITION_UNKNOWN = 0;
        /** Body position constant representing standing up. */
        public static final int BODY_POSITION_STANDING_UP = 1;
        /** Body position constant representing sitting down. */
        public static final int BODY_POSITION_SITTING_DOWN = 2;
        /** Body position constant representing lying down. */
        public static final int BODY_POSITION_LYING_DOWN = 3;
        /** Body position constant representing semi-recumbent (partially reclining) pose. */
        public static final int BODY_POSITION_RECLINING = 4;

        private BodyPosition() {}

        /** @hide */
        @IntDef({
            BODY_POSITION_UNKNOWN,
            BODY_POSITION_STANDING_UP,
            BODY_POSITION_SITTING_DOWN,
            BODY_POSITION_LYING_DOWN,
            BODY_POSITION_RECLINING
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface BodyPositionType {}
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
        BloodPressureRecord that = (BloodPressureRecord) o;
        return getMeasurementLocation() == that.getMeasurementLocation()
                && getBodyPosition() == that.getBodyPosition()
                && getSystolic().equals(that.getSystolic())
                && getDiastolic().equals(that.getDiastolic());
    }

    /** Returns a hash code value for the object. */
    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                getMeasurementLocation(),
                getSystolic(),
                getDiastolic(),
                getBodyPosition());
    }

    /** Builder class for {@link BloodPressureRecord} */
    public static final class Builder {
        private final Metadata mMetadata;
        private final Instant mTime;
        private ZoneOffset mZoneOffset;
        private final int mMeasurementLocation;
        private final Pressure mSystolic;
        private final Pressure mDiastolic;
        private final int mBodyPosition;

        /**
         * @param metadata Metadata to be associated with the record. See {@link Metadata}.
         * @param time Start time of this activity
         * @param measurementLocation The arm and part of the arm where the measurement was taken.
         *     Optional field. Allowed values: {@link BodyTemperatureMeasurementLocation}.
         * @param systolic Systolic blood pressure measurement, in {@link Pressure} unit. Required
         *     field. Valid range: 20-200 mmHg.
         * @param diastolic Diastolic blood pressure measurement, in {@link Pressure} unit. Required
         *     field. Valid range: 10-180 mmHg.
         * @param bodyPosition The user's body position when the measurement was taken. Optional
         *     field. Allowed values: {@link BodyPosition}.
         */
        public Builder(
                @NonNull Metadata metadata,
                @NonNull Instant time,
                @BloodPressureMeasurementLocation.BloodPressureMeasurementLocations
                        int measurementLocation,
                @NonNull Pressure systolic,
                @NonNull Pressure diastolic,
                @BodyPosition.BodyPositionType int bodyPosition) {
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(time);
            Objects.requireNonNull(systolic);
            Objects.requireNonNull(diastolic);
            mMetadata = metadata;
            mTime = time;
            mMeasurementLocation = measurementLocation;
            mSystolic = systolic;
            mDiastolic = diastolic;
            mBodyPosition = bodyPosition;
            mZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(time);
        }

        /** Sets the zone offset of the user when the activity happened */
        @NonNull
        public Builder setZoneOffset(@NonNull ZoneOffset zoneOffset) {
            Objects.requireNonNull(zoneOffset);
            mZoneOffset = zoneOffset;
            return this;
        }

        /** Sets the zone offset of this record to system default. */
        @NonNull
        public Builder clearZoneOffset() {
            mZoneOffset = RecordUtils.getDefaultZoneOffset();
            return this;
        }

        /**
         * @return Object of {@link BloodPressureRecord}
         */
        @NonNull
        public BloodPressureRecord build() {
            return new BloodPressureRecord(
                    mMetadata,
                    mTime,
                    mZoneOffset,
                    mMeasurementLocation,
                    mSystolic,
                    mDiastolic,
                    mBodyPosition);
        }
    }
}
