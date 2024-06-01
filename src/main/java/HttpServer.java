import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

public class HttpServer {

    private final int port;
    private final Controller controller;

    public HttpServer(int port, Controller controller) {
        this.port = port;
        this.controller = controller;
    }

    public void run() {
        System.out.printf("Server operational on port %d 🚜%n", port);

        try(var serverSocket = new ServerSocket(port)) {
            // Setting SO_REUSEADDR ensures that we don't run into 'Address already in use' errors
            // if we restart the server
            serverSocket.setReuseAddress(true);
            while (true) {
                var clientSocket = serverSocket.accept(); // Wait for connection from client.
                var thread = new Thread(() -> handleHttpConnection(clientSocket), "HTTP connection");
                thread.start();
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    private void handleHttpConnection(Socket clientSocket) {
        try {
            System.out.println("Accepted new connection from " + clientSocket.getRemoteSocketAddress());
            try (var httpRequestReader = new HttpRequestReader(clientSocket.getInputStream());
                 var httpRequestWriter = new HttpRequestWriter(clientSocket.getOutputStream())) {
                var request = httpRequestReader.read();
                var response = controller.routeRequest(request);
                updateCompressionHeaders(request, response);
                httpRequestWriter.write(response);
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateCompressionHeaders(HttpRequest request, HttpResponse response) {
        if (gzipIsAccepted(request)) {
            response.withHeader("Content-Encoding", "gzip");
        }
    }

    private boolean gzipIsAccepted(HttpRequest request) {
        if (!request.headers().containsKey("Accept-Encoding")) return false;
        var acceptedEncodings = request.getHeader("Accept-Encoding").split(",");
        return Arrays.stream(acceptedEncodings).map(String::trim).anyMatch("gzip"::equals);
    }
}
