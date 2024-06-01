import java.util.Map;

public record HttpRequest(
        String method,
        String path,
        Map<String, String> headers,
        String body) {

    public String getHeader(String header) {
        return headers.get(header);
    }
}
