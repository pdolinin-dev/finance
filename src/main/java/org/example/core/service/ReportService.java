package org.example.core.service;

import org.example.core.model.Transaction;
import org.example.core.model.TransactionType;
import org.example.core.model.Wallet;

import java.util.*;

public final class ReportService {

    public double totalIncome(Wallet wallet) {
        return wallet.getTransactions().stream()
                .filter(t -> t.getType() == TransactionType.INCOME || t.getType() == TransactionType.TRANSFER_IN)
                .mapToDouble(Transaction::getAmount).sum();
    }

    public double totalExpense(Wallet wallet) {
        return wallet.getTransactions().stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE || t.getType() == TransactionType.TRANSFER_OUT)
                .mapToDouble(Transaction::getAmount).sum();
    }

    public Map<String, Double> incomeByCategory(Wallet wallet, Set<String> only) {
        Map<String, Double> m = new HashMap<>();
        for (Transaction t : wallet.getTransactions()) {
            if (!(t.getType() == TransactionType.INCOME || t.getType() == TransactionType.TRANSFER_IN)) continue;
            if (only != null && !only.contains(t.getCategory())) continue;
            m.merge(t.getCategory(), t.getAmount(), Double::sum);
        }
        return m;
    }

    public Map<String, Double> expenseByCategory(Wallet wallet, Set<String> only) {
        Map<String, Double> m = new HashMap<>();
        for (Transaction t : wallet.getTransactions()) {
            if (!(t.getType() == TransactionType.EXPENSE || t.getType() == TransactionType.TRANSFER_OUT)) continue;
            if (only != null && !only.contains(t.getCategory())) continue;
            m.merge(t.getCategory(), t.getAmount(), Double::sum);
        }
        return m;
    }

    public List<String> missingCategories(Wallet wallet, Set<String> only) {
        if (only == null) return List.of();
        List<String> missing = new ArrayList<>();
        for (String c : only) {
            if (!wallet.getCategories().contains(c)) missing.add(c);
        }
        return missing;
    }

    public Map<String, BudgetStatus> budgetStatuses(Wallet wallet) {
        Map<String, BudgetStatus> out = new HashMap<>();
        for (var e : wallet.getBudgetsByCategory().entrySet()) {
            String cat = e.getKey();
            double limit = e.getValue();
            double spent = wallet.getTransactions().stream()
                    .filter(t -> t.getType() == TransactionType.EXPENSE || t.getType() == TransactionType.TRANSFER_OUT)
                    .filter(t -> cat.equals(t.getCategory()))
                    .mapToDouble(Transaction::getAmount).sum();
            out.put(cat, new BudgetStatus(limit, spent, limit - spent));
        }
        return out;
    }

    public record BudgetStatus(double limit, double spent, double remaining) {}
}