import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    public static void main(String[] args) {
        // You can use print statements as follows for debugging, they'll be visible
        // when running tests.
        System.out.println("Logs from your program will appear here!");

        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(4221);
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
            System.out.println("accepted new connection");
            var templateResponse = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: %d\r\n\r\n%s";
            try (var reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                var path = reader.readLine().split(" ")[1];
                if (path.equals("/")) {
                    clientSocket.getOutputStream().write("HTTP/1.1 200 OK\r\n\r\n".getBytes());
                } else if (path.startsWith("/echo/")) {
                    var echoArgument = path.substring("/echo/".length());
                    var response = String.format(templateResponse, echoArgument.length(), echoArgument);
                    clientSocket.getOutputStream().write(response.getBytes());
                } else if (path.equals("/user-agent")) {
                    // Read headers until user agent is found
                    String userAgent = null;
                    String header = reader.readLine();
                    while (header != null) {
                        if (header.startsWith("User-Agent:")) {
                            userAgent = header.substring("User-Agent:".length()).trim();
                            break;
                        }
                        header = reader.readLine();
                    }
                    var response = String.format(templateResponse, userAgent.length(), userAgent);
                    clientSocket.getOutputStream().write(response.getBytes());
                } else {
                    clientSocket.getOutputStream().write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
                }
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }
}
