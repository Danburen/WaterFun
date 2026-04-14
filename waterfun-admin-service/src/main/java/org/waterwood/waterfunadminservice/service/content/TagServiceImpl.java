package org.waterwood.waterfunadminservice.service.content;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.waterwood.waterfunadminservice.api.request.content.UpdateTagReq;
import org.waterwood.waterfunadminservice.infrastructure.mapper.TagMapper;
import org.waterwood.waterfunservicecore.entity.post.Tag;
import org.waterwood.waterfunservicecore.exception.NotFoundException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.TagRepository;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {
    private final TagRepository tagRepository;
    private final TagMapper tagMapper;

    @Override
    public Page<Tag> list(Specification<Tag> spec, Pageable pageable) {
        return tagRepository.findAll(spec, pageable);
    }

    @Override
    public void deleteTagById(Integer id) {
        tagRepository.deleteById(id);
    }

    @Override
    public void updateTag(Integer id, UpdateTagReq req) {
        Tag tag = tagRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Tag ID: " + id)
        );
        tagMapper.partialUpdate(req, tag);
        tagRepository.save(tag);
    }

    @Override
    public Tag getTag(Integer id) {
        return tagRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Tag ID: " + id)
        );
    }
}
