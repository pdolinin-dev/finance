package org.example.cli;

import java.util.ArrayList;
import java.util.List;

public final class CommandParser {

    public List<String> tokenize(String line) {

        // поддержка кавычек: income add "Зарплата" 20000 "за январь"
        List<String> tokens = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
                continue;
            }
            if (!inQuotes && Character.isWhitespace(c)) {
                if (!cur.isEmpty()) {
                    tokens.add(cur.toString());
                    cur.setLength(0);
                }
                continue;
            }
            cur.append(c);
        }
        if (!cur.isEmpty()) tokens.add(cur.toString());
        return tokens;
    }
}
