import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class Protocol {
    private static final String END = "416END";
    private static final int MAX_SIZE = 1_000_000;

    public static void writeRequest(OutputStream output, String command)
            throws IOException {
        if (command.contains(END)) {
            throw new IOException("Command contains the message marker.");
        }

        byte[] data = command.getBytes(StandardCharsets.UTF_8);

        if (data.length > MAX_SIZE) {
            throw new IOException("Command is too long.");
        }

        output.write(data);
        output.write(END.getBytes(StandardCharsets.US_ASCII));
        output.flush();
    }

    public static String readRequest(InputStream input) throws IOException {
        byte[] data = readUntil(input, END, MAX_SIZE);

        if (data == null) {
            return null;
        }

        return new String(data, StandardCharsets.UTF_8);
    }

    public static void writeResponse(
            OutputStream output, int code, String message, String body)
            throws IOException {
        byte[] data = body.getBytes(StandardCharsets.UTF_8);

        if (data.length > MAX_SIZE) {
            throw new IOException("Body is too long.");
        }

        String header = code + " " + message + " " + data.length + "\r\n";

        output.write(header.getBytes(StandardCharsets.US_ASCII));
        output.write(data);
        output.write(END.getBytes(StandardCharsets.US_ASCII));
        output.flush();
    }

    public static Response readResponse(InputStream input) throws IOException {
        byte[] headerBytes = readUntil(input, "\r\n", 1024);

        if (headerBytes == null) {
            return null;
        }

        String header = new String(
                headerBytes, StandardCharsets.US_ASCII);
        String[] parts = header.split(" ");

        if (parts.length != 3) {
            throw new IOException("Invalid response header.");
        }

        int code;
        int length;

        try {
            code = Integer.parseInt(parts[0]);
            length = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            throw new IOException("Invalid response numbers.");
        }

        if (length < 0 || length > MAX_SIZE) {
            throw new IOException("Invalid body length.");
        }

        byte[] body = readExactly(input, length);
        byte[] marker = readExactly(input, END.length());

        String markerText = new String(
                marker, StandardCharsets.US_ASCII);

        if (!END.equals(markerText)) {
            throw new IOException("Invalid message marker.");
        }

        return new Response(
                code, parts[1],
                new String(body, StandardCharsets.UTF_8));
    }

    private static byte[] readExactly(InputStream input, int length)
            throws IOException {
        byte[] data = new byte[length];
        int total = 0;

        while (total < length) {
            int count = input.read(data, total, length - total);

            if (count == -1) {
                throw new EOFException("Connection closed during a message.");
            }

            total += count;
        }

        return data;
    }

    private static byte[] readUntil(
            InputStream input, String delimiter, int limit)
            throws IOException {
        byte[] marker = delimiter.getBytes(StandardCharsets.US_ASCII);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int matched = 0;

        while (true) {
            int value = input.read();

            if (value == -1) {
                if (buffer.size() == 0) {
                    return null;
                }

                throw new EOFException("Connection closed during a message.");
            }

            buffer.write(value);

            if (value == marker[matched]) {
                matched++;
            } else {
                matched = value == marker[0] ? 1 : 0;
            }

            if (matched == marker.length) {
                byte[] data = buffer.toByteArray();
                int length = data.length - marker.length;

                if (length > limit) {
                    throw new IOException("Message is too long.");
                }

                byte[] result = new byte[length];
                System.arraycopy(data, 0, result, 0, length);
                return result;
            }

            if (buffer.size() > limit + marker.length) {
                throw new IOException("Message is too long.");
            }
        }
    }

    public static class Response {
        public final int code;
        public final String message;
        public final String body;

        public Response(int code, String message, String body) {
            this.code = code;
            this.message = message;
            this.body = body;
        }
    }
}
