package org.example.core.service;

import org.example.core.exception.NotFoundException;
import org.example.core.exception.ValidationException;
import org.example.core.model.Transaction;
import org.example.core.model.TransactionType;
import org.example.core.model.Wallet;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public final class WalletService {
    private final AlertService alertService;

    public WalletService(AlertService alertService) {
        this.alertService = alertService;
    }

    public void addCategory(Wallet wallet, String name) {
        String n = normalizeCategory(name);
        wallet.getCategories().add(n);
    }

    public void renameCategory(Wallet wallet, String oldName, String newName) {
        String oldN = normalizeCategory(oldName);
        String newN = normalizeCategory(newName);

        if (!wallet.getCategories().contains(oldN)) throw new NotFoundException("Категория не найдена: " + oldN);
        if (wallet.getCategories().contains(newN)) throw new ValidationException("Категория уже существует: " + newN);

        wallet.getCategories().remove(oldN);
        wallet.getCategories().add(newN);

        // перенос бюджета
        if (wallet.getBudgetsByCategory().containsKey(oldN)) {
            double v = wallet.getBudgetsByCategory().remove(oldN);
            wallet.getBudgetsByCategory().put(newN, v);
        }

        // перенос операций
        for (Transaction tx : wallet.getTransactions()) {
            if (oldN.equals(tx.getCategory())) tx.setCategory(newN);
        }
    }

    public void setBudget(Wallet wallet, String category, double limit) {
        String c = normalizeCategory(category);
        if (limit <= 0) throw new ValidationException("Лимит должен быть > 0");
        requireCategory(wallet, c);
        wallet.getBudgetsByCategory().put(c, limit);
    }

    public CommandResult addIncome(Wallet wallet, String category, double amount, String note) {
        String c = normalizeCategory(category);
        validateAmount(amount);
        requireCategory(wallet, c);

        wallet.getTransactions().add(new Transaction(
                TransactionType.INCOME,
                c,
                amount,
                LocalDateTime.now(),
                safeNote(note),
                null
        ));
        return CommandResult.ok();
    }

    public CommandResult addExpense(Wallet wallet, String category, double amount, String note) {
        String c = normalizeCategory(category);
        validateAmount(amount);
        requireCategory(wallet, c);

        wallet.getTransactions().add(new Transaction(
                TransactionType.EXPENSE,
                c,
                amount,
                LocalDateTime.now(),
                safeNote(note),
                null
        ));

        List<String> alerts = alertService.checkAfterExpense(wallet, c);
        return CommandResult.withMessages(alerts);
    }

    public void replaceWalletState(Wallet target, Wallet imported) {
        // Не меняем владельца, просто переносим состояния
        target.setCategories(new HashSet<>(imported.getCategories()));
        target.setBudgetsByCategory(new HashMap<>(imported.getBudgetsByCategory()));
        target.setTransactions(new ArrayList<>(imported.getTransactions()));
    }

    private static void requireCategory(Wallet wallet, String c) {
        if (!wallet.getCategories().contains(c))
            throw new NotFoundException("Категория не найдена: " + c + ". Создайте: category add \"" + c + "\"");
    }

    private static void validateAmount(double amount) {
        if (amount <= 0) throw new ValidationException("Сумма должна быть > 0");
        if (Double.isNaN(amount) || Double.isInfinite(amount)) throw new ValidationException("Некорректная сумма");
    }

    private static String normalizeCategory(String s) {
        if (s == null) throw new ValidationException("Категория пустая");
        String v = s.trim();
        if (v.isEmpty()) throw new ValidationException("Категория пустая");
        return v;
    }

    private static String safeNote(String note) {
        if (note == null) return "";
        return note.trim();
    }
}
