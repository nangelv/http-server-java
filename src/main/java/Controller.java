import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Controller {

    private final String staticFilesDirectory;

    public Controller(String staticFilesDirectory) {
        this.staticFilesDirectory = staticFilesDirectory;
    }

    public HttpResponse routeRequest(HttpRequest request) throws IOException {
        return switch (request.method()) {
            case "GET" -> handleGetRequest(request);
            case "POST" -> handlePostRequest(request);
            default -> HttpResponse.notFound();
        };
    }

    private HttpResponse handleGetRequest(HttpRequest request) throws IOException {
        if (request.path().equals("/")) {
            return HttpResponse.ok();
        } else if (request.path().startsWith("/echo/")) {
            var echoArgument = request.path().substring("/echo/".length());
            return HttpResponse.ok()
                    .withHeader("Content-TypeContent-Type", "text/plain")
                    .withBody(echoArgument);
        } else if (request.path().equals("/user-agent")) {
            var requestUserAgent = request.getHeader("User-Agent");
            return HttpResponse.ok()
                    .withHeader("Content-TypeContent-Type", "text/plain")
                    .withBody(requestUserAgent);
        } else if(request.path().startsWith("/files/")) {
            var fileName = request.path().substring("/files/".length());
            var filePath = Path.of(staticFilesDirectory, fileName);
            if (Files.exists(filePath)) {
                var fileContent = Files.readString(filePath);
                return HttpResponse.ok()
                        .withHeader("Content-TypeContent-Type", "application/octet-stream")
                        .withBody(fileContent);
            }
        }

        return HttpResponse.notFound();
    }

    private HttpResponse handlePostRequest(HttpRequest request) throws IOException {
        if(request.path().startsWith("/files/")) {
            var fileName = request.path().substring("/files/".length());
            var filePath = Path.of(staticFilesDirectory, fileName);
            if (!Files.exists(filePath.getParent())) {
                System.out.println("Creating parent directory " + filePath.getParent());
                Files.createDirectories(filePath.getParent());
            }
            Files.writeString(filePath, request.body());
            System.out.println("Saved new file at " + filePath + " with content:\n" + request.body());
            return HttpResponse.created();
        }

        return HttpResponse.notFound();
    }
}
