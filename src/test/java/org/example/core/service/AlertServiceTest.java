package org.example.core.service;

import org.example.core.model.Wallet;
import org.junit.jupiter.api.Test;
import org.example.TestWalletFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AlertServiceTest {

    private final AlertService alertService = new AlertService();
    private final WalletService walletService = new WalletService(alertService);

    @Test
    void alerts_whenBudgetExceeded() {
        Wallet w = TestWalletFactory.wallet("u1");
        walletService.setBudget(w, "Еда", 1000);

        walletService.addExpense(w, "Еда", 1200, "");
        List<String> alerts = alertService.checkAfterExpense(w, "Еда");

        assertTrue(alerts.stream().anyMatch(s -> s.contains("Превышен бюджет")));
    }

    @Test
    void alerts_whenBudgetAbove80Percent() {
        Wallet w = TestWalletFactory.wallet("u1");
        walletService.setBudget(w, "Еда", 1000);

        walletService.addExpense(w, "Еда", 800, "");
        List<String> alerts = alertService.checkAfterExpense(w, "Еда");

        assertTrue(alerts.stream().anyMatch(s -> s.contains(">= 80%")));
    }

    @Test
    void alerts_whenExpensesExceedIncome() {
        Wallet w = TestWalletFactory.wallet("u1");

        walletService.addIncome(w, "Зарплата", 500, "");
        walletService.addExpense(w, "Еда", 600, "");

        List<String> alerts = alertService.checkAfterExpense(w, "Еда");
        assertTrue(alerts.stream().anyMatch(s -> s.contains("Расходы превысили доходы")));
    }
}