import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.HashMap;

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
                var requestLine = reader.readLine().split(" ");
                var method = requestLine[0];
                var path = requestLine[1];
                if (method.equals("GET")) {
                    if (path.equals("/")) {
                        okResponse(clientSocket);
                    } else if (path.startsWith("/echo/")) {
                        var echoArgument = path.substring("/echo/".length());
                        okResponse(clientSocket, echoArgument);
                    } else if (path.equals("/user-agent")) {
                        var headers = readHeaders(reader);
                        var userAgent = headers.get("User-Agent");
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
                } else if (method.equals("POST")) {
                    if(path.startsWith("/files/")) {
                        var fileName = path.substring("/files/".length());
                        var filePath = Path.of(SERVER_DIRECTORY, fileName);
                        readHeaders(reader);
                        var body = readBody(reader);
                        System.out.println("Creating parent directory " + filePath.getParent());
                        Files.createDirectories(filePath.getParent());
//                        if (!Files.exists(filePath.getParent())) {
//                            System.out.println("Creating parent directory " + filePath.getParent());
//                            Files.createDirectories(filePath.getParent());
//                        }
                        Files.writeString(filePath, body);
                        System.out.println("Saved new file at " + filePath);
                        System.out.println("With content:\n" + body);
                        okResponse(clientSocket, 201);
                    } else {
                        notFoundResponse(clientSocket);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static HashMap<String, String> readHeaders(BufferedReader reader) throws IOException {
        var headers = new HashMap<String, String>();
        var headerLine = reader.readLine();
        while (headerLine != null && !headerLine.isEmpty()) {
            var keyValuePair = headerLine.split(": ");
            headers.put(keyValuePair[0], keyValuePair[1]);
            headerLine = reader.readLine();
        }
        return headers;
    }

    private static String readBody(BufferedReader reader) throws IOException {
        var stringBuilder = new StringBuilder();
        var ch = reader.read();
        while (ch != -1) {
            stringBuilder.append((char) ch);
            ch = reader.read();
            System.out.print(ch);
        }
        return stringBuilder.toString();
    }

    private static void notFoundResponse(Socket clientSocket) throws IOException {
        clientSocket.getOutputStream().write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
    }

    private static void okResponse(Socket clientSocket, String body, String contentType, int statusCode) throws IOException {
        var okResponse = "HTTP/1.1 %d OK\r\n".formatted(statusCode);
        if (body != null) {
            okResponse += "Content-Type: %s\r\nContent-Length: %d\r\n\r\n%s".formatted(contentType, body.length(), body);
        } else {
            okResponse += "\r\n";
        }
        System.out.println("Full ok response");
        System.out.println(okResponse);
        clientSocket.getOutputStream().write(okResponse.getBytes());
    }

    private static void okResponse(Socket clientSocket, int statusCode) throws IOException {
        okResponse(clientSocket, null, null, statusCode);
    }

    private static void okResponse(Socket clientSocket, String body) throws IOException {
        okResponse(clientSocket, body, "text/plain", 200);
    }

    private static void okResponse(Socket clientSocket, String body, String contentType) throws IOException {
        okResponse(clientSocket, body, contentType, 200);
    }

    private static void okResponse(Socket clientSocket) throws IOException {
        okResponse(clientSocket, null, null, 200);
    }
}
