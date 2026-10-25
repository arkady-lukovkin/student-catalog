package catalog;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Читает UTF-8 CSV/TXT с обязательной строкой заголовка и разделителем ;. */
public final class StudentFileReader {
    private static final String HEADER = "name;averageGrade;recordBookNumber";

    public record LoadResult(List<Student> students, List<String> errors) {
        public LoadResult {
            students = List.copyOf(students);
            errors = List.copyOf(errors);
        }
    }

    public LoadResult read(Path path) throws IOException {
        List<Student> students = new DynamicArrayList<>();
        List<String> errors = new DynamicArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            if (header != null && header.startsWith("\uFEFF")) {
                header = header.substring(1);
            }
            if (header == null || !HEADER.equals(header.strip())) {
                throw new IOException("Ожидается заголовок: " + HEADER);
            }
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                try {
                    String[] fields = line.split(";", -1);
                    if (fields.length != 3) {
                        throw new IllegalArgumentException("Ожидаются три поля через ;.");
                    }
                    students.add(new Student(fields[0], Student.parseGrade(fields[1]), fields[2]));
                } catch (IllegalArgumentException e) {
                    errors.add("Строка " + lineNumber + ": " + e.getMessage());
                }
            }
        }
        return new LoadResult(students, errors);
    }
}
