package org.example.cli;

import org.example.core.exception.AppException;
import org.example.core.model.Wallet;
import org.example.core.service.*;

import java.nio.file.Path;
import java.util.*;

public final class CommandLoop {
    private final Output out;
    private Output currentOut;

    private final CommandParser parser;
    private final AuthService auth;
    private final WalletService walletService;
    private final ReportService reportService;
    private final TransferService transferService;
    private final ReportExporter reportExporter;

    private String currentLogin = null;
    private Wallet currentWallet = null;

    public CommandLoop(Output out,
                       CommandParser parser,
                       AuthService auth,
                       WalletService walletService,
                       ReportService reportService,
                       TransferService transferService,
                       ReportExporter reportExporter) {
        this.out = out;
        this.currentOut = out;
        this.parser = parser;
        this.auth = auth;
        this.walletService = walletService;
        this.reportService = reportService;
        this.transferService = transferService;
        this.reportExporter = reportExporter;
    }

    public void run() {
        currentOut.println("Finance CLI (Java) — введи help");
        Scanner sc = new Scanner(System.in);

        while (true) {
            currentOut.println(prompt());
            String line;
            try {
                line = sc.nextLine();
            } catch (NoSuchElementException e) {
                safeExit();
                return;
            }

            line = line == null ? "" : line.trim();
            if (line.isEmpty()) continue;

            List<String> t = parser.tokenize(line);
            String cmd = t.get(0).toLowerCase(Locale.ROOT);

            try {
                if (cmd.equals("help")) {
                    printHelp();
                    continue;
                }
                if (cmd.equals("exit")) {
                    safeExit();
                    return;
                }
                if (cmd.equals("output")) {
                    handleOutput(t);
                    continue;
                }

                // auth
                if (cmd.equals("register")) {
                    handleRegister(t);
                    continue;
                }
                if (cmd.equals("login")) {
                    handleLogin(t);
                    continue;
                }
                if (cmd.equals("logout")) {
                    handleLogout();
                    continue;
                }

                requireAuth();

                // categories
                if (cmd.equals("category")) {
                    handleCategory(t);
                    continue;
                }

                // budget
                if (cmd.equals("budget")) {
                    handleBudget(t);
                    continue;
                }

                // income/expense
                if (cmd.equals("income") || cmd.equals("expense")) {
                    handleIncomeExpense(t);
                    continue;
                }

                // report
                if (cmd.equals("report")) {
                    handleReport(t);
                    continue;
                }

                // export/import
                if (cmd.equals("export")) {
                    handleExport(t);
                    continue;
                }
                if (cmd.equals("import")) {
                    handleImport(t);
                    continue;
                }

                // transfer
                if (cmd.equals("transfer")) {
                    handleTransfer(t);
                    continue;
                }

                currentOut.println("Неизвестная команда. Введи help.");
            } catch (AppException e) {
                currentOut.println("Ошибка: " + e.getMessage());
            } catch (Exception e) {
                currentOut.println("Непредвиденная ошибка: " + e.getMessage());
            }
        }
    }

    private String prompt() {
        return currentLogin == null ? "> " : currentLogin + "> ";
    }

    private void printHelp() {
        currentOut.println("""
Команды:
  help
  exit

Авторизация:
  register <login> <password>
  login <login> <password>
  logout

Вывод:
  output console
  output file <path>

Категории:
  category add <name>
  category rename <old> <new>
  category list

Бюджеты:
  budget set <category> <limit>
  budget edit <category> <newLimit>
  budget list

Операции:
  income add <category> <amount> [note...]
  expense add <category> <amount> [note...]

Отчёты:
  report summary
  report categories
  report budget
  report categories --only=Еда,Такси

Экспорт/импорт:
  export json <path>
  import json <path>

Переводы:
  transfer <toLogin> <amount> [note...]

Примеры с кавычками:
  category add "Коммунальные услуги"
  expense add "Коммунальные услуги" 3000 "за декабрь"
""");
    }

    private void handleOutput(List<String> t) {
        if (t.size() < 2) throw new org.example.core.exception.ValidationException("output console|file <path>");
        String mode = t.get(1).toLowerCase(Locale.ROOT);
        if (mode.equals("console")) {
            currentOut = out;
            currentOut.println("Вывод переключен в консоль.");
            return;
        }
        if (mode.equals("file")) {
            if (t.size() < 3) throw new org.example.core.exception.ValidationException("output file <path>");
            currentOut = new FileOutput(Path.of(t.get(2)));
            currentOut.println("Вывод переключен в файл: " + t.get(2));
            return;
        }
        throw new org.example.core.exception.ValidationException("output console|file <path>");
    }

