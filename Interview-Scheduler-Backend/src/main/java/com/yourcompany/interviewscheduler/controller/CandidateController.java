package com.yourcompany.interviewscheduler.controller;

import com.yourcompany.interviewscheduler.model.dto.CandidateDTO;
import com.yourcompany.interviewscheduler.model.entity.Batch;
import com.yourcompany.interviewscheduler.model.entity.Candidate;
import com.yourcompany.interviewscheduler.repository.BatchRepository;
import com.yourcompany.interviewscheduler.repository.CandidateRepository;
import com.yourcompany.interviewscheduler.repository.MessageLogRepository;
import com.yourcompany.interviewscheduler.service.ExcelParserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/candidates")
@CrossOrigin(origins = "*")
@Tag(name = "Candidates", description = "Endpoints for uploading Excel files and managing candidate data")
public class CandidateController {

    @Autowired
    private ExcelParserService excelParserService;

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private MessageLogRepository messageLogRepository;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    @Operation(summary = "Upload candidate Excel file", description = "Parses candidates from Excel and stores them as a new batch")
    public ResponseEntity<Batch> uploadExcel(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // Initialize a new Batch
        Batch batch = Batch.builder()
                .fileName(file.getOriginalFilename())
                .totalCandidates(0) // Will update after parsing
                .status("UPLOADED")
                .build();

        batch = batchRepository.save(batch);

        // Parse candidates
        List<Candidate> candidates = excelParserService.parseExcel(file, batch);

        // Save candidates
        candidateRepository.saveAll(candidates);

        // Update batch size
        batch.setTotalCandidates(candidates.size());
        batch = batchRepository.save(batch);

        return ResponseEntity.status(HttpStatus.CREATED).body(batch);
    }

    @GetMapping("/batches")
    @Operation(summary = "List all upload batches", description = "Returns a list of all uploaded batches")
    public ResponseEntity<List<Batch>> listAllBatches() {
        return ResponseEntity.ok(batchRepository.findAll());
    }

    @GetMapping
    @Operation(summary = "List candidates by batch ID", description = "Returns all candidates associated with a specific upload batch")
    public ResponseEntity<List<CandidateDTO>> listCandidates(@RequestParam("batchId") Long batchId) {
        if (!batchRepository.existsById(batchId)) {
            return ResponseEntity.notFound().build();
        }

        List<Candidate> candidates = candidateRepository.findByBatchId(batchId);
        List<CandidateDTO> dtos = candidates.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get single candidate by ID", description = "Returns candidate details for a specific ID")
    public ResponseEntity<CandidateDTO> getCandidate(@PathVariable("id") Long id) {
        return candidateRepository.findById(id)
                .map(candidate -> ResponseEntity.ok(convertToDTO(candidate)))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/batch/{batchId}")
    @Transactional
    @Operation(summary = "Delete an upload batch", description = "Permanently deletes a batch, its candidates, and all associated message logs")
    public ResponseEntity<Void> deleteBatch(@PathVariable("batchId") Long batchId) {
        if (!batchRepository.existsById(batchId)) {
            return ResponseEntity.notFound().build();
        }

        // Cascade delete manually since we didn't specify foreign key cascades in DB schema
        messageLogRepository.deleteByBatchId(batchId);
        candidateRepository.deleteByBatchId(batchId);
        batchRepository.deleteById(batchId);

        return ResponseEntity.noContent().build();
    }

    private CandidateDTO convertToDTO(Candidate candidate) {
        return CandidateDTO.builder()
                .id(candidate.getId())
                .batchId(candidate.getBatch().getId())
                .name(candidate.getName())
                .email(candidate.getEmail())
                .whatsappNumber(candidate.getWhatsappNumber())
                .role(candidate.getRole())
                .companyName(candidate.getCompanyName())
                .panelTiming(candidate.getPanelTiming())
                .gmeetLink(candidate.getGmeetLink())
                .interviewerName(candidate.getInterviewerName())
                .build();
    }
}
