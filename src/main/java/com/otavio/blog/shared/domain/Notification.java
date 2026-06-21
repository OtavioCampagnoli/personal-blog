package com.otavio.blog.shared.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implementação do Notification Pattern para acumular erros de validação
 */
public class Notification {
    
    private final List<Error> errors;

    private Notification() {
        this.errors = new ArrayList<>();
    }

    public static Notification create() {
        return new Notification();
    }

    /**
     * Adiciona um erro à notificação
     * 
     * @param field Campo que gerou o erro
     * @param code Código do erro (ex: TITLE_TOO_SHORT)
     * @param message Mensagem descritiva do erro
     */
    public void addError(String field, String code, String message) {
        errors.add(new Error(field, code, message));
    }

    /**
     * Adiciona um erro à notificação com valor fornecido e exemplo
     */
    public void addError(String field, String code, String message, Object providedValue, String example) {
        errors.add(new Error(field, code, message, providedValue, example));
    }

    /**
     * Verifica se há erros acumulados
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Retorna lista imutável de erros
     */
    public List<Error> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    /**
     * Retorna quantidade de erros
     */
    public int errorCount() {
        return errors.size();
    }

    /**
     * Lança exceção se houver erros acumulados
     */
    public void throwIfHasErrors() {
        if (hasErrors()) {
            throw new DomainValidationException(this);
        }
    }

    /**
     * Retorna mensagem consolidada com todos os erros
     */
    public String getMessage() {
        if (errors.isEmpty()) {
            return "No errors";
        }
        
        StringBuilder sb = new StringBuilder("Validation errors: ");
        for (int i = 0; i < errors.size(); i++) {
            Error error = errors.get(i);
            sb.append(error.field()).append(": ").append(error.message());
            if (i < errors.size() - 1) {
                sb.append("; ");
            }
        }
        return sb.toString();
    }

    /**
     * Record representando um erro de validação
     */
    public record Error(
        String field,
        String code,
        String message,
        Object providedValue,
        String example
    ) {
        public Error(String field, String code, String message) {
            this(field, code, message, null, null);
        }
    }

    @Override
    public String toString() {
        return getMessage();
    }
}
