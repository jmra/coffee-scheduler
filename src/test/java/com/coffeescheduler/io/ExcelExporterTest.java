package com.coffeescheduler.io;

import com.coffeescheduler.model.BlockLengthRange;
import com.coffeescheduler.model.Clinician;
import com.coffeescheduler.model.ContractedWeeks;
import com.coffeescheduler.model.Schedule;
import com.coffeescheduler.model.WeekState;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExcelExporterTest {

    private static final LocalDate START = LocalDate.of(2026, 1, 5);

    @Test
    void exportsGridWithCorrectDimensions(@TempDir Path tempDir) throws Exception {
        Clinician a = clinician("Dr. Adams");
        Clinician b = clinician("Dr. Baker");
        Schedule s = new Schedule(START, 4, List.of(a, b));
        s.setState(a, 1, WeekState.ON);
        s.setState(b, 2, WeekState.UNAVAILABLE);

        Path file = tempDir.resolve("test.xlsx");
        ExcelExporter.export(s, file);

        try (XSSFWorkbook wb = new XSSFWorkbook(new FileInputStream(file.toFile()))) {
            XSSFSheet sheet = wb.getSheetAt(0);
            assertEquals("Dr. Adams", sheet.getRow(0).getCell(1).getStringCellValue());
            assertEquals("Dr. Baker", sheet.getRow(0).getCell(2).getStringCellValue());
            assertEquals("ON", sheet.getRow(1).getCell(1).getStringCellValue());
            assertEquals("UNAVAIL", sheet.getRow(2).getCell(2).getStringCellValue());
            assertEquals(4, sheet.getLastRowNum()); // header + 4 weeks
        }
    }

    private static Clinician clinician(String name) {
        return new Clinician(name, new ContractedWeeks(20, 24), 6, 2, new BlockLengthRange(4, 5));
    }
}
