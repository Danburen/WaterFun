package org.waterwood.waterfunadminservice.service.content;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.waterwood.api.VO.BatchResult;
import org.waterwood.api.VO.OptionVO;
import org.waterwood.utils.generator.IdentifierGenerator;
import org.waterwood.waterfunadminservice.api.request.content.CreateTagRequest;
import org.waterwood.utils.CollectionUtil;
import org.waterwood.waterfunadminservice.api.request.content.DeleteTagsRequest;
import org.waterwood.waterfunadminservice.api.request.content.UpdateTagReq;
import org.waterwood.waterfunadminservice.infrastructure.mapper.TagMapper;
import org.waterwood.waterfunservicecore.entity.post.Tag;
import org.waterwood.waterfunservicecore.exception.notfound.TagNotFoundException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.TagRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.services.user.UserCoreService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {
    private final TagRepository tagRepository;
    private final TagMapper tagMapper;
    private final IdentifierGenerator identifierGenerator;
    private final UserCoreService userCoreService;
    private final UserRepository userRepository;

    @Override
    public void createTag(CreateTagRequest req) {
        Tag tag = tagMapper.toEntity(req);
        tag.setSlug(identifierGenerator.fromSlug(req.getSlug(), req.getName(), tagRepository));
        tag.setCreator(userRepository.getReferenceById(UserCtxHolder.getUserUid()));
        tagRepository.save(tag);
    }

    @Override
    public Page<Tag> list(Specification<Tag> spec, Pageable pageable) {
        return tagRepository.findAll(spec, pageable);
    }

    @Override
    public void deleteTagById(Long id) {
        tagRepository.deleteById(id);
    }

    @Override
    public void updateTag(Long id, UpdateTagReq req) {
        Tag tag = getTag(id);
        tagMapper.partialUpdate(req, tag);
        tagRepository.save(tag);
    }

    @Override
    public Tag getTag(Long id) {
        return tagRepository.findById(id).orElseThrow(TagNotFoundException::new);
    }

    @Transactional
    @Override
    public BatchResult deleteTags(DeleteTagsRequest req) {
        int removed = 0;
        if(CollectionUtil.isNotEmpty(req.getTagIds())){
            removed = tagRepository.removeByIdIn(req.getTagIds());
        }
        return BatchResult.of(req.getTagIds() == null ? 0 : req.getTagIds().size(), removed);
    }
    @Transactional
    @Override
    public List<OptionVO<Long>> getOptions() {
        return tagRepository.findAll().stream()
                .filter(t -> ! t.getIsDeleted())
                .map(t -> OptionVO.<Long>builder()
                        .id(t.getId())
                        .code(t.getSlug())
                        .name(t.getName())
                        .build()).toList();
    }

    @Override
    public List<OptionVO<Long>> searchTags(String keyword, int limit) {
        return tagRepository.searchByKeyword(keyword, PageRequest.of(0, limit))
                .stream()
                .map(t -> OptionVO.<Long>builder()
                        .id(t.getId())
                        .code(t.getSlug())
                        .name(t.getName())
                        .build())
                .toList();
    }

    @Override
    public List<OptionVO<Long>> getHotTags(int limit) {
        return tagRepository.findTopByOrderByUsageCountDesc(limit)
                .stream()
                .map(t -> OptionVO.<Long>builder()
                        .id(t.getId())
                        .code(t.getSlug())
                        .name(t.getName())
                        .build())
                .toList();
    }
}
