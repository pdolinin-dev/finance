package org.example.core.service;

import org.example.core.model.Transaction;
import org.example.core.model.TransactionType;
import org.example.core.model.Wallet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class AlertService {

    public List<String> checkAfterExpense(Wallet wallet, String category) {
        List<String> alerts = new ArrayList<>();

        double totalIncome = wallet.getTransactions().stream()
                .filter(t -> t.getType() == TransactionType.INCOME || t.getType() == TransactionType.TRANSFER_IN)
                .mapToDouble(Transaction::getAmount).sum();

        double totalExpense = wallet.getTransactions().stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE || t.getType() == TransactionType.TRANSFER_OUT)
                .mapToDouble(Transaction::getAmount).sum();

        if (totalExpense > totalIncome) {
            alerts.add("Расходы превысили доходы. Текущий баланс: " + (totalIncome - totalExpense));
        }

        Map<String, Double> budgets = wallet.getBudgetsByCategory();
        if (budgets.containsKey(category)) {
            double limit = budgets.get(category);
            double spent = wallet.getTransactions().stream()
                    .filter(t -> t.getType() == TransactionType.EXPENSE || t.getType() == TransactionType.TRANSFER_OUT)
                    .filter(t -> category.equals(t.getCategory()))
                    .mapToDouble(Transaction::getAmount).sum();

            if (limit > 0) {
                double ratio = spent / limit;
                if (ratio >= 0.8 && spent <= limit) {
                    alerts.add("Вы потратили >= 80% бюджета по категории '" + category + "'. Потрачено: " + spent + " из " + limit);
                }
                if (spent > limit) {
                    alerts.add("Превышен бюджет по категории '" + category + "'. Потрачено: " + spent + " при лимите " + limit);
                }
            }
        }

        return alerts;
    }
}
