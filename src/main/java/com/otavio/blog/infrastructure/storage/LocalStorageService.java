package com.otavio.blog.infrastructure.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Implementação local do StorageService — usada quando AWS não está configurada.
 * Salva os arquivos em /tmp/blog-uploads/.
 *
 * TODO: Implementar S3StorageService quando configurar AWS em produção.
 *       Usar @Profile("aws") nessa classe e @Profile("!aws") aqui.
 */
@Service
public class LocalStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);

    private static final Path BASE_DIR = Paths.get(System.getProperty("java.io.tmpdir"), "blog-uploads");

    @Override
    public String upload(MultipartFile file, String key) {
        try {
            Path target = BASE_DIR.resolve(key);
            Files.createDirectories(target.getParent());
            Files.write(target, file.getBytes());
            log.info("File saved locally: {}", target);
            return key;
        } catch (IOException e) {
            throw new StorageException("Failed to save file locally: " + key, e);
        }
    }

    @Override
    public String resolveUrl(String key) {
        // Em desenvolvimento, aponta para um endpoint simulado
        return "http://localhost:8080/uploads/" + key;
    }

    @Override
    public void delete(String key) {
        try {
            Path target = BASE_DIR.resolve(key);
            Files.deleteIfExists(target);
        } catch (IOException e) {
            log.warn("Could not delete local file: {}", key, e);
        }
    }
}
