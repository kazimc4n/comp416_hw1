public class CreateCommandTest {
    public static void main(String[] args) {
        CreateCommand command = CreateCommand.parse(
                "/create -category \"General Knowledge\""
                        + " -difficulty \"Hard\" -count 5");

        check(command.category.equals("General Knowledge")
                        && command.difficulty.equals("hard")
                        && command.count == 5,
                "Valid command with quotes");

        CreateCommand reordered = CreateCommand.parse(
                "/create -count 3 -difficulty easy -category Sports");

        check(reordered.category.equals("Sports")
                        && reordered.count == 3,
                "Different option order");

        reject(
                "/create -category Sports -difficulty wrong -count 5",
                "Invalid difficulty");

        reject(
                "/create -category Sports -difficulty easy -count 0",
                "Invalid count");

        reject(
                "/create -category Sports -difficulty easy -count abc",
                "Non-numeric count");

        reject(
                "/create -category Sports -difficulty easy",
                "Missing option");

        reject(
                "/create -category Sports -category History -count 5",
                "Duplicate option");

        reject(
                "/create -category \"Sports -difficulty easy -count 5",
                "Missing closing quote");

        System.out.println("All create command tests passed.");
    }

    private static void reject(String command, String name) {
        try {
            CreateCommand.parse(command);
        } catch (IllegalArgumentException e) {
            System.out.println("PASS: " + name);
            return;
        }

        throw new IllegalStateException("FAIL: " + name);
    }

    private static void check(boolean condition, String name) {
        if (!condition) {
            throw new IllegalStateException("FAIL: " + name);
        }

        System.out.println("PASS: " + name);
    }
}
