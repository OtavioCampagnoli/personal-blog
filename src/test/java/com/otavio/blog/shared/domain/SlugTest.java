package com.otavio.blog.shared.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class SlugTest {

    @ParameterizedTest(name = "''{0}'' -> ''{1}''")
    @CsvSource({
        "Hello World,           hello-world",
        "Como Construir uma API RESTful, como-construir-uma-api-restful",
        "Java 21 é Incrível!,   java-21-e-incrivel",
        "  espaços no inicio e fim  , espacos-no-inicio-e-fim",
        "múltiplos   espaços,    multiplos-espacos",
        "Acentuação: áéíóú,      acentuacao-aeiou"
    })
    void shouldNormalizeTextToSlug(String input, String expected) {
        Slug slug = Slug.from(input.strip());
        assertEquals(expected.strip(), slug.getValue());
    }

    @Test
    void shouldAcceptValidSlug() {
        Slug slug = Slug.of("valid-slug-123");
        assertEquals("valid-slug-123", slug.getValue());
    }

    @Test
    void shouldThrowOnInvalidSlugFormat() {
        assertThrows(IllegalArgumentException.class, () -> Slug.of("Invalid Slug"));
        assertThrows(IllegalArgumentException.class, () -> Slug.of("UPPERCASE"));
        assertThrows(IllegalArgumentException.class, () -> Slug.of("-starts-with-hyphen"));
    }

    @Test
    void shouldThrowOnNullOrBlank() {
        assertThrows(IllegalArgumentException.class, () -> Slug.from(null));
        assertThrows(IllegalArgumentException.class, () -> Slug.from("   "));
    }

    @Test
    void shouldAddSuffix() {
        Slug base = Slug.of("my-post");
        Slug withSuffix = base.withSuffix(2);
        assertEquals("my-post-2", withSuffix.getValue());
    }

    @Test
    void shouldBeEqualForSameValue() {
        assertEquals(Slug.of("same-slug"), Slug.of("same-slug"));
        assertNotEquals(Slug.of("slug-a"), Slug.of("slug-b"));
    }
}
