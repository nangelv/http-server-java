import java.io.*;
import java.util.zip.GZIPOutputStream;

public class HttpRequestWriter implements Closeable {

    private final OutputStream outputStream;

    public HttpRequestWriter(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public void write(HttpResponse response) throws IOException {
        // Compress body if required and update content-length
        byte[] responseBodyRaw = null;
        if (response.getBody() != null) {
            responseBodyRaw = response.getBody().getBytes();
            if ("gzip".equals(response.getHeader(Headers.CONTENT_ENCODING))) {
                responseBodyRaw = gzipCompress(responseBodyRaw);
            }
            response.withHeader(Headers.CONTENT_LENGTH, Integer.toString(responseBodyRaw.length));
        }

        // Response status line
        outputStream.write("HTTP/1.1 %d %s\r\n".formatted(response.getStatusCode(), response.getStatusMessage()).getBytes());
        // Headers
        for (var header : response.getHeaders().entrySet()) {
            outputStream.write("%s: %s\r\n".formatted(header.getKey(), header.getValue()).getBytes());
        }
        // Body separator line
        outputStream.write("\r\n".getBytes());
        // Body
        if (responseBodyRaw != null) {
            outputStream.write(responseBodyRaw);
        }
    }

    @Override
    public void close() throws IOException {
        outputStream.close();
    }

    private byte[] gzipCompress(byte[] payload) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(outputStream)) {
            gzip.write(payload);
        }
        return outputStream.toByteArray();
    }
}
