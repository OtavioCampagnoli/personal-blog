# Task 04: Criar Endpoint POST /api/blog/images para Upload de Imagem no S3

## User Story
Como um **Autor**  
Eu quero **fazer upload de imagens para o S3**  
Para que **eu possa usar essas imagens nos tópicos dos meus posts**

## Business Context
Autores precisam adicionar imagens aos seus posts para enriquecer o conteúdo. As imagens devem ser armazenadas no S3 (serverless, escalável, CDN-friendly) e os metadados salvos no banco. Este endpoint apenas faz upload; a associação com tópicos é feita em outro endpoint.

## Acceptance Criteria

### Happy Path
1. Given eu sou um autor autenticado  
   When eu faço POST para `/api/blog/images` com uma imagem válida  
   Then a imagem é enviada para o S3 no bucket configurado  
   And um registro Image é criado no banco com URL do S3  
   And eu recebo HTTP 201 Created com metadados da imagem  
   And o header Location contém a URL da imagem

2. Given eu envio uma imagem JPEG de 2MB  
   When faço o upload  
   Then a imagem é salva no S3 com key: `images/{authorId}/{uuid}.jpg`  
   And o Content-Type é preservado como `image/jpeg`  
   And a imagem fica acessível publicamente via CloudFront/S3 URL

3. Given eu envio uma imagem PNG com nome "screenshot.png"  
   When faço o upload  
   Then o nome original é ignorado (usamos UUID)  
   And a extensão é preservada: `{uuid}.png`

### Validation Rules
- **File**: Obrigatório, não pode ser vazio
- **Content-Type**: Deve ser `image/jpeg`, `image/png`, `image/webp`, ou `image/gif`
- **File Size**: Máximo 5MB (5,242,880 bytes)
- **Dimensions**: Mínimo 100x100px, máximo 4000x4000px
- **altText**: Opcional, máximo 200 caracteres (acessibilidade)

### Error Scenarios
1. Given eu não estou autenticado  
   When eu tento fazer upload  
   Then eu recebo HTTP 401 Unauthorized

2. Given eu envio um arquivo PDF (não é imagem)  
   When eu faço o upload  
   Then eu recebo HTTP 400 Bad Request  
   And a resposta inclui: "Tipo de arquivo não suportado. Use: JPEG, PNG, WebP ou GIF"

3. Given eu envio uma imagem de 8MB  
   When eu faço o upload  
   Then eu recebo HTTP 413 Payload Too Large  
   And a resposta inclui: "Tamanho máximo permitido: 5MB (fornecido: 8MB)"

4. Given eu envio uma imagem 50x50px (menor que mínimo)  
   When eu faço o upload  
   Then eu recebo HTTP 400 Bad Request  
   And a resposta inclui: "Dimensões mínimas: 100x100px (fornecido: 50x50px)"

5. Given eu envio uma imagem 5000x5000px (maior que máximo)  
   When eu faço o upload  
   Then eu recebo HTTP 400 Bad Request  
   And a resposta inclui: "Dimensões máximas: 4000x4000px (fornecido: 5000x5000px)"

6. Given ocorre erro ao conectar com S3  
   When eu faço o upload  
   Then eu recebo HTTP 502 Bad Gateway  
   And a resposta inclui: "Erro ao fazer upload da imagem. Tente novamente"  
   And o erro é logado para debugging

7. Given eu envio um arquivo vazio  
   When eu faço o upload  
   Then eu recebo HTTP 400 Bad Request  
   And a resposta inclui: "Arquivo não pode ser vazio"

### Edge Cases
1. Given eu faço upload da mesma imagem duas vezes  
   When ambos uploads são bem-sucedidos  
   Then cada um gera um UUID diferente  
   And ambos são salvos no S3 (não há deduplicação)

2. Given eu forneço altText com 250 caracteres  
   When eu faço o upload  
   Then eu recebo HTTP 400 Bad Request  
   And a resposta inclui: "Texto alternativo não pode exceder 200 caracteres"

3. Given a imagem tem EXIF data com orientação rotacionada  
   When eu faço o upload  
   Then a orientação EXIF é aplicada automaticamente  
   And a imagem é salva na orientação correta

4. Given eu envio uma imagem PNG com transparência  
   When eu faço o upload  
   Then a transparência é preservada  
   And a imagem é salva como PNG (não convertida para JPEG)

