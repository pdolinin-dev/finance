package org.example.infra.store;

import org.example.core.model.User;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemoryUserStore implements UserStore {
    private final Map<String, User> users = new HashMap<>();

    @Override
    public Optional<User> findByLogin(String login) {
        return Optional.ofNullable(users.get(login));
    }

    @Override
    public void save(User user) {
        users.put(user.getLogin(), user);
    }

    @Override
    public boolean exists(String login) {
        return users.containsKey(login);
    }
}
