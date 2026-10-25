package catalog;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

/** Самостоятельный набор проверок: только JDK, без сторонних зависимостей. */
public final class CatalogTest {
    private static int checks;

    private CatalogTest() {
    }

    public static void main(String[] args) throws IOException {
        testDynamicArray();
        testValidationAndReport();
        testFiles();
        testConsole();
        System.out.println("Все проверки пройдены: " + checks);
    }

    private static void testDynamicArray() {
        List<Integer> actual = new DynamicArrayList<>();
        List<Integer> expected = new ArrayList<>();
        Random random = new Random(20261025);
        for (int i = 0; i < 100; i++) {
            actual.add(i);
            expected.add(i);
        }
        check(actual.equals(expected), "Расширение массива сохраняет элементы");
        for (int step = 0; step < 2_000; step++) {
            int action = random.nextInt(3);
            int index = actual.isEmpty() ? 0 : random.nextInt(actual.size());
            if (actual.isEmpty() || action == 0) {
                int value = random.nextInt(1000);
                actual.add(index, value);
                expected.add(index, value);
            } else if (action == 1) {
                check(actual.remove(index).equals(expected.remove(index)), "Результат remove");
            } else {
                int value = random.nextInt(1000);
                check(actual.set(index, value).equals(expected.set(index, value)), "Результат set");
            }
            check(actual.equals(expected), "Сравнение с ArrayList, шаг " + step);
        }
        check(actual.stream().toList().equals(expected), "Stream собственной коллекции");
        expect(IndexOutOfBoundsException.class, () -> actual.get(-1));
        expect(IndexOutOfBoundsException.class, () -> actual.get(actual.size()));
        expect(IndexOutOfBoundsException.class, () -> actual.remove(actual.size()));
        expect(IndexOutOfBoundsException.class, () -> actual.add(actual.size() + 1, 0));
        var iterator = actual.iterator();
        actual.add(7);
        expect(ConcurrentModificationException.class, iterator::next);
        actual.clear();
        check(actual.isEmpty(), "Очистка коллекции");
        actual.add(null);
        check(actual.remove(0) == null && actual.isEmpty(), "Поддержка null и удаление последнего");
    }

    private static void testValidationAndReport() {
        Student student = new Student(" Анна ", 4.5, " DEMO-1 ");
        check(student.name().equals("Анна") && student.recordBookNumber().equals("DEMO-1"),
                "Нормализация пробелов");
        check(Student.parseGrade("4,5") == 4.5, "Десятичная запятая");
        expect(IllegalArgumentException.class, () -> new Student(" ", 4, "1"));
        expect(IllegalArgumentException.class, () -> new Student("А;Б", 4, "1"));
        expect(IllegalArgumentException.class, () -> new Student("А\nБ", 4, "1"));
        expect(IllegalArgumentException.class, () -> new Student("Анна", -1, "1"));
        expect(IllegalArgumentException.class, () -> new Student("Анна", 5.1, "1"));
        expect(IllegalArgumentException.class, () -> new Student("Анна", Double.NaN, "1"));
        expect(IllegalArgumentException.class,
                () -> new Student("Анна", Double.POSITIVE_INFINITY, "1"));
        expect(IllegalArgumentException.class, () -> new Student("Анна", 4, "bad id"));
        StudentCatalog catalog = new StudentCatalog();
        Student low = new Student("Вера", 3, "DEMO-2");
        Student same = new Student("Борис", 4.5, "DEMO-3");
        catalog.add(same);
        catalog.add(low);
        catalog.add(student);
        List<Student> before = catalog.all();
        check(catalog.report(4).equals(List.of(StudentCatalog.format(student),
                StudentCatalog.format(same))), "Фильтрация, сортировка и map в отчёте");
        check(catalog.all().equals(before), "Отчёт не меняет исходную коллекцию");
        check(catalog.report(5).isEmpty(), "Пустой результат");
        check(catalog.report(0).size() == 3, "Нижняя граница фильтра");
        expect(IllegalArgumentException.class, () -> catalog.report(Double.NaN));
        expect(IllegalArgumentException.class, () -> catalog.report(6));
        expect(IllegalArgumentException.class,
                () -> catalog.add(new Student("Дубликат", 4, "demo-1")));
        expect(UnsupportedOperationException.class, () -> catalog.all().clear());
        check(catalog.get(1).equals(low), "Получение по индексу");
        check(catalog.remove(1).equals(low) && catalog.size() == 2, "Удаление студента");
    }

