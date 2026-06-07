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
 * Represents a single logical page within a {@link PageSequence}.
 *
 * <p>Instances are immutable. The {@link #getPhysicalPosition()} field reflects the page's
 * current 0-based index in the sequence and is reassigned by {@link PageSequence#reindex()}
 * whenever pages are inserted, removed, or moved. Use {@link #toBuilder()} to create a copy
 * with an updated position.
 */
public final class QuirePage {

    private final int physicalPosition;
    private final PageType pageType;
    private final Optional<PageZone> pageZone;
    private final Optional<Integer> logicalPageNumber;
    private final Optional<String> sourceDocumentId;
    private final Optional<Integer> sourcePageIndex;

    private QuirePage(Builder builder) {
        this.physicalPosition = builder.physicalPosition;
        this.pageType = builder.pageType;
        this.pageZone = builder.pageZone;
        this.logicalPageNumber = builder.logicalPageNumber;
        this.sourceDocumentId = builder.sourceDocumentId;
        this.sourcePageIndex = builder.sourcePageIndex;
    }

    /** Returns the 0-based position of this page in its containing sequence. */
    public int getPhysicalPosition() {
        return physicalPosition;
    }

    /** Returns the type of this page. */
    public PageType getPageType() {
        return pageType;
    }

    /**
     * Returns the numbering zone this page belongs to, if assigned.
     * Absent for {@link PageType#COMPLETION_BLANK} and {@link PageType#FILLER_BLANK} pages,
     * and for aesthetic pages sandwiched between content pages.
     */
    public Optional<PageZone> getPageZone() {
        return pageZone;
    }

    /**
     * Returns the logical (folio) page number, if this page carries one.
     * Absent for {@link PageType#COMPLETION_BLANK} and {@link PageType#FILLER_BLANK} pages.
     */
    public Optional<Integer> getLogicalPageNumber() {
        return logicalPageNumber;
    }

    /**
     * Returns the identifier of the source document this page originates from.
     * Absent for synthesised pages (aesthetic, blank).
     */
    public Optional<String> getSourceDocumentId() {
        return sourceDocumentId;
    }

    /**
     * Returns the 0-based page index within the source document.
     * Absent when {@link #getSourceDocumentId()} is absent.
     */
    public Optional<Integer> getSourcePageIndex() {
        return sourcePageIndex;
    }

    /**
     * Returns a new builder pre-populated with the values of this instance.
     *
     * @return a builder copying all fields from this instance
     */
    public Builder toBuilder() {
        return new Builder()
                .physicalPosition(this.physicalPosition)
                .pageType(this.pageType)
                .pageZone(this.pageZone.orElse(null))
                .logicalPageNumber(this.logicalPageNumber.orElse(null))
                .sourceDocumentId(this.sourceDocumentId.orElse(null))
                .sourcePageIndex(this.sourcePageIndex.orElse(null));
    }

    /** Returns a new {@link Builder}. */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder for {@link QuirePage}. */
    public static final class Builder {

        private int physicalPosition;
        private PageType pageType;
        private Optional<PageZone> pageZone = Optional.empty();
        private Optional<Integer> logicalPageNumber = Optional.empty();
        private Optional<String> sourceDocumentId = Optional.empty();
        private Optional<Integer> sourcePageIndex = Optional.empty();

        private Builder() {
        }

        /**
         * Sets the 0-based physical position in the sequence.
         *
         * @param physicalPosition must be zero or positive
         * @return this builder
         */
        public Builder physicalPosition(int physicalPosition) {
            if (physicalPosition < 0) {
                throw new IllegalArgumentException("physicalPosition must be >= 0");
            }
            this.physicalPosition = physicalPosition;
            return this;
        }

        /**
         * Sets the page type.
         *
         * @param pageType must not be null
         * @return this builder
         */
        public Builder pageType(PageType pageType) {
            this.pageType = Objects.requireNonNull(pageType, "pageType");
            return this;
        }

        /**
         * Sets the numbering zone, or null if this page belongs to no zone.
         *
         * @param pageZone the zone, or null
         * @return this builder
         */
        public Builder pageZone(PageZone pageZone) {
            this.pageZone = Optional.ofNullable(pageZone);
            return this;
        }

        /**
         * Sets the logical page number, or null for unnumbered pages.
         *
         * @param logicalPageNumber positive value, or null
         * @return this builder
         */
        public Builder logicalPageNumber(Integer logicalPageNumber) {
            if (logicalPageNumber != null && logicalPageNumber <= 0) {
                throw new IllegalArgumentException("logicalPageNumber must be > 0");
            }
            this.logicalPageNumber = Optional.ofNullable(logicalPageNumber);
            return this;
        }

        /**
         * Sets the source document identifier, or null for synthesised pages.
         *
         * @param sourceDocumentId the identifier, or null
         * @return this builder
         */
        public Builder sourceDocumentId(String sourceDocumentId) {
            this.sourceDocumentId = Optional.ofNullable(sourceDocumentId);
            return this;
        }

        /**
         * Sets the 0-based page index within the source document, or null if not applicable.
         *
         * @param sourcePageIndex zero or positive, or null
         * @return this builder
         */
        public Builder sourcePageIndex(Integer sourcePageIndex) {
            if (sourcePageIndex != null && sourcePageIndex < 0) {
                throw new IllegalArgumentException("sourcePageIndex must be >= 0");
            }
            this.sourcePageIndex = Optional.ofNullable(sourcePageIndex);
            return this;
        }

        /**
         * Builds the {@link QuirePage}.
         *
         * @return a new immutable instance
         * @throws NullPointerException if pageType is not set
         */
        public QuirePage build() {
            Objects.requireNonNull(pageType, "pageType must be set");
            return new QuirePage(this);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof QuirePage other)) {
            return false;
        }
        return physicalPosition == other.physicalPosition
                && pageType == other.pageType
                && pageZone.equals(other.pageZone)
                && logicalPageNumber.equals(other.logicalPageNumber)
                && sourceDocumentId.equals(other.sourceDocumentId)
                && sourcePageIndex.equals(other.sourcePageIndex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(physicalPosition, pageType, pageZone, logicalPageNumber,
                sourceDocumentId, sourcePageIndex);
    }

    @Override
    public String toString() {
        return "QuirePage{"
                + "physicalPosition=" + physicalPosition
                + ", pageType=" + pageType
                + ", pageZone=" + pageZone.map(Enum::name).orElse("none")
                + ", logicalPageNumber=" + logicalPageNumber
                + '}';
    }
}
