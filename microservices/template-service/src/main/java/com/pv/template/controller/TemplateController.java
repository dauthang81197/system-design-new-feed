package com.pv.template.controller;

import com.pv.common.dto.PageResponse;
import com.pv.common.security.UserPrincipal;
import com.pv.template.dto.TemplateRequest;
import com.pv.template.dto.TemplateResponse;
import com.pv.template.service.TemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/templates")
@RequiredArgsConstructor
@Tag(name = "Templates", description = "Template CRUD operations")
public class TemplateController {

    private final TemplateService templateService;

    @PostMapping
    @Operation(summary = "Create a new template")
    public ResponseEntity<TemplateResponse> create(
            @Valid @RequestBody TemplateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(templateService.create(request, principal.getUserId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get template by ID")
    public ResponseEntity<TemplateResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(templateService.findById(id));
    }

    @GetMapping
    @Operation(summary = "List templates with pagination")
    public ResponseEntity<PageResponse<TemplateResponse>> findAll(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Filter by status") @RequestParam(required = false) String status) {
        return ResponseEntity.ok(templateService.findAll(page, size, status));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a template")
    public ResponseEntity<TemplateResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody TemplateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(templateService.update(id, request, principal.getUserId()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a template")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        templateService.delete(id, principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole(\'ADMIN\')")
    @Operation(summary = "Admin: list all templates")
    public ResponseEntity<PageResponse<TemplateResponse>> adminFindAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(templateService.findAll(page, size, null));
    }
}
