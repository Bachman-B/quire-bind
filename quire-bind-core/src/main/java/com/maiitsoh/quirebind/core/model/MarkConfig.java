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
import java.util.Optional;

/**
 * Controls which print marks are added to the imposed output.
 */
public final class MarkConfig {

    private final boolean foldLines;
    private final boolean signatureProofMarkers;
    private final boolean trimLines;
    private final boolean sewingHoles;
    private final Optional<SewingConfig> sewingConfig;

    private MarkConfig(Builder builder) {
        this.foldLines = builder.foldLines;
        this.signatureProofMarkers = builder.signatureProofMarkers;
        this.trimLines = builder.trimLines;
        this.sewingHoles = builder.sewingHoles;
        this.sewingConfig = builder.sewingConfig;
    }

    /** Returns true if fold line overlays are added to each imposed sheet. */
    public boolean isFoldLines() {
        return foldLines;
    }

    /**
     * Returns true if signature proof markers are added to the spine edge of each signature.
     * Markers are staggered so that a misorder or missing signature breaks the diagonal.
     */
    public boolean isSignatureProofMarkers() {
        return signatureProofMarkers;
    }

    /**
     * Returns true if trim line marks are added. Always false in Phase 1.
     */
    public boolean isTrimLines() {
        return trimLines;
    }

    /**
     * Returns true if sewing hole marks are added. Always false in Phase 1;
     * configurable in Phase 2.
     */
    public boolean isSewingHoles() {
        return sewingHoles;
    }

    /**
     * Returns the sewing configuration. Always empty in Phase 1.
     * Populated in Phase 2 when {@link #isSewingHoles()} is true.
     */
    public Optional<SewingConfig> getSewingConfig() {
        return sewingConfig;
    }

    /**
     * Returns a new builder pre-populated with the values of this instance.
     *
     * @return a builder copying all fields from this instance
     */
    public Builder toBuilder() {
        return new Builder()
                .foldLines(this.foldLines)
                .signatureProofMarkers(this.signatureProofMarkers)
                .trimLines(this.trimLines)
                .sewingHoles(this.sewingHoles)
                .sewingConfig(this.sewingConfig.orElse(null));
    }

    /** Returns a new {@link Builder}. */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder for {@link MarkConfig}. */
    public static final class Builder {

        private boolean foldLines = false;
        private boolean signatureProofMarkers = false;
        private boolean trimLines = false;
        private boolean sewingHoles = false;
        private Optional<SewingConfig> sewingConfig = Optional.empty();

        private Builder() {
        }

        /**
         * Sets whether fold line overlays are added.
         *
         * @param foldLines true to add fold lines
         * @return this builder
         */
        public Builder foldLines(boolean foldLines) {
            this.foldLines = foldLines;
            return this;
        }

        /**
         * Sets whether signature proof markers are added.
         *
         * @param signatureProofMarkers true to add proof markers
         * @return this builder
         */
        public Builder signatureProofMarkers(boolean signatureProofMarkers) {
            this.signatureProofMarkers = signatureProofMarkers;
            return this;
        }

        /**
         * Sets whether trim line marks are added. Phase 1 always passes false.
         *
         * @param trimLines true to add trim lines
         * @return this builder
         */
        public Builder trimLines(boolean trimLines) {
            this.trimLines = trimLines;
            return this;
        }

        /**
         * Sets whether sewing hole marks are added. Phase 1 always passes false.
         *
         * @param sewingHoles true to add sewing holes
         * @return this builder
         */
        public Builder sewingHoles(boolean sewingHoles) {
            this.sewingHoles = sewingHoles;
            return this;
        }

        /**
         * Sets the sewing configuration. Pass null to clear (Phase 1 always passes null).
         *
         * @param sewingConfig the sewing configuration, or null for empty
         * @return this builder
         */
        public Builder sewingConfig(SewingConfig sewingConfig) {
            this.sewingConfig = Optional.ofNullable(sewingConfig);
            return this;
        }

        /** Builds the {@link MarkConfig}. */
        public MarkConfig build() {
            return new MarkConfig(this);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof MarkConfig other)) {
            return false;
        }
        return foldLines == other.foldLines
                && signatureProofMarkers == other.signatureProofMarkers
                && trimLines == other.trimLines
                && sewingHoles == other.sewingHoles
                && sewingConfig.equals(other.sewingConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(foldLines, signatureProofMarkers, trimLines, sewingHoles, sewingConfig);
    }

    @Override
    public String toString() {
        return "MarkConfig{"
                + "foldLines=" + foldLines
                + ", signatureProofMarkers=" + signatureProofMarkers
                + ", trimLines=" + trimLines
                + ", sewingHoles=" + sewingHoles
                + ", sewingConfig=" + sewingConfig
                + '}';
    }
}
