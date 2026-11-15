import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final int clientId;
    private final OpenTdbService service;
    private final ChallengeManager manager;

    private Challenge challenge;

    public ClientHandler(
            Socket socket,
            int clientId,
            OpenTdbService service,
            ChallengeManager manager) {
        this.socket = socket;
        this.clientId = clientId;
        this.service = service;
        this.manager = manager;
    }

    @Override
    public void run() {
        System.out.println("Client " + clientId + " connected.");

        try (Socket connection = socket) {
            connection.setSoTimeout(90000);

            String command;

            while ((command = Protocol.readRequest(
                    connection.getInputStream())) != null) {

                command = command.trim();

                System.out.println(
                        "Client " + clientId + ": " + command);

                if (command.equals("/exit")) {
                    send(200, "OK", "Goodbye.");
                    break;
                }

                if (command.equals("/list")
                        || command.startsWith("/list ")
                        || command.startsWith("/list\t")) {
                    try {
                        send(200, "OK", manager.list(command));
                    } catch (IllegalArgumentException e) {
                        send(400, "BAD_REQUEST", e.getMessage());
                    }
                    continue;
                }

                if (command.equals("/create")
                        || command.startsWith("/create ")
                        || command.startsWith("/create\t")) {
                    handleCreate(command);
                } else {
                    send(
                            400,
                            "BAD_REQUEST",
                            "Unknown or not yet supported command.");
                }
            }
        } catch (SocketTimeoutException e) {
            System.out.println("Client " + clientId + " timed out.");
        } catch (IOException e) {
            System.out.println(
                    "Client " + clientId + " error: " + e.getMessage());
        } finally {
            stopChallenge();

            System.out.println(
                    "Client " + clientId + " disconnected.");
        }
    }

    private synchronized void handleCreate(String command)
            throws IOException {
        if (challenge != null) {
            send(
                    400,
                    "BAD_REQUEST",
                    "You already have a challenge.");
            return;
        }

        CreateCommand settings;

        try {
            settings = CreateCommand.parse(command);
        } catch (IllegalArgumentException e) {
            send(400, "BAD_REQUEST", e.getMessage());
            return;
        }

        if (service.getCategoryId(settings.category) == null) {
            send(400, "BAD_REQUEST", "Unknown category.");
            return;
        }

        challenge = manager.newChallenge(this, settings);
        manager.submit(challenge);
    }

    public synchronized void clearChallenge(Challenge finished) {
        if (challenge == finished) {
            challenge = null;
        }
    }

    private void stopChallenge() {
        Challenge current;

        synchronized (this) {
            current = challenge;
        }

        if (current != null) {
            current.stop();
        }
    }

    public synchronized void send(
            int code, String message, String body) throws IOException {
        Protocol.writeResponse(
                socket.getOutputStream(), code, message, body);
    }
}
