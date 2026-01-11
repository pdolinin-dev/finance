package org.example;

import org.example.core.model.Wallet;

public final class TestWalletFactory {
    private TestWalletFactory() {}

    public static Wallet wallet(String login) {
        Wallet w = new Wallet(login);
        // часто используемые категории
        w.getCategories().add("Еда");
        w.getCategories().add("Зарплата");
        w.getCategories().add("Перевод");
        return w;
    }
}
