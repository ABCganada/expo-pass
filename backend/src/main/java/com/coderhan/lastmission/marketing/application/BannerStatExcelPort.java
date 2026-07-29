package com.coderhan.lastmission.marketing.application;

import java.util.List;
import com.coderhan.lastmission.marketing.domain.BannerDailyStat;

public interface BannerStatExcelPort {
    byte[] write(List<BannerDailyStat> stats);
}
