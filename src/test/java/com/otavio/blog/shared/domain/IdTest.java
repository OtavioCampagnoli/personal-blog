package com.otavio.blog.shared.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class IdTest {

    @Test
    void shouldGenerateUniqueIds() {
        Id<Object> a = Id.generate();
        Id<Object> b = Id.generate();
        assertNotNull(a);
        assertNotNull(b);
        assertNotEquals(a, b);
    }

    @Test
    void shouldCreateFromValidString() {
        String uuid = UUID.randomUUID().toString();
        Id<Object> id = Id.of(uuid);
        assertEquals(uuid, id.asString());
    }

    @Test
    void shouldCreateFromUUID() {
        UUID uuid = UUID.randomUUID();
        Id<Object> id = Id.of(uuid);
        assertEquals(uuid, id.getValue());
    }

    @Test
    void shouldThrowOnNullString() {
        assertThrows(IllegalArgumentException.class, () -> Id.of((String) null));
    }

    @Test
    void shouldThrowOnInvalidUUIDFormat() {
        assertThrows(IllegalArgumentException.class, () -> Id.of("not-a-uuid"));
    }

    @Test
    void shouldBeEqualForSameValue() {
        UUID uuid = UUID.randomUUID();
        assertEquals(Id.of(uuid), Id.of(uuid));
    }

    @Test
    void shouldHaveConsistentHashCode() {
        UUID uuid = UUID.randomUUID();
        assertEquals(Id.of(uuid).hashCode(), Id.of(uuid).hashCode());
    }
}
