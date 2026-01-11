package org.example.core.service;

import org.example.TestWalletFactory;
import org.example.core.exception.NotFoundException;
import org.example.core.exception.ValidationException;
import org.example.core.model.TransactionType;
import org.example.core.model.Wallet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WalletServiceTest {

    private final WalletService service = new WalletService(new AlertService());

    @Test
    void addCategory_addsNewCategory() {
        Wallet w = TestWalletFactory.wallet("u1");
        service.addCategory(w, "Развлечения");
        assertTrue(w.getCategories().contains("Развлечения"));
    }

    @Test
    void setBudget_requiresCategoryExists() {
        Wallet w = TestWalletFactory.wallet("u1");
        assertThrows(NotFoundException.class, () -> service.setBudget(w, "НетКатегории", 1000));
    }

    @Test
    void setBudget_rejectsNonPositiveLimit() {
        Wallet w = TestWalletFactory.wallet("u1");
        assertThrows(ValidationException.class, () -> service.setBudget(w, "Еда", 0));
        assertThrows(ValidationException.class, () -> service.setBudget(w, "Еда", -10));
    }

    @Test
    void addIncome_requiresCategoryExists() {
        Wallet w = TestWalletFactory.wallet("u1");
        assertThrows(NotFoundException.class, () -> service.addIncome(w, "Бонус", 100, ""));
    }

    @Test
    void addIncome_rejectsNonPositiveAmount() {
        Wallet w = TestWalletFactory.wallet("u1");
        assertThrows(ValidationException.class, () -> service.addIncome(w, "Зарплата", 0, ""));
        assertThrows(ValidationException.class, () -> service.addIncome(w, "Зарплата", -1, ""));
    }

    @Test
    void addExpense_rejectsNonPositiveAmount() {
        Wallet w = TestWalletFactory.wallet("u1");
        assertThrows(ValidationException.class, () -> service.addExpense(w, "Еда", 0, ""));
        assertThrows(ValidationException.class, () -> service.addExpense(w, "Еда", -5, ""));
    }

    @Test
    void addIncome_addsTransaction() {
        Wallet w = TestWalletFactory.wallet("u1");
        CommandResult res = service.addIncome(w, "Зарплата", 20000, "январь");

        assertNotNull(res);
        assertEquals(1, w.getTransactions().size());
        assertEquals(TransactionType.INCOME, w.getTransactions().get(0).getType());
        assertEquals("Зарплата", w.getTransactions().get(0).getCategory());
        assertEquals(20000.0, w.getTransactions().get(0).getAmount());
    }

    @Test
    void addExpense_addsTransaction() {
        Wallet w = TestWalletFactory.wallet("u1");
        CommandResult res = service.addExpense(w, "Еда", 300, "кофе");

        assertNotNull(res);
        assertEquals(1, w.getTransactions().size());
        assertEquals(TransactionType.EXPENSE, w.getTransactions().get(0).getType());
        assertEquals("Еда", w.getTransactions().get(0).getCategory());
        assertEquals(300.0, w.getTransactions().get(0).getAmount());
    }

    @Test
    void renameCategory_movesBudgetAndTransactions() {
        Wallet w = TestWalletFactory.wallet("u1");
        // бюджет на "Еда"
        service.setBudget(w, "Еда", 4000);
        // расход по "Еда"
        service.addExpense(w, "Еда", 500, "");

        service.renameCategory(w, "Еда", "Питание");

        assertFalse(w.getCategories().contains("Еда"));
        assertTrue(w.getCategories().contains("Питание"));
        assertFalse(w.getBudgetsByCategory().containsKey("Еда"));
        assertEquals(4000.0, w.getBudgetsByCategory().get("Питание"));

        assertEquals("Питание", w.getTransactions().get(0).getCategory());
    }

    @Test
    void renameCategory_rejectsWhenOldMissing() {
        Wallet w = TestWalletFactory.wallet("u1");
        assertThrows(NotFoundException.class, () -> service.renameCategory(w, "Нет", "Новое"));
    }

    @Test
    void renameCategory_rejectsWhenNewAlreadyExists() {
        Wallet w = TestWalletFactory.wallet("u1");
        w.getCategories().add("Развлечения");
        assertThrows(ValidationException.class, () -> service.renameCategory(w, "Еда", "Развлечения"));
    }
}