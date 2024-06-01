public class Main {

    public static String SERVER_DIRECTORY;

    public static void main(String[] args) {
        if (args.length > 1 && args[0].equals("--directory")) {
            SERVER_DIRECTORY = args[1];
        }

        var httpServer = new HttpServer(4221, new Controller(SERVER_DIRECTORY));
        httpServer.run();
    }
}