    private static void testFiles() throws IOException {
        StudentFileReader reader = new StudentFileReader();
        var good = reader.read(Path.of("data/students.csv"));
        check(good.students().size() == 5 && good.errors().isEmpty(), "Загрузка UTF-8 CSV");
        var bad = reader.read(Path.of("data/invalid-students.csv"));
        check(bad.students().size() == 2 && bad.errors().size() == 5, "Ошибки отдельных строк");
        check(bad.errors().get(0).startsWith("Строка 3:"), "Номер ошибочной строки");
        Path temporary = Files.createTempFile("student-catalog-", ".csv");
        try {
            Files.writeString(temporary, "\uFEFFname;averageGrade;recordBookNumber\n\nА;0;1\nБ;5;2\n");
            var result = reader.read(temporary);
            check(result.students().size() == 2, "BOM, пустые строки и границы балла");
            Files.writeString(temporary, "wrong;header\n");
            expect(IOException.class, () -> reader.read(temporary));
            Files.writeString(temporary, "");
            expect(IOException.class, () -> reader.read(temporary));
            Files.writeString(temporary, "name;averageGrade;recordBookNumber\n");
            check(reader.read(temporary).students().isEmpty(), "Файл только с заголовком");
        } finally {
            Files.deleteIfExists(temporary);
        }
        expect(IOException.class, () -> reader.read(Path.of("data/does-not-exist.csv")));
    }

    private static void testConsole() {
        String scenario = """
                x
                99
                1
                Тестовый студент
                4,5
                MANUAL-1
                2
                data/students.csv
                2
                data/invalid-students.csv
                2
                data/does-not-exist.csv
                3
                4
                4
                5
                1
                6
                1
                5
                999
                0
                """;
        String output = runConsole(scenario);
        check(output.contains("Студент добавлен."), "Добавление вручную");
        check(output.contains("Добавлено: 5. Пропущено: 0."), "Импорт корректного файла");
        check(output.contains("Добавлено: 1. Пропущено: 6."), "Ошибки и дубликаты при импорте");
        check(output.contains("Не удалось загрузить файл:"), "Ошибка чтения обработана");
        check(output.contains("Ошибка: введите корректное число."), "Нечисловая команда");
        check(output.contains("Неизвестная команда."), "Неизвестная команда");
        check(output.contains("Удалён: Тестовый студент"), "Удаление через меню");
        check(output.contains("такого номера в списке нет"), "Ошибка индекса обработана");
        check(output.endsWith("Завершение работы программы.\n"), "Явный выход из цикла");
        check(runConsole("1\nАнна\n").contains("Ввод завершён"), "EOF в середине ввода");
        check(runConsole("4\n3\n5\n0\n").contains("Подходящих студентов нет"), "Пустой список");
        check(runConsole("1\nАнна\nNaN\n1\n0\n").contains("от 0 до 5"), "Невалидный балл");
        check(runConsole("2\ndata/students.csv\n2\ndata/students.csv\n0\n")
                .contains("Добавлено: 0. Пропущено: 5."), "Повторный импорт без дубликатов");
    }

    private static String runConsole(String input) {
        var buffer = new ByteArrayOutputStream();
        try (var scanner = new Scanner(input);
                var output = new PrintStream(buffer, true, StandardCharsets.UTF_8)) {
            new ConsoleApplication(scanner, output).run();
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void expect(Class<? extends Throwable> type, ThrowingRunnable action) {
        checks++;
        try {
            action.run();
        } catch (Exception e) {
            if (type.isInstance(e)) {
                return;
            }
            throw new AssertionError("Ожидалось " + type.getName() + ", получено " + e, e);
        }
        throw new AssertionError("Ожидалось исключение " + type.getName());
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
