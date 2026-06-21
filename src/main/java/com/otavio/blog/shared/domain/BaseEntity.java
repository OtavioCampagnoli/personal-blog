package com.otavio.blog.shared.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Classe base para todas as entidades do domínio
 * 
 * @param <T> O tipo da entidade
 */
public abstract class BaseEntity<T> {
    
    protected Id<T> id;
    protected Instant createdAt;
    protected Instant updatedAt;
    protected Instant deletedAt; // Soft delete

    protected BaseEntity() {
        this.id = Id.generate();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    protected BaseEntity(Id<T> id) {
        this.id = Objects.requireNonNull(id, "Id cannot be null");
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Valida a entidade e acumula erros na notificação
     */
    public abstract void validate(Notification notification);

    /**
     * Marca a entidade como atualizada
     */
    public void markAsUpdated() {
        this.updatedAt = Instant.now();
    }

    /**
     * Soft delete - marca a entidade como deletada
     */
    public void markAsDeleted() {
        this.deletedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Verifica se a entidade foi deletada (soft delete)
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Restaura uma entidade deletada
     */
    public void restore() {
        this.deletedAt = null;
        this.updatedAt = Instant.now();
    }

    // Getters
    public Id<T> getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    // Setters para reconstrução a partir do banco
    public void setId(Id<T> id) {
        this.id = id;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseEntity<?> that = (BaseEntity<?>) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "id=" + id +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", deletedAt=" + deletedAt +
                '}';
    }
}
