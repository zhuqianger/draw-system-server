package org.example.drawsystemserver.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.example.drawsystemserver.entity.User;

@Mapper
public interface UserMapper {
    User selectByUsername(String username);
    User selectById(Long id);
    int insert(User user);
    int update(User user);
}
