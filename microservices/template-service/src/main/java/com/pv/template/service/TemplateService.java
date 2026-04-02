package com.pv.template.service;

import com.pv.common.dto.PageResponse;
import com.pv.template.dto.TemplateRequest;
import com.pv.template.dto.TemplateResponse;

import java.util.UUID;

public interface TemplateService {
    TemplateResponse create(TemplateRequest request, String userId);
    TemplateResponse findById(UUID id);
    PageResponse<TemplateResponse> findAll(int page, int size, String status);
    TemplateResponse update(UUID id, TemplateRequest request, String userId);
    void delete(UUID id, String userId);
}
