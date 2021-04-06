package server;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

public class NioTelnetServer {
    private final ByteBuffer buffer = ByteBuffer.allocate(512);
    private static final Integer PORT = 1234;
    private static String directory;

    public static final String LS_COMMAND = "\tls          view all files from current directory\n\r";
    public static final String MKDIR_COMMAND = "\tmkdir       view all files from current directory\n\r";
    public static final String CHANGE_NICKNAME_COMMAND = "\tnick        change nickname\n\r";
    public static final String COPY = "\tcopy        copy file\n\r";
    public static final String REMOVE = "\trm          remove file or directory\n\r";
    public static final String CREATEFILE = "\ttouch       create new file\n\r";
    public static final String MOVING_TO_DIRECTORY = "\tcd          moving to a directory\n\r";
    public static final String MOVING_TO_PARENT_DIRECTORY = "\t-cd         moving to a parent directory\n\r";
    public static final String PRINT= "\tcat         output content\n\r";



    private Map<String, SocketAddress> clients = new HashMap<>();

    public NioTelnetServer() throws IOException {
        ServerSocketChannel server = ServerSocketChannel.open(); // открыли
        server.bind(new InetSocketAddress(PORT));
        server.configureBlocking(false);
        Selector selector = Selector.open();
        server.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Server started");
        directory = "server";
        while (server.isOpen()) {
            selector.select();
            var selectionKeys = selector.selectedKeys();
            var iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                var key = iterator.next();
                if (key.isAcceptable()) {
                    handleAccept(key, selector);
                } else if (key.isReadable()) {
                    handleRead(key, selector);
                }
                iterator.remove();
            }
        }
    }

    private void handleRead(SelectionKey key, Selector selector) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        SocketAddress client = channel.getRemoteAddress();
        String nickname = "";
        int readBytes = channel.read(buffer);
        if (readBytes < 0) {
            channel.close();
            return;
        } else if (readBytes == 0) {
            return;
        }

        buffer.flip();
        StringBuilder sb = new StringBuilder();
        while (buffer.hasRemaining()) {
            sb.append((char) buffer.get());
        }
        buffer.clear();


        // + touch (имя файла) - создание файла
        // + mkdir (имя директории) - создание директории
        // + cd (path) - перемещение по дереву папок
        // + rm (имя файла или папки) - удаление объекта
        // + copy (src, target) - копирование файла
        // + cat (имя файла) - вывод в консоль содержимого

        if (key.isValid()) {
            String command = sb.toString()
                    .replace("\n", "")
                    .replace("\r", "");
            if ("--help".equals(command)) {
                sendMessage(LS_COMMAND, selector, client);
                sendMessage(CHANGE_NICKNAME_COMMAND, selector, client);
                sendMessage(CREATEFILE, selector, client);
                sendMessage(COPY, selector, client);
                sendMessage(MKDIR_COMMAND, selector, client);
                sendMessage(MOVING_TO_DIRECTORY, selector, client);
                sendMessage(MOVING_TO_PARENT_DIRECTORY, selector, client);
                sendMessage(PRINT, selector, client);
                sendMessage(REMOVE, selector, client);


            }
            else if (command.startsWith("nick ")) {
                nickname = command.split(" ")[1];
                clients.put(nickname, client);
                System.out.println("Client [" + client.toString() + "] changes nickname on [" + nickname + "]");
            }
            else if ("ls".equals(command)) {
                sendMessage(getFilesList().concat("\n"), selector, client);
            }
            else if (command.startsWith("cd ")){
                directory += File.separator + command.split( " ")[1];
                sendMessage(getFilesList().concat("\n"), selector, client);
            }
            else if(command.equals("-cd")){
                Path path = Paths.get(directory);
                System.out.println(path.getParent());
                if(path.getParent() == null){
                    sendMessage("You are in root directory\n\r", selector, client);
                }else {
                    directory = String.valueOf(path.getParent());
                    sendMessage(getFilesList().concat("\n"), selector, client);
                }
            }
            else if(command.startsWith("copy ")){

                Path sourcePath      = Paths.get(directory + File.separator + command.split(" ")[1]);
                Path destinationPath = Paths.get(directory + File.separator+ command.split(" ")[2]);

                try {
                    Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
                } catch(FileAlreadyExistsException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            else if(command.startsWith("touch ")){
                Files.createFile(Path.of(directory + File.separator + command.split(" ")[1]));
            }
            else if (command.startsWith("mkdir ")){
                Path path = Paths.get(directory);
                Files.createDirectory(Path.of(directory + File.separator+command.split(" ")[1]));
            }
            else if (command.startsWith("rm ")) {
                Path path = Paths.get("server" + File.separator + command.split(" ")[1]);
                if(Files.exists(path)){
                    if(!Files.isDirectory(path)){
                        try {
                            Files.delete(Path.of(String.valueOf(path)));
                        } catch (IOException e) {
                             e.printStackTrace();
                        }
                    }
                    else{

                        try {
                            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                                @Override
                                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                    System.out.println("delete file: " + file.toString());
                                    Files.delete(file);
                                    return FileVisitResult.CONTINUE;
                                }

                                @Override
                                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                                    Files.delete(dir);
                                    System.out.println("delete dir: " + dir.toString());
                                    return FileVisitResult.CONTINUE;
                                }
                            });
                        } catch(IOException e){
                            e.printStackTrace();
                        }
                    }
                }
            }
            else if (command.startsWith("cat ")){
                String file =directory + File.separator +  command.split(" ")[1];
                if (!Files.isDirectory(Path.of(file))){
                    String contents = readUsingFiles(file);
                    sendMessage(contents,selector,client);
                }


            }
            else if ("exit".equals(command)) {
                System.out.println("Client logged out. IP: " + channel.getRemoteAddress());
                channel.close();
                return;
            }
        }

        for (Map.Entry<String, SocketAddress> clientInfo : clients.entrySet()) {
            if (clientInfo.getValue().equals(client)) {
                nickname = clientInfo.getKey();
            }
        }
        sendName(channel, nickname);
    }

    private void sendName(SocketChannel channel, String nickname) throws IOException {
        if (nickname.isEmpty()) {
            nickname = channel.getRemoteAddress().toString();
        }
        channel.write(
                ByteBuffer.wrap(nickname
                        .concat(">: ")
                        .getBytes(StandardCharsets.UTF_8)

                )
        );
    }

    private String getFilesList() {
        String toOut;
        toOut = String.join("\t", new File(directory).list()) + "\r";
        return toOut;
    }

    private void sendMessage(String message, Selector selector, SocketAddress client) throws IOException {
        for (SelectionKey key : selector.keys()) {
            if (key.isValid() && key.channel() instanceof SocketChannel) {
                if (((SocketChannel) key.channel()).getRemoteAddress().equals(client)) {
                    ((SocketChannel) key.channel())
                            .write(ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8)));
                }
            }
        }
    }


    private static String readUsingFiles(String fileName) throws IOException {
        return new String(Files.readAllBytes(Paths.get(fileName)));
    }





    private void handleAccept(SelectionKey key, Selector selector) throws IOException {
        SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
        channel.configureBlocking(false);
        System.out.println("Client accepted. IP: " + channel.getRemoteAddress());
        channel.register(selector, SelectionKey.OP_READ, "some attach");
        channel.write(ByteBuffer.wrap("Hello user!\n\r".getBytes(StandardCharsets.UTF_8)));
        channel.write(ByteBuffer.wrap("Enter --help for support info\n\r".getBytes(StandardCharsets.UTF_8)));
        sendName(channel, "");
    }

    public static void main(String[] args) throws IOException {
        new NioTelnetServer();
    }
}