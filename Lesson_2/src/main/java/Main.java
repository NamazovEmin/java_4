import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


public class Main {
    public static void main(String[] args) throws IOException {

        NioServer nioServer = new NioServer();
        nioServer.start();
//        Path newPath = Path.of("user.home.Videos");
//        if (Files.exists(newPath) && Files.isDirectory(newPath)){
//            System.out.println(true);
//        } else {
//            System.out.println(false);
//        }
    }
}
