public class HttpResponse {

    private final String body;
    private final String contentType;
    private final String statusMessage;
    private final int statusCode;

    private HttpResponse(Builder builder) {
        this.body = builder.body;
        this.contentType = builder.contentType;
        this.statusMessage = builder.statusMessage;
        this.statusCode = builder.statusCode;
    }

    public String getBody() {
        return body;
    }

    public String getContentType() {
        return contentType;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponse() {
        var response = "HTTP/1.1 %d %s\r\n".formatted(statusCode, statusCode);
        if (body != null) {
            response += "Content-Type: %s\r\nContent-Length: %d\r\n\r\n%s".formatted(contentType, body.length(), body);
        } else {
            response += "\r\n";
        }
        return response;
    }

    public byte[] getResponseBytes() {
        return getResponse().getBytes();
    }

    public static Builder builder() {
        return new HttpResponse.Builder();
    }

    public static HttpResponse.Builder notFound() {
        return HttpResponse.builder()
                .withStatusCode(404)
                .withStatusMessage("Not Found");
    }

    public static HttpResponse.Builder ok() {
        return HttpResponse.builder()
                .withStatusCode(200)
                .withStatusMessage("OK");
    }

    public static HttpResponse.Builder created() {
        return HttpResponse.builder()
                .withStatusCode(201)
                .withStatusMessage("Created");
    }

    public static class Builder {
        private String body;
        private String contentType;
        private String statusMessage;
        private int statusCode;

        public Builder() {
        }

        public Builder withBody(String body) {
            this.body = body;
            return this;
        }

        public Builder withContentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder withStatusMessage(String statusMessage) {
            this.statusMessage = statusMessage;
            return this;
        }

        public Builder withStatusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public HttpResponse build() {
            return new HttpResponse(this);
        }
    }

    @Override
    public String toString() {
        return "HttpResponse{" +
                "body='" + body + '\'' +
                ", contentType='" + contentType + '\'' +
                ", statusMessage='" + statusMessage + '\'' +
                ", statusCode=" + statusCode +
                '}';
    }
}

