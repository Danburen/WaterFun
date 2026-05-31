package org.waterwood.waterfunservice.service.post.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.utils.CollectionUtil;
import org.waterwood.waterfunservice.service.user.UserService;
import org.waterwood.waterfunservicecore.entity.post.Tag;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.exception.BizException;
import org.waterwood.waterfunservicecore.exception.notfound.TagNotFoundException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.TagRepository;
import org.waterwood.waterfunservice.service.post.TagService;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.services.user.UserCoreService;
import org.waterwood.utils.generator.IdentifierGenerator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;
    private final IdentifierGenerator slugGenerator;
    private final UserCoreService userCoreService;
    private final UserRepository userRepository;
    private final UserService userService;

    @Value("${user.quota.tags:50}")
    private int userMaxTagCreateCount = 50;

    @Override
    public void createTag(Tag tag) {
        // Generate slug && check
        String slug = slugGenerator.generateSlug(tag.getName().toLowerCase(), tagRepository);
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
    public Tag getTag(Long id) {
        return tagRepository.findById(id).orElseThrow(TagNotFoundException::new);
    }

    @Override
    public void updateTag(Tag tag) {
        Tag t = tagRepository.findById(tag.getId()).orElseThrow(TagNotFoundException::new);
        if(tag.getName() !=  null){
            t.setName(tag.getName().toLowerCase());
            t.setSlug(slugGenerator.generateSlug(tag.getName(), tagRepository));
        }
        if(tag.getDescription() != null) t.setDescription(tag.getDescription());
        tagRepository.save(t);
    }

    @Override
    public void deleteTag(Long id) {
        Tag t = tagRepository.findById(id).orElseThrow(TagNotFoundException::new);

        if(! t.getCreator().getUid().equals(UserCtxHolder.getUserUid())){
            throw new BizException(BaseResponseCode.FORBIDDEN);
        }

        tagRepository.delete(t);
    }

    @Override
    public Set<Tag> getTags(Iterable<Long> tagIds, boolean strict) {
        return new HashSet<>(tagRepository.findAllById(tagIds));
    }

    // TODO achieve tag audit
    @Transactional
    @Override
    public List<Tag> createNewTags(Set<String> newTagNames, Long userUid) {
        if(CollectionUtil.isEmpty(newTagNames)) return List.of();
        User u = userRepository.getReferenceById(userUid);
        List<Tag> existingTags = tagRepository.findAllByNameIn(newTagNames);
        Set<String> existingNames = existingTags.stream()
                .map(Tag::getName)
                .map(String::toLowerCase)  // unified lower case
                .collect(Collectors.toSet());

        List<Tag> tagsToCreate = newTagNames.stream()
                .filter(name -> !existingNames.contains(name.toLowerCase()))
                .map(name -> {
                    Tag t = new Tag();
                    t.setName(name.toLowerCase());
                    String slug = slugGenerator.generateSlug(name, tagRepository);
                    t.setSlug(slug);
                    t.setCreator(u);
                    return t;
                })
                .toList();
        if (!tagsToCreate.isEmpty()) {
            int createdCount = tagRepository.countByCreatorUid(userUid);
            if(createdCount > userMaxTagCreateCount) {
                if(userCoreService.isCurrentUserAdmin()){
                    // Admin can bypass the quota limit, but we still want to log it for audit
                    log.warn("Admin user {} is creating tags beyond the quota limit. Current count: {}, Attempting to create: {}, Quota limit: {}",
                            userUid, createdCount, tagsToCreate.size(), userMaxTagCreateCount);
                } else {
                    throw new BizException(BaseResponseCode.USER_TAG_QUOTA_EXCEEDED);
                }
            };
            return tagRepository.saveAll(tagsToCreate);
        }
        return List.of();
    }
}
