package com.coderhan.lastmission.event.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.coderhan.lastmission.event.domain.Event;
import com.coderhan.lastmission.event.domain.EventCategory;
import com.coderhan.lastmission.event.domain.EventImage;
import com.coderhan.lastmission.event.domain.EventImageType;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class EventImageServiceTest {

    private static final long EVENT_ID = 1L;
    private static final long MANAGER_ID = 100L;
    private static final long OTHER_MANAGER_ID = 200L;

    @Mock EventRepository eventRepository;
    @Mock EventImageRepository eventImageRepository;
    @Mock EventImageStorage eventImageStorage;
    @Spy EventOwnershipValidator ownershipValidator = new EventOwnershipValidator();

    @InjectMocks EventImageService service;

    // registerCleanupOnRollback()이 내부적으로 TransactionSynchronizationManager를 쓰기 때문에,
    // 실제 @Transactional 프록시 없이 서비스 메서드를 직접 호출하려면 동기화를 수동으로 활성화해야 함
    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    private static final byte[] PNG_BYTES =
            {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 0};

    @Test
    void uploadImageAsManager_성공하면_reserveUrl_save_uploadTo_순서로_수행() {
        Event event = event(MANAGER_ID);
        when(eventRepository.findNotDeletedById(EVENT_ID)).thenReturn(Optional.of(event));
        when(eventImageRepository.countByEventId(EVENT_ID)).thenReturn(0L);
        when(eventImageStorage.reserveUrl(EVENT_ID, EventImageContentType.PNG)).thenReturn("https://bucket/a-key.png");
        when(eventImageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", PNG_BYTES);

        EventImage saved = service.uploadImageAsManager(EVENT_ID, MANAGER_ID, EventImageType.GENERAL, file);

        assertThat(saved.getImageUrl()).isEqualTo("https://bucket/a-key.png");
        assertThat(saved.getDisplayOrder()).isZero();

        // reserveUrl -> save -> uploadTo 순서로 호출되었는지 검증
        InOrder order = inOrder(eventImageStorage, eventImageRepository);
        order.verify(eventImageStorage).reserveUrl(EVENT_ID, EventImageContentType.PNG);
        order.verify(eventImageRepository).save(any());
        order.verify(eventImageStorage).uploadTo("https://bucket/a-key.png", EventImageContentType.PNG, PNG_BYTES);

        // 커밋되었으므로 cleanup은 절대 호출되면 X
        assertNoCleanupOnCommit("https://bucket/a-key.png");
    }

    @Test
    void uploadImageAsManager_업로드_도중_실패하면_롤백시_S3_파일이_정리() {
        Event event = event(MANAGER_ID);
        when(eventRepository.findNotDeletedById(EVENT_ID)).thenReturn(Optional.of(event));
        when(eventImageRepository.countByEventId(EVENT_ID)).thenReturn(0L);
        when(eventImageStorage.reserveUrl(EVENT_ID, EventImageContentType.PNG)).thenReturn("https://bucket/a-key.png");
        when(eventImageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new BusinessException(ErrorCode.EVENT_IMAGE_UPLOAD_FAILED, "S3 장애"))
                .when(eventImageStorage).uploadTo(anyString(), any(EventImageContentType.class), any());

        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", PNG_BYTES);

        assertThatThrownBy(() ->
                service.uploadImageAsManager(EVENT_ID, MANAGER_ID, EventImageType.GENERAL, file))
                .isInstanceOf(BusinessException.class);

        simulateRollback();

        verify(eventImageStorage).cleanup("https://bucket/a-key.png");
    }

    @Test
    void uploadImageAsManager_DB_save_실패하면_업로드_시도하지_않음() {
        Event event = event(MANAGER_ID);
        when(eventRepository.findNotDeletedById(EVENT_ID)).thenReturn(Optional.of(event));
        when(eventImageRepository.countByEventId(EVENT_ID)).thenReturn(0L);
        when(eventImageStorage.reserveUrl(EVENT_ID, EventImageContentType.PNG)).thenReturn("https://bucket/a-key.png");
        when(eventImageRepository.save(any())).thenThrow(new RuntimeException("DB 장애"));

        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", PNG_BYTES);

        assertThatThrownBy(() ->
                service.uploadImageAsManager(EVENT_ID, MANAGER_ID, EventImageType.GENERAL, file))
                .isInstanceOf(RuntimeException.class);

        // save 자체가 실패했으므로 아직 파일을 올리지 않은 상태 -> uploadTo가 호출되면 X
        verify(eventImageStorage, never()).uploadTo(anyString(), any(EventImageContentType.class), any());

        simulateRollback();

        // registerCleanupOnRollback도 save 이후에 등록되므로, save 실패 시엔 cleanup도 호출되지 않음
        verify(eventImageStorage, never()).cleanup(anyString());
    }

    @Test
    void uploadImageAsManager_담당하지_않는_행사면_거부() {
        Event event = event(OTHER_MANAGER_ID);
        when(eventRepository.findNotDeletedById(EVENT_ID)).thenReturn(Optional.of(event));

        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", PNG_BYTES);

        assertThatThrownBy(() ->
                service.uploadImageAsManager(EVENT_ID, MANAGER_ID, EventImageType.GENERAL, file))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.EVENT_ACCESS_DENIED));
        verify(eventImageStorage, never()).reserveUrl(anyLong(), any(EventImageContentType.class));
    }

    @Test
    void uploadImageAsManager_허용되지_않는_파일형식이면_거부() {
        Event event = event(MANAGER_ID);
        when(eventRepository.findNotDeletedById(EVENT_ID)).thenReturn(Optional.of(event));

        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", "data".getBytes());

        assertThatThrownBy(() ->
                service.uploadImageAsManager(EVENT_ID, MANAGER_ID, EventImageType.GENERAL, file))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.EVENT_IMAGE_INVALID_REQUEST));
    }

    @Test
    void uploadImageAsManager_확장자는_이미지지만_내용이_다른_형식이면_거부() {
        Event event = event(MANAGER_ID);
        when(eventRepository.findNotDeletedById(EVENT_ID)).thenReturn(Optional.of(event));

        // 파일명/Content-Type 헤더는 image/png로 위장했지만 실제 바이트는 PNG 시그니처가 아님
        MockMultipartFile file = new MockMultipartFile("file", "fake.png", "image/png", "not-a-real-image".getBytes());

        assertThatThrownBy(() ->
                service.uploadImageAsManager(EVENT_ID, MANAGER_ID, EventImageType.GENERAL, file))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.EVENT_IMAGE_INVALID_REQUEST));
        verify(eventImageStorage, never()).reserveUrl(anyLong(), any(EventImageContentType.class));
    }

    @Test
    void uploadAllAsManager_전체_성공하면_모두_저장되고_displayOrder가_이어짐() {
        Event event = event(MANAGER_ID);
        when(eventRepository.findNotDeletedById(EVENT_ID)).thenReturn(Optional.of(event));
        when(eventImageRepository.countByEventId(EVENT_ID)).thenReturn(3L); // 기존 3장 존재
        when(eventImageStorage.reserveUrl(eq(EVENT_ID), any(EventImageContentType.class)))
                .thenReturn("https://bucket/1.png", "https://bucket/2.png");
        when(eventImageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<MultipartFile> files = List.of(
                new MockMultipartFile("files", "1.png", "image/png", PNG_BYTES),
                new MockMultipartFile("files", "2.png", "image/png", PNG_BYTES));
        List<EventImageType> types = List.of(EventImageType.THUMBNAIL, EventImageType.GENERAL);

        List<EventImage> saved = service.uploadAllAsManager(EVENT_ID, MANAGER_ID, types, files);

        assertThat(saved).extracting(EventImage::getDisplayOrder).containsExactly(3, 4);
        verify(eventImageStorage).uploadTo("https://bucket/1.png", EventImageContentType.PNG, PNG_BYTES);
        verify(eventImageStorage).uploadTo("https://bucket/2.png", EventImageContentType.PNG, PNG_BYTES);
    }

    @Test
    void uploadAllAsManager_두번째_파일_업로드가_실패하면_첫번째_파일도_함께_정리() {
        Event event = event(MANAGER_ID);
        when(eventRepository.findNotDeletedById(EVENT_ID)).thenReturn(Optional.of(event));
        when(eventImageRepository.countByEventId(EVENT_ID)).thenReturn(0L);
        when(eventImageStorage.reserveUrl(eq(EVENT_ID), any(EventImageContentType.class)))
                .thenReturn("https://bucket/1.png", "https://bucket/2.png");
        when(eventImageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(eventImageStorage).uploadTo(eq("https://bucket/1.png"), any(EventImageContentType.class), any());
        doThrow(new BusinessException(ErrorCode.EVENT_IMAGE_UPLOAD_FAILED, "S3 장애"))
                .when(eventImageStorage).uploadTo(eq("https://bucket/2.png"), any(EventImageContentType.class), any());

        List<MultipartFile> files = List.of(
                new MockMultipartFile("files", "1.png", "image/png", PNG_BYTES),
                new MockMultipartFile("files", "2.png", "image/png", PNG_BYTES));
        List<EventImageType> types = List.of(EventImageType.GENERAL, EventImageType.GENERAL);

        assertThatThrownBy(() -> service.uploadAllAsManager(EVENT_ID, MANAGER_ID, types, files))
                .isInstanceOf(BusinessException.class);

        simulateRollback();

        // 실패한 2번 파일뿐 아니라, 이미 업로드에 성공했던 1번 파일도 정리되어야 한다.
        verify(eventImageStorage).cleanup("https://bucket/1.png");
        verify(eventImageStorage).cleanup("https://bucket/2.png");
    }

    @Test
    void uploadAllAsManager_파일과_imageType_개수가_다르면_아무것도_시도하지_않고_거부() {
        Event event = event(MANAGER_ID);
        when(eventRepository.findNotDeletedById(EVENT_ID)).thenReturn(Optional.of(event));

        List<MultipartFile> files = List.of(
                new MockMultipartFile("files", "1.png", "image/png", "d1".getBytes()));
        List<EventImageType> types = List.of(EventImageType.GENERAL, EventImageType.GENERAL);

        assertThatThrownBy(() -> service.uploadAllAsManager(EVENT_ID, MANAGER_ID, types, files))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.EVENT_IMAGE_INVALID_REQUEST));
        verify(eventImageStorage, never()).reserveUrl(anyLong(), any(EventImageContentType.class));
    }

    @Test
    void deleteImageAsManager_삭제후_커밋되면_S3_파일도_정리() {
        Event event = event(MANAGER_ID);
        EventImage image = new EventImage(event, "https://bucket/a-key.png", EventImageType.GENERAL, 0);
        when(eventRepository.findNotDeletedById(EVENT_ID)).thenReturn(Optional.of(event));
        when(eventImageRepository.findByIdAndEventId(10L, EVENT_ID)).thenReturn(Optional.of(image));

        service.deleteImageAsManager(EVENT_ID, 10L, MANAGER_ID);

        verify(eventImageRepository).delete(image);
        verify(eventImageStorage, never()).cleanup(anyString());

        simulateCommit();

        verify(eventImageStorage).cleanup("https://bucket/a-key.png");
    }

    private void simulateRollback() {
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
    }

    private void simulateCommit() {
        // 실제 커밋 시 Spring은 afterCommit() -> afterCompletion(STATUS_COMMITTED) 순으로 호출
        TransactionSynchronizationManager.getSynchronizations().forEach(sync -> {
            sync.afterCommit();
            sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
        });
    }

    private void assertNoCleanupOnCommit(String imageUrl) {
        simulateCommit();
        verify(eventImageStorage, never()).cleanup(imageUrl);
    }

    private Event event(long managerId) {
        EventCategory category = new EventCategory("MUSIC", "음악", true);
        Event event = new Event("테스트 행사", category, managerId);
        ReflectionTestUtils.setField(event, "id", EVENT_ID);
        return event;
    }
}