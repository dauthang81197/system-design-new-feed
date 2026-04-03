package com.pv.template.mapper;

import com.pv.template.dto.TemplateRequest;
import com.pv.template.dto.TemplateResponse;
import com.pv.template.entity.TemplateEntity;
import org.mapstruct.*;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface TemplateMapper {

    TemplateResponse toResponse(TemplateEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    TemplateEntity toEntity(TemplateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    void updateEntityFromRequest(TemplateRequest request, @MappingTarget TemplateEntity entity);
}