### Non-Functional Requirements
- **Performance**: Upload deve completar em <3s para imagens de 5MB
- **Storage**: S3 bucket com CloudFront CDN configurado
- **Security**: 
  - Imagens são públicas (leitura), mas upload requer autenticação
  - Validar magic bytes do arquivo (não confiar apenas em Content-Type)
  - Sanitizar nomes de arquivo para prevenir path traversal
- **Scalability**: Suportar uploads concorrentes de múltiplos usuários
- **Monitoring**: Logar tamanho, tipo e tempo de upload para métricas
- **HATEOAS**: Incluir links para: `self`, `delete`

## Request Example (Multipart Form)
```http
POST /api/blog/images
Content-Type: multipart/form-data
Authorization: Bearer {token}
Accept-Language: pt-BR

------WebKitFormBoundary7MA4YWxkTrZu0gW
Content-Disposition: form-data; name="file"; filename="architecture-diagram.png"
Content-Type: image/png

<binary image data>
------WebKitFormBoundary7MA4YWxkTrZu0gW
Content-Disposition: form-data; name="altText"

Diagrama de arquitetura do sistema mostrando componentes e suas interações
------WebKitFormBoundary7MA4YWxkTrZu0gW--
```

## Response Example (Success - 201 Created)
```json
HTTP/1.1 201 Created
Location: /api/blog/images/img-550e8400-e29b-41d4-a716-446655440000
Content-Type: application/json

{
  "id": "img-550e8400-e29b-41d4-a716-446655440000",
  "url": "https://d1234567890.cloudfront.net/images/author-123/550e8400-e29b-41d4-a716-446655440000.png",
  "s3Key": "images/author-123/550e8400-e29b-41d4-a716-446655440000.png",
  "altText": "Diagrama de arquitetura do sistema mostrando componentes e suas interações",
  "contentType": "image/png",
  "sizeBytes": 2458624,
  "width": 1920,
  "height": 1080,
  "authorId": "author-123",
  "uploadedAt": "2026-06-18T12:00:00Z",
  "_links": {
    "self": {
      "href": "/api/blog/images/img-550e8400-e29b-41d4-a716-446655440000"
    },
    "delete": {
      "href": "/api/blog/images/img-550e8400-e29b-41d4-a716-446655440000"
    },
    "cdn": {
      "href": "https://d1234567890.cloudfront.net/images/author-123/550e8400-e29b-41d4-a716-446655440000.png"
    }
  }
}
```

## Response Example (Validation Error - File Too Large)
```json
HTTP/1.1 413 Payload Too Large
Content-Type: application/problem+json

{
  "timestamp": "2026-06-18T12:00:00Z",
  "status": 413,
  "error": "Payload Too Large",
  "code": "IMAGE_TOO_LARGE",
  "message": "Tamanho máximo permitido: 5MB (fornecido: 8.2MB)",
  "details": {
    "maxSizeBytes": 5242880,
    "providedSizeBytes": 8601600,
    "maxSizeMB": 5.0,
    "providedSizeMB": 8.2
  },
  "_links": {
    "docs": {
      "href": "/api/blog/docs/images"
    }
  }
}
```

## Response Example (Invalid File Type)
```json
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{
  "timestamp": "2026-06-18T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "code": "UNSUPPORTED_FILE_TYPE",
  "message": "Tipo de arquivo não suportado. Use: JPEG, PNG, WebP ou GIF",
  "details": {
    "providedContentType": "application/pdf",
    "supportedTypes": ["image/jpeg", "image/png", "image/webp", "image/gif"]
  },
  "_links": {
    "docs": {
      "href": "/api/blog/docs/images"
    }
  }
}
```

## Technical Notes

### S3 Configuration
```yaml
aws:
  s3:
    bucket-name: otavio-blog-images-prod
    region: us-east-1
    cloudfront-domain: d1234567890.cloudfront.net
  credentials:
    access-key: ${AWS_ACCESS_KEY}
    secret-key: ${AWS_SECRET_KEY}
```

### Image Processing
Use biblioteca **Thumbnailator** ou **ImageIO**:
```java
public ImageMetadata extractMetadata(MultipartFile file) throws IOException {
    BufferedImage image = ImageIO.read(file.getInputStream());
    
    if (image == null) {
        throw new InvalidImageException("Cannot read image data");
    }
    
    int width = image.getWidth();
    int height = image.getHeight();
    
    if (width < 100 || height < 100) {
        throw new ImageDimensionException("Minimum dimensions: 100x100px");
    }
    
    if (width > 4000 || height > 4000) {
        throw new ImageDimensionException("Maximum dimensions: 4000x4000px");
    }
    
    return new ImageMetadata(width, height);
}
```