    private void handleRegister(List<String> t) {
        if (t.size() != 3) throw new org.example.core.exception.ValidationException("register <login> <password>");
        auth.register(t.get(1), t.get(2));
        currentOut.println("Пользователь зарегистрирован: " + t.get(1));
    }

    private void handleLogin(List<String> t) {
        if (t.size() != 3) throw new org.example.core.exception.ValidationException("login <login> <password>");
        if (currentLogin != null) currentOut.println("Вы уже вошли как " + currentLogin + ". Используйте logout.");
        var session = auth.login(t.get(1), t.get(2));
        currentLogin = session.login();
        currentWallet = session.wallet();
        currentOut.println("Вход выполнен: " + currentLogin);
    }

    private void handleLogout() {
        if (currentLogin == null) {
            currentOut.println("Вы не авторизованы.");
            return;
        }
        auth.saveWallet(currentWallet);
        currentOut.println("Данные сохранены. Выход: " + currentLogin);
        currentLogin = null;
        currentWallet = null;
    }

    private void safeExit() {
        if (currentLogin != null && currentWallet != null) {
            try {
                auth.saveWallet(currentWallet);
                currentOut.println("Данные сохранены. Выход из приложения.");
            } catch (Exception ignored) {}
        }
        currentOut.println("Пока!");
    }

    private void requireAuth() {
        if (currentLogin == null || currentWallet == null)
            throw new org.example.core.exception.AuthException("Сначала выполните login.");
    }

    private void handleCategory(List<String> t) {
        if (t.size() < 2) throw new org.example.core.exception.ValidationException("category add|rename|list ...");
        String sub = t.get(1).toLowerCase(Locale.ROOT);
        if (sub.equals("add")) {
            if (t.size() < 3) throw new org.example.core.exception.ValidationException("category add <name>");
            walletService.addCategory(currentWallet, joinFrom(t, 2));
            currentOut.println("Категория добавлена.");
            return;
        }
        if (sub.equals("rename")) {
            if (t.size() < 4) throw new org.example.core.exception.ValidationException("category rename <old> <new>");
            walletService.renameCategory(currentWallet, t.get(2), t.get(3));
            currentOut.println("Категория переименована.");
            return;
        }
        if (sub.equals("list")) {
            currentOut.println("Категории:");
            for (String c : new TreeSet<>(currentWallet.getCategories())) {
                currentOut.println("  - " + c);
            }
            return;
        }
        throw new org.example.core.exception.ValidationException("category add|rename|list ...");
    }

    private void handleBudget(List<String> t) {
        if (t.size() < 2) throw new org.example.core.exception.ValidationException("budget set|edit|list ...");
        String sub = t.get(1).toLowerCase(Locale.ROOT);
        if (sub.equals("set") || sub.equals("edit")) {
            if (t.size() < 4) throw new org.example.core.exception.ValidationException("budget set <category> <limit>");
            String category = t.get(2);
            double limit = parseDouble(t.get(3), "limit");
            walletService.setBudget(currentWallet, category, limit);
            currentOut.println("Бюджет установлен: " + category + " = " + limit);
            return;
        }
        if (sub.equals("list")) {
            currentOut.println("Бюджеты:");
            var m = currentWallet.getBudgetsByCategory();
            if (m.isEmpty()) {
                currentOut.println("  (нет)");
                return;
            }
            for (var e : new TreeMap<>(m).entrySet()) {
                currentOut.println("  - " + e.getKey() + ": " + e.getValue());
            }
            return;
        }
        throw new org.example.core.exception.ValidationException("budget set|edit|list ...");
    }

    private void handleIncomeExpense(List<String> t) {
        if (t.size() < 2) throw new org.example.core.exception.ValidationException("income add <category> <amount> [note...]");
        String sub = t.get(1).toLowerCase(Locale.ROOT);
        if (!sub.equals("add")) throw new org.example.core.exception.ValidationException("income|expense add ...");
        if (t.size() < 4) throw new org.example.core.exception.ValidationException("income|expense add <category> <amount> [note...]");

        String type = t.get(0).toLowerCase(Locale.ROOT);
        String category = t.get(2);
        double amount = parseDouble(t.get(3), "amount");
        String note = t.size() > 4 ? joinFrom(t, 4) : "";

        CommandResult res;
        if (type.equals("income")) {
            res = walletService.addIncome(currentWallet, category, amount, note);
            currentOut.println("Доход добавлен.");
        } else {
            res = walletService.addExpense(currentWallet, category, amount, note);
            currentOut.println("Расход добавлен.");
        }
        for (String msg : res.messages()) currentOut.println("⚠ " + msg);
    }

