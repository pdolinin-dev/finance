package org.example;

import org.example.cli.CommandLoop;
import org.example.cli.CommandParser;
import org.example.cli.ConsoleOutput;
import org.example.cli.Output;
import org.example.core.service.*;
import org.example.infra.export.JsonReportExporter;
import org.example.infra.store.FileWalletStore;
import org.example.infra.store.InMemoryUserStore;
import org.example.infra.store.UserStore;
import org.example.infra.store.WalletStore;

import java.nio.file.Path;

public class Main {

    public static void main(String[] args) {

        Output output = new ConsoleOutput();

        UserStore userStore = new InMemoryUserStore();
        WalletStore walletStore = new FileWalletStore(Path.of("data"));

        AlertService alertService = new AlertService();
        AuthService authService = new AuthService(userStore, walletStore);
        WalletService walletService = new WalletService(alertService);
        ReportService reportService = new ReportService();
        TransferService transferService = new TransferService(userStore, walletStore, alertService);

        ReportExporter reportExporter = new JsonReportExporter();

        CommandParser parser = new CommandParser();
        CommandLoop loop = new CommandLoop(
                output,
                parser,
                authService,
                walletService,
                reportService,
                transferService,
                reportExporter
        );

        loop.run();
    }

}