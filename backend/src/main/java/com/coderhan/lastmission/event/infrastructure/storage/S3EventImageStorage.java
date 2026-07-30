package com.coderhan.lastmission.event.infrastructure.storage;

import java.net.URI;
import java.util.UUID;

import com.coderhan.lastmission.event.application.EventImageStorage;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Component
class S3EventImageStorage implements EventImageStorage {

    private final S3Client s3Client;
    private final String bucket;

    S3EventImageStorage(S3Client s3Client, @Value("${aws.s3.bucket}") String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    @Override
    public String reserveUrl(long eventId, String originalFilename) {
        // S3 저장 경로 (실제 업로드는 하지 않고 키/URL만 발급)
        String key = "events/%d/images/%s%s".formatted(eventId, UUID.randomUUID(), extensionOf(originalFilename));
        return s3Client.utilities().getUrl(builder -> builder
                .bucket(bucket)
                .key(key))
                .toString();
    }

    @Override
    public void uploadTo(String imageUrl, String contentType, byte[] content) {
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(keyFromUrl(imageUrl))
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromBytes(content));
        } catch (SdkException e) {
            throw new BusinessException(ErrorCode.EVENT_IMAGE_UPLOAD_FAILED, "이미지 업로드에 실패했습니다: " + e.getMessage());
        }
    }

    @Override
    public void cleanup(String imageUrl) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(keyFromUrl(imageUrl))
                    .build());
        } catch (SdkException e) {
            log.warn("S3 고아 파일 cleanup 실패. url={}", imageUrl, e);
        }
    }

    private static String keyFromUrl(String imageUrl) {
        String path = URI.create(imageUrl).getPath();
        return path.startsWith("/") ? path.substring(1) : path;
    }

    // 파일 확장자 추출
    private static String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex >= 0 ? filename.substring(dotIndex) : "";
    }
}