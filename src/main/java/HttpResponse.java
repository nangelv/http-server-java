import java.util.HashMap;
import java.util.Map;

public class HttpResponse {

    private final String statusMessage;
    private final int statusCode;
    private final Map<String, String> headers;
    private String body;

    public HttpResponse(String statusMessage, int statusCode) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.headers = new HashMap<>();
    }

    public String getBody() {
        return body;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getHeader(String header) {
        return headers.get(header);
    }

    public HttpResponse withHeader(String header, String value) {
        headers.put(header, value);
        return this;
    }

    public HttpResponse withBody(String body) {
        this.body = body;
        return this;
    }

    public static HttpResponse notFound() {
        return new HttpResponse("Not Found", 404);
    }

    public static HttpResponse ok() {
        return new HttpResponse("OK", 200);
    }

    public static HttpResponse created() {
        return new HttpResponse("Created", 201);
    }
}

