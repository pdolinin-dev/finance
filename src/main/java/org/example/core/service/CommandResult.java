package org.example.core.service;

import java.util.List;

public record CommandResult(List<String> messages) {
    public static CommandResult ok() { return new CommandResult(List.of()); }
    public static CommandResult withMessages(List<String> msgs) { return new CommandResult(msgs); }
}
