import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Challenge implements Runnable {
    public final int id;

    private final ClientHandler creator;
    private final CreateCommand settings;
    private final OpenTdbService service;
    private final ChallengeManager manager;

    private final List<ClientHandler> players = new ArrayList<>();
    private final Map<ClientHandler, Integer> scores = new HashMap<>();
    private final Map<ClientHandler, String> answers = new HashMap<>();

    private final BlockingQueue<Runnable> requests =
            new LinkedBlockingQueue<>();

    private List<Question> questions;
    private volatile boolean started;
    private volatile boolean closed;
    private volatile int playerCount;
    private volatile int questionNumber;

    private boolean running = true;
    private long deadline;
    private String endReason = "All questions completed.";

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

    private synchronized boolean submit(Runnable request) {
        if (closed) {
            return false;
        }

        requests.add(request);
        return true;
    }

    public boolean requestJoin(ClientHandler player) {
        return submit(() -> handleJoin(player));
    }

    public boolean requestAnswer(ClientHandler player, String answer) {
        int number = questionNumber;
        long receivedAt = System.nanoTime();

        return submit(() ->
                handleAnswer(player, answer, number, receivedAt));
    }

    public boolean requestLeave(ClientHandler player) {
        return submit(() -> {
            if (!players.contains(player)) {
                reply(player, 400, "BAD_REQUEST",
                        "You are not in this challenge.");
                return;
            }

            reply(player, 200, "OK", "You left the challenge.");
            endReason = "Player " + player.getPlayerId() + " left.";
            running = false;
        });
    }

    public void stop() {
        submit(() -> {
            endReason = "A player disconnected.";
            running = false;
        });
    }

    public boolean matches(String category, String difficulty) {
        return !closed
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
            Integer categoryId =
                    service.getCategoryId(settings.category);

            if (categoryId == null) {
                reply(creator, 400, "BAD_REQUEST", "Unknown category.");
                return;
            }

            try {
                questions = service.getQuestions(
                        categoryId, settings.difficulty, settings.count);
            } catch (IOException e) {
                reply(creator, 503, "SERVICE_UNAVAILABLE",
                        "Could not load questions.");
                System.out.println("API error: " + e.getMessage());
                return;
            }

            players.add(creator);
            scores.put(creator, 0);
            playerCount = 1;

            // Process a disconnect received during the API request.
            processPending();

            if (!running) {
                return;
            }

            manager.register(id, this);

            reply(creator, 200, "OK",
                    "Challenge created. ID: " + id
                            + "\nWaiting for another player.");

            System.out.println("Challenge " + id + " created.");

            while (running) {
                if (!started) {
                    requests.take().run();
                    continue;
                }

                long remaining = deadline - System.nanoTime();

                if (remaining > 0) {
                    Runnable request = requests.poll(
                            remaining, TimeUnit.NANOSECONDS);

                    if (request != null) {
                        request.run();
                    }
                } else {
                    processPending();

                    if (running) {
                        finishQuestion();
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            endReason = "Challenge interrupted.";
        } catch (RuntimeException e) {
            endReason = "An internal error occurred.";
            broadcast(500, "INTERNAL_SERVER_ERROR", endReason);
            System.out.println("Challenge error: " + e.getMessage());
        } finally {
            List<Runnable> pending = new ArrayList<>();

            synchronized (this) {
                closed = true;
                running = false;
                requests.drainTo(pending);
            }

            manager.remove(id);

            // Resolve requests queued just before the challenge closed.
            for (Runnable request : pending) {
                request.run();
            }

            for (ClientHandler player : players) {
                player.clearChallenge(this);
            }

            creator.clearChallenge(this);

            if (!players.isEmpty()) {
                broadcast(212, "CHALLENGE_END",
                        endReason + "\n" + scoreText());
            }

            System.out.println("Challenge " + id + " closed.");
        }
    }

    private void processPending() {
        List<Runnable> pending = new ArrayList<>();
        requests.drainTo(pending);

        for (Runnable request : pending) {
            request.run();
        }
    }

    private void handleJoin(ClientHandler player) {
        if (!running || closed || started) {
            reply(player, 400, "BAD_REQUEST",
                    "Challenge has started or closed.");
            return;
        }

        if (!player.claimChallenge(this)) {
            reply(player, 400, "BAD_REQUEST",
                    "You are disconnected or already have a challenge.");
            return;
        }

        players.add(player);
        scores.put(player, 0);
        playerCount = players.size();

        reply(player, 200, "OK", "Joined challenge " + id + ".");

        if (!running) {
            return;
        }

        started = true;
        sendQuestion();
    }

    private void sendQuestion() {
        int nextNumber = questionNumber + 1;
        questionNumber = 0;
        answers.clear();

        Question question = questions.get(nextNumber - 1);

        StringBuilder body = new StringBuilder();
        body.append("Challenge ID: ").append(id);
        body.append("\nQuestion ID: ").append(nextNumber);
        body.append("\n").append(question.text);

        for (int i = 0; i < question.options.size(); i++) {
            body.append("\n").append(i + 1)
                    .append(". ").append(question.options.get(i));
        }

        body.append("\nTime: 30 seconds");
        body.append("\nUse /answer -id ").append(id)
                .append(" -answer \"option text\"");

        broadcast(210, "QUESTION", body.toString());

        deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        questionNumber = nextNumber;
    }

    private void handleAnswer(
            ClientHandler player, String answer,
            int number, long receivedAt) {
        if (!running || closed || !started
                || number == 0 || number != questionNumber
                || receivedAt > deadline) {
            reply(player, 400, "BAD_REQUEST",
                    "There is no open question for this answer.");
            return;
        }

        if (!players.contains(player)) {
            reply(player, 400, "BAD_REQUEST",
                    "You are not in this challenge.");
            return;
        }

        if (answers.containsKey(player)) {
            reply(player, 400, "BAD_REQUEST",
                    "You already answered this question.");
            return;
        }

        Question question = questions.get(questionNumber - 1);

        if (!question.options.contains(answer)) {
            reply(player, 400, "BAD_REQUEST",
                    "Answer must match an option exactly.");
            return;
        }

        answers.put(player, answer);
        reply(player, 200, "OK", "Answer accepted.");
    }

    private void finishQuestion() {
        Question question = questions.get(questionNumber - 1);

        StringBuilder result = new StringBuilder();
        result.append("Question ID: ").append(questionNumber);
        result.append("\nCorrect answer: ").append(question.correctAnswer);

        for (ClientHandler player : players) {
            String answer = answers.get(player);
            boolean correct = question.correctAnswer.equals(answer);

            if (correct) {
                scores.put(player, scores.get(player) + 1);
            }

            String outcome = answer == null
                    ? "No answer" : correct ? "Correct" : "Wrong";

            result.append("\nPlayer ").append(player.getPlayerId())
                    .append(": ").append(outcome);
        }

        result.append("\n").append(scoreText());
        broadcast(211, "RESULT", result.toString());

        if (!running) {
            return;
        }

        if (questionNumber == questions.size()) {
            int first = scores.get(players.get(0));
            int second = scores.get(players.get(1));

            if (first == second) {
                endReason = "Game finished. Draw.";
            } else {
                ClientHandler winner = first > second
                        ? players.get(0) : players.get(1);

                endReason = "Game finished. Winner: Player "
                        + winner.getPlayerId() + ".";
            }

            running = false;
        } else {
            sendQuestion();
        }
    }

    private String scoreText() {
        StringBuilder text = new StringBuilder("Scores:");

        for (ClientHandler player : players) {
            text.append("\nPlayer ").append(player.getPlayerId())
                    .append(": ").append(scores.get(player));
        }

        return text.toString();
    }

    private void broadcast(int code, String message, String body) {
        for (ClientHandler player : players) {
            reply(player, code, message, body);
        }
    }

    private void reply(
            ClientHandler player, int code, String message, String body) {
        try {
            player.send(code, message, body);
        } catch (IOException e) {
            if (players.contains(player)) {
                endReason = "A player disconnected.";
                running = false;
            }

            System.out.println("Could not send to player "
                    + player.getPlayerId() + ".");
        }
    }
}
