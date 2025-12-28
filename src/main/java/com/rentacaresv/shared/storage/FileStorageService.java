package com.rentacaresv.shared.storage;

import com.rentacaresv.settings.domain.Settings;
import com.rentacaresv.settings.infrastructure.SettingsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Servicio para gestión de archivos en Digital Ocean Spaces
 */
@Service
@Slf4j
public class FileStorageService {

    private final S3Client s3Client;
    private final SettingsRepository settingsRepository;
    private final String bucketName;
    private final String baseUrl;
    private final String basePath = "rentacaresv";

    public FileStorageService(
            @Value("${do.spaces.key}") String accessKey,
            @Value("${do.spaces.secret}") String secretKey,
            @Value("${do.spaces.bucket}") String bucketName,
            @Value("${do.spaces.region}") String region,
            @Value("${do.spaces.endpoint}") String endpoint,
            SettingsRepository settingsRepository) {

        this.bucketName = bucketName;
        this.baseUrl = endpoint;
        this.settingsRepository = settingsRepository;

        // Configurar cliente S3 para Digital Ocean Spaces
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();

        log.info("FileStorageService initialized with endpoint: {}", endpoint);
    }

    /**
     * Inicializa las carpetas del tenant al iniciar la aplicación
     */
    public void initializeTenantFolders() {
        log.info("Inicializando carpetas del tenant...");

        Settings settings = settingsRepository.findGlobalSettings()
                .orElseGet(() -> createDefaultSettings());

        if (!settings.getFoldersInitialized()) {
            String tenantId = settings.getTenantId();
            log.info("Creando estructura de carpetas para tenant: {}", tenantId);

            // Crear carpetas principales
            for (FolderType folderType : FolderType.values()) {
                createFolder(tenantId, folderType.getFolderName());
            }

            // Crear subcarpetas en car_details
            createFolder(tenantId, "car_details/delivery");
            createFolder(tenantId, "car_details/return");

            settings.markFoldersAsInitialized();
            settingsRepository.save(settings);

            log.info("Carpetas inicializadas exitosamente para tenant: {}", tenantId);
        } else {
            log.info("Carpetas ya estaban inicializadas para tenant: {}", settings.getTenantId());
        }
    }

    /**
     * Crea configuración por defecto si no existe
     */
    private Settings createDefaultSettings() {
        Settings settings = Settings.builder()
                .tenantId(UUID.randomUUID().toString())
                .companyName("RentaCar ESV")
                .foldersInitialized(false)
                .build();

        settings = settingsRepository.save(settings);
        log.info("Settings creados con tenant ID: {}", settings.getTenantId());
        return settings;
    }

    /**
     * Crea una carpeta en Digital Ocean Spaces
     */
    private void createFolder(String tenantId, String folderName) {
        try {
            String key = basePath + "/" + tenantId + "/" + folderName + "/";

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .acl(ObjectCannedACL.PUBLIC_READ)
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(new byte[0]));
            log.debug("Carpeta creada: {}", key);
        } catch (Exception e) {
            log.error("Error creando carpeta {}: {}", folderName, e.getMessage());
        }
    }

    /**
     * Sube un archivo a Digital Ocean Spaces
     *
     * @param inputStream Stream del archivo
     * @param fileName Nombre del archivo
     * @param contentType Tipo de contenido (ej: image/jpeg)
     * @param folderType Tipo de carpeta
     * @param subFolder Subcarpeta opcional (ej: "vehicle-123" o "rental-456-delivery")
     * @return URL pública del archivo
     */
    public String uploadFile(InputStream inputStream, String fileName, String contentType,
                              FolderType folderType, String subFolder) {

        Settings settings = settingsRepository.findGlobalSettings()
                .orElseThrow(() -> new IllegalStateException("Settings no configurados"));

        String tenantId = settings.getTenantId();

        // Generar nombre único para el archivo
        String uniqueFileName = generateUniqueFileName(fileName);

        // Construir key del objeto
        String key = buildKey(tenantId, folderType, subFolder, uniqueFileName);

        try {
            // Subir archivo
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .acl(ObjectCannedACL.PUBLIC_READ)
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, inputStream.available()));

            // Construir URL pública
            String url = baseUrl + "/" + key;
            log.info("Archivo subido exitosamente: {}", url);
            return url;

        } catch (Exception e) {
            log.error("Error subiendo archivo: {}", e.getMessage(), e);
            throw new RuntimeException("Error al subir archivo: " + e.getMessage(), e);
        }
    }

    /**
     * Elimina un archivo de Digital Ocean Spaces
     */
    public void deleteFile(String fileUrl) {
        try {
            // Extraer key de la URL
            String key = fileUrl.replace(baseUrl + "/", "");

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
        Settings settings = settingsRepository.findGlobalSettings()
                .orElseThrow(() -> new IllegalStateException("Settings no configurados"));

        String prefix = buildPrefix(settings.getTenantId(), folderType, subFolder);
        List<String> fileUrls = new ArrayList<>();

        try {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(request);

            for (S3Object s3Object : response.contents()) {
                String url = baseUrl + "/" + s3Object.key();
                fileUrls.add(url);
            }

        } catch (Exception e) {
            log.error("Error listando archivos: {}", e.getMessage());
        }

        return fileUrls;
    }

    /**
     * Obtiene el tenant ID actual
     */
    public String getCurrentTenantId() {
        return settingsRepository.findGlobalSettings()
                .map(Settings::getTenantId)
                .orElse(null);
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

    private String generateUniqueFileName(String originalFileName) {
        String extension = "";
        int lastDot = originalFileName.lastIndexOf('.');
        if (lastDot > 0) {
            extension = originalFileName.substring(lastDot);
        }

        return UUID.randomUUID().toString() + extension;
    }
}
