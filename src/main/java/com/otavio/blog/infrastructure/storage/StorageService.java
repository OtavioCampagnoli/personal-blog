package com.otavio.blog.infrastructure.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    /**
     * Faz upload do arquivo e retorna a key no storage
     */
    String upload(MultipartFile file, String key);

    /**
     * Resolve a URL pública para uma key
     */
    String resolveUrl(String key);

    /**
     * Remove o arquivo do storage
     */
    void delete(String key);
}
