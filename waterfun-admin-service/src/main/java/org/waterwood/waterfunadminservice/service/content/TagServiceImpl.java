package org.waterwood.waterfunadminservice.service.content;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
import org.waterwood.waterfunservicecore.exception.NotFoundException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.TagRepository;
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

    @Override
    public void createTag(CreateTagRequest req) {
        Tag tag = tagMapper.toEntity(req);
        tag.setSlug(identifierGenerator.fromSlug(req.getSlug(), req.getName(), tagRepository));
        tag.setCreator(userCoreService.getUserByUid(UserCtxHolder.getUserUid()));
        tagRepository.save(tag);
    }

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
        Tag tag = getTag(id);
        tagMapper.partialUpdate(req, tag);
        tagRepository.save(tag);
    }

    @Override
    public Tag getTag(Integer id) {
        return tagRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Tag ID: " + id)
        );
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
    public List<OptionVO<Integer>> getOptions() {
        return tagRepository.findAll().stream()
                .filter(t -> ! t.getIsDeleted())
                .map(t -> {
                    return OptionVO.<Integer>builder()
                            .id(t.getId())
                            .code(t.getSlug())
                            .name(t.getName())
                            .build();
                }).toList();
    }
}
