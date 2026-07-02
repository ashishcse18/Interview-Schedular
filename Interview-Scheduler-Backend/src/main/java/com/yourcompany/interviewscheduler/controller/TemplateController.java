package com.yourcompany.interviewscheduler.controller;

import com.yourcompany.interviewscheduler.model.entity.MessageTemplate;
import com.yourcompany.interviewscheduler.service.TemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
@CrossOrigin(origins = "*")
@Tag(name = "Templates", description = "Endpoints for managing message templates and placeholders")
public class TemplateController {

    @Autowired
    private TemplateService templateService;

    @PostMapping
    @Operation(summary = "Create a new message template", description = "Saves a new template containing placeholders like {{candidateName}}")
    public ResponseEntity<MessageTemplate> createTemplate(@Valid @RequestBody MessageTemplate template) {
        MessageTemplate created = templateService.createTemplate(template);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @Operation(summary = "List all message templates", description = "Returns a list of all saved message templates")
    public ResponseEntity<List<MessageTemplate>> getAllTemplates() {
        List<MessageTemplate> templates = templateService.getAllTemplates();
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get single message template by ID")
    public ResponseEntity<MessageTemplate> getTemplateById(@PathVariable("id") Long id) {
        return templateService.getTemplateById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing template")
    public ResponseEntity<MessageTemplate> updateTemplate(@PathVariable("id") Long id, @Valid @RequestBody MessageTemplate template) {
        try {
            MessageTemplate updated = templateService.updateTemplate(id, template);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a template by ID")
    public ResponseEntity<Void> deleteTemplate(@PathVariable("id") Long id) {
        try {
            templateService.deleteTemplate(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