    private void handleReport(List<String> t) {
        if (t.size() < 2) throw new org.example.core.exception.ValidationException("report summary|categories|budget ...");
        String sub = t.get(1).toLowerCase(Locale.ROOT);

        if (sub.equals("summary")) {
            double inc = reportService.totalIncome(currentWallet);
            double exp = reportService.totalExpense(currentWallet);
            double bal = inc - exp;
            currentOut.println("Общий доход: " + inc);
            currentOut.println("Общие расходы: " + exp);
            currentOut.println("Баланс: " + bal);
            return;
        }

        if (sub.equals("categories")) {
            Set<String> only = parseOnlyFilter(t);
            var incomeMap = reportService.incomeByCategory(currentWallet, only);
            var expenseMap = reportService.expenseByCategory(currentWallet, only);
            var missing = reportService.missingCategories(currentWallet, only);

            currentOut.println("Доходы по категориям:");
            if (incomeMap.isEmpty()) currentOut.println("  (нет)");
            for (var e : new TreeMap<>(incomeMap).entrySet())
                currentOut.println("  " + e.getKey() + ": " + e.getValue());

            currentOut.println("Расходы по категориям:");
            if (expenseMap.isEmpty()) currentOut.println("  (нет)");
            for (var e : new TreeMap<>(expenseMap).entrySet())
                currentOut.println("  " + e.getKey() + ": " + e.getValue());

            if (!missing.isEmpty()) currentOut.println("⚠ Не найдены категории: " + String.join(", ", missing));
            return;
        }

        if (sub.equals("budget")) {
            var statuses = reportService.budgetStatuses(currentWallet);
            currentOut.println("Бюджет по категориям:");
            if (statuses.isEmpty()) {
                currentOut.println("  (нет)");
                return;
            }
            // сортировка по названию категории
            var keys = new ArrayList<>(statuses.keySet());
            Collections.sort(keys);
            for (String cat : keys) {
                var st = statuses.get(cat);
                currentOut.println(cat + ": " + st.limit() + ", Потрачено: " + st.spent() + ", Оставшийся бюджет: " + st.remaining());
            }
            return;
        }

        throw new org.example.core.exception.ValidationException("report summary|categories|budget");
    }

    private void handleExport(List<String> t) {
        if (t.size() < 3) throw new org.example.core.exception.ValidationException("export json <path>");
        String fmt = t.get(1).toLowerCase(Locale.ROOT);
        String path = t.get(2);
        if (!fmt.equals("json")) throw new org.example.core.exception.ValidationException("Поддерживается только export json <path>");
        reportExporter.exportWalletSnapshot(currentWallet, Path.of(path));
        currentOut.println("Экспортировано: " + path);
    }

    private void handleImport(List<String> t) {
        if (t.size() < 3) throw new org.example.core.exception.ValidationException("import json <path>");
        String fmt = t.get(1).toLowerCase(Locale.ROOT);
        String path = t.get(2);
        if (!fmt.equals("json")) throw new org.example.core.exception.ValidationException("Поддерживается только import json <path>");
        Wallet imported = reportExporter.importWalletSnapshot(Path.of(path));
        // применяем к текущему кошельку (без смены владельца)
        walletService.replaceWalletState(currentWallet, imported);
        currentOut.println("Импортировано в текущий кошелёк: " + path);
    }

    private void handleTransfer(List<String> t) {
        if (t.size() < 3) throw new org.example.core.exception.ValidationException("transfer <toLogin> <amount> [note...]");
        String to = t.get(1);
        double amount = parseDouble(t.get(2), "amount");
        String note = t.size() > 3 ? joinFrom(t, 3) : "";
        CommandResult res = transferService.transfer(currentLogin, currentWallet, to, amount, note);
        currentOut.println("Перевод выполнен -> " + to + ": " + amount);
        for (String msg : res.messages()) currentOut.println("⚠ " + msg);
    }

    private static String joinFrom(List<String> t, int i) {
        StringBuilder sb = new StringBuilder();
        for (int k = i; k < t.size(); k++) {
            if (k > i) sb.append(' ');
            sb.append(t.get(k));
        }
        return sb.toString().trim();
    }

    private static double parseDouble(String s, String field) {
        try {
            return Double.parseDouble(s.replace(",", "."));
        } catch (NumberFormatException e) {
            throw new org.example.core.exception.ValidationException("Некорректное число для " + field + ": " + s);
        }
    }

    private static Set<String> parseOnlyFilter(List<String> t) {
        for (String token : t) {
            if (token.startsWith("--only=")) {
                String raw = token.substring("--only=".length()).trim();
                if (raw.isEmpty()) return Set.of();
                String[] parts = raw.split(",");
                Set<String> out = new HashSet<>();
                for (String p : parts) {
                    String v = p.trim();
                    if (!v.isEmpty()) out.add(v);
                }
                return out;
            }
        }
        return null; // null => без фильтра
    }
}
