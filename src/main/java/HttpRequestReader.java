import java.io.*;
import java.util.HashMap;

public class HttpRequestReader implements Closeable {

    private final BufferedReader reader;

    public HttpRequestReader(InputStream inputStream) {
        this.reader = new BufferedReader(new InputStreamReader(inputStream));;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    public HttpRequest read() throws IOException {
        var requestLine = reader.readLine();
        var requestArgs = requestLine.split(" ");
        var method = requestArgs[0];
        var path = requestArgs[1];
        var headers = readHeaders();
        var contentLength = Integer.parseInt(headers.getOrDefault("Content-Length", "-1"));
        var body = readBody(contentLength);
        return new HttpRequest(method, path, headers, body);
    }

    private HashMap<String, String> readHeaders() throws IOException {
        var headers = new HashMap<String, String>();
        var headerLine = reader.readLine();
        while (headerLine != null && !headerLine.isEmpty()) {
            var keyValuePair = headerLine.split(": ");
            headers.put(keyValuePair[0], keyValuePair[1]);
            headerLine = reader.readLine();
        }
        return headers;
    }

    private String readBody(int contentLength) throws IOException {
        if (contentLength <= 0) return null;
        var content = new char[contentLength];
        if (reader.read(content) == -1) {
            throw new RuntimeException("No content found");
        }
        return new String(content);
    }
}
