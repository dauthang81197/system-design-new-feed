package com.pv.template.service;

import com.pv.template.document.TemplateDocument;
import com.pv.common.dto.PageResponse;
import com.pv.template.dto.TemplateRequest;
import com.pv.template.dto.TemplateResponse;
import com.pv.template.entity.TemplateEntity;
import com.pv.common.exception.ForbiddenException;
import com.pv.common.exception.ResourceNotFoundException;
import com.pv.template.kafka.TemplateEventProducer;
import com.pv.template.mapper.TemplateMapper;
import com.pv.template.repository.jpa.TemplateRepository;
import com.pv.template.repository.mongo.TemplateMongoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TemplateServiceImpl implements TemplateService {

    private final TemplateRepository templateRepository;
    private final TemplateMongoRepository templateMongoRepository;
    private final TemplateMapper templateMapper;
    private final TemplateEventProducer eventProducer;

    @Override
    @Transactional
    public TemplateResponse create(TemplateRequest request, String userId) {
        log.info("Creating template for user={}", userId);
        TemplateEntity entity = templateMapper.toEntity(request);
        entity.setCreatedBy(userId);
        TemplateEntity saved = templateRepository.save(entity);
        saveAuditEvent(saved.getId().toString(), "CREATED", request, userId);
        eventProducer.publishEvent("CREATED", saved.getId().toString(), userId);
        return templateMapper.toResponse(saved);
    }

    @Override
    public TemplateResponse findById(UUID id) {
        return templateMapper.toResponse(getOrThrow(id));
    }

    @Override
    public PageResponse<TemplateResponse> findAll(int page, int size, String status) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<TemplateEntity> entityPage = (status != null && !status.isBlank())
            ? templateRepository.findAllByStatus(status, pageable)
            : templateRepository.findAll(pageable);
        return PageResponse.<TemplateResponse>builder()
            .content(entityPage.map(templateMapper::toResponse).toList())
            .page(entityPage.getNumber())
            .size(entityPage.getSize())
            .totalElements(entityPage.getTotalElements())
            .totalPages(entityPage.getTotalPages())
            .last(entityPage.isLast())
            .build();
    }

    @Override
    @Transactional
    public TemplateResponse update(UUID id, TemplateRequest request, String userId) {
        TemplateEntity entity = getOrThrow(id);
        if (!entity.getCreatedBy().equals(userId)) {
            throw new ForbiddenException("You are not allowed to update this resource");
        }
        templateMapper.updateEntityFromRequest(request, entity);
        TemplateEntity saved = templateRepository.save(entity);
        saveAuditEvent(saved.getId().toString(), "UPDATED", request, userId);
        eventProducer.publishEvent("UPDATED", saved.getId().toString(), userId);
        return templateMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(UUID id, String userId) {
        TemplateEntity entity = getOrThrow(id);
        if (!entity.getCreatedBy().equals(userId)) {
            throw new ForbiddenException("You are not allowed to delete this resource");
        }
        templateRepository.delete(entity);
        saveAuditEvent(id.toString(), "DELETED", null, userId);
        eventProducer.publishEvent("DELETED", id.toString(), userId);
        log.info("Deleted template id={} by user={}", id, userId);
    }

    private TemplateEntity getOrThrow(UUID id) {
        return templateRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Template", "id", id));
    }

    private void saveAuditEvent(String templateId, String eventType, Object payload, String actorId) {
        templateMongoRepository.save(TemplateDocument.builder()
            .templateId(templateId).eventType(eventType)
            .payload(payload).actorId(actorId).build());
    }
}
