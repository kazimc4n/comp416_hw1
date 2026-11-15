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

    public void remove(int id) {
        challenges.remove(id);
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
