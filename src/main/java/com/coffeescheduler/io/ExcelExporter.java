package com.coffeescheduler.io;

import com.coffeescheduler.model.Clinician;
import com.coffeescheduler.model.Schedule;
import com.coffeescheduler.model.WeekState;
import com.coffeescheduler.ui.WeekHeader;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.coffeescheduler.ui.UIConstants;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

public final class ExcelExporter {

    private ExcelExporter() {}

    public static void export(Schedule schedule, Path path) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("Schedule");

            XSSFCellStyle headerStyle = headerStyle(wb);
            XSSFCellStyle onStyle = cellStyle(wb, UIConstants.EXCEL_COLOR_ON);
            XSSFCellStyle unavailStyle = cellStyle(wb, UIConstants.EXCEL_COLOR_UNAVAILABLE);
            XSSFCellStyle offStyle = cellStyle(wb, UIConstants.EXCEL_COLOR_OFF);

            XSSFRow header = sheet.createRow(0);
            header.createCell(0).setCellValue("Week");
            header.getCell(0).setCellStyle(headerStyle);
            for (int c = 0; c < schedule.roster().size(); c++) {
                header.createCell(c + 1).setCellValue(schedule.roster().get(c).name());
                header.getCell(c + 1).setCellStyle(headerStyle);
            }

            for (int w = 1; w <= schedule.lengthWeeks(); w++) {
                XSSFRow row = sheet.createRow(w);
                row.createCell(0).setCellValue(WeekHeader.format(w, schedule.startMonday(), schedule.lengthWeeks()));
                row.getCell(0).setCellStyle(headerStyle);
                for (int c = 0; c < schedule.roster().size(); c++) {
                    Clinician clin = schedule.roster().get(c);
                    WeekState state = schedule.stateOf(clin, w);
                    var cell = row.createCell(c + 1);
                    if (state == WeekState.ON) {
                        cell.setCellValue("ON");
                        cell.setCellStyle(onStyle);
                    } else if (state == WeekState.UNAVAILABLE) {
                        cell.setCellValue("UNAVAIL");
                        cell.setCellStyle(unavailStyle);
                    } else {
                        cell.setCellStyle(offStyle);
                    }
                }
            }

            sheet.setColumnWidth(0, UIConstants.EXCEL_WEEK_COLUMN_WIDTH);
            for (int c = 0; c < schedule.roster().size(); c++) {
                sheet.setColumnWidth(c + 1, UIConstants.EXCEL_CLINICIAN_COLUMN_WIDTH);
            }

            try (FileOutputStream out = new FileOutputStream(path.toFile())) {
                wb.write(out);
            }
        }
    }

    private static XSSFCellStyle headerStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(UIConstants.EXCEL_COLOR_HEADER, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        applyBorders(style);
        return style;
    }

    private static XSSFCellStyle cellStyle(XSSFWorkbook wb, byte[] rgb) {
        XSSFCellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(new XSSFColor(rgb, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        applyBorders(style);
        return style;
    }

    private static void applyBorders(XSSFCellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}
