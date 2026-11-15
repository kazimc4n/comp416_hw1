import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class OpenTdbService {
    private final Map<String, Integer> categories = new LinkedHashMap<>();

    public OpenTdbService() throws IOException {
        loadCategories();
    }

    private void loadCategories() throws IOException {
        URL url = new URL("https://opentdb.com/api_category.php");
        HttpURLConnection connection =
                (HttpURLConnection) url.openConnection();

        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);

        try {
            if (connection.getResponseCode() != 200) {
                throw new IOException("Category request failed.");
            }

            try (Reader reader = new InputStreamReader(
                    connection.getInputStream(),
                    StandardCharsets.UTF_8)) {

                CategoryResponse response =
                        new Gson().fromJson(reader, CategoryResponse.class);

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
        } catch (RuntimeException e) {
            throw new IOException("Invalid category response.", e);
        } finally {
            connection.disconnect();
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

    private static class CategoryResponse {
        Category[] trivia_categories;
    }

    private static class Category {
        int id;
        String name;
    }
}
