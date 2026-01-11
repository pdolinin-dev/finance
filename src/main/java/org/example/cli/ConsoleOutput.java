package org.example.cli;

public class ConsoleOutput implements Output {

    @Override
    public void println(String s) {
        System.out.println(s);
    }
}
