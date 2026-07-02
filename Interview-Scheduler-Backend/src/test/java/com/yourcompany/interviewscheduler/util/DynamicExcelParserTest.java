package com.yourcompany.interviewscheduler.util;

import com.yourcompany.interviewscheduler.model.entity.Batch;
import com.yourcompany.interviewscheduler.model.entity.Candidate;
import com.yourcompany.interviewscheduler.service.ExcelParserService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DynamicExcelParserTest {

    private final ExcelParserService parserService = new ExcelParserService();

    @Test
    void testParseUserExcel() throws Exception {
        File file = new File("C:\\Users\\ashis\\Downloads\\Candidates_Test_Data.xlsx");
        if (!file.exists()) {
            System.out.println("Local file Candidates_Test_Data.xlsx not found, skipping assertion");
            return;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            MultipartFile multipartFile = new MockMultipartFile(
                    "file",
                    file.getName(),
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    fis
            );

            Batch batch = Batch.builder().id(1L).fileName(file.getName()).build();
            List<Candidate> candidates = parserService.parseExcel(multipartFile, batch);

            assertNotNull(candidates);
            // 8 data rows total. Row 6 has invalid phone '12345', so it should be skipped.
            // 7 candidates should parse successfully.
            assertEquals(7, candidates.size(), "Should parse 7 valid candidates and skip 1 invalid row");

            // Check first row mappings
            Candidate first = candidates.get(0);
            assertEquals("Ashish Kumar", first.getName());
            assertEquals("+919876543210", first.getWhatsappNumber());
            assertEquals("Java Developer", first.getRole());
            assertEquals("https://meet.google.com/abc-defg-hij", first.getGmeetLink());

            System.out.println("DynamicExcelParserTest: Successfully verified 7 candidate records parsed from Candidates_Test_Data.xlsx");
        }
    }
}
