import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.zip.GZIPOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

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
                var request = getRequest(reader);
                var response = routeRequest(request);
                handleCompression(request, response);
                sendResponse(clientSocket, response);
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void handleCompression(HttpRequest request, HttpResponse.Builder response) throws IOException {
        var acceptedEncodings = request.getHeader("Accept-Encoding");
        if (acceptedEncodings != null) {
            var encodings = acceptedEncodings.split(",");
            if (Arrays.stream(encodings).map(String::trim).anyMatch("gzip"::equals)) {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                try (GZIPOutputStream gzip = new GZIPOutputStream(outputStream)) {
                    gzip.write(response.getBody().getBytes(UTF_8));
                }
                var gzippedContent = outputStream.toString(UTF_8);
                response.withContentEncoding("gzip");
                System.out.println("Response before encoding " + response.getBody());
                response.withBody(gzippedContent);
                System.out.println("Response after encoding " + response.getBody());
            }
        }
    }

    private static HttpRequest getRequest(BufferedReader reader) throws IOException {
        var requestLine = reader.readLine();
        if (requestLine == null) return null;
        var requestArgs = requestLine.split(" ");
        var method = requestArgs[0];
        var path = requestArgs[1];
        var headers = readHeaders(reader);
        var body = readBody(reader, headers);
        return new HttpRequest(method, path, headers, body);
    }

    private static HttpResponse.Builder routeRequest(HttpRequest request) throws IOException {
        if (request.method().equals("GET")) {
            if (request.path().equals("/")) {
                return HttpResponse.ok();
            } else if (request.path().startsWith("/echo/")) {
                var echoArgument = request.path().substring("/echo/".length());
                return HttpResponse.ok()
                        .withContentType("text/plain")
                        .withBody(echoArgument);
            } else if (request.path().equals("/user-agent")) {
                return HttpResponse.ok()
                        .withContentType("text/plain")
                        .withBody(request.getHeader("User-Agent"));
            } else if(request.path().startsWith("/files/")) {
                var fileName = request.path().substring("/files/".length());
                var filePath = Path.of(SERVER_DIRECTORY, fileName);
                if (Files.exists(filePath)) {
                    var fileContent = Files.readString(filePath);
                    return HttpResponse.ok()
                            .withContentType("application/octet-stream")
                            .withBody(fileContent);
                }
            }
        } else if (request.method().equals("POST")) {
            if(request.path().startsWith("/files/")) {
                var fileName = request.path().substring("/files/".length());
                var filePath = Path.of(SERVER_DIRECTORY, fileName);
                if (!Files.exists(filePath.getParent())) {
                    System.out.println("Creating parent directory " + filePath.getParent());
                    Files.createDirectories(filePath.getParent());
                }
                Files.writeString(filePath, request.body());
                System.out.println("Saved new file at " + filePath + " with content:\n" + request.body());
                return HttpResponse.created();
            }
        }
        return HttpResponse.notFound();
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

    private static String readBody(BufferedReader reader, HashMap<String, String> headers) throws IOException {
        if (!headers.containsKey("Content-Length")) return null;
        var contentLength = Integer.parseInt(headers.get("Content-Length"));
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
