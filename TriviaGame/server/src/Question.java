import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Question {
    public final String text;
    public final String correctAnswer;
    public final List<String> options;

    public Question(
            String text, String correctAnswer, List<String> wrongAnswers) {
        this.text = text;
        this.correctAnswer = correctAnswer;

        List<String> answers = new ArrayList<>(wrongAnswers);
        answers.add(correctAnswer);
        Collections.shuffle(answers);

        this.options = Collections.unmodifiableList(answers);
    }
}
