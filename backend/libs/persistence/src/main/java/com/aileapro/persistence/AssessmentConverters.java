package com.aileapro.persistence;

import com.aileapro.domain.AssessmentKind;
import com.aileapro.domain.BloomLevel;
import com.aileapro.domain.ItemType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Item types, Bloom levels, and assessment kinds are stored lowercase: all three appear in
 * the seed files and on the wire, so the database spelling and the API spelling have to be
 * the same one.
 */
public final class AssessmentConverters {

    private AssessmentConverters() {
    }

    @Converter(autoApply = true)
    public static class ItemTypeConverter implements AttributeConverter<ItemType, String> {
        @Override
        public String convertToDatabaseColumn(ItemType attribute) {
            return attribute == null ? null : attribute.code();
        }

        @Override
        public ItemType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : ItemType.fromCode(dbData);
        }
    }

    @Converter(autoApply = true)
    public static class BloomConverter implements AttributeConverter<BloomLevel, String> {
        @Override
        public String convertToDatabaseColumn(BloomLevel attribute) {
            return attribute == null ? null : attribute.code();
        }

        @Override
        public BloomLevel convertToEntityAttribute(String dbData) {
            return dbData == null ? null : BloomLevel.fromCode(dbData);
        }
    }

    @Converter(autoApply = true)
    public static class AssessmentKindConverter implements AttributeConverter<AssessmentKind, String> {
        @Override
        public String convertToDatabaseColumn(AssessmentKind attribute) {
            return attribute == null ? null : attribute.code();
        }

        @Override
        public AssessmentKind convertToEntityAttribute(String dbData) {
            return dbData == null ? null : AssessmentKind.fromCode(dbData);
        }
    }
}
