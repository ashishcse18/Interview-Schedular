package com.yourcompany.interviewscheduler.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.FileOutputStream;
import java.io.IOException;

class GenerateSampleExcelTest {

    @Test
    void generateSampleExcel() throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Candidates");

        Row header = sheet.createRow(0);
        String[] headers = {
                "Candidate Name", "Email ID", "WhatsApp Number",
                "Role", "Company Name", "Panel Date & Time",
                "GMeet Link", "Interviewer Name"
        };
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
        }

        Object[][] data = {
                {"Ashish", "ashish@example.com", "+918169057877", "Developer", "TechCorp", "2026-06-20 10:00 AM", "https://meet.google.com/abc-xyz", "HR Team"},
                {"manish", "manish@example.com", "+919546594650", "ias", "Govt India", "2026-06-20 11:30 AM", "https://meet.google.com/def-uvw", "Board Members"}
        };

        for (int r = 0; r < data.length; r++) {
            Row row = sheet.createRow(r + 1);
            for (int c = 0; c < data[r].length; c++) {
                Cell cell = row.createCell(c);
                cell.setCellValue((String) data[r][c]);
            }
        }

        String targetPath = "C:\\Users\\ashis\\.gemini\\antigravity\\scratch\\test_candidates.xlsx";
        try (FileOutputStream fileOut = new FileOutputStream(targetPath)) {
            workbook.write(fileOut);
        }
        workbook.close();
        System.out.println("Custom testing Excel generated successfully at: " + targetPath);
    }
}
