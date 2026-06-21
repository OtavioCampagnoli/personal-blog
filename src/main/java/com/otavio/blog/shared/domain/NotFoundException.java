package com.otavio.blog.shared.domain;

/**
 * Exception para recursos não encontrados
 */
public class NotFoundException extends DomainException {
    
    public NotFoundException(String code, String message) {
        super(code, message);
    }

    public static NotFoundException of(String resourceType, String identifier) {
        return new NotFoundException(
            resourceType.toUpperCase() + "_NOT_FOUND",
            resourceType + " not found with identifier: " + identifier
        );
    }
}
