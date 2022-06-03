import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class NioServer {

    private ServerSocketChannel server;
    private Selector selector;
    private StringBuilder fileDir = new StringBuilder(System.getProperty("user.home"));
    private final String CURRENT_DIRECTORY_FILES = "is";
    private final String CURRENT_FILES_IN_THIS_DIRECTORY_READ = "cat";
    private final String CHANGE_DIRECTORY_IN = "cd";
    private Path pathDir = Path.of(System.getProperty("user.home"));
    public NioServer() throws IOException {
        server = ServerSocketChannel.open();
        selector = Selector.open();
        server.bind(new InetSocketAddress(8189));
        server.configureBlocking(false);
        server.register(selector, SelectionKey.OP_ACCEPT);

    }

    public void start() throws IOException {
        while (server.isOpen()) {
            selector.select();

            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = keys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                if (key.isAcceptable()) {
                    handleAccept();
                }
                if (key.isReadable()) {
                handleRead(key);
                }
                iterator.remove();
            }
        }
    }

    private void handleRead(SelectionKey key) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(1024);
        StringBuilder stringBuilder = new StringBuilder();
        SocketChannel channel = (SocketChannel) key.channel();
        while (channel.isOpen()) {
            int read = channel.read(buf);
            if (read < 0) {
                channel.close();
                return;
            }
            if (read == 0) {
                break;
            }
            buf.flip();
            while (buf.hasRemaining()) {
            stringBuilder.append((char) buf.get());
            }
            buf.clear();
        }
//я сравнение сделал криво, в стринг билдер заходит элемент и новый обзац,
// танцы с бубнами не помогли, подскажите как грамотно реализовать сравнение
        if (stringBuilder.toString().startsWith(CURRENT_DIRECTORY_FILES)) {
          wrapCurrentDirectoryFiles(channel);
        }
        if (stringBuilder.toString().startsWith(CURRENT_FILES_IN_THIS_DIRECTORY_READ)) {
            searchFileInCurrentDirectoryAndRead(stringBuilder,channel);
        }
        if (stringBuilder.toString().startsWith(CHANGE_DIRECTORY_IN)) {
            pathDir = changeDirectoryIN(stringBuilder,channel);
        }
        stringBuilder.append("->");
        stringBuilder.append(pathDir);
        stringBuilder.append(">");
        byte[] message = stringBuilder.toString().getBytes(StandardCharsets.UTF_8);
        channel.write(ByteBuffer.wrap(message));
    }



    private void handleAccept() throws IOException {
    SocketChannel channel = server.accept();
    channel.configureBlocking(false);
    channel.register(selector, SelectionKey.OP_READ);
    channel.write(ByteBuffer.wrap(("Welcome in terminal" + System.getProperty("line.separator")).getBytes(StandardCharsets.UTF_8)));
}

    private List<String> getFiles(StringBuilder dir) {
        String[] list = new File(dir.toString()).list();
        assert list != null;
        return Arrays.asList(list);
    }
    private  void wrapCurrentDirectoryFiles(SocketChannel channel) throws IOException {
        List<String> list= getFiles(new StringBuilder(String.valueOf(pathDir)));
        if (list.size() != 0){
        channel.write(ByteBuffer.wrap("AllFiles in ".getBytes(StandardCharsets.UTF_8)));
        channel.write(ByteBuffer.wrap(pathDir.toString().getBytes(StandardCharsets.UTF_8)));
        channel.write(ByteBuffer.wrap(System.getProperty("line.separator").getBytes(StandardCharsets.UTF_8)));
        for (String file : list) {
            channel.write(ByteBuffer.wrap(file.getBytes(StandardCharsets.UTF_8)));
            channel.write(ByteBuffer.wrap(System.getProperty("line.separator").getBytes(StandardCharsets.UTF_8)));
        }
        } else{
            channel.write(ByteBuffer.wrap("false".getBytes(StandardCharsets.UTF_8)));
            channel.write(ByteBuffer.wrap(System.getProperty("line.separator").getBytes(StandardCharsets.UTF_8)));
        }
    }

    private void searchFileInCurrentDirectoryAndRead(StringBuilder stringBuilder, SocketChannel channel) throws IOException {
        String[] text = stringBuilder.toString().split(" ");
        if (text.length >= 2){
            String fileName = text[1].substring(0, text[1].length()-2);
            List<String> list= getFiles(new StringBuilder(String.valueOf(pathDir)));
            Path newPath = pathDir.resolve(fileName);
            for (String file: list) {
                if (file.equals(fileName) && !Files.isDirectory(newPath)) {
                    System.out.println(newPath);
                    BufferedReader bufferedReader = new BufferedReader(new FileReader(String.valueOf(newPath)));
                    String line;
                    do {
                        line = bufferedReader.readLine();
                        channel.write(ByteBuffer.wrap(line.getBytes(StandardCharsets.UTF_8)));
                        channel.write(ByteBuffer.wrap(System.getProperty("line.separator").getBytes(StandardCharsets.UTF_8)));
                    }while (bufferedReader.readLine() != null);
                    channel.write(ByteBuffer.wrap("true".getBytes(StandardCharsets.UTF_8)));
                    channel.write(ByteBuffer.wrap(System.getProperty("line.separator").getBytes(StandardCharsets.UTF_8)));
                    return;
                }
            }
            channel.write(ByteBuffer.wrap("there is no such file or it is a directory".getBytes(StandardCharsets.UTF_8)));
            channel.write(ByteBuffer.wrap(System.getProperty("line.separator").getBytes(StandardCharsets.UTF_8)));
        }
    }
    private Path changeDirectoryIN(StringBuilder stringBuilder, SocketChannel channel) throws IOException {
        String[] text = stringBuilder.toString().split(" ");
        if (text.length >= 2){
            String fileName = text[1].substring(0, text[1].length() - 2);
        System.out.println(pathDir);
        Path newPath = pathDir.resolve(fileName);
            System.out.println(newPath);
        if (Files.exists(newPath) && Files.isDirectory(newPath)) {
            return newPath;
        } else {
            channel.write(ByteBuffer.wrap("There is no such directory or this file is not a directory. ".getBytes(StandardCharsets.UTF_8)));
            channel.write(ByteBuffer.wrap(System.getProperty("line.separator").getBytes(StandardCharsets.UTF_8)));
        }
    }
        return pathDir;
    }
}
