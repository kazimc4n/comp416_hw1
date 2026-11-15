import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ChallengeManager implements Runnable {
    private final OpenTdbService service;

    private final BlockingQueue<Challenge> createRequests =
            new LinkedBlockingQueue<>();

    private final ConcurrentMap<Integer, Challenge> challenges =
            new ConcurrentHashMap<>();

    private final AtomicInteger nextId = new AtomicInteger(1);

    public ChallengeManager(OpenTdbService service) {
        this.service = service;
    }

    public Challenge newChallenge(
            ClientHandler creator, CreateCommand settings) {
        return new Challenge(
                nextId.getAndIncrement(), creator, settings,
                service, this);
    }

    public void submit(Challenge challenge) {
        createRequests.add(challenge);
    }

    public void register(int id, Challenge challenge) {
        challenges.put(id, challenge);
    }

    public Challenge getChallenge(int id) {
        return challenges.get(id);
    }

    public void remove(int id) {
        challenges.remove(id);
    }

    public String list(String command) {
        List<String> tokens = CreateCommand.tokenize(command);

        if (tokens.isEmpty()
                || !tokens.get(0).equals("/list")
                || tokens.size() % 2 == 0) {
            throw new IllegalArgumentException(
                    "Use /list [-category \"name\"] [-difficulty easy]");
        }

        String category = null;
        String difficulty = null;

        for (int i = 1; i < tokens.size(); i += 2) {
            String option = tokens.get(i);
            String value = tokens.get(i + 1);

            switch (option) {
                case "-category":
                    if (category != null) {
                        throw new IllegalArgumentException(
                                "Duplicate category.");
                    }

                    if (service.getCategoryId(value) == null) {
                        throw new IllegalArgumentException(
                                "Unknown category.");
                    }

                    category = value;
                    break;

                case "-difficulty":
                    if (difficulty != null) {
                        throw new IllegalArgumentException(
                                "Duplicate difficulty.");
                    }

                    if (!value.equalsIgnoreCase("easy")
                            && !value.equalsIgnoreCase("medium")
                            && !value.equalsIgnoreCase("hard")) {
                        throw new IllegalArgumentException(
                                "Difficulty must be easy, medium or hard.");
                    }

                    difficulty = value;
                    break;

                default:
                    throw new IllegalArgumentException(
                            "Unknown option: " + option);
            }
        }

        StringBuilder result = new StringBuilder();

        for (Challenge challenge : challenges.values()) {
            if (challenge.matches(category, difficulty)) {
                if (result.length() > 0) {
                    result.append("\n");
                }

                result.append(challenge.getSummary());
            }
        }

        if (result.length() == 0) {
            return "No challenges found.";
        }

        return result.toString();
    }

    @Override
    public void run() {
        try {
            while (true) {
                Challenge challenge = createRequests.take();

                Thread thread = new Thread(
                        challenge, "challenge-" + challenge.id);

                thread.start();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
