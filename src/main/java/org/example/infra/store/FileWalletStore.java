package org.example.infra.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.core.model.Wallet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class FileWalletStore implements WalletStore {
    private final Path dir;
    private final ObjectMapper mapper;

    public FileWalletStore(Path dir) {
        this.dir = dir;
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
    }

    private Path pathFor(String login) {
        return dir.resolve(login + ".wallet.json");
    }

    @Override
    public Optional<Wallet> load(String login) {
        try {
            Files.createDirectories(dir);
            Path p = pathFor(login);
            if (!Files.exists(p)) return Optional.empty();
            Wallet w = mapper.readValue(p.toFile(), Wallet.class);
            return Optional.of(w);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось загрузить кошелёк: " + e.getMessage(), e);
        }
    }

    @Override
    public void save(Wallet wallet) {
        try {
            Files.createDirectories(dir);
            Path p = pathFor(wallet.getOwnerLogin());
            mapper.writerWithDefaultPrettyPrinter().writeValue(p.toFile(), wallet);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось сохранить кошелёк: " + e.getMessage(), e);
        }
    }
}
