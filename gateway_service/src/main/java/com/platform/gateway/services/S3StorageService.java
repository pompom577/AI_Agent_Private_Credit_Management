package com.platform.gateway.services;

import com.platform.gateway.config.S3Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.IOException;
import java.util.Objects;

/**
 * AWS S3 implementation of {@link StorageService}. Streams the validated archive
 * into the secure bucket via {@code PutObject}. Covers TC-GW-05.
 */
@Service
public class S3StorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);

    private final S3Config s3Config;
    private final S3Client s3Client;

    public S3StorageService(S3Config s3Config, S3Client s3Client) {
        this.s3Config = s3Config;
        this.s3Client = s3Client;
    }

    @Override
    public String put(String dealId, MultipartFile file) {
        String fileName = Objects.requireNonNullElse(file.getOriginalFilename(), "upload.zip");
        String key = dealId + "/" + fileName;

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(s3Config.getBucket())
                .key(key)
                .contentType(Objects.requireNonNullElse(file.getContentType(), "application/zip"))
                .contentLength(file.getSize())
                .build();

        try {
            PutObjectResponse response = s3Client.putObject(
                    request,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            log.info("Stored deal {} to s3://{}/{} (etag={})",
                    dealId, s3Config.getBucket(), key, response.eTag());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read upload stream for deal " + dealId, e);
        }

        return "s3://" + s3Config.getBucket() + "/" + key;
    }
}
