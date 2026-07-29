package com.coderhan.lastmission.event.infrastructure.storage;

import java.util.UUID;

import com.coderhan.lastmission.event.application.EventImageStorage;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
class S3EventImageStorage implements EventImageStorage {

    private final S3Client s3Client;
    private final String bucket;

    S3EventImageStorage(S3Client s3Client, @Value("${aws.s3.bucket}") String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    @Override
    public String upload(long eventId, String originalFilename, String contentType, byte[] content) {
        // S3 저장 경로
        String key = "events/%d/images/%s%s".formatted(eventId, UUID.randomUUID(), extensionOf(originalFilename));
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromBytes(content));
        } catch (SdkException e) {
            throw new BusinessException(ErrorCode.EVENT_IMAGE_UPLOAD_FAILED, "이미지 업로드에 실패했습니다: " + e.getMessage());
        }
        return s3Client.utilities().getUrl(builder -> builder.bucket(bucket).key(key)).toString();
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