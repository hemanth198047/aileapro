package com.aileapro.persistence;

import com.aileapro.domain.EvidenceState;
import com.aileapro.domain.LearningEventKind;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Evidence states and event kinds are stored lowercase because both are part of the API
 * surface, so they cannot use plain {@code @Enumerated(STRING)}. Mastery states are stored
 * uppercase and do not need a converter.
 */
public final class LearnerConverters {

    private LearnerConverters() {
    }

    @Converter(autoApply = true)
    public static class EvidenceStateConverter implements AttributeConverter<EvidenceState, String> {
        @Override
        public String convertToDatabaseColumn(EvidenceState attribute) {
            return attribute == null ? null : attribute.code();
        }

        @Override
        public EvidenceState convertToEntityAttribute(String dbData) {
            return dbData == null ? null : EvidenceState.fromCode(dbData);
        }
    }

    @Converter(autoApply = true)
    public static class LearningEventKindConverter
            implements AttributeConverter<LearningEventKind, String> {
        @Override
        public String convertToDatabaseColumn(LearningEventKind attribute) {
            return attribute == null ? null : attribute.code();
        }

        @Override
        public LearningEventKind convertToEntityAttribute(String dbData) {
            return dbData == null ? null : LearningEventKind.fromCode(dbData);
        }
    }
}
