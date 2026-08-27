package com.coderhan.lastmission.event.application;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * 행사 이미지로 허용되는 파일 형식과, 형식별 MIME 타입/확장자/매직 바이트 시그니처를 한곳에서 관리한다.
 * 업로드 검증(content-type 판별)과 스토리지 확장자 결정이 서로 다른 값을 참조하지 않도록 단일 소스로 둔다.
 */
public enum EventImageContentType {

    PNG("image/png", ".png", EventImageContentType::isPng),
    JPEG("image/jpeg", ".jpg", EventImageContentType::isJpeg),
    GIF("image/gif", ".gif", EventImageContentType::isGif),
    WEBP("image/webp", ".webp", EventImageContentType::isWebp);

    public static final int HEADER_PROBE_BYTES = 12;

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] GIF_SIGNATURE = {'G', 'I', 'F', '8'};
    private static final byte[] WEBP_RIFF_SIGNATURE = {'R', 'I', 'F', 'F'};
    private static final byte[] WEBP_FORMAT_SIGNATURE = {'W', 'E', 'B', 'P'};

    private final String mimeType;
    private final String extension;
    private final Predicate<byte[]> signatureMatcher; // Predicate: byte[] 받아서 boolean 반환

    EventImageContentType(String mimeType, String extension, Predicate<byte[]> signatureMatcher) {
        this.mimeType = mimeType;
        this.extension = extension;
        this.signatureMatcher = signatureMatcher;
    }

    public String mimeType() {
        return mimeType;
    }

    public String extension() {
        return extension;
    }

    // 파일 헤더를 매직 바이트 시그니처와 대조해 실제 형식을 판별
    public static Optional<EventImageContentType> detect(byte[] header) {
        return Arrays.stream(values())
                .filter(type -> type.signatureMatcher.test(header))
                .findFirst();
    }

    private static boolean isPng(byte[] header) {
        return startsWith(header, PNG_SIGNATURE);
    }

    private static boolean isJpeg(byte[] header) {
        return startsWith(header, JPEG_SIGNATURE);
    }

    private static boolean isGif(byte[] header) {
        return startsWith(header, GIF_SIGNATURE);
    }

    private static boolean isWebp(byte[] header) {
        return header.length >= HEADER_PROBE_BYTES
                && startsWith(header, WEBP_RIFF_SIGNATURE)
                && Arrays.equals(Arrays.copyOfRange(header, 8, 12), WEBP_FORMAT_SIGNATURE);
    }

    private static boolean startsWith(byte[] data, byte[] signature) {
        if (data.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (data[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }
}