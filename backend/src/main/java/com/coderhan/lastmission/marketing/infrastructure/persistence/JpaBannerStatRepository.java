package com.coderhan.lastmission.marketing.infrastructure.persistence;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import com.coderhan.lastmission.marketing.application.BannerStatRepository;
import com.coderhan.lastmission.marketing.domain.BannerAdStats;
import com.coderhan.lastmission.marketing.domain.BannerDailyStat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class JpaBannerStatRepository implements BannerStatRepository {
    private final BannerImpressionJpaRepository impressionJpa;
    private final BannerClickJpaRepository clickJpa;

    @Override
    public void incrementImpression(UUID adId, LocalDate date) {
        impressionJpa.upsertIncrement(adId, date);
    }

    @Override
    public void incrementClick(UUID adId, LocalDate date) {
        clickJpa.upsertIncrement(adId, date);
    }

    @Override
    public BannerAdStats sumStats(UUID adId) {
        long impressions = impressionJpa.sumCountByAdId(adId);
        long clicks = clickJpa.sumCountByAdId(adId);
        return BannerAdStats.of(adId, impressions, clicks);
    }

    @Override
    public BannerAdStats sumStatsByDateRange(UUID adId, LocalDate from, LocalDate to) {
        long impressions = impressionJpa.sumCountByAdIdAndDateRange(adId, from, to);
        long clicks = clickJpa.sumCountByAdIdAndDateRange(adId, from, to);
        return BannerAdStats.of(adId, impressions, clicks);
    }

    @Override
    public List<BannerDailyStat> findDailyStats(UUID adId, LocalDate from, LocalDate to) {
        Map<LocalDate, Long> impressionMap = toDateMap(impressionJpa.findDailyCountByAdIdAndDateRange(adId, from, to));
        Map<LocalDate, Long> clickMap = toDateMap(clickJpa.findDailyCountByAdIdAndDateRange(adId, from, to));

        return Stream.iterate(from, d -> !d.isAfter(to), d -> d.plusDays(1))
                .map(date -> BannerDailyStat.of(date,
                        impressionMap.getOrDefault(date, 0L),
                        clickMap.getOrDefault(date, 0L)))
                .toList();
    }

    private Map<LocalDate, Long> toDateMap(List<Object[]> rows) {
        Map<LocalDate, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((LocalDate) row[0], ((Number) row[1]).longValue());
        }
        return map;
    }
}
