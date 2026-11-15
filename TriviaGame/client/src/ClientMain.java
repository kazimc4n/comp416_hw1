import java.io.IOException;
import java.net.Socket;

public class ClientMain {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 41601;

        System.out.println("Connecting to the server...");

        try (Socket socket = new Socket(host, port)) {
            System.out.println("Connected to the server.");
        } catch (IOException e) {
            System.out.println("Client error: " + e.getMessage());
        }

        System.out.println("Client stopped.");
    }
}
