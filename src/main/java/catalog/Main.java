package catalog;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        try (Scanner input = new Scanner(System.in, StandardCharsets.UTF_8)) {
            new ConsoleApplication(input, System.out).run();
        }
    }
}
