package com.coderhan.lastmission.marketing.infrastructure.excel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import com.coderhan.lastmission.marketing.application.BannerStatExcelPort;
import com.coderhan.lastmission.marketing.domain.BannerDailyStat;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

@Component
public class BannerStatExcelWriter implements BannerStatExcelPort {

    private static final String[] HEADERS = {"날짜", "노출수", "클릭수", "CTR (%)"};

    public byte[] write(List<BannerDailyStat> stats) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("배너 통계");

            CellStyle headerStyle = buildHeaderStyle(workbook);
            writeHeader(sheet, headerStyle);
            writeRows(sheet, stats);
            writeSummaryRow(sheet, stats, headerStyle);
            autoSizeColumns(sheet);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("XLSX 생성 중 오류가 발생했습니다.", e);
        }
    }

    private void writeHeader(Sheet sheet, CellStyle style) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(style);
        }
    }

    private void writeRows(Sheet sheet, List<BannerDailyStat> stats) {
        int rowIndex = 1;
        for (BannerDailyStat stat : stats) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(stat.date().toString());
            row.createCell(1).setCellValue(stat.impressions());
            row.createCell(2).setCellValue(stat.clicks());
            row.createCell(3).setCellValue(String.format("%.2f", stat.ctr()));
        }
    }

    private void writeSummaryRow(Sheet sheet, List<BannerDailyStat> stats, CellStyle style) {
        int rowIndex = stats.size() + 1;
        Row row = sheet.createRow(rowIndex);

        long totalImpressions = stats.stream().mapToLong(BannerDailyStat::impressions).sum();
        long totalClicks = stats.stream().mapToLong(BannerDailyStat::clicks).sum();
        double totalCtr = totalImpressions == 0 ? 0.0 : (double) totalClicks / totalImpressions * 100;

        Cell labelCell = row.createCell(0);
        labelCell.setCellValue("합계");
        labelCell.setCellStyle(style);

        Cell impressionCell = row.createCell(1);
        impressionCell.setCellValue(totalImpressions);
        impressionCell.setCellStyle(style);

        Cell clickCell = row.createCell(2);
        clickCell.setCellValue(totalClicks);
        clickCell.setCellStyle(style);

        Cell ctrCell = row.createCell(3);
        ctrCell.setCellValue(String.format("%.2f", totalCtr));
        ctrCell.setCellStyle(style);
    }

    private CellStyle buildHeaderStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        return style;
    }

    private void autoSizeColumns(Sheet sheet) {
        for (int i = 0; i < HEADERS.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
