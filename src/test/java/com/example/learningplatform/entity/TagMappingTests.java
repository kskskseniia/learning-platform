package com.example.learningplatform.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;

import jakarta.persistence.Column;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

class TagMappingTests {

    @Test
    void mapsOnlyFieldsFromErDiagram() {
        assertThat(Tag.class.getDeclaredFields())
                .extracting(Field::getName)
                .containsExactlyInAnyOrder("id", "name");
    }

    @Test
    void mapsRequiredTagName() throws NoSuchFieldException {
        Column name = Tag.class.getDeclaredField("name").getAnnotation(Column.class);

        Table table = Tag.class.getAnnotation(Table.class);

        assertThat(table.name()).isEqualTo("tag");
        assertThat(name.nullable()).isFalse();
        assertThat(table.indexes()).anySatisfy(index -> {
            assertThat(index.name()).isEqualTo("uk_tag_name");
            assertThat(index.columnList()).isEqualTo("name");
            assertThat(index.unique()).isTrue();
        });
    }

    @Test
    void createsTagWithName() {
        Tag tag = new Tag("Java");

        assertThat(tag.getId()).isNull();
        assertThat(tag.getName()).isEqualTo("Java");
    }
}
