package com.aileapro.persistence;

import com.aileapro.domain.BlockType;
import com.aileapro.domain.ContentStatus;
import com.aileapro.domain.UnitKind;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Unit kinds, statuses, and block types are stored lowercase (they are part of the API
 * surface and of the seed files), so they cannot use plain @Enumerated(STRING).
 */
public final class CurriculumConverters {

    private CurriculumConverters() {
    }

    @Converter(autoApply = true)
    public static class UnitKindConverter implements AttributeConverter<UnitKind, String> {
        @Override
        public String convertToDatabaseColumn(UnitKind attribute) {
            return attribute == null ? null : attribute.code();
        }

        @Override
        public UnitKind convertToEntityAttribute(String dbData) {
            return dbData == null ? null : UnitKind.fromCode(dbData);
        }
    }

    @Converter(autoApply = true)
    public static class ContentStatusConverter implements AttributeConverter<ContentStatus, String> {
        @Override
        public String convertToDatabaseColumn(ContentStatus attribute) {
            return attribute == null ? null : attribute.code();
        }

        @Override
        public ContentStatus convertToEntityAttribute(String dbData) {
            return dbData == null ? null : ContentStatus.fromCode(dbData);
        }
    }

    @Converter(autoApply = true)
    public static class BlockTypeConverter implements AttributeConverter<BlockType, String> {
        @Override
        public String convertToDatabaseColumn(BlockType attribute) {
            return attribute == null ? null : attribute.code();
        }

        @Override
        public BlockType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : BlockType.fromCode(dbData);
        }
    }
}
