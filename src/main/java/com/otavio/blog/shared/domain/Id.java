package com.otavio.blog.shared.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Value Object representando um identificador único tipado.
 * 
 * @param <T> O tipo da entidade que este ID identifica
 */
public final class Id<T> implements Serializable {
    
    private final UUID value;

    private Id(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("Id value cannot be null");
        }
        this.value = value;
    }

    /**
     * Cria um novo ID com valor UUID aleatório
     */
    public static <T> Id<T> generate() {
        return new Id<>(UUID.randomUUID());
    }

    /**
     * Cria um ID a partir de uma string UUID
     */
    public static <T> Id<T> of(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            throw new IllegalArgumentException("Id string cannot be null or blank");
        }
        try {
            return new Id<>(UUID.fromString(uuid));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID format: " + uuid, e);
        }
    }

    /**
     * Cria um ID a partir de um UUID
     */
    public static <T> Id<T> of(UUID uuid) {
        return new Id<>(uuid);
    }

    public UUID getValue() {
        return value;
    }

    public String asString() {
        return value.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Id<?> id = (Id<?>) o;
        return Objects.equals(value, id.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
