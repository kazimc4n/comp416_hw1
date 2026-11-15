import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Challenge implements Runnable {
    public final int id;

    private final ClientHandler creator;
    private final CreateCommand settings;
    private final OpenTdbService service;
    private final ChallengeManager manager;

    private final List<ClientHandler> players = new ArrayList<>();

    private final BlockingQueue<ClientHandler> joinRequests =
            new LinkedBlockingQueue<>();

    private List<Question> questions;

    private volatile boolean stopped;
    private volatile boolean started;
    private volatile int playerCount;

    public Challenge(
            int id,
            ClientHandler creator,
            CreateCommand settings,
            OpenTdbService service,
            ChallengeManager manager) {
        this.id = id;
        this.creator = creator;
        this.settings = settings;
        this.service = service;
        this.manager = manager;
    }

    public synchronized boolean requestJoin(ClientHandler player) {
        if (stopped) {
            return false;
        }

        joinRequests.add(player);
        return true;
    }

    public synchronized void stop() {
        stopped = true;
        joinRequests.offer(creator);
    }

    public boolean matches(String category, String difficulty) {
        return !stopped
                && !started
                && (category == null
                || settings.category.equalsIgnoreCase(category))
                && (difficulty == null
                || settings.difficulty.equalsIgnoreCase(difficulty));
    }

    public String getSummary() {
        return "ID: " + id
                + " | Category: " + settings.category
                + " | Difficulty: " + settings.difficulty
                + " | Players: " + playerCount + "/2";
    }

    @Override
    public void run() {
        try {
            if (stopped) {
                return;
            }

            Integer categoryId =
                    service.getCategoryId(settings.category);

            if (categoryId == null) {
                creator.send(400, "BAD_REQUEST", "Unknown category.");
                return;
            }

            try {
                questions = service.getQuestions(
                        categoryId, settings.difficulty, settings.count);
            } catch (IOException e) {
                creator.send(
                        503,
                        "SERVICE_UNAVAILABLE",
                        "Could not load questions.");
                System.out.println(
                        "Challenge " + id + " API error: " + e.getMessage());
                return;
            }

            players.add(creator);
            playerCount = players.size();

            if (stopped) {
                return;
            }

            manager.register(id, this);

            creator.send(
                    200,
                    "OK",
                    "Challenge created. ID: " + id
                            + "\nWaiting for another player.");

            System.out.println("Challenge " + id + " created.");

            while (!stopped) {
                ClientHandler player = joinRequests.take();

                if (stopped) {
                    if (player != creator) {
                        reply(player, 400, "BAD_REQUEST",
                                "Challenge is closed.");
                    }
                    break;
                }

                handleJoin(player);
            }
        } catch (IOException e) {
            System.out.println(
                    "Challenge " + id + " connection error: "
                            + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            stop();
            manager.remove(id);

            for (ClientHandler player : players) {
                player.clearChallenge(this);
            }

            creator.clearChallenge(this);

            ClientHandler pending;

            while ((pending = joinRequests.poll()) != null) {
                if (pending != creator) {
                    reply(pending, 400, "BAD_REQUEST",
                            "Challenge is closed.");
                }
            }

            System.out.println("Challenge " + id + " closed.");
        }
    }

    private void handleJoin(ClientHandler player) throws IOException {
        if (started) {
            reply(player, 400, "BAD_REQUEST",
                    "Challenge has already started.");
            return;
        }

        if (!player.claimChallenge(this)) {
            reply(player, 400, "BAD_REQUEST",
                    "You are disconnected or already have a challenge.");
            return;
        }

        players.add(player);
        playerCount = players.size();

        player.send(200, "OK", "Joined challenge " + id + ".");

        started = true;

        System.out.println("Challenge " + id + " started.");

        sendFirstQuestion();
    }

    private void sendFirstQuestion() throws IOException {
        Question question = questions.get(0);

        StringBuilder body = new StringBuilder();
        body.append("Challenge ID: ").append(id);
        body.append("\nQuestion ID: 1");
        body.append("\n").append(question.text);

        for (int i = 0; i < question.options.size(); i++) {
            body.append("\n")
                    .append(i + 1)
                    .append(". ")
                    .append(question.options.get(i));
        }

        for (ClientHandler player : players) {
            player.send(210, "QUESTION", body.toString());
        }
    }

    private void reply(
            ClientHandler player, int code, String message, String body) {
        try {
            player.send(code, message, body);
        } catch (IOException e) {
            System.out.println("Could not send a challenge response.");
        }
    }
}
