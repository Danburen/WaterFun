package org.waterwood.waterfunservice.service.post.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.waterfunservicecore.entity.post.Tag;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.common.exceptions.BizException;
import org.waterwood.waterfunservicecore.exception.NotFoundException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.TagRepository;
import org.waterwood.waterfunservice.service.post.TagService;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.services.user.UserCoreService;
import org.waterwood.utils.generator.IdentifierGenerator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;
    private final IdentifierGenerator slugGenerator;
    private final UserCoreService userCoreService;

    @Override
    public void createTag(Tag tag) {
        // Generate slug && check
        String slug = slugGenerator.generateSlug(tag.getName(), tagRepository);
        tag.setSlug(slug);
        User u = userCoreService.getUserByUid(UserCtxHolder.getUserUid());
        tag.setCreator(u);

        if(tag.getUsageCount() == null) tag.setUsageCount(0L);
        tagRepository.save(tag);
    }

    @Override
    public List<Tag> getTags() {
        return tagRepository.findAllByCreatorUid(UserCtxHolder.getUserUid());
    }

    @Override
    public Tag getTag(Integer id) {
        return tagRepository.findById(id).orElseThrow(
                () -> new BizException(BaseResponseCode.NOT_FOUND, "Tag ID: " + id)
        );
    }

    @Override
    public void updateTag(Tag tag) {
        Tag t = tagRepository.findById(tag.getId()).orElseThrow(
                () -> new NotFoundException("Tag ID: " + tag.getId())
        );
        if(tag.getName() !=  null){
            t.setName(tag.getName());
            t.setSlug(slugGenerator.generateSlug(tag.getName(), tagRepository));
        }
        if(tag.getDescription() != null) t.setDescription(tag.getDescription());
        tagRepository.save(t);
    }

    @Override
    public void deleteTag(Integer id) {
        Tag t = tagRepository.findById(id).orElseThrow(
                () -> new BizException(BaseResponseCode.HTTP_NOT_FOUND)
        );

        if(! t.getCreator().getUid().equals(UserCtxHolder.getUserUid())){
            throw new BizException(BaseResponseCode.FORBIDDEN);
        }

        tagRepository.delete(t);
    }

    @Override
    public Set<Tag> getTags(Iterable<Integer> tagIds, boolean strict) {
        return new HashSet<>(tagRepository.findAllById(tagIds));
    }
}
