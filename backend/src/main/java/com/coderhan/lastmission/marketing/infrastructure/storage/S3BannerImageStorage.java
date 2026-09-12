package com.coderhan.lastmission.marketing.infrastructure.storage;

import java.util.UUID;

import com.coderhan.lastmission.marketing.application.BannerImageStorage;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Component
class S3BannerImageStorage implements BannerImageStorage {

    private final S3Client s3Client;
    private final String bucket;

    S3BannerImageStorage(S3Client s3Client, @Value("${aws.s3.bucket}") String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    @Override
    public String upload(String originalFilename, String contentType, byte[] content) {
        String key = "banner-ads/%s%s".formatted(UUID.randomUUID(), extensionOf(originalFilename));
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromBytes(content));
        } catch (SdkException e) {
            throw new BusinessException(ErrorCode.BANNER_AD_INVALID_REQUEST,
                    "이미지 업로드에 실패했습니다: " + e.getMessage());
        }
        return s3Client.utilities().getUrl(b -> b.bucket(bucket).key(key)).toString();
    }

    private static String extensionOf(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : "";
    }
}
