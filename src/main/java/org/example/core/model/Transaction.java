package org.example.core.model;

import java.time.LocalDateTime;
import java.util.UUID;

public final class Transaction {
    private UUID id;
    private TransactionType type;
    private String category;
    private double amount;
    private LocalDateTime createdAt;
    private String note;
    private String counterpartyLogin;

    public Transaction() { }

    public Transaction(TransactionType type, String category, double amount, LocalDateTime createdAt, String note, String counterpartyLogin) {
        this.id = UUID.randomUUID();
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.createdAt = createdAt;
        this.note = note;
        this.counterpartyLogin = counterpartyLogin;
    }

    public UUID getId() { return id; }
    public TransactionType getType() { return type; }
    public String getCategory() { return category; }
    public double getAmount() { return amount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getNote() { return note; }
    public String getCounterpartyLogin() { return counterpartyLogin; }

    public void setId(UUID id) { this.id = id; }
    public void setType(TransactionType type) { this.type = type; }
    public void setCategory(String category) { this.category = category; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setNote(String note) { this.note = note; }
    public void setCounterpartyLogin(String counterpartyLogin) { this.counterpartyLogin = counterpartyLogin; }
}
