package com.pv.posts.mapper;

import com.pv.posts.dto.PostResponse;
import com.pv.posts.entity.Post;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PostMapper {

    PostResponse toResponse(Post post);
}

