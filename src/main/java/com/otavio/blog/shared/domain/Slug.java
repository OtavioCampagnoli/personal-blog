package com.otavio.blog.shared.domain;

import java.io.Serializable;
import java.text.Normalizer;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value Object representando um slug (URL-friendly identifier)
 */
public final class Slug implements Serializable {
    
    private static final Pattern NON_ASCII = Pattern.compile("[^\\p{ASCII}]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern SPECIAL_CHARS = Pattern.compile("[^a-z0-9\\-]");
    private static final Pattern MULTIPLE_HYPHENS = Pattern.compile("-+");
    
    private final String value;

    private Slug(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Slug cannot be null or blank");
        }
        
        if (!isValid(value)) {
            throw new IllegalArgumentException("Invalid slug format: " + value + 
                ". Must contain only lowercase letters, numbers and hyphens");
        }
        
        this.value = value;
    }

    /**
     * Cria um slug a partir de uma string já formatada
     */
    public static Slug of(String slug) {
        return new Slug(slug);
    }

    /**
     * Gera um slug a partir de um texto qualquer
     */
    public static Slug from(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Text cannot be null or blank");
        }
        
        String slug = normalize(text);
        return new Slug(slug);
    }

    /**
     * Normaliza um texto para formato slug
     */
    private static String normalize(String text) {
        // Remove acentos
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        normalized = NON_ASCII.matcher(normalized).replaceAll("");
        
        // Lowercase
        normalized = normalized.toLowerCase();
        
        // Substitui espaços por hífens
        normalized = WHITESPACE.matcher(normalized).replaceAll("-");
        
        // Remove caracteres especiais
        normalized = SPECIAL_CHARS.matcher(normalized).replaceAll("");
        
        // Remove múltiplos hífens consecutivos
        normalized = MULTIPLE_HYPHENS.matcher(normalized).replaceAll("-");
        
        // Remove hífens no início e fim
        normalized = normalized.replaceAll("^-+|-+$", "");
        
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Cannot generate slug from text: " + text);
        }
        
        return normalized;
    }

    /**
     * Valida se uma string está no formato slug válido
     */
    private static boolean isValid(String slug) {
        return slug.matches("^[a-z0-9]+(?:-[a-z0-9]+)*$");
    }

    /**
     * Cria um novo slug adicionando um sufixo numérico
     */
    public Slug withSuffix(int suffix) {
        return new Slug(value + "-" + suffix);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Slug slug = (Slug) o;
        return Objects.equals(value, slug.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
