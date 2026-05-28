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
 * Controls folio (page number) assignment across the three page zones.
 *
 * <p>Zones: front matter (aesthetic + completion front), body (content pages),
 * rear matter (completion rear + aesthetic rear). The rear matter style is always
 * {@link FolioStyle#NONE} in Phase 1.
 */
public final class NumberingConfig {

    private final FolioStyle frontMatterStyle;
    private final FolioStyle bodyStyle;
    private final FolioStyle rearMatterStyle;
    private final int frontMatterStartNumber;
    private final int bodyStartNumber;
    private final int rearMatterStartNumber;
    private final boolean suppressFirstBodyFolio;
    private final FolioPosition folioPosition;

    private NumberingConfig(Builder builder) {
        this.frontMatterStyle = builder.frontMatterStyle;
        this.bodyStyle = builder.bodyStyle;
        this.rearMatterStyle = builder.rearMatterStyle;
        this.frontMatterStartNumber = builder.frontMatterStartNumber;
        this.bodyStartNumber = builder.bodyStartNumber;
        this.rearMatterStartNumber = builder.rearMatterStartNumber;
        this.suppressFirstBodyFolio = builder.suppressFirstBodyFolio;
        this.folioPosition = builder.folioPosition;
    }

    /** Returns the folio style for front matter pages. */
    public FolioStyle getFrontMatterStyle() {
        return frontMatterStyle;
    }

    /** Returns the folio style for body (content) pages. */
    public FolioStyle getBodyStyle() {
        return bodyStyle;
    }

    /** Returns the folio style for rear matter pages. Always {@link FolioStyle#NONE} in Phase 1. */
    public FolioStyle getRearMatterStyle() {
        return rearMatterStyle;
    }

    /** Returns the number assigned to the first front matter page. */
    public int getFrontMatterStartNumber() {
        return frontMatterStartNumber;
    }

    /** Returns the number assigned to the first body page. */
    public int getBodyStartNumber() {
        return bodyStartNumber;
    }

    /** Returns the number assigned to the first rear matter page. */
    public int getRearMatterStartNumber() {
        return rearMatterStartNumber;
    }

    /**
     * Returns true if the folio is suppressed on the first body page.
     * The page still receives a logical number; only the printed folio is omitted.
     */
    public boolean isSuppressFirstBodyFolio() {
        return suppressFirstBodyFolio;
    }

    /** Returns the horizontal position of the printed folio. */
    public FolioPosition getFolioPosition() {
        return folioPosition;
    }

    /**
     * Returns a new builder pre-populated with the values of this instance.
     *
     * @return a builder copying all fields from this instance
     */
    public Builder toBuilder() {
        return new Builder()
                .frontMatterStyle(this.frontMatterStyle)
                .bodyStyle(this.bodyStyle)
                .rearMatterStyle(this.rearMatterStyle)
                .frontMatterStartNumber(this.frontMatterStartNumber)
                .bodyStartNumber(this.bodyStartNumber)
                .rearMatterStartNumber(this.rearMatterStartNumber)
                .suppressFirstBodyFolio(this.suppressFirstBodyFolio)
                .folioPosition(this.folioPosition);
    }

    /** Returns a new {@link Builder} with sensible defaults. */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder for {@link NumberingConfig}. */
    public static final class Builder {

        private FolioStyle frontMatterStyle = FolioStyle.NONE;
        private FolioStyle bodyStyle = FolioStyle.ARABIC;
        private FolioStyle rearMatterStyle = FolioStyle.NONE;
        private int frontMatterStartNumber = 1;
        private int bodyStartNumber = 1;
        private int rearMatterStartNumber = 1;
        private boolean suppressFirstBodyFolio = false;
        private FolioPosition folioPosition = FolioPosition.BOTTOM_OUTER;

        private Builder() {
        }

        /**
         * Sets the front matter folio style.
         *
         * @param frontMatterStyle must not be null
         * @return this builder
         */
        public Builder frontMatterStyle(FolioStyle frontMatterStyle) {
            this.frontMatterStyle = Objects.requireNonNull(frontMatterStyle, "frontMatterStyle");
            return this;
        }

        /**
         * Sets the body folio style.
         *
         * @param bodyStyle must not be null
         * @return this builder
         */
        public Builder bodyStyle(FolioStyle bodyStyle) {
            this.bodyStyle = Objects.requireNonNull(bodyStyle, "bodyStyle");
            return this;
        }

        /**
         * Sets the rear matter folio style. Always {@link FolioStyle#NONE} in Phase 1.
         *
         * @param rearMatterStyle must not be null
         * @return this builder
         */
        public Builder rearMatterStyle(FolioStyle rearMatterStyle) {
            this.rearMatterStyle = Objects.requireNonNull(rearMatterStyle, "rearMatterStyle");
            return this;
        }

        /**
         * Sets the first front matter page number.
         *
         * @param frontMatterStartNumber must be positive
         * @return this builder
         * @throws IllegalArgumentException if not positive
         */
        public Builder frontMatterStartNumber(int frontMatterStartNumber) {
            if (frontMatterStartNumber < 1) {
                throw new IllegalArgumentException("frontMatterStartNumber must be >= 1");
            }
            this.frontMatterStartNumber = frontMatterStartNumber;
            return this;
        }

        /**
         * Sets the first body page number.
         *
         * @param bodyStartNumber must be positive
         * @return this builder
         * @throws IllegalArgumentException if not positive
         */
        public Builder bodyStartNumber(int bodyStartNumber) {
            if (bodyStartNumber < 1) {
                throw new IllegalArgumentException("bodyStartNumber must be >= 1");
            }
            this.bodyStartNumber = bodyStartNumber;
            return this;
        }

        /**
         * Sets the first rear matter page number.
         *
         * @param rearMatterStartNumber must be positive
         * @return this builder
         * @throws IllegalArgumentException if not positive
         */
        public Builder rearMatterStartNumber(int rearMatterStartNumber) {
            if (rearMatterStartNumber < 1) {
                throw new IllegalArgumentException("rearMatterStartNumber must be >= 1");
            }
            this.rearMatterStartNumber = rearMatterStartNumber;
            return this;
        }

        /**
         * Sets whether the printed folio is suppressed on the first body page.
         *
         * @param suppressFirstBodyFolio true to suppress
         * @return this builder
         */
        public Builder suppressFirstBodyFolio(boolean suppressFirstBodyFolio) {
            this.suppressFirstBodyFolio = suppressFirstBodyFolio;
            return this;
        }

        /**
         * Sets the horizontal position of the printed folio.
         *
         * @param folioPosition must not be null
         * @return this builder
         */
        public Builder folioPosition(FolioPosition folioPosition) {
            this.folioPosition = Objects.requireNonNull(folioPosition, "folioPosition");
            return this;
        }

        /** Builds the {@link NumberingConfig}. */
        public NumberingConfig build() {
            return new NumberingConfig(this);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof NumberingConfig other)) {
            return false;
        }
        return frontMatterStartNumber == other.frontMatterStartNumber
                && bodyStartNumber == other.bodyStartNumber
                && rearMatterStartNumber == other.rearMatterStartNumber
                && suppressFirstBodyFolio == other.suppressFirstBodyFolio
                && frontMatterStyle == other.frontMatterStyle
                && bodyStyle == other.bodyStyle
                && rearMatterStyle == other.rearMatterStyle
                && folioPosition == other.folioPosition;
    }

    @Override
    public int hashCode() {
        return Objects.hash(frontMatterStyle, bodyStyle, rearMatterStyle,
                frontMatterStartNumber, bodyStartNumber, rearMatterStartNumber,
                suppressFirstBodyFolio, folioPosition);
    }

    @Override
    public String toString() {
        return "NumberingConfig{"
                + "frontMatterStyle=" + frontMatterStyle
                + ", bodyStyle=" + bodyStyle
                + ", rearMatterStyle=" + rearMatterStyle
                + ", frontMatterStartNumber=" + frontMatterStartNumber
                + ", bodyStartNumber=" + bodyStartNumber
                + ", rearMatterStartNumber=" + rearMatterStartNumber
                + ", suppressFirstBodyFolio=" + suppressFirstBodyFolio
                + ", folioPosition=" + folioPosition
                + '}';
    }
}
