import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {
    public static void main(String[] args) {
        int port = 41601;
        int nextClientId = 1;

        try {
            System.out.println("Loading categories...");

            OpenTdbService service = new OpenTdbService();

            System.out.println(
                    "Categories loaded: "
                            + service.getCategoryCount());

            try (ServerSocket serverSocket = new ServerSocket(port)) {
                ChallengeManager manager =
                        new ChallengeManager(service);

                Thread managerThread =
                        new Thread(manager, "challenge-manager");

                managerThread.setDaemon(true);
                managerThread.start();

                System.out.println(
                        "Server is listening on port " + port);

                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    int clientId = nextClientId++;

                    ClientHandler handler = new ClientHandler(
                            clientSocket, clientId, service, manager);

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
