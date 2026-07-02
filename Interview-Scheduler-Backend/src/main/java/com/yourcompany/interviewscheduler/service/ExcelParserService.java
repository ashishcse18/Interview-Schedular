package com.yourcompany.interviewscheduler.service;

import com.yourcompany.interviewscheduler.exception.ExcelParseException;
import com.yourcompany.interviewscheduler.model.entity.Batch;
import com.yourcompany.interviewscheduler.model.entity.Candidate;
import com.yourcompany.interviewscheduler.util.PhoneNumberValidator;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ExcelParserService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    public List<Candidate> parseExcel(MultipartFile file, Batch batch) {
        List<Candidate> candidates = new ArrayList<>();

        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new ExcelParseException("The uploaded Excel workbook contains no sheets.");
            }

            int rowCount = sheet.getPhysicalNumberOfRows();
            if (rowCount <= 1) {
                throw new ExcelParseException("The Excel sheet is empty or only contains the header row.");
            }

            // Read the header row and map column indices
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new ExcelParseException("Header row is missing.");
            }

            int nameCol = -1;
            int emailCol = -1;
            int phoneCol = -1;
            int roleCol = -1;
            int companyCol = -1;
            int timingCol = -1;
            int dateCol = -1;
            int timeCol = -1;
            int meetCol = -1;
            int interviewerCol = -1;

            for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                Cell cell = headerRow.getCell(c);
                if (cell == null) continue;
                String val = getCellValueAsString(cell).trim().toLowerCase();

                if (val.contains("whatsapp") || val.contains("phone") || val.contains("number") || val.contains("mobile") || val.contains("contact")) {
                    phoneCol = c;
                } else if (val.contains("candidate name") || val.contains("name")) {
                    nameCol = c;
                } else if (val.contains("role") || val.contains("position") || val.contains("job")) {
                    roleCol = c;
                } else if (val.contains("company")) {
                    companyCol = c;
                } else if (val.contains("gmeet") || val.contains("meet") || val.contains("link")) {
                    meetCol = c;
                } else if (val.contains("interviewer")) {
                    interviewerCol = c;
                } else if (val.contains("date & time") || val.contains("timing") || val.contains("panel timing") || val.contains("schedule")) {
                    timingCol = c;
                } else if (val.contains("date")) {
                    dateCol = c;
                } else if (val.contains("time")) {
                    timeCol = c;
                } else if (val.contains("email")) {
                    emailCol = c;
                }
            }

            // Verify headers matching essential details
            if (nameCol == -1) {
                throw new ExcelParseException("Could not identify 'Candidate Name' or 'Name' header column.");
            }
            if (phoneCol == -1) {
                throw new ExcelParseException("Could not identify 'Phone Number' or 'WhatsApp' header column.");
            }
            if (meetCol == -1) {
                throw new ExcelParseException("Could not identify 'GMeet' or 'Meeting Link' header column.");
            }

            // Iterate through rows, skipping header (row 0)
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                try {
                    Candidate candidate = parseDynamicRow(row, batch, r + 1,
                            nameCol, emailCol, phoneCol, roleCol, companyCol,
                            timingCol, dateCol, timeCol, meetCol, interviewerCol);
                    candidates.add(candidate);
                } catch (Exception e) {
                    log.warn("Skipping Row {} due to parsing warning: {}", r + 1, e.getMessage());
                }
            }

        } catch (ExcelParseException e) {
            throw e;
        } catch (Exception e) {
            throw new ExcelParseException("Failed to parse Excel file: " + e.getMessage(), e);
        }

        if (candidates.isEmpty()) {
            throw new ExcelParseException("No valid candidates found in the Excel file.");
        }

        return candidates;
    }

    private Candidate parseDynamicRow(Row row, Batch batch, int rowNum,
                                      int nameCol, int emailCol, int phoneCol,
                                      int roleCol, int companyCol, int timingCol,
                                      int dateCol, int timeCol, int meetCol,
                                      int interviewerCol) {

        String name = getCellValueAsString(row.getCell(nameCol));
        String rawPhone = getCellValueAsString(row.getCell(phoneCol));
        String gmeetLink = getCellValueAsString(row.getCell(meetCol));

        // Required Validations
        if (name.isEmpty()) throw new IllegalArgumentException("Candidate Name is empty");
        if (rawPhone.isEmpty()) throw new IllegalArgumentException("Phone number is empty");
        if (gmeetLink.isEmpty()) throw new IllegalArgumentException("GMeet Link is empty");

        // Optional fields
        String email = emailCol != -1 ? getCellValueAsString(row.getCell(emailCol)) : "";
        if (email.isEmpty()) {
            email = "no-email@example.com";
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }

        String role = roleCol != -1 ? getCellValueAsString(row.getCell(roleCol)) : "";
        if (role.isEmpty()) {
            role = "Candidate";
        }

        String companyName = companyCol != -1 ? getCellValueAsString(row.getCell(companyCol)) : "";
        if (companyName.isEmpty()) {
            companyName = "Our Company";
        }

        String interviewerName = interviewerCol != -1 ? getCellValueAsString(row.getCell(interviewerCol)) : "";
        if (interviewerName.isEmpty()) {
            interviewerName = "HR Team";
        }

        // Validate and Normalize Phone Number
        String whatsappNumber;
        try {
            whatsappNumber = PhoneNumberValidator.validateAndNormalize(rawPhone);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid phone number '" + rawPhone + "': " + e.getMessage());
        }

        // Parse Panel Timing
        LocalDateTime panelTiming = null;
        if (timingCol != -1) {
            panelTiming = parseDateTime(row.getCell(timingCol));
        } else if (dateCol != -1) {
            String dateStr = getCellValueAsString(row.getCell(dateCol));
            String timeStr = timeCol != -1 ? getCellValueAsString(row.getCell(timeCol)) : "";
            panelTiming = combineDateAndTime(dateStr, timeStr);
        } else {
            // Default to tomorrow at 10:00 AM
            panelTiming = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        }

        return Candidate.builder()
                .batch(batch)
                .name(name)
                .email(email)
                .whatsappNumber(whatsappNumber)
                .role(role)
                .companyName(companyName)
                .panelTiming(panelTiming)
                .gmeetLink(gmeetLink)
                .interviewerName(interviewerName)
                .build();
    }

    private LocalDateTime combineDateAndTime(String dateStr, String timeStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Interview Date is empty");
        }
        dateStr = dateStr.trim();
        timeStr = timeStr != null ? timeStr.trim() : "";

        // Normalize timeStr if it is full LocalDateTime string from POI
        if (timeStr.contains("T")) {
            timeStr = timeStr.substring(timeStr.indexOf("T") + 1); // e.g. "10:00"
        } else if (timeStr.contains(" ")) {
            String[] parts = timeStr.split("\\s+");
            if (parts.length > 0) {
                if (parts[0].contains(":")) {
                    // Starts with time (e.g. "11:30 AM")
                    timeStr = parts[0] + (parts.length > 1 ? " " + parts[1] : "");
                } else {
                    // Starts with date (e.g. "31-Dec-1899 10:00:00 AM")
                    if (parts.length > 1) {
                        timeStr = parts[1] + (parts.length > 2 ? " " + parts[2] : "");
                    }
                }
            }
        }

        if (timeStr.isEmpty()) {
            timeStr = "09:00:00"; // default time
        }

        LocalDate date = parseLocalDate(dateStr);
        LocalTime time = parseLocalTime(timeStr);

        return LocalDateTime.of(date, time);
    }

    private LocalDate parseLocalDate(String dateStr) {
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("d MMMM yyyy"),
                DateTimeFormatter.ofPattern("dd MMMM yyyy"),
                DateTimeFormatter.ofPattern("d-MMM-yyyy"),
                DateTimeFormatter.ofPattern("dd-MMM-yyyy"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                DateTimeFormatter.ofPattern("d/M/yyyy"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd")
        );

        if (dateStr.contains("T")) {
            try {
                return LocalDateTime.parse(dateStr).toLocalDate();
            } catch (Exception ignored) {
            }
        }

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(dateStr, new DateTimeFormatterBuilder()
                        .parseCaseInsensitive()
                        .append(formatter)
                        .toFormatter(Locale.ENGLISH));
            } catch (Exception ignored) {
            }
        }

        throw new IllegalArgumentException("Unable to parse date: '" + dateStr + "'. Expected formats like: 20 June 2026 or 25-Jun-2026.");
    }

    private LocalTime parseLocalTime(String timeStr) {
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("hh:mm a"),
                DateTimeFormatter.ofPattern("h:mm a"),
                DateTimeFormatter.ofPattern("HH:mm:ss"),
                DateTimeFormatter.ofPattern("HH:mm"),
                DateTimeFormatter.ofPattern("H:m")
        );

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalTime.parse(timeStr, new DateTimeFormatterBuilder()
                        .parseCaseInsensitive()
                        .append(formatter)
                        .toFormatter(Locale.ENGLISH));
            } catch (Exception ignored) {
            }
        }

        throw new IllegalArgumentException("Unable to parse time: '" + timeStr + "'. Expected formats like: 10:00 AM or 14:30.");
    }

    private LocalDateTime parseDateTime(Cell cell) {
        if (cell == null) {
            throw new IllegalArgumentException("Panel Date & Time is empty");
        }

        // Handle numeric date-formatted cells (native Excel date)
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue();
        }

        // Handle string representation of date
        String dateStr = getCellValueAsString(cell).trim();
        if (dateStr.isEmpty()) {
            throw new IllegalArgumentException("Panel Date & Time is empty");
        }

        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd h:mm a"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
        );

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDateTime.parse(dateStr, new DateTimeFormatterBuilder()
                        .parseCaseInsensitive()
                        .append(formatter)
                        .toFormatter(Locale.ENGLISH));
            } catch (Exception ignored) {
            }
        }

        throw new IllegalArgumentException("Unable to parse date: '" + dateStr + "'. Expected format: yyyy-MM-dd HH:mm");
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toString();
                }
                double val = cell.getNumericCellValue();
                if (val == (long) val) {
                    return String.valueOf((long) val);
                }
                return BigDecimal.valueOf(val).toPlainString();
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue().trim();
                } catch (Exception e) {
                    try {
                        double numericVal = cell.getNumericCellValue();
                        if (numericVal == (long) numericVal) {
                            return String.valueOf((long) numericVal);
                        }
                        return BigDecimal.valueOf(numericVal).toPlainString();
                    } catch (Exception ex) {
                        return "";
                    }
                }
            case BLANK:
            default:
                return "";
        }
    }

    private boolean isRowEmpty(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }
}
