package org.example.infra.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.core.model.Wallet;
import org.example.core.service.ReportExporter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class JsonReportExporter implements ReportExporter {
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    public void exportWalletSnapshot(Wallet wallet, Path path) {
        try {
            if (path.getParent() != null) Files.createDirectories(path.getParent());
            mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), wallet);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось экспортировать: " + e.getMessage(), e);
        }
    }

    @Override
    public Wallet importWalletSnapshot(Path path) {
        try {
            return mapper.readValue(path.toFile(), Wallet.class);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось импортировать: " + e.getMessage(), e);
        }
    }
}
