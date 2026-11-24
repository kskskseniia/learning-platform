package com.example.learningplatform.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;

import jakarta.persistence.Column;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

class CategoryMappingTests {

    @Test
    void mapsOnlyFieldsFromErDiagram() {
        assertThat(Category.class.getDeclaredFields())
                .extracting(Field::getName)
                .containsExactlyInAnyOrder("id", "name");
    }

    @Test
    void mapsRequiredCategoryName() throws NoSuchFieldException {
        Column name = Category.class.getDeclaredField("name").getAnnotation(Column.class);

        Table table = Category.class.getAnnotation(Table.class);

        assertThat(table.name()).isEqualTo("category");
        assertThat(name.nullable()).isFalse();
        assertThat(table.indexes()).anySatisfy(index -> {
            assertThat(index.name()).isEqualTo("uk_category_name");
            assertThat(index.columnList()).isEqualTo("name");
            assertThat(index.unique()).isTrue();
        });
    }

    @Test
    void createsCategoryWithName() {
        Category category = new Category("Programming");

        assertThat(category.getId()).isNull();
        assertThat(category.getName()).isEqualTo("Programming");
    }
}
