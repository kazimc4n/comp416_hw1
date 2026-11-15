import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {
    public static void main(String[] args) {
        int port = 41601;
        int nextClientId = 1;

        try {
            System.out.println("Loading categories...");

            OpenTdbService triviaService = new OpenTdbService();

            System.out.println(
                    "Categories loaded: "
                            + triviaService.getCategoryCount());

            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println(
                        "Server is listening on port " + port);

                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    int clientId = nextClientId++;

                    ClientHandler handler =
                            new ClientHandler(clientSocket, clientId);

                    Thread thread =
                            new Thread(handler, "client-" + clientId);

                    thread.start();
                }
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }
}
