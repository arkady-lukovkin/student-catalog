package catalog;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

public final class ConsoleApplication {
    private final Scanner input;
    private final PrintStream output;
    private final StudentCatalog catalog = new StudentCatalog();
    private final StudentFileReader fileReader = new StudentFileReader();

    public ConsoleApplication(Scanner input, PrintStream output) {
        this.input = input;
        this.output = output;
    }

    public void run() {
        while (true) {
            output.println("""

                    ===== КАТАЛОГ СТУДЕНТОВ =====
                    1. Добавить студента вручную
                    2. Загрузить студентов из файла
                    3. Обработать данные (Stream API)
                    4. Показать всех студентов
                    5. Получить студента по номеру в списке
                    6. Удалить студента по номеру в списке
                    0. Выход""");
            try {
                int command = Integer.parseInt(read("Выберите пункт меню: "));
                switch (command) {
                    case 0 -> {
                        output.println("Завершение работы программы.");
                        return;
                    }
                    case 1 -> addManually();
                    case 2 -> loadFile();
                    case 3 -> process();
                    case 4 -> showAll();
                    case 5 -> output.println(StudentCatalog.format(catalog.get(readIndex())));
                    case 6 -> output.println("Удалён: "
                            + StudentCatalog.format(catalog.remove(readIndex())));
                    default -> output.println("Неизвестная команда.");
                }
            } catch (NumberFormatException e) {
                output.println("Ошибка: введите корректное число.");
            } catch (IndexOutOfBoundsException e) {
                output.println("Ошибка: такого номера в списке нет.");
            } catch (IllegalArgumentException e) {
                output.println("Ошибка: " + e.getMessage());
            } catch (NoSuchElementException e) {
                output.println("\nВвод завершён. Программа закрыта.");
                return;
            }
        }
    }

    private String read(String prompt) {
        output.print(prompt);
        return input.nextLine().strip();
    }

    private int readIndex() {
        int number = Integer.parseInt(read("Номер в списке (с 1): "));
        if (number < 1 || number > catalog.size()) {
            throw new IndexOutOfBoundsException();
        }
        return number - 1;
    }

    private void addManually() {
        String name = read("Имя: ");
        double grade = Student.parseGrade(read("Средний балл (0–5): "));
        String recordBookNumber = read("Номер зачётной книжки: ");
        catalog.add(new Student(name, grade, recordBookNumber));
        output.println("Студент добавлен.");
    }

    private void loadFile() {
        String filename = read("Путь к файлу CSV/TXT в UTF-8: ");
        try {
            StudentFileReader.LoadResult result = fileReader.read(Path.of(filename));
            result.errors().forEach(output::println);
            int added = 0;
            int skipped = result.errors().size();
            for (Student student : result.students()) {
                try {
                    catalog.add(student);
                    added++;
                } catch (IllegalArgumentException e) {
                    skipped++;
                    output.println("Пропущен студент " + student.name() + ": " + e.getMessage());
                }
            }
            output.printf("Добавлено: %d. Пропущено: %d.%n", added, skipped);
        } catch (IOException | InvalidPathException | SecurityException e) {
            output.println("Не удалось загрузить файл: " + e.getMessage());
        }
    }

    private void process() {
        double minimumGrade = Student.parseGrade(read("Минимальный средний балл (0–5): "));
        List<String> report = catalog.report(minimumGrade);
        output.println("Студенты с баллом не ниже " + minimumGrade + ", по возрастанию балла:");
        if (report.isEmpty()) {
            output.println("Подходящих студентов нет.");
        } else {
            report.forEach(output::println);
        }
    }

    private void showAll() {
        List<Student> students = catalog.all();
        if (students.isEmpty()) {
            output.println("Список пуст.");
        }
        for (int i = 0; i < students.size(); i++) {
            output.println((i + 1) + ". " + StudentCatalog.format(students.get(i)));
        }
    }
}
