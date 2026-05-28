/*
 * Copyright 2025 QuireBind Contributors
 *
 * This file is part of QuireBind.
 *
 * QuireBind is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QuireBind is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with QuireBind.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.maiitsoh.quirebind.core.model;

import java.util.Objects;

/**
 * Configuration for sewing hole placement along the spine.
 *
 * <p>Holes are evenly distributed between a head end-margin and a tail end-margin,
 * following the standard Western bookbinding convention where the outermost holes
 * act as kettle-stitch anchors.
 *
 * <ul>
 *   <li><b>holeCount</b> — total number of piercing holes (3 = pamphlet/kettle,
 *       5 = standard, 7+ = long books). Minimum 2.</li>
 *   <li><b>endMarginMm</b> — distance in millimetres from the head and tail of the
 *       spine to the first and last hole. Typical range 10–20 mm; default 15 mm.</li>
 * </ul>
 */
public final class SewingConfig {

    private static final int DEFAULT_HOLE_COUNT = 5;
    private static final double DEFAULT_END_MARGIN_MM = 15.0;

    private final int holeCount;
    private final double endMarginMm;

    private SewingConfig(Builder builder) {
        this.holeCount = builder.holeCount;
        this.endMarginMm = builder.endMarginMm;
    }

    /** Returns a {@link SewingConfig} with default values (5 holes, 15 mm end margin). */
    public static SewingConfig defaults() {
        return builder().build();
    }

    /** Returns the total number of sewing holes. */
    public int getHoleCount() {
        return holeCount;
    }

    /** Returns the distance in mm from the head/tail of the spine to the first/last hole. */
    public double getEndMarginMm() {
        return endMarginMm;
    }

    /** Returns a new {@link Builder}. */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder for {@link SewingConfig}. */
    public static final class Builder {

        private int holeCount = DEFAULT_HOLE_COUNT;
        private double endMarginMm = DEFAULT_END_MARGIN_MM;

        private Builder() {
        }

        /**
         * Sets the number of sewing holes.
         *
         * @param holeCount must be at least 2
         * @return this builder
         * @throws IllegalArgumentException if {@code holeCount < 2}
         */
        public Builder holeCount(int holeCount) {
            if (holeCount < 2) {
                throw new IllegalArgumentException("holeCount must be >= 2, got: " + holeCount);
            }
            this.holeCount = holeCount;
            return this;
        }

        /**
         * Sets the end margin in millimetres.
         *
         * @param endMarginMm must be positive
         * @return this builder
         * @throws IllegalArgumentException if {@code endMarginMm <= 0}
         */
        public Builder endMarginMm(double endMarginMm) {
            if (endMarginMm <= 0) {
                throw new IllegalArgumentException(
                        "endMarginMm must be > 0, got: " + endMarginMm);
            }
            this.endMarginMm = endMarginMm;
            return this;
        }

        /** Builds the {@link SewingConfig}. */
        public SewingConfig build() {
            return new SewingConfig(this);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SewingConfig other)) {
            return false;
        }
        return holeCount == other.holeCount
                && Double.compare(endMarginMm, other.endMarginMm) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(holeCount, endMarginMm);
    }

    @Override
    public String toString() {
        return "SewingConfig{"
                + "holeCount=" + holeCount
                + ", endMarginMm=" + endMarginMm
                + '}';
    }
}
