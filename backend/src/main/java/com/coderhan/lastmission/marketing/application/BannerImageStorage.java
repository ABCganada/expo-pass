package com.coderhan.lastmission.marketing.application;

/**
 * 배너 광고 이미지를 오브젝트 스토리지에 업로드하는 포트.
 */
public interface BannerImageStorage {
    /**
     * 파일을 S3에 업로드하고 접근 가능한 URL을 반환한다.
     */
    String upload(String originalFilename, String contentType, byte[] content);
}
