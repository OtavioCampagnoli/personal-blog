package com.otavio.blog.shared.domain;

/**
 * Exception para operações não autorizadas
 */
public class ForbiddenException extends DomainException {
    
    public ForbiddenException(String code, String message) {
        super(code, message);
    }

    public static ForbiddenException notAuthor() {
        return new ForbiddenException(
            "NOT_AUTHOR",
            "You don't have permission to perform this operation. Only the author can edit this resource."
        );
    }
}
