import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final int clientId;
    private final OpenTdbService service;
    private final ChallengeManager manager;

    private Challenge challenge;
    private boolean connected = true;

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

                List<String> tokens;

                try {
                    tokens = CreateCommand.tokenize(command);
                } catch (IllegalArgumentException e) {
                    send(400, "BAD_REQUEST", e.getMessage());
                    continue;
                }

                if (tokens.isEmpty()) {
                    send(400, "BAD_REQUEST", "Empty command.");
                    continue;
                }

                switch (tokens.get(0)) {
                    case "/create":
                        handleCreate(command);
                        break;

                    case "/list":
                        try {
                            send(200, "OK", manager.list(command));
                        } catch (IllegalArgumentException e) {
                            send(400, "BAD_REQUEST", e.getMessage());
                        }
                        break;

                    case "/join":
                        handleJoin(tokens);
                        break;

                    default:
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
            disconnect();

            System.out.println(
                    "Client " + clientId + " disconnected.");
        }
    }

    private void handleCreate(String command) throws IOException {
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

        Challenge created = manager.newChallenge(this, settings);

        if (!claimChallenge(created)) {
            send(400, "BAD_REQUEST", "You already have a challenge.");
            return;
        }

        manager.submit(created);
    }

    private void handleJoin(List<String> tokens) throws IOException {
        if (tokens.size() != 3 || !tokens.get(1).equals("-id")) {
            send(400, "BAD_REQUEST", "Use /join -id number");
            return;
        }

        int id;

        try {
            id = Integer.parseInt(tokens.get(2));
        } catch (NumberFormatException e) {
            send(400, "BAD_REQUEST", "ID must be a whole number.");
            return;
        }

        if (id <= 0) {
            send(400, "BAD_REQUEST", "ID must be greater than zero.");
            return;
        }

        Challenge target = manager.getChallenge(id);

        if (target == null) {
            send(404, "NOT_FOUND", "Challenge was not found.");
            return;
        }

        if (!target.requestJoin(this)) {
            send(404, "NOT_FOUND", "Challenge is closed.");
        }
    }

    public synchronized boolean claimChallenge(Challenge target) {
        if (!connected || challenge != null) {
            return false;
        }

        challenge = target;
        return true;
    }

    public synchronized void clearChallenge(Challenge finished) {
        if (challenge == finished) {
            challenge = null;
        }
    }

    private void disconnect() {
        Challenge current;

        synchronized (this) {
            connected = false;
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
