package org.example.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class FileOutput implements Output {
    private final Path path;

    public FileOutput(Path path) {
        this.path = path;
    }

    @Override
    public void println(String s) {
        try {
            Files.createDirectories(path.getParent() == null ? Path.of(".") : path.getParent());
            Files.writeString(path, s + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.out.println("Не удалось записать в файл вывода: " + e.getMessage());
        }
    }
}
