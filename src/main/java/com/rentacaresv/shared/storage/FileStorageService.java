package com.rentacaresv.shared.storage;

import com.rentacaresv.settings.application.SettingsCache;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Servicio para gestión de archivos en Digital Ocean Spaces.
 * Utiliza SettingsCache para obtener el tenant_id de forma eficiente.
 */
@Service
@Slf4j
public class FileStorageService {

    private S3Client s3Client;
    private final SettingsCache settingsCache;
    private final String bucketName;
    private final String baseUrl;
    private final String basePath = "rentacaresv";
    private final String accessKey;
    private final String secretKey;
    private final String region;
    private final String endpoint;

    private boolean initialized = false;

    public FileStorageService(
            @Value("${do.spaces.key}") String accessKey,
            @Value("${do.spaces.secret}") String secretKey,
            @Value("${do.spaces.bucket}") String bucketName,
            @Value("${do.spaces.region}") String region,
            @Value("${do.spaces.endpoint}") String endpoint,
            @Lazy SettingsCache settingsCache) {

        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.bucketName = bucketName;
        this.region = region;
        this.endpoint = endpoint;
        this.baseUrl = endpoint;
        this.settingsCache = settingsCache;

        log.info("FileStorageService constructor called with endpoint: {}", endpoint);
    }

    @PostConstruct
    public void init() {
        try {
            log.info("Inicializando FileStorageService...");
            log.info("Endpoint: {}", endpoint);
            log.info("Bucket: {}", bucketName);
            log.info("Region: {}", region);

            // Configurar cliente S3 para Digital Ocean Spaces
            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

            S3Configuration s3Config = S3Configuration.builder()
                    .pathStyleAccessEnabled(true)
                    .build();

            this.s3Client = S3Client.builder()
                    .region(Region.of(region))
                    .endpointOverride(URI.create(endpoint))
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .serviceConfiguration(s3Config)
                    .build();

            log.info("✅ FileStorageService initialized successfully");
            initialized = true;

        } catch (Exception e) {
            log.error("❌ Error initializing FileStorageService: {}", e.getMessage(), e);
            log.error("La aplicación continuará pero las funciones de almacenamiento no estarán disponibles");
            initialized = false;
        }
    }

    /**
     * Sube un archivo a Digital Ocean Spaces
     */
    public String uploadFile(InputStream inputStream, String fileName, String contentType,
            FolderType folderType, String subFolder) {

        if (!initialized) {
            throw new IllegalStateException("FileStorageService no está inicializado");
        }

        String tenantId = settingsCache.getTenantId();

        // Validación crítica
        if (tenantId == null || tenantId.trim().isEmpty()) {
            log.error("❌ ERROR: tenant_id es NULL o vacío");
            throw new IllegalStateException("tenant_id no puede ser NULL o vacío");
        }

        String uniqueFileName = generateUniqueFileName(fileName);
        String key = buildKey(tenantId, folderType, subFolder, uniqueFileName);

        log.info("📤 Subiendo archivo: {}", fileName);
        log.info("   - Tenant ID: {}", tenantId);
        log.info("   - Folder Type: {}", folderType.getFolderName());
        log.info("   - Key: {}", key);

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .acl(ObjectCannedACL.PUBLIC_READ)
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, inputStream.available()));

            String url = baseUrl + "/" + bucketName + "/" + key;
            log.info("✅ Archivo subido exitosamente: {}", url);
            return url;

        } catch (Exception e) {
            log.error("❌ Error subiendo archivo: {}", e.getMessage(), e);
            throw new RuntimeException("Error al subir archivo: " + e.getMessage(), e);
        }
    }

    /**
     * Elimina un archivo de Digital Ocean Spaces
     */
    public void deleteFile(String fileUrl) {
        if (!initialized) {
            log.warn("Saltando eliminación de archivo: servicio no inicializado");
            return;
        }

        try {
            String key = extractKeyFromUrl(fileUrl);

            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(request);
            log.info("Archivo eliminado: {}", key);

        } catch (Exception e) {
            log.error("Error eliminando archivo {}: {}", fileUrl, e.getMessage());
            throw new RuntimeException("Error al eliminar archivo: " + e.getMessage(), e);
        }
    }

    /**
     * Lista archivos en una carpeta
     */
    public List<String> listFiles(FolderType folderType, String subFolder) {
        if (!initialized) {
            log.warn("Saltando listado de archivos: servicio no inicializado");
            return new ArrayList<>();
        }

        String tenantId = settingsCache.getTenantId();
        String prefix = buildPrefix(tenantId, folderType, subFolder);
        List<String> fileUrls = new ArrayList<>();

        try {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(request);

            for (S3Object s3Object : response.contents()) {
                String url = baseUrl + "/" + bucketName + "/" + s3Object.key();
                fileUrls.add(url);
            }

        } catch (Exception e) {
            log.error("Error listando archivos: {}", e.getMessage());
        }

        return fileUrls;
    }

    /**
     * Obtiene el tenant ID actual desde cache
     */
    public String getCurrentTenantId() {
        return settingsCache.getTenantId();
    }

    public boolean isInitialized() {
        return initialized;
    }

    // ========================================
    // Métodos auxiliares
    // ========================================

    private String buildKey(String tenantId, FolderType folderType, String subFolder, String fileName) {
        StringBuilder key = new StringBuilder();
        key.append(basePath).append("/").append(tenantId).append("/").append(folderType.getFolderName());

        if (subFolder != null && !subFolder.isEmpty()) {
            key.append("/").append(subFolder);
        }

        key.append("/").append(fileName);
        return key.toString();
    }

    private String buildPrefix(String tenantId, FolderType folderType, String subFolder) {
        StringBuilder prefix = new StringBuilder();
        prefix.append(basePath).append("/").append(tenantId).append("/").append(folderType.getFolderName());

        if (subFolder != null && !subFolder.isEmpty()) {
            prefix.append("/").append(subFolder);
        }

        prefix.append("/");
        return prefix.toString();
    }

    private String extractKeyFromUrl(String fileUrl) {
        String[] parts = fileUrl.split(bucketName + "/");
        if (parts.length > 1) {
            return parts[1];
        }
        return fileUrl.replace(baseUrl + "/", "").replace(bucketName + "/", "");
    }

    private String generateUniqueFileName(String originalFileName) {
        String extension = "";
        int lastDot = originalFileName.lastIndexOf('.');
        if (lastDot > 0) {
            extension = originalFileName.substring(lastDot);
        }

        return UUID.randomUUID().toString() + extension;
    }
}
