import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OpenTdbService {
    private final Map<String, Integer> categories = new LinkedHashMap<>();

    public OpenTdbService() throws IOException {
        CategoryResponse response = getJson(
                "https://opentdb.com/api_category.php",
                CategoryResponse.class);

        if (response == null
                || response.trivia_categories == null
                || response.trivia_categories.length == 0) {
            throw new IOException("Category list is empty.");
        }

        for (Category category : response.trivia_categories) {
            if (category == null
                    || category.name == null
                    || category.name.trim().isEmpty()
                    || category.id <= 0) {
                throw new IOException("Invalid category data.");
            }

            categories.put(category.name, category.id);
        }
    }

    public Integer getCategoryId(String name) {
        for (Map.Entry<String, Integer> entry : categories.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }

        return null;
    }

    public int getCategoryCount() {
        return categories.size();
    }

    public List<Question> getQuestions(
            int categoryId, String difficulty, int count)
            throws IOException {

        String level = difficulty.toLowerCase(Locale.ROOT);

        if (count <= 0
                || (!level.equals("easy")
                && !level.equals("medium")
                && !level.equals("hard"))) {
            throw new IllegalArgumentException(
                    "Invalid question settings.");
        }

        String address = "https://opentdb.com/api.php"
                + "?amount=" + count
                + "&category=" + categoryId
                + "&difficulty=" + level
                + "&type=multiple"
                + "&encode=base64";

        QuestionResponse response = getJson(
                address, QuestionResponse.class);

        if (response == null || response.response_code == null) {
            throw new IOException("Invalid question response.");
        }

        if (response.response_code != 0) {
            throw new IOException(
                    "Question request failed. API code: "
                            + response.response_code);
        }

        if (response.results == null
                || response.results.length != count) {
            throw new IOException(
                    "The API did not return all requested questions.");
        }

        List<Question> questions = new ArrayList<>();

        for (QuestionData data : response.results) {
            if (data == null
                    || data.incorrect_answers == null
                    || data.incorrect_answers.length != 3) {
                throw new IOException("Invalid question data.");
            }

            List<String> wrongAnswers = new ArrayList<>();

            for (String answer : data.incorrect_answers) {
                wrongAnswers.add(decode(answer));
            }

            questions.add(new Question(
                    decode(data.question),
                    decode(data.correct_answer),
                    wrongAnswers));
        }

        return questions;
    }

    private String decode(String value) throws IOException {
        if (value == null || value.isEmpty()) {
            throw new IOException("Missing question text or answer.");
        }

        try {
            return new String(
                    Base64.getDecoder().decode(value),
                    StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid encoded text.", e);
        }
    }

    private <T> T getJson(String address, Class<T> responseType)
            throws IOException {
        HttpURLConnection connection =
                (HttpURLConnection) new URL(address).openConnection();

        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);

        try {
            int status = connection.getResponseCode();

            if (status != 200) {
                throw new IOException(
                        "API request failed. HTTP status: " + status);
            }

            try (Reader reader = new InputStreamReader(
                    connection.getInputStream(),
                    StandardCharsets.UTF_8)) {
                return new Gson().fromJson(reader, responseType);
            }
        } catch (RuntimeException e) {
            throw new IOException("Invalid API response.", e);
        } finally {
            connection.disconnect();
        }
    }

    private static class CategoryResponse {
        Category[] trivia_categories;
    }

    private static class Category {
        int id;
        String name;
    }

    private static class QuestionResponse {
        Integer response_code;
        QuestionData[] results;
    }

    private static class QuestionData {
        String question;
        String correct_answer;
        String[] incorrect_answers;
    }
}
