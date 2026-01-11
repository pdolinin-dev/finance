package org.example.infra.store;

import org.example.core.model.Wallet;

import java.util.Optional;

public interface WalletStore {
    Optional<Wallet> load(String login);
    void save(Wallet wallet);
}