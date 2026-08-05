package com.coderhan.lastmission.monitoring.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.coderhan.lastmission.monitoring.domain.TraceCategory;
import com.coderhan.lastmission.monitoring.domain.TraceDetail;
import com.coderhan.lastmission.monitoring.domain.TraceSearchResult;
import com.coderhan.lastmission.monitoring.domain.TraceStatus;
import com.coderhan.lastmission.monitoring.domain.TraceSummary;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class TraceQueryServiceTest {

    @Test
    void delegatesSearchAndDetailToMonitoringPort() {
        AtomicReference<TraceCategory> category = new AtomicReference<>();
        TraceSearchResult expectedSearch = new TraceSearchResult(
                60,
                1_785_889_163L,
                List.of(new TraceSummary(
                        "0123456789abcdef0123456789abcdef",
                        "last-mission-backend",
                        "http get /api/me",
                        1_785_889_140,
                        42,
                        5,
                        false)));
        TraceDetail expectedDetail = new TraceDetail(
                "0123456789abcdef0123456789abcdef",
                "last-mission-backend",
                "http get /api/me",
                1_785_889_140,
                42,
                false,
                List.of());
        TraceProvider provider = new TraceProvider() {
            @Override
            public TraceSearchResult search(
                    TraceCategory traceCategory,
                    int rangeMinutes,
                    TraceStatus status,
                    long minDurationMs,
                    String operation,
                    int limit) {
                category.set(traceCategory);
                return expectedSearch;
            }

            @Override
            public TraceDetail findById(String traceId) {
                return expectedDetail;
            }
        };
        TraceQueryService service = new TraceQueryService(provider);

        var search = service.search(
                TraceCategory.REQUEST,
                60,
                TraceStatus.ALL,
                0,
                "",
                50);
        var detail = service.findById(expectedDetail.traceId());

        assertThat(search).isEqualTo(expectedSearch);
        assertThat(detail).isEqualTo(expectedDetail);
        assertThat(category.get()).isEqualTo(TraceCategory.REQUEST);
    }
}
