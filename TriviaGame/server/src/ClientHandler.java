import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final int clientId;

    public ClientHandler(Socket socket, int clientId) {
        this.socket = socket;
        this.clientId = clientId;
    }

    @Override
    public void run() {
        System.out.println("Client " + clientId + " connected.");

        try (Socket connection = socket) {
            connection.setSoTimeout(90000);

            String command;

            while ((command = Protocol.readRequest(
                    connection.getInputStream())) != null) {

                System.out.println(
                        "Client " + clientId + ": " + command);

                if (command.equals("/exit")) {
                    send(200, "OK", "Goodbye.");
                    break;
                }

                send(400, "BAD_REQUEST",
                        "Unknown or not yet supported command.");
            }
        } catch (SocketTimeoutException e) {
            System.out.println("Client " + clientId + " timed out.");
        } catch (IOException e) {
            System.out.println(
                    "Client " + clientId + " error: " + e.getMessage());
        } finally {
            System.out.println("Client " + clientId + " disconnected.");
        }
    }

    public synchronized void send(
            int code, String message, String body) throws IOException {
        Protocol.writeResponse(
                socket.getOutputStream(), code, message, body);
    }
}
