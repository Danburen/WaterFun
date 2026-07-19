package org.waterwood.waterfunservice.service.post.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.utils.CollectionUtil;
import org.waterwood.waterfunservice.api.request.content.CreateTagRequest;
import org.waterwood.waterfunservice.infrastructure.mapper.TagMapper;
import org.waterwood.waterfunservice.service.user.UserService;
import org.waterwood.waterfunservicecore.entity.post.Tag;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.exception.BizException;
import org.waterwood.waterfunservicecore.exception.TagLimitExceededException;
import org.waterwood.waterfunservicecore.exception.conflict.TagConflictException;
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
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;
    private final IdentifierGenerator slugGenerator;
    private final UserCoreService userCoreService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final TagMapper tagMapper;

    @Value("${user.quota.tags:50}")
    private int userMaxTagCreateCount = 50;

    @Override
    public void createTag(CreateTagRequest req) {
        tagRepository.findByName(req.getName()).ifPresent(_ -> {
            throw new TagConflictException();
        });
        int createdCount = tagRepository.countByCreatorUid(UserCtxHolder.getUserUid());
        if(createdCount > userMaxTagCreateCount ) {
            throw new TagLimitExceededException();
        }
        Tag tag = new Tag();
        tag.setName(req.getName());
        tag.setSlug(slugGenerator.generateSlug(
                req.getName(), tagRepository
        ));
        tagRepository.save(tag);
    }

    @Override
    public List<Tag> getSelfTags() {
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
    public Set<Tag> getSelfTags(Iterable<Long> tagIds, boolean strict) {
        return new HashSet<>(tagRepository.findAllById(tagIds));
    }

    @Override
    public List<Tag> createNewTags(Set<String> newTagNames, Long userUid) {
        int existingCount = tagRepository.countByCreatorUid(userUid);
        return createNewTags(newTagNames, userUid, existingCount);
    }

    @Transactional
    @Override
    public List<Tag> createNewTags(Set<String> newTagNames, Long userUid, int existingCount) {
        if (CollectionUtil.isEmpty(newTagNames)) return List.of();
        User u = userRepository.getReferenceById(userUid);
        List<Tag> existingTags = tagRepository.findAllByNameIn(newTagNames);
        Set<String> existingNames = existingTags.stream()
                .map(Tag::getName)
                .map(String::toLowerCase)
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
            if (existingCount > userMaxTagCreateCount) {
                if (userCoreService.isUserAdmin(userUid)) {
                    log.warn("Admin user {} is creating tags beyond the quota limit. Current count: {}, Attempting to create: {}, Quota limit: {}",
                            userUid, existingCount, tagsToCreate.size(), userMaxTagCreateCount);
                } else {
                    throw new TagLimitExceededException();
                }
            }
            return tagRepository.saveAll(tagsToCreate);
        }
        return List.of();
    }

    @Override
    public Page<Tag> getHotTags(Pageable pageable) {
        int size = Math.min(pageable.getPageSize(), 50);
        Pageable fixedSort = PageRequest.of(
                pageable.getPageNumber(),
                size,
                Sort.by(Sort.Direction.DESC, "usageCount")
        );;
        Page<Tag> tags = tagRepository
                .findAllByIsDeleted(false, fixedSort);
        return tags;
    }

    @Override
    public List<Tag> searchTags(String keyword, int limit) {
        limit = Math.max(limit, 20);
        Pageable pageable = PageRequest.of(
                0,
                limit,
                Sort.by(Sort.Direction.DESC, "usageCount")
        );
        return tagRepository.searchByKeyword(keyword, pageable);
    }
}
