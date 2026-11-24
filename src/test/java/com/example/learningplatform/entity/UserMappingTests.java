package com.example.learningplatform.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

class UserMappingTests {

    @Test
    void mapsOnlyFieldsFromErDiagram() {
        assertThat(User.class.getDeclaredFields())
                .extracting(Field::getName)
                .containsExactlyInAnyOrder("id", "name", "email", "role");
    }

    @Test
    void mapsUserToSafeTableNameWithUniqueEmail() throws NoSuchFieldException {
        Table table = User.class.getAnnotation(Table.class);
        Column email = User.class.getDeclaredField("email").getAnnotation(Column.class);

        assertThat(table.name()).isEqualTo("users");
        assertThat(email.nullable()).isFalse();
        assertThat(email.unique()).isTrue();
    }

    @Test
    void storesRoleAsText() throws NoSuchFieldException {
        Enumerated role = User.class.getDeclaredField("role").getAnnotation(Enumerated.class);

        assertThat(role.value()).isEqualTo(EnumType.STRING);
    }

    @Test
    void createsUserWithoutAuthenticationData() {
        User user = new User("Анна", "anna@example.com", Role.STUDENT);

        assertThat(user.getId()).isNull();
        assertThat(user.getName()).isEqualTo("Анна");
        assertThat(user.getEmail()).isEqualTo("anna@example.com");
        assertThat(user.getRole()).isEqualTo(Role.STUDENT);
    }
}
