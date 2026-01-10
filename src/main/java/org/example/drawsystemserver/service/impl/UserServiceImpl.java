package org.example.drawsystemserver.service.impl;

import org.example.drawsystemserver.entity.User;
import org.example.drawsystemserver.mapper.UserMapper;
import org.example.drawsystemserver.service.UserService;
import org.example.drawsystemserver.util.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public User login(String username, String password) {
        User user = userMapper.selectByUsername(username);
        if (user != null && PasswordUtil.matches(password, user.getPassword())) {
            return user;
        }
        return null;
    }

    @Override
    public User getById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public User getByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    @Override
    public boolean isAdmin(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null && "ADMIN".equals(user.getUserType());
    }

    @Override
    public boolean isCaptain(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null && "CAPTAIN".equals(user.getUserType());
    }
}
