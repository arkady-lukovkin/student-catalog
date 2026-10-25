package catalog;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Хранение и обработка студентов; консоль и файловый ввод вынесены отдельно. */
public final class StudentCatalog {
    private final List<Student> students = new DynamicArrayList<>();

    public void add(Student student) {
        Objects.requireNonNull(student, "student");
        if (students.stream().anyMatch(existing ->
                existing.recordBookNumber().equalsIgnoreCase(student.recordBookNumber()))) {
            throw new IllegalArgumentException("Номер зачётной книжки уже существует: "
                    + student.recordBookNumber());
        }
        students.add(student);
    }

    public Student get(int index) {
        return students.get(index);
    }

    public Student remove(int index) {
        return students.remove(index);
    }

    public int size() {
        return students.size();
    }

    public List<Student> all() {
        return List.copyOf(students);
    }

    /** filter -> sorted -> map -> toList: исходная коллекция не меняется. */
    public List<String> report(double minimumGrade) {
        if (!Double.isFinite(minimumGrade) || minimumGrade < 0 || minimumGrade > 5) {
            throw new IllegalArgumentException("Порог должен быть от 0 до 5.");
        }
        return students.stream()
                .filter(student -> student.averageGrade() >= minimumGrade)
                .sorted(Comparator.comparingDouble(Student::averageGrade)
                        .thenComparing(Student::name)
                        .thenComparing(Student::recordBookNumber))
                .map(StudentCatalog::format)
                .toList();
    }

    public static String format(Student student) {
        return String.format(Locale.ROOT, "%s | средний балл: %.2f | зачётная книжка: %s",
                student.name(), student.averageGrade(), student.recordBookNumber());
    }
}
