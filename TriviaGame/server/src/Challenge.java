import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Challenge implements Runnable {
    public final int id;

    private final ClientHandler creator;
    private final CreateCommand settings;
    private final OpenTdbService service;
    private final ChallengeManager manager;

    private final BlockingQueue<Runnable> requests =
            new LinkedBlockingQueue<>();

    private List<Question> questions;
    private volatile boolean stopped;

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

    public synchronized void stop() {
        stopped = true;
        requests.offer(() -> {});
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

            questions = service.getQuestions(
                    categoryId, settings.difficulty, settings.count);

            synchronized (this) {
                if (stopped) {
                    return;
                }

                manager.register(id, this);

                creator.send(
                        200,
                        "OK",
                        "Challenge created. ID: " + id
                                + "\nWaiting for another player.");
            }

            System.out.println(
                    "Challenge " + id + " created with "
                            + questions.size() + " questions.");

            while (!stopped) {
                requests.take().run();
            }
        } catch (IOException e) {
            System.out.println(
                    "Challenge " + id + " error: " + e.getMessage());

            if (!stopped) {
                try {
                    creator.send(
                            503,
                            "SERVICE_UNAVAILABLE",
                            "Could not prepare the challenge.");
                } catch (IOException sendError) {
                    System.out.println(
                            "Could not send the challenge error.");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            manager.remove(id);
            creator.clearChallenge(this);

            System.out.println("Challenge " + id + " closed.");
        }
    }
}
