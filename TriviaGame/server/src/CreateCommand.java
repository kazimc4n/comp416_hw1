import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CreateCommand {
    public final String category;
    public final String difficulty;
    public final int count;

    private CreateCommand(String category, String difficulty, int count) {
        this.category = category;
        this.difficulty = difficulty;
        this.count = count;
    }

    public static CreateCommand parse(String command) {
        List<String> tokens = tokenize(command);

        if (tokens.size() != 7 || !tokens.get(0).equals("/create")) {
            throw new IllegalArgumentException(
                    "Use /create -category \"name\" -difficulty easy -count 5");
        }

        String category = null;
        String difficulty = null;
        String countText = null;

        for (int i = 1; i < tokens.size(); i += 2) {
            String option = tokens.get(i);
            String value = tokens.get(i + 1);

            switch (option) {
                case "-category":
                    if (category != null) {
                        throw new IllegalArgumentException(
                                "Duplicate category.");
                    }
                    category = value;
                    break;

                case "-difficulty":
                    if (difficulty != null) {
                        throw new IllegalArgumentException(
                                "Duplicate difficulty.");
                    }
                    difficulty = value;
                    break;

                case "-count":
                    if (countText != null) {
                        throw new IllegalArgumentException(
                                "Duplicate count.");
                    }
                    countText = value;
                    break;

                default:
                    throw new IllegalArgumentException(
                            "Unknown option: " + option);
            }
        }

        if (category == null || category.trim().isEmpty()
                || difficulty == null || countText == null) {
            throw new IllegalArgumentException(
                    "Category, difficulty and count are required.");
        }

        difficulty = difficulty.toLowerCase(Locale.ROOT);

        if (!difficulty.equals("easy")
                && !difficulty.equals("medium")
                && !difficulty.equals("hard")) {
            throw new IllegalArgumentException(
                    "Difficulty must be easy, medium or hard.");
        }

        int count;

        try {
            count = Integer.parseInt(countText);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Count must be a whole number.");
        }

        if (count <= 0) {
            throw new IllegalArgumentException(
                    "Count must be greater than zero.");
        }

        return new CreateCommand(category.trim(), difficulty, count);
    }

    private static List<String> tokenize(String command) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        boolean started = false;

        for (char character : command.toCharArray()) {
            if (character == '"') {
                quoted = !quoted;
                started = true;
            } else if (Character.isWhitespace(character) && !quoted) {
                if (started) {
                    tokens.add(current.toString());
                    current.setLength(0);
                    started = false;
                }
            } else {
                current.append(character);
                started = true;
            }
        }

        if (quoted) {
            throw new IllegalArgumentException(
                    "Missing closing quote.");
        }

        if (started) {
            tokens.add(current.toString());
        }

        return tokens;
    }
}
