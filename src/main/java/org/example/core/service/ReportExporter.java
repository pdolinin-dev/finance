package org.example.core.service;


import org.example.core.model.Wallet;

import java.nio.file.Path;

public interface ReportExporter {
    void exportWalletSnapshot(Wallet wallet, Path path);
    Wallet importWalletSnapshot(Path path);
}
