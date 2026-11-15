import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ProtocolTest {
    public static void main(String[] args) throws IOException {
        testRequests();
        testResponses();

        System.out.println("All protocol tests passed.");
    }

    private static void testRequests() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        Protocol.writeRequest(output, "/list");
        Protocol.writeRequest(output, "/exit");

        ByteArrayInputStream input =
                new ByteArrayInputStream(output.toByteArray());

        check("/list".equals(Protocol.readRequest(input)),
                "First request");

        check("/exit".equals(Protocol.readRequest(input)),
                "Second request");

        check(Protocol.readRequest(input) == null,
                "End of request stream");
    }

    private static void testResponses() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        String body = "Hello \u00e7\u011f\u0131\u015f\nMarker: 416END";

        Protocol.writeResponse(output, 200, "OK", body);
        Protocol.writeResponse(output, 200, "OK", "");

        ByteArrayInputStream input =
                new ByteArrayInputStream(output.toByteArray()) {
                    @Override
                    public synchronized int read(
                            byte[] data, int offset, int length) {
                        return super.read(
                                data, offset, Math.min(length, 2));
                    }
                };

        Protocol.Response first = Protocol.readResponse(input);

        check(first != null
                        && first.code == 200
                        && "OK".equals(first.message),
                "Response header");

        check(body.equals(first.body),
                "UTF-8 body with a message marker");

        Protocol.Response second = Protocol.readResponse(input);

        check(second != null && second.body.isEmpty(),
                "Empty body and consecutive responses");

        check(Protocol.readResponse(input) == null,
                "End of response stream");
    }

    private static void check(boolean condition, String name) {
        if (!condition) {
            throw new IllegalStateException("FAIL: " + name);
        }

        System.out.println("PASS: " + name);
    }
}
