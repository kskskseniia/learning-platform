package com.example.learningplatform.service;

import com.example.learningplatform.dto.NameRequest;
import com.example.learningplatform.dto.NamedResourceResponse;
import com.example.learningplatform.entity.Tag;
import com.example.learningplatform.exception.DuplicateResourceException;
import com.example.learningplatform.exception.ResourceNotFoundException;
import com.example.learningplatform.repository.TagRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TagService {

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    public List<NamedResourceResponse> findAll() {
        return tagRepository.findAll().stream().map(this::toResponse).toList();
    }

    public NamedResourceResponse findById(Integer id) {
        return toResponse(getTag(id));
    }

    @Transactional
    public NamedResourceResponse create(NameRequest request) {
        ensureNameAvailable(request.name(), null);
        return toResponse(tagRepository.save(new Tag(request.name())));
    }

    @Transactional
    public NamedResourceResponse update(Integer id, NameRequest request) {
        Tag tag = getTag(id);
        ensureNameAvailable(request.name(), id);
        tag.setName(request.name());
        return toResponse(tag);
    }

    @Transactional
    public void delete(Integer id) {
        tagRepository.delete(getTag(id));
    }

    private Tag getTag(Integer id) {
        return tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag", id));
    }

    private void ensureNameAvailable(String name, Integer currentId) {
        boolean exists = currentId == null
                ? tagRepository.existsByNameIgnoreCase(name)
                : tagRepository.existsByNameIgnoreCaseAndIdNot(name, currentId);
        if (exists) {
            throw new DuplicateResourceException("Tag with this name already exists");
        }
    }

    private NamedResourceResponse toResponse(Tag tag) {
        return new NamedResourceResponse(tag.getId(), tag.getName());
    }
}
