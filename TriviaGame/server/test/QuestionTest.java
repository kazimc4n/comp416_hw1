import java.io.IOException;
import java.util.List;

public class QuestionTest {
    public static void main(String[] args) {
        try {
            System.out.println("Loading categories...");

            OpenTdbService service = new OpenTdbService();
            Integer categoryId = service.getCategoryId(
                    "General Knowledge");

            if (categoryId == null) {
                throw new IOException("Test category was not found.");
            }

            System.out.println("Loading 2 questions...");

            List<Question> questions =
                    service.getQuestions(categoryId, "easy", 2);

            if (questions.size() != 2) {
                throw new IllegalStateException(
                        "Wrong number of questions.");
            }

            System.out.println("PASS: Requested question count");

            for (int i = 0; i < questions.size(); i++) {
                Question question = questions.get(i);

                if (question.text.trim().isEmpty()
                        || question.options.size() != 4
                        || !question.options.contains(
                                question.correctAnswer)) {
                    throw new IllegalStateException(
                            "Invalid question or options.");
                }

                System.out.println();
                System.out.println(
                        "Question " + (i + 1) + ": " + question.text);

                for (String option : question.options) {
                    System.out.println("- " + option);
                }
            }

            System.out.println();
            System.out.println("PASS: Question text and options");
            System.out.println("Question loading test passed.");
        } catch (IOException e) {
            System.err.println("Test failed: " + e.getMessage());
            System.exit(1);
        }
    }
}
