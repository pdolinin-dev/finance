package org.example.core.model;

import java.util.*;

public final class Wallet {
    private String ownerLogin;
    private Set<String> categories = new HashSet<>();
    private Map<String, Double> budgetsByCategory = new HashMap<>();
    private List<Transaction> transactions = new ArrayList<>();

    public Wallet() {}

    public Wallet(String ownerLogin) {
        this.ownerLogin = ownerLogin;
    }

    public String getOwnerLogin() { return ownerLogin; }
    public Set<String> getCategories() { return categories; }
    public Map<String, Double> getBudgetsByCategory() { return budgetsByCategory; }
    public List<Transaction> getTransactions() { return transactions; }

    public void setOwnerLogin(String ownerLogin) { this.ownerLogin = ownerLogin; }
    public void setCategories(Set<String> categories) { this.categories = categories; }
    public void setBudgetsByCategory(Map<String, Double> budgetsByCategory) { this.budgetsByCategory = budgetsByCategory; }
    public void setTransactions(List<Transaction> transactions) { this.transactions = transactions; }
}
