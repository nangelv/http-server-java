import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

    public static String SERVER_DIRECTORY;

    public static void main(String[] args) {
        if (args.length > 1 && args[0].equals("--directory")) {
            SERVER_DIRECTORY = args[1];
        }
        var port = 4221;
        System.out.printf("Server operational on port %d 🚜%n", port);

        try(var serverSocket = new ServerSocket(port)) {
            // Since the tester restarts your program quite often, setting SO_REUSEADDR
            // ensures that we don't run into 'Address already in use' errors
            serverSocket.setReuseAddress(true);
            while (true) {
                Socket clientSocket = serverSocket.accept(); // Wait for connection from client.
                var thread = new Thread(() -> handleHttpConnection(clientSocket), "HTTP connection");
                thread.start();
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    private static void handleHttpConnection(Socket clientSocket) {
        try {
            System.out.println("Accepted new connection from " + clientSocket.getRemoteSocketAddress());
            try (var reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                var path = reader.readLine().split(" ")[1];
                if (path.equals("/")) {
                    okResponse(clientSocket);
                } else if (path.startsWith("/echo/")) {
                    var echoArgument = path.substring("/echo/".length());
                    okResponse(clientSocket, echoArgument);
                } else if (path.equals("/user-agent")) {
                    String userAgent = readHeader(reader, "User-Agent");
                    okResponse(clientSocket, userAgent);
                } else if(path.startsWith("/files/")) {
                    var fileName = path.substring("/files/".length());
                    var filePath = Path.of(SERVER_DIRECTORY, fileName);
                    if (Files.exists(filePath)) {
                        var fileContent = Files.readString(filePath);
                        okResponse(clientSocket, fileContent, "application/octet-stream");
                    } else {
                        notFoundResponse(clientSocket);
                    }
                } else {
                    notFoundResponse(clientSocket);
                }
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    private static String readHeader(BufferedReader reader, String headerToFind) throws IOException {
        if (!headerToFind.endsWith(":")) {
            headerToFind += ":";
        }
        String headerLine = reader.readLine();
        String headerValue = null;
        while (headerLine != null) {
            // Implementation is case-sensitive, HTTP headers are case-insensitive by specification
            if (headerLine.startsWith(headerToFind)) {
                headerValue = headerLine.substring(headerToFind.length()).trim();
                break;
            }
            headerLine = reader.readLine();
        }
        return headerValue;
    }

    private static void notFoundResponse(Socket clientSocket) throws IOException {
        clientSocket.getOutputStream().write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
    }

    private static void okResponse(Socket clientSocket, String body, String contentType) throws IOException {
        var okResponse = "HTTP/1.1 200 OK\r\n";
        if (body != null) {
            okResponse += "Content-Type: %s\r\nContent-Length: %d\r\n\r\n%s".formatted(contentType, body.length(), body);
        } else {
            okResponse += "\r\n";
        }
        System.out.println("Full ok response");
        System.out.println(okResponse);
        clientSocket.getOutputStream().write(okResponse.getBytes());
    }

    private static void okResponse(Socket clientSocket, String body) throws IOException {
        okResponse(clientSocket, body, "text/plain");
    }

    private static void okResponse(Socket clientSocket) throws IOException {
        okResponse(clientSocket, null);
    }
}
