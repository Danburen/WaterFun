package org.waterwood.waterfunadminservice.service.perm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.api.VO.BatchResult;
import org.waterwood.api.VO.OptionVO;
import org.waterwood.utils.CollectionUtil;
import org.waterwood.utils.StringUtil;
import org.waterwood.utils.generator.IdentifierGenerator;
import org.waterwood.waterfunadminservice.api.request.perm.CreatePermRequest;
import org.waterwood.waterfunadminservice.api.request.perm.DeletePermsRequest;
import org.waterwood.waterfunadminservice.api.request.perm.UpdatePermRequest;
import org.waterwood.waterfunadminservice.api.response.user.AssignedUserRes;
import org.waterwood.waterfunadminservice.infrastructure.exception.BuiltInResourceProtectedException;
import org.waterwood.waterfunadminservice.infrastructure.exception.PermissionNotFoundException;
import org.waterwood.waterfunadminservice.infrastructure.mapper.PermissionMapper;
import org.waterwood.waterfunservicecore.entity.perm.Permission;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.entity.user.UserPermission;
import org.waterwood.waterfunservicecore.infrastructure.persistence.PermissionRepo;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserPermRepo;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {
    private final PermissionRepo permissionRepo;
    private final PermissionMapper permissionMapper;
    private final IdentifierGenerator identifierGenerator;
    private final UserRepository userRepository;
    private final UserPermRepo userPermRepo;

    @Override
    public Permission getPermission(int PermId){
        return permissionRepo.findById(PermId)
                .orElseThrow(PermissionNotFoundException::new);
    }

    @Override
    public Page<Permission> listPermissions(Specification<Permission> spec, Pageable pageable) {
        return permissionRepo.findAll(spec, pageable);
    }

    @Override
    public Permission addPermission(CreatePermRequest req) {
        Permission perm = new Permission();
        perm.setName(req.getName());
        perm.setDescription(req.getDescription());
        perm.setCode(identifierGenerator.fromCode(req.getCode(), req.getName(), permissionRepo));
        perm.setParent(req.getParentId() == null ? null : getPermission(req.getParentId()));
        perm.setType(req.getType());
        perm.setResource(req.getResource());
        perm.setOrderWeight(req.getOrderWeight() == null ? 1 : req.getOrderWeight());
        perm.setIsSystem(Boolean.TRUE.equals(req.getIsSystem()));
        return permissionRepo.save(perm);
    }

    @Override
    public Permission fullUpdate(int id, UpdatePermRequest req) {
        Permission perm = getPermission(id);
        Integer parentId = req.getParentId();
        String code = req.getCode();
        if(parentId != null){
            Permission parent = permissionRepo.findById(parentId)
                    .orElseThrow(PermissionNotFoundException::new);
            perm.setParent(parent);
        }
        if(StringUtil.isNotBlank(code) && !code.equals(perm.getCode())){
            perm.setCode(identifierGenerator
                    .fromCode(code, StringUtil.isBlank(req.getName()) ? code : req.getName(), permissionRepo)
            );
        }
        permissionMapper.update(req, perm);
        return permissionRepo.save(perm);
    }

    @Override
    public void deletePerm(Integer id) {
        Permission perm = permissionRepo.findById(id)
                        .orElseThrow(PermissionNotFoundException::new);
        if(perm.getIsSystem()){
            throw new BuiltInResourceProtectedException("Permission");
        }
        permissionRepo.deleteById(id);
    }

    @Override
    public BatchResult assignPermToUsers(Integer id, List<Long> items, Instant expireAt) {
        Permission perm = getPermission(id);
        int success = 0;
        if(CollectionUtil.isNotEmpty(items)){
            List<Long> distinctUserUids = items.stream().distinct().toList();
            List<User> users = userRepository.findAllById(distinctUserUids);
            if (CollectionUtil.isNotEmpty(users)) {
                Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getUid, u -> u));
                List<UserPermission> exists = userPermRepo.findByPermissionIdAndUserUidIn(id, new ArrayList<>(userMap.keySet()));
                Map<Long, UserPermission> existsMap = exists.stream()
                        .collect(Collectors.toMap(up -> up.getUser().getUid(), up -> up));

                List<UserPermission> updates = new ArrayList<>();
                List<UserPermission> inserts = new ArrayList<>();
                for (Long uid : distinctUserUids) {
                    User user = userMap.get(uid);
                    if (user == null) {
                        continue;
                    }
                    UserPermission existed = existsMap.get(uid);
                    if (existed == null) {
                        UserPermission up = new UserPermission();
                        up.setUser(user);
                        up.setPermission(perm);
                        up.setExpiresAt(expireAt);
                        inserts.add(up);
                    } else {
                        existed.setExpiresAt(expireAt);
                        updates.add(existed);
                    }
                }

                if (CollectionUtil.isNotEmpty(updates)) {
                    userPermRepo.saveAll(updates);
                }
                if (CollectionUtil.isNotEmpty(inserts)) {
                    userPermRepo.saveAll(inserts);
                }
                success = updates.size() + inserts.size();
            }
        }
        return BatchResult.of(items.size(), success);
    }

    @Override
    public Page<AssignedUserRes> listPermUsers(int id, Long userUid, String username, String nickname, Pageable pageable) {
        Page<UserPermission> res = userPermRepo.listPermUsers(id, userUid, username, nickname, pageable);
        return res.map(up -> {
            AssignedUserRes assign = new AssignedUserRes();
            assign.setAssignedAt(up.getCreatedAt());
            assign.setExpiresAt(up.getExpiresAt());
            User u = up.getUser();
            assign.setUserUid(u.getUid());
            assign.setNickname(u.getNickname());
            assign.setUsername(u.getUsername());
            return assign;
        });
    }

    @Override
    public BatchResult replacePermUsers(int id, List<Long> userUids, Instant expiresAt) {
        int removed = userPermRepo.deleteByPermissionId(id);
        int inserted = assignPermToUsers(id, userUids, expiresAt).getSuccess();
        return BatchResult.of(userUids.size(), removed + inserted);
    }

    @Override
    public BatchResult removePermUsers(int id, @NotNull List<Long> userUids) {
        int removed = userPermRepo.deleteByPermissionIdAndUserUidIn(id, userUids);
        return BatchResult.of(userUids.size(), removed);
    }

    @Override
    public List<OptionVO<Integer>> getAllPermOptions() {
        return permissionRepo.findAll().stream()
                .map(Permission::toOption)
                .toList();
    }

    @Transactional
    @Override
    public BatchResult removePerms(DeletePermsRequest req) {
        int deleted = permissionRepo.deletePermissionsByIdIn(req.getPermIds());
        return BatchResult.of(req.getPermIds().size(), deleted);
    }

}
