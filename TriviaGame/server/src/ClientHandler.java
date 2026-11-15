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

    public int getPlayerId() {
        return clientId;
    }

    @Override
    public void run() {
        System.out.println("Client " + clientId + " connected.");

        try (Socket connection = socket) {
            connection.setSoTimeout(90000);
            send(200, "OK", "Your player ID: " + clientId);

            String command;

            while ((command = Protocol.readRequest(
                    connection.getInputStream())) != null) {
                command = command.trim();

                System.out.println(
                        "Client " + clientId + ": " + command);

                try {
                    List<String> tokens =
                            CreateCommand.tokenize(command);

                    if (tokens.isEmpty()) {
                        throw new IllegalArgumentException(
                                "Empty command.");
                    }

                    if (tokens.get(0).equals("/exit")) {
                        if (tokens.size() != 1) {
                            throw new IllegalArgumentException(
                                    "Use /exit");
                        }

                        send(200, "OK", "Goodbye.");
                        break;
                    }

                    switch (tokens.get(0)) {
                        case "/create":
                            handleCreate(command);
                            break;

                        case "/list":
                            send(200, "OK", manager.list(command));
                            break;

                        case "/join":
                        case "/leave":
                        case "/answer":
                            handleChallengeCommand(tokens);
                            break;

                        default:
                            throw new IllegalArgumentException(
                                    "Unknown command.");
                    }
                } catch (IllegalArgumentException e) {
                    send(400, "BAD_REQUEST", e.getMessage());
                } catch (RuntimeException e) {
                    send(500, "INTERNAL_SERVER_ERROR",
                            "Could not process the command.");
                    System.out.println(
                            "Command error: " + e.getMessage());
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
        CreateCommand settings = CreateCommand.parse(command);

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

    private void handleChallengeCommand(List<String> tokens)
            throws IOException {
        String method = tokens.get(0);
        boolean answerCommand = method.equals("/answer");
        int expectedSize = answerCommand ? 5 : 3;

        if (tokens.size() != expectedSize
                || !tokens.get(1).equals("-id")
                || (answerCommand
                && !tokens.get(3).equals("-answer"))) {
            throw new IllegalArgumentException(
                    answerCommand
                            ? "Use /answer -id number -answer \"option text\""
                            : "Use " + method + " -id number");
        }

        int id;

        try {
            id = Integer.parseInt(tokens.get(2));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "ID must be a whole number.");
        }

        if (id <= 0) {
            throw new IllegalArgumentException(
                    "ID must be greater than zero.");
        }

        Challenge target = manager.getChallenge(id);

        if (target == null) {
            send(404, "NOT_FOUND", "Challenge was not found.");
            return;
        }

        boolean queued;

        switch (method) {
            case "/join":
                queued = target.requestJoin(this);
                break;

            case "/leave":
                queued = target.requestLeave(this);
                break;

            default:
                queued = target.requestAnswer(this, tokens.get(4));
        }

        if (!queued) {
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
