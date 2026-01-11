package org.example.core.service;

import org.example.core.exception.NotFoundException;
import org.example.core.exception.ValidationException;
import org.example.core.model.Transaction;
import org.example.core.model.TransactionType;
import org.example.core.model.User;
import org.example.core.model.Wallet;
import org.example.infra.store.UserStore;
import org.example.infra.store.WalletStore;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class TransferService {
    private final UserStore userStore;
    private final WalletStore walletStore;
    private final AlertService alertService;

    public TransferService(UserStore userStore, WalletStore walletStore, AlertService alertService) {
        this.userStore = userStore;
        this.walletStore = walletStore;
        this.alertService = alertService;
    }

    public CommandResult transfer(String fromLogin, Wallet fromWallet, String toLogin, double amount, String note) {
        if (toLogin == null || toLogin.isBlank()) throw new ValidationException("toLogin пустой");
        if (amount <= 0) throw new ValidationException("Сумма должна быть > 0");
        if (fromLogin.equals(toLogin)) throw new ValidationException("Нельзя перевести самому себе.");

        User toUser = userStore.findByLogin(toLogin).orElseThrow(() -> new NotFoundException("Получатель не найден: " + toLogin));

        // баланс отправителя (разрешаем отрицательный? — здесь НЕ запрещаем, просто предупреждаем)
        // Если хочешь запрет: if (balance < amount) throw ValidationException(...)
        double inc = fromWallet.getTransactions().stream()
                .filter(t -> t.getType() == TransactionType.INCOME || t.getType() == TransactionType.TRANSFER_IN)
                .mapToDouble(Transaction::getAmount).sum();
        double exp = fromWallet.getTransactions().stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE || t.getType() == TransactionType.TRANSFER_OUT)
                .mapToDouble(Transaction::getAmount).sum();
        double balance = inc - exp;

        // Запись у отправителя
        fromWallet.getTransactions().add(new Transaction(
                TransactionType.TRANSFER_OUT,
                "Перевод",
                amount,
                LocalDateTime.now(),
                safeNote(note),
                toLogin
        ));
        // алерты для отправителя (используем категорию "Перевод" для бюджета если задан)
        List<String> alerts = new ArrayList<>(alertService.checkAfterExpense(fromWallet, "Перевод"));
        if (balance - amount < 0) alerts.add("После перевода баланс стал отрицательным: " + (balance - amount));

        // Загрузка кошелька получателя из файла (или новый), обновление и сохранение сразу
        Wallet toWallet = walletStore.load(toUser.getLogin()).orElseGet(() -> new Wallet(toUser.getLogin()));
        toWallet.setOwnerLogin(toUser.getLogin());

        // Можно требовать наличия категории "Перевод" у получателя? Для простоты — добавим автоматически.
        toWallet.getCategories().add("Перевод");

        toWallet.getTransactions().add(new Transaction(
                TransactionType.TRANSFER_IN,
                "Перевод",
                amount,
                LocalDateTime.now(),
                safeNote(note),
                fromLogin
        ));

        walletStore.save(toWallet);
        return CommandResult.withMessages(alerts);
    }

    private static String safeNote(String note) {
        if (note == null) return "";
        return note.trim();
    }
}
