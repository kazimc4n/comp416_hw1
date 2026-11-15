import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class ClientMain {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 41601;

        System.out.println("Connecting to the server...");

        try (Socket socket = new Socket(host, port)) {
            System.out.println("Connected to the server.");
            System.out.println("Enter a command. Use /exit to leave.");

            Thread inputThread = new Thread(
                    () -> readCommands(socket), "console-input");

            inputThread.setDaemon(true);
            inputThread.start();

            Protocol.Response response;

            while ((response = Protocol.readResponse(
                    socket.getInputStream())) != null) {

                System.out.println(
                        response.code + " " + response.message);

                if (!response.body.isEmpty()) {
                    System.out.println(response.body);
                }
            }

            System.out.println("Server closed the connection.");
        } catch (IOException e) {
            System.out.println("Client error: " + e.getMessage());
        }

        System.out.println("Client stopped.");
    }

    private static void readCommands(Socket socket) {
        Scanner console = new Scanner(System.in);

        try {
            while (console.hasNextLine()) {
                String command = console.nextLine().trim();

                if (command.isEmpty()) {
                    continue;
                }

                Protocol.writeRequest(
                        socket.getOutputStream(), command);

                if (command.equals("/exit")) {
                    return;
                }
            }

            socket.shutdownOutput();
        } catch (IOException e) {
            if (!socket.isClosed()) {
                System.out.println(
                        "Input error: " + e.getMessage());

                try {
                    socket.close();
                } catch (IOException closeError) {
                    System.out.println(
                            "Close error: " + closeError.getMessage());
                }
            }
        }
    }
}