### Magic Bytes Validation
```java
private static final Map<String, byte[]> MAGIC_BYTES = Map.of(
    "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
    "image/png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47},
    "image/gif", new byte[]{0x47, 0x49, 0x46, 0x38},
    "image/webp", new byte[]{0x52, 0x49, 0x46, 0x46}
);

public void validateFileType(MultipartFile file) throws IOException {
    byte[] fileBytes = file.getBytes();
    String contentType = file.getContentType();
    
    byte[] magicBytes = MAGIC_BYTES.get(contentType);
    if (magicBytes == null) {
        throw new UnsupportedFileTypeException(contentType);
    }
    
    for (int i = 0; i < magicBytes.length; i++) {
        if (fileBytes[i] != magicBytes[i]) {
            throw new InvalidFileException("File content does not match declared type");
        }
    }
}
```

### S3 Upload Service
```java
@Service
public class S3ImageService {
    private final AmazonS3 s3Client;
    private final String bucketName;
    
    public String uploadImage(MultipartFile file, String authorId) {
        String fileExtension = getFileExtension(file.getOriginalFilename());
        String key = String.format("images/%s/%s.%s", 
            authorId, UUID.randomUUID(), fileExtension);
        
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());
        metadata.setCacheControl("public, max-age=31536000"); // 1 year
        
        PutObjectRequest request = new PutObjectRequest(
            bucketName, 
            key, 
            file.getInputStream(), 
            metadata
        ).withCannedAcl(CannedAccessControlList.PublicRead);
        
        s3Client.putObject(request);
        
        return key;
    }
}
```

### Domain Model
```java
public class Image extends BaseEntity<Image> {
    private Id<Image> id;
    private String s3Key;
    private String url; // CloudFront URL
    private String altText;
    private String contentType;
    private long sizeBytes;
    private int width;
    private int height;
    private Id<Author> authorId;
    private Instant uploadedAt;
    
    public void validate(Notification notification) {
        if (altText != null && altText.length() > 200) {
            notification.addError("altText", "ALT_TEXT_TOO_LONG", 
                "Alt text cannot exceed 200 characters");
        }
        if (width < 100 || height < 100) {
            notification.addError("dimensions", "DIMENSIONS_TOO_SMALL", 
                "Minimum dimensions: 100x100px");
        }
        if (width > 4000 || height > 4000) {
            notification.addError("dimensions", "DIMENSIONS_TOO_LARGE", 
                "Maximum dimensions: 4000x4000px");
        }
    }
}
```

## Definition of Done
- [ ] POST /api/blog/images endpoint implementado
- [ ] UploadImageRequest DTO criado
- [ ] ImageService.uploadImage() implementado
- [ ] S3ImageService configurado com AWS SDK
- [ ] Validação de tipo de arquivo (Content-Type + magic bytes)
- [ ] Validação de tamanho (máximo 5MB)
- [ ] Validação de dimensões (100x100 a 4000x4000)
- [ ] Extração de metadados da imagem (width, height)
- [ ] Correção de orientação EXIF
- [ ] Geração de URL CloudFront
- [ ] Image entity criada
- [ ] ImageRepository implementado
- [ ] Unit tests para ImageService
- [ ] Integration tests com LocalStack (S3 mock)
- [ ] Tratamento de erros S3 (connection timeout, access denied)
- [ ] Logging de uploads para métricas
- [ ] HATEOAS links incluídos
- [ ] Mensagens i18n (en-US, pt-BR, es-ES)
- [ ] OpenAPI documentation atualizada
- [ ] S3 bucket e CloudFront configurados em staging
- [ ] Code review aprovado
- [ ] Deploy em staging

## Dependencies
- [ ] AWS SDK for Java configurado
- [ ] S3 bucket criado com políticas corretas
- [ ] CloudFront distribution configurado
- [ ] Database schema: tabela `images` criada
- [ ] Author entity deve existir

## Questions / Clarifications Needed
- [ ] Devemos gerar thumbnails automaticamente (ex: 300x300)?
- [ ] Precisamos de diferentes versões (small, medium, large)?
- [ ] Como lidar com imagens órfãs (uploaded mas nunca associadas a tópico)?
- [ ] Devemos ter quota de storage por autor?
- [ ] Compressão automática de imagens grandes?

## Estimated Effort
**2.5 dias**
