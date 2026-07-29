package com.coderhan.lastmission.event.application;

/**
 * 행사 이미지 파일을 오브젝트 스토리지에 업로드하는 포트.
 */
public interface EventImageStorage {

    String upload(long eventId, String originalFilename, String contentType, byte[] content);
}