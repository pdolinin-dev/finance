package org.example;

import org.example.core.exception.AuthException;
import org.example.core.exception.ValidationException;
import org.example.core.model.Wallet;
import org.example.core.service.AlertService;
import org.example.core.service.AuthService;
import org.example.core.service.WalletService;
import org.example.infra.store.FileWalletStore;
import org.example.infra.store.InMemoryUserStore;
import org.example.infra.store.UserStore;
import org.example.infra.store.WalletStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AuthAndStoreIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void register_and_login_success() {
        UserStore userStore = new InMemoryUserStore();
        WalletStore walletStore = new FileWalletStore(tempDir);

        AuthService auth = new AuthService(userStore, walletStore);

        auth.register("ivan", "1234");
        AuthService.Session session = auth.login("ivan", "1234");

        Assertions.assertEquals("ivan", session.login());
        Assertions.assertNotNull(session.wallet());
        Assertions.assertEquals("ivan", session.wallet().getOwnerLogin());
    }

    @Test
    void login_wrongPassword_throws() {
        UserStore userStore = new InMemoryUserStore();
        WalletStore walletStore = new FileWalletStore(tempDir);

        AuthService auth = new AuthService(userStore, walletStore);
        auth.register("ivan", "1234");

        assertThrows(AuthException.class, () -> auth.login("ivan", "nope"));
    }

    @Test
    void register_invalidLogin_throws() {
        UserStore userStore = new InMemoryUserStore();
        WalletStore walletStore = new FileWalletStore(tempDir);

        AuthService auth = new AuthService(userStore, walletStore);

        assertThrows(ValidationException.class, () -> auth.register("ab", "1234"));
        assertThrows(ValidationException.class, () -> auth.register("ivan", "1"));
    }

    @Test
    void wallet_is_saved_on_logout_and_loaded_on_next_login() {
        UserStore userStore = new InMemoryUserStore();
        WalletStore walletStore = new FileWalletStore(tempDir);

        AlertService alertService = new AlertService();
        WalletService walletService = new WalletService(alertService);

        AuthService auth = new AuthService(userStore, walletStore);
        auth.register("ivan", "1234");

        // login -> get wallet -> modify -> save
        AuthService.Session s1 = auth.login("ivan", "1234");
        Wallet w1 = s1.wallet();
        w1.getCategories().add("Еда");
        w1.getCategories().add("Зарплата");
        walletService.addIncome(w1, "Зарплата", 1000, "");
        auth.saveWallet(w1);

        // next login -> loaded
        AuthService.Session s2 = auth.login("ivan", "1234");
        Wallet w2 = s2.wallet();

        Assertions.assertTrue(w2.getCategories().contains("Еда"));
        Assertions.assertEquals(1, w2.getTransactions().size());
        Assertions.assertEquals(1000.0, w2.getTransactions().get(0).getAmount());
    }
}
