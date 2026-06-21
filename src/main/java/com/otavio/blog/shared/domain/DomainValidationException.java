package com.otavio.blog.shared.domain;

import java.util.List;

/**
 * Exception lançada quando há erros de validação de domínio
 */
public class DomainValidationException extends RuntimeException {
    
    private final List<Notification.Error> errors;

    public DomainValidationException(Notification notification) {
        super(notification.getMessage());
        this.errors = notification.getErrors();
    }

    public DomainValidationException(String message) {
        super(message);
        Notification notification = Notification.create();
        notification.addError("general", "VALIDATION_ERROR", message);
        this.errors = notification.getErrors();
    }

    public List<Notification.Error> getErrors() {
        return errors;
    }

    public boolean hasMultipleErrors() {
        return errors.size() > 1;
    }
}
