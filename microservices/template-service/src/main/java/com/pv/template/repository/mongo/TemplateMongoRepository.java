package com.pv.template.repository.mongo;

import com.pv.template.document.TemplateDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TemplateMongoRepository extends MongoRepository<TemplateDocument, String> {

    Page<TemplateDocument> findByTemplateId(String templateId, Pageable pageable);
}
