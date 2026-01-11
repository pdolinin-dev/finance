package org.example.core.service;

import org.example.core.exception.AuthException;
import org.example.core.exception.ValidationException;
import org.example.core.model.User;
import org.example.core.model.Wallet;
import org.example.infra.store.UserStore;
import org.example.infra.store.WalletStore;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class AuthService {
    private final UserStore userStore;
    private final WalletStore walletStore;

    public AuthService(UserStore userStore, WalletStore walletStore) {
        this.userStore = userStore;
        this.walletStore = walletStore;
    }

    public void register(String login, String password) {
        validateLoginPassword(login, password);
        if (userStore.exists(login)) throw new ValidationException("Логин уже занят.");
        String hash = hash(password);
        userStore.save(new User(login, hash));
        // кошелёк создаётся при первом логине
    }

    public Session login(String login, String password) {
        validateLoginPassword(login, password);
        User user = userStore.findByLogin(login).orElseThrow(() -> new AuthException("Пользователь не найден."));
        if (!user.getPasswordHash().equals(hash(password))) throw new AuthException("Неверный пароль.");

        Wallet wallet = walletStore.load(login).orElseGet(() -> new Wallet(login));
        // на всякий случай: если файл поврежден/владелец другой
        wallet.setOwnerLogin(login);
        return new Session(login, wallet);
    }

    public void saveWallet(Wallet wallet) {
        walletStore.save(wallet);
    }

    private static void validateLoginPassword(String login, String password) {
        if (login == null || login.isBlank()) throw new ValidationException("Логин пустой.");
        if (password == null || password.isBlank()) throw new ValidationException("Пароль пустой.");
        if (login.length() < 3) throw new ValidationException("Логин должен быть >= 3 символов.");
        if (password.length() < 4) throw new ValidationException("Пароль должен быть >= 4 символов.");
    }

    private static String hash(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] out = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out);
        } catch (Exception e) {
            throw new RuntimeException("Hash error", e);
        }
    }

    public record Session(String login, Wallet wallet) {}
}
