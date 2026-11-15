import java.io.IOException;
import java.io.InputStream;
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

            InputStream input = connection.getInputStream();

            while (input.read() != -1) {
                // Message handling will be added in the next step.
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
}
