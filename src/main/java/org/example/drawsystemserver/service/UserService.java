package org.example.drawsystemserver.service;

import org.example.drawsystemserver.entity.User;

public interface UserService {
    User login(String username, String password);
    User getById(Long id);
    User getByUsername(String username);
    boolean isAdmin(Long userId);
    boolean isCaptain(Long userId);
}
