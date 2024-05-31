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
    Socket clientSocket = null;

    try {
      serverSocket = new ServerSocket(4221);
      // Since the tester restarts your program quite often, setting SO_REUSEADDR
      // ensures that we don't run into 'Address already in use' errors
      serverSocket.setReuseAddress(true);
      clientSocket = serverSocket.accept(); // Wait for connection from client.
      System.out.println("accepted new connection");
      try (var reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
        var path = reader.readLine().split(" ")[1];
        if (path.equals("/")) {
          clientSocket.getOutputStream().write("HTTP/1.1 200 OK\r\n\r\n".getBytes());
        } else if (path.startsWith("/echo/")) {
          var echoArgument = path.substring("/echo/".length());
          var templateResponse = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: %d\r\n\r\n%s";
          var response = String.format(templateResponse, echoArgument.length(), echoArgument);
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
