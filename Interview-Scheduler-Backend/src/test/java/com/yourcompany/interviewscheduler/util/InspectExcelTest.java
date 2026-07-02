package com.yourcompany.interviewscheduler.util;

import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.IOException;

class InspectExcelTest {

    @Test
    void inspectExcel() throws IOException {
        try (FileInputStream fis = new FileInputStream("C:\\Users\\ashis\\Downloads\\Candidates_Test_Data.xlsx");
             Workbook workbook = WorkbookFactory.create(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            System.out.println("INSPECT_START");
            System.out.println("Sheet name: " + sheet.getSheetName());
            System.out.println("Last row num: " + sheet.getLastRowNum());
            System.out.println("Physical number of rows: " + sheet.getPhysicalNumberOfRows());

            Row header = sheet.getRow(0);
            if (header != null) {
                System.out.print("Header cells: ");
                for (int c = 0; c < header.getLastCellNum(); c++) {
                    System.out.print("[" + c + "]: " + header.getCell(c) + " | ");
                }
                System.out.println();
            }

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    System.out.println("Row " + r + " is null");
                    continue;
                }
                System.out.print("Row " + r + ": ");
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    Cell cell = row.getCell(c);
                    System.out.print("[" + c + "]: " + (cell != null ? cell.toString() : "null") + " , ");
                }
                System.out.println();
            }
            System.out.println("INSPECT_END");
        }
    }
}
