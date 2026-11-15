import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class ClientMain {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 41601;

        System.out.println("Connecting to the server...");

        try (Socket socket = new Socket(host, port);
             Scanner console = new Scanner(System.in)) {

            System.out.println("Connected to the server.");
            System.out.println("Press Enter to disconnect.");

            if (console.hasNextLine()) {
                console.nextLine();
            }
        } catch (IOException e) {
            System.out.println("Client error: " + e.getMessage());
        }

        System.out.println("Client stopped.");
    }
}
