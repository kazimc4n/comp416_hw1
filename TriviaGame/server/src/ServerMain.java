import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {
    public static void main(String[] args) {
        int port = 41601;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is listening on port " + port);
            System.out.println("Waiting for a client...");

            try (Socket clientSocket = serverSocket.accept()) {
                System.out.println("Client connected.");
            }

            System.out.println("Server stopped.");
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }
}
