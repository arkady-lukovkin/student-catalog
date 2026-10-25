package catalog;

/** Студент: имя, средний балл и уникальный номер зачётной книжки. */
public record Student(String name, double averageGrade, String recordBookNumber) {
    public Student {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Имя не должно быть пустым.");
        }
        name = name.strip();
        if (name.length() > 100 || name.contains(";")
                || name.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Имя: до 100 символов, без ; и управляющих символов.");
        }
        if (!Double.isFinite(averageGrade) || averageGrade < 0 || averageGrade > 5) {
            throw new IllegalArgumentException("Средний балл должен быть числом от 0 до 5.");
        }
        if (recordBookNumber == null
                || !recordBookNumber.strip().matches("[\\p{L}\\p{N}-]{1,30}")) {
            throw new IllegalArgumentException("Номер зачётной книжки: 1–30 букв, цифр или дефисов.");
        }
        recordBookNumber = recordBookNumber.strip();
    }

    public static double parseGrade(String value) {
        return Double.parseDouble(value.strip().replace(',', '.'));
    }
}
