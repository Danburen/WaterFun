package org.waterwood.waterfunservicecore.services.user;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.common.CloudFSRoot;
import org.waterwood.waterfunservicecore.api.resp.CloudResPresignedUrlResp;
import org.waterwood.waterfunservicecore.api.resp.user.UserBrief;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;
import org.waterwood.waterfunservicecore.entity.user.UserBriefDO;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.services.sys.storage.CloudFileService;

import java.util.*;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserBriefServiceImpl implements UserBriefService {
    private final UserRepository userRepository;
    private final CloudFileService cloudFileService;
    private final UserQueryService userQueryService;

    @Override
    public List<UserBrief> listUseBriefs(List<Long> userUids){
        List<UserBriefDO> dos = userQueryService.listBriefDOs(userUids);
        if (dos.isEmpty()) return Collections.emptyList();
        Map<Long, String> userAvatarResourceKey = new HashMap<>();
        for(UserBriefDO userBriefDO : dos){
            userAvatarResourceKey.put(
                    userBriefDO.getUid(),
                    userBriefDO.getAvatarResourceKey()
            );
        }
        Map<Long, CloudResPresignedUrlResp> avatarPresignedUrlMap = cloudFileService.batchGetReadPublicUrlCached(
                CloudFSRoot.UPLOADS,
                userAvatarResourceKey,
                TargetType.USER_AVATAR
        );
        List<UserBrief> result = new ArrayList<>();
        for(UserBriefDO userBriefDO : dos){
            result.add(new UserBrief(
                    userBriefDO.getUid(),
                    userBriefDO.getDisplayName(),
                    avatarPresignedUrlMap.get(userBriefDO.getUid()),
                    userBriefDO.getLevel(),
                    userBriefDO.getUserType()
            ));
        }
        return result;
    }

    @Override
    public Map<Long, UserBrief> queryForMapUserIdBriefMap(List<Long> userUids) {
        return listUseBriefs(userUids).stream().collect(
                HashMap::new,
                (m, v) -> m.put(v.getUid(), v),
                HashMap::putAll
        );
    }

    @Override
    public UserBrief getUserBrief(long userUid) {
        UserBriefDO userBriefDO = userQueryService.queryForSingalUserBriefDO(userUid);
        CloudResPresignedUrlResp avatarPresignedUrl = cloudFileService.getReadUrlCached(
                CloudFSRoot.UPLOADS,
                userBriefDO.getAvatarResourceKey(),
                userUid,
                TargetType.USER_AVATAR
        );
        return new UserBrief(
                userUid,
                userBriefDO.getDisplayName(),
                avatarPresignedUrl,
                userBriefDO.getLevel(),
                userBriefDO.getUserType()
        );
    }
}
