package com.coderhan.lastmission.event.application;

/**
 * 행사 이미지 파일을 오브젝트 스토리지에 업로드하는 포트.
 */
public interface EventImageStorage {

    // 실제 업로드 없이 URL(키)만 발급
    String reserveUrl(long eventId, String originalFilename);

    // reserveUrl로 발급받은 URL에 실제 업로드
    void uploadTo(String imageUrl, String contentType, byte[] content);

    // 고아 파일 정리용
    void cleanup(String imageUrl);
}