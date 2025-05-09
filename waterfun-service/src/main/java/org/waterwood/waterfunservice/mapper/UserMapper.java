package org.waterwood.waterfunservice.mapper;


import org.apache.ibatis.annotations.*;
import org.waterwood.waterfunservice.entity.User.AccountStatus;
import org.waterwood.waterfunservice.entity.User.User;

import java.util.List;

@Mapper
public interface UserMapper {
    @Insert("INSERT INTO user (username, password_hash, role, account_status, " +
            "status_changed_at, status_change_reason) " +
            "VALUES (#{username}, #{passwordHash}, #{role}, #{accountStatus}, " +
            "#{statusChangedAt}, #{statusChangeReason})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("UPDATE user SET username=#{username}, password_hash=#{passwordHash}, " +
            "role=#{role}, account_status=#{accountStatus}, " +
            "status_changed_at=#{statusChangedAt}, status_change_reason=#{statusChangeReason} " +
            "WHERE id=#{id}")
    int update(User user);

    @Update("UPDATE user SET account_status='DELETED', " +
            "status_changed_at=CURRENT_TIMESTAMP " +
            "WHERE id=#{id}")
    int deleteById(Long id);

    @Select("SELECT * FROM user WHERE id=#{id}")
    @Results({
            @Result(property = "passwordHash", column = "password_hash"),
            @Result(property = "accountStatus", column = "account_status"),
            @Result(property = "statusChangedAt", column = "status_changed_at"),
            @Result(property = "statusChangeReason", column = "status_change_reason"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    User selectById(Long id);

    @Select("SELECT * FROM user WHERE username=#{username}")
    @Results({
            @Result(property = "passwordHash", column = "password_hash"),
            @Result(property = "accountStatus", column = "account_status"),
            @Result(property = "statusChangedAt", column = "status_changed_at"),
            @Result(property = "statusChangeReason", column = "status_change_reason"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    User selectByUsername(String username);

    @Select("SELECT * FROM user WHERE account_status=#{status}")
    @Results({
            @Result(property = "passwordHash", column = "password_hash"),
            @Result(property = "accountStatus", column = "account_status"),
            @Result(property = "statusChangedAt", column = "status_changed_at"),
            @Result(property = "statusChangeReason", column = "status_change_reason"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<User> selectByStatus(AccountStatus status);

    @Update("UPDATE user SET account_status=#{status}, " +
            "status_changed_at=CURRENT_TIMESTAMP, " +
            "status_change_reason=#{reason} " +
            "WHERE id=#{id}")
    int updateStatus(@Param("id") Long id,
                     @Param("status") AccountStatus status,
                     @Param("reason") String reason);

    @Select("SELECT COUNT(*) FROM user WHERE username=#{username}")
    boolean existsByUsername(String username);
}
