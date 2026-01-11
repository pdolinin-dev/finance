package org.example.core.service;

import org.example.core.exception.NotFoundException;
import org.example.core.exception.ValidationException;
import org.example.core.model.TransactionType;
import org.example.core.model.Wallet;
import org.example.infra.store.FileWalletStore;
import org.example.infra.store.InMemoryUserStore;
import org.example.infra.store.UserStore;
import org.example.infra.store.WalletStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TransferServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void transfer_createsOutForSender_andInForReceiver_andSavesReceiverWallet() {
        UserStore userStore = new InMemoryUserStore();
        WalletStore walletStore = new FileWalletStore(tempDir);
        AlertService alertService = new AlertService();

        AuthService auth = new AuthService(userStore, walletStore);
        auth.register("alice", "1234");
        auth.register("bob", "1234");

        Wallet sender = new Wallet("alice");
        sender.getCategories().add("Перевод");
        sender.getCategories().add("Еда");
        sender.getCategories().add("Зарплата");

        TransferService transferService = new TransferService(userStore, walletStore, alertService);

        CommandResult res = transferService.transfer("alice", sender, "bob", 250, "на кофе");
        assertNotNull(res);

        // sender got TRANSFER_OUT
        assertEquals(1, sender.getTransactions().size());
        assertEquals(TransactionType.TRANSFER_OUT, sender.getTransactions().get(0).getType());
        assertEquals(250.0, sender.getTransactions().get(0).getAmount());
        assertEquals("bob", sender.getTransactions().get(0).getCounterpartyLogin());

        // receiver wallet saved to disk
        Wallet receiver = walletStore.load("bob").orElseThrow();
        assertEquals("bob", receiver.getOwnerLogin());
        assertEquals(1, receiver.getTransactions().size());
        assertEquals(TransactionType.TRANSFER_IN, receiver.getTransactions().get(0).getType());
        assertEquals(250.0, receiver.getTransactions().get(0).getAmount());
        assertEquals("alice", receiver.getTransactions().get(0).getCounterpartyLogin());
    }

    @Test
    void transfer_toUnknownUser_throws() {
        UserStore userStore = new InMemoryUserStore();
        WalletStore walletStore = new FileWalletStore(tempDir);
        AlertService alertService = new AlertService();

        AuthService auth = new AuthService(userStore, walletStore);
        auth.register("alice", "1234");

        Wallet sender = new Wallet("alice");
        sender.getCategories().add("Перевод");

        TransferService transferService = new TransferService(userStore, walletStore, alertService);

        assertThrows(NotFoundException.class, () -> transferService.transfer("alice", sender, "noone", 100, ""));
    }

    @Test
    void transfer_rejectsNonPositiveAmount_and_selfTransfer() {
        UserStore userStore = new InMemoryUserStore();
        WalletStore walletStore = new FileWalletStore(tempDir);
        AlertService alertService = new AlertService();

        AuthService auth = new AuthService(userStore, walletStore);
        auth.register("alice", "1234");

        Wallet sender = new Wallet("alice");
        sender.getCategories().add("Перевод");

        TransferService transferService = new TransferService(userStore, walletStore, alertService);

        assertThrows(ValidationException.class, () -> transferService.transfer("alice", sender, "alice", 100, ""));
        assertThrows(ValidationException.class, () -> transferService.transfer("alice", sender, "alice", 0, ""));
        assertThrows(ValidationException.class, () -> transferService.transfer("alice", sender, "alice", -1, ""));
    }
}