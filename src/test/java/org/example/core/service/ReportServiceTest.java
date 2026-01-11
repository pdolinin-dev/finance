package org.example.core.service;

import org.example.TestWalletFactory;
import org.example.core.model.Wallet;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ReportServiceTest {

    private final WalletService walletService = new WalletService(new AlertService());
    private final ReportService reportService = new ReportService();

    @Test
    void totalIncome_countsIncomeAndTransferIn() {
        Wallet w = TestWalletFactory.wallet("u1");
        // нужные категории
        w.getCategories().add("Бонус");

        walletService.addIncome(w, "Зарплата", 20000, "");
        walletService.addIncome(w, "Зарплата", 40000, "");
        walletService.addIncome(w, "Бонус", 3000, "");
        // transfer in
        w.getTransactions().add(new org.example.core.model.Transaction(
                org.example.core.model.TransactionType.TRANSFER_IN, "Перевод", 500, java.time.LocalDateTime.now(), "", "u2"
        ));

        assertEquals(63500.0, reportService.totalIncome(w));
    }

    @Test
    void totalExpense_countsExpenseAndTransferOut() {
        Wallet w = TestWalletFactory.wallet("u1");
        w.getCategories().add("Такси");

        walletService.addExpense(w, "Еда", 300, "");
        walletService.addExpense(w, "Еда", 500, "");
        walletService.addExpense(w, "Такси", 1500, "");
        // transfer out
        w.getTransactions().add(new org.example.core.model.Transaction(
                org.example.core.model.TransactionType.TRANSFER_OUT, "Перевод", 200, java.time.LocalDateTime.now(), "", "u2"
        ));

        assertEquals(2500.0, reportService.totalExpense(w));
    }

    @Test
    void incomeByCategory_aggregatesCorrectly() {
        Wallet w = TestWalletFactory.wallet("u1");
        w.getCategories().add("Бонус");

        walletService.addIncome(w, "Зарплата", 20000, "");
        walletService.addIncome(w, "Зарплата", 40000, "");
        walletService.addIncome(w, "Бонус", 3000, "");

        Map<String, Double> m = reportService.incomeByCategory(w, null);
        assertEquals(60000.0, m.get("Зарплата"));
        assertEquals(3000.0, m.get("Бонус"));
    }

    @Test
    void expenseByCategory_aggregatesCorrectly() {
        Wallet w = TestWalletFactory.wallet("u1");
        w.getCategories().add("Такси");

        walletService.addExpense(w, "Еда", 300, "");
        walletService.addExpense(w, "Еда", 500, "");
        walletService.addExpense(w, "Такси", 1500, "");

        Map<String, Double> m = reportService.expenseByCategory(w, null);
        assertEquals(800.0, m.get("Еда"));
        assertEquals(1500.0, m.get("Такси"));
    }

    @Test
    void categoriesFilter_onlySelectedAreCounted() {
        Wallet w = TestWalletFactory.wallet("u1");
        w.getCategories().add("Такси");

        walletService.addExpense(w, "Еда", 300, "");
        walletService.addExpense(w, "Такси", 1500, "");

        Map<String, Double> m = reportService.expenseByCategory(w, Set.of("Еда"));
        assertEquals(1, m.size());
        assertEquals(300.0, m.get("Еда"));
        assertNull(m.get("Такси"));
    }

    @Test
    void missingCategories_returnsNotFoundOnFilter() {
        Wallet w = TestWalletFactory.wallet("u1");
        var missing = reportService.missingCategories(w, Set.of("Еда", "НетКатегории"));
        assertEquals(1, missing.size());
        assertEquals("НетКатегории", missing.get(0));
    }

    @Test
    void budgetStatuses_calculatesRemainingCorrectly() {
        Wallet w = TestWalletFactory.wallet("u1");
        w.getCategories().add("Коммунальные услуги");
        w.getCategories().add("Развлечения");

        walletService.setBudget(w, "Еда", 4000);
        walletService.setBudget(w, "Развлечения", 3000);
        walletService.setBudget(w, "Коммунальные услуги", 2500);

        walletService.addExpense(w, "Еда", 800, "");
        walletService.addExpense(w, "Развлечения", 3000, "");
        walletService.addExpense(w, "Коммунальные услуги", 3000, "");

        var statuses = reportService.budgetStatuses(w);

        assertEquals(4000.0, statuses.get("Еда").limit());
        assertEquals(800.0, statuses.get("Еда").spent());
        assertEquals(3200.0, statuses.get("Еда").remaining());

        assertEquals(0.0, statuses.get("Развлечения").remaining());
        assertEquals(-500.0, statuses.get("Коммунальные услуги").remaining());
    }
}