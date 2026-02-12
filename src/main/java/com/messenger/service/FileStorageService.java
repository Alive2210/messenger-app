package com.messenger.service;

import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final MinioClient minioClient;
    private final AudioProcessingService audioProcessingService;

    @Value("${minio.bucket-name:messenger-files}")
    private String bucketName;

    public String uploadFile(MultipartFile file, String userId) {
        try {
            ensureBucketExists();

            String fileName = generateFileName(file.getOriginalFilename(), userId);
            String contentType = file.getContentType();

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(contentType)
                            .build());

            log.info("File uploaded successfully: {}", fileName);
            return fileName;
        } catch (Exception e) {
            log.error("Error uploading file", e);
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    @Value("${audio.processing.enabled:true}")
    private boolean audioProcessingEnabled;

    @Value("${audio.processing.noise-suppression:true}")
    private boolean noiseSuppressionEnabled;

    @Value("${audio.processing.echo-cancellation:false}")
    private boolean echoCancellationEnabled;

    @Value("${audio.processing.normalization:true}")
    private boolean normalizationEnabled;

    public String uploadVoiceMessage(byte[] audioData, String userId, String format) {
        try {
            ensureBucketExists();

            // Применяем обработку аудио
            byte[] processedAudio = audioData;
            if (audioProcessingEnabled && "wav".equalsIgnoreCase(format)) {
                processedAudio = audioProcessingService.processAudio(
                        audioData,
                        noiseSuppressionEnabled,
                        echoCancellationEnabled,
                        normalizationEnabled);
                log.debug("Audio processing applied: {} -> {} bytes",
                        audioData.length, processedAudio.length);
            }

            String fileName = String.format("voice/%s/%s.%s", userId, UUID.randomUUID(), format);
            String contentType = format.equals("mp3") ? "audio/mpeg" : "audio/ogg";

            try (InputStream stream = new ByteArrayInputStream(processedAudio)) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(fileName)
                                .stream(stream, processedAudio.length, -1)
                                .contentType(contentType)
                                .build());
            }

            log.info("Voice message uploaded: {}", fileName);
            return fileName;
        } catch (Exception e) {
            log.error("Error uploading voice message", e);
            throw new RuntimeException("Failed to upload voice message", e);
        }
    }

    public String getFileUrl(String fileName) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(fileName)
                            .expiry(7, TimeUnit.DAYS)
                            .build());
        } catch (Exception e) {
            log.error("Error generating file URL", e);
            throw new RuntimeException("Failed to generate file URL", e);
        }
    }

    public byte[] downloadFile(String fileName) {
        try {
            GetObjectResponse response = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build());
            return response.readAllBytes();
        } catch (Exception e) {
            log.error("Error downloading file", e);
            throw new RuntimeException("Failed to download file", e);
        }
    }

    public void deleteFile(String fileName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build());
            log.info("File deleted: {}", fileName);
        } catch (Exception e) {
            log.error("Error deleting file", e);
            throw new RuntimeException("Failed to delete file", e);
        }
    }

    private void ensureBucketExists() throws ServerException, InsufficientDataException,
            ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException,
            InvalidResponseException, XmlParserException, InternalException {
        boolean bucketExists = minioClient.bucketExists(
                BucketExistsArgs.builder()
                        .bucket(bucketName)
                        .build());

        if (!bucketExists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket(bucketName)
                            .build());
            log.info("Created bucket: {}", bucketName);
        }
    }

    private String generateFileName(String originalFilename, String userId) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return String.format("files/%s/%s%s", userId, UUID.randomUUID(), extension);
    }
}
