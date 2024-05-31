import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
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
            System.out.println("Starting at " + LocalDateTime.now());
            try (var reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                var requestLine = reader.readLine().split(" ");
                var method = requestLine[0];
                var path = requestLine[1];
                if (method.equals("GET")) {
                    if (path.equals("/")) {
                        sendResponse(clientSocket, HttpResponse.ok());
                    } else if (path.startsWith("/echo/")) {
                        var echoArgument = path.substring("/echo/".length());
                        sendResponse(clientSocket, HttpResponse.ok()
                                .withContentType("text/plain")
                                .withBody(echoArgument));
                    } else if (path.equals("/user-agent")) {
                        var headers = readHeaders(reader);
                        var userAgent = headers.get("User-Agent");
                        sendResponse(clientSocket, HttpResponse.ok()
                                .withContentType("text/plain")
                                .withBody(userAgent));
                    } else if(path.startsWith("/files/")) {
                        var fileName = path.substring("/files/".length());
                        var filePath = Path.of(SERVER_DIRECTORY, fileName);
                        if (Files.exists(filePath)) {
                            var fileContent = Files.readString(filePath);
                            sendResponse(clientSocket, HttpResponse.ok()
                                    .withContentType("application/octet-stream")
                                    .withBody(fileContent));
                        } else {
                            sendResponse(clientSocket, HttpResponse.notFound());
                        }
                    } else {
                        sendResponse(clientSocket, HttpResponse.notFound());
                    }
                } else if (method.equals("POST")) {
                    if(path.startsWith("/files/")) {
                        var fileName = path.substring("/files/".length());
                        var filePath = Path.of(SERVER_DIRECTORY, fileName);
                        var headers = readHeaders(reader);
                        var body = readBody(reader, Integer.parseInt(headers.get("Content-Length")));
                        if (!Files.exists(filePath.getParent())) {
                            System.out.println("Creating parent directory " + filePath.getParent());
                            Files.createDirectories(filePath.getParent());
                        }
                        Files.writeString(filePath, body);
                        System.out.println("Saved new file at " + filePath + " with content:\n" + body);
                        sendResponse(clientSocket, HttpResponse.created());
                    } else {
                        sendResponse(clientSocket, HttpResponse.notFound());
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
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

    private static String readBody(BufferedReader reader, int contentLength) throws IOException {
        var content = new char[contentLength];
        if (reader.read(content) == -1) {
            throw new RuntimeException("No content found");
        }
        return new String(content);
    }

    private static void sendResponse(Socket clientSocket, HttpResponse.Builder response) throws IOException {
        clientSocket.getOutputStream().write(response.build().getResponseBytes());
    }
}
