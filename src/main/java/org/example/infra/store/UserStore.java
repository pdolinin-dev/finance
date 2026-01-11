package org.example.infra.store;

import org.example.core.model.User;

import java.util.Optional;

public interface UserStore {
    Optional<User> findByLogin(String login);
    void save(User user);
    boolean exists(String login);
}
