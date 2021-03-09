package client;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;

public class Client {
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;

    public Client() throws IOException {
        socket = new Socket("localhost", 1002);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        runClient();
    }

    private void runClient() {
        JFrame frame = new JFrame("Cloud Storage");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        frame.setResizable(false);
        frame.setSize(400, 300);
        frame.setVisible(true);
        JTextArea ta = new JTextArea();

        // TODO: 02.03.2021
        // list file - JList

        JButton uploadButton = new JButton("Upload");
        JButton downloadButton = new JButton("Download");
        JButton removeButton = new JButton("Remove");

        frame.getContentPane().add(BorderLayout.NORTH, ta);
        frame.getContentPane().add(BorderLayout.CENTER, downloadButton);
        frame.getContentPane().add(BorderLayout.EAST, uploadButton);
        frame.getContentPane().add(BorderLayout.WEST, removeButton);

        uploadButton.addActionListener(e -> {
            System.out.println(sendFile(ta.getText()));
        });

        removeButton.addActionListener(e -> {
            System.out.println(removeFile(ta.getText()));
        });

        downloadButton.addActionListener(e ->{
            System.out.println(downloadFile(ta.getText()));
        });
    }

    private String  downloadFile(String filename) {
        try {
            out.writeUTF("download");
            out.writeUTF(filename);
            out.flush();
            if ("x00DF".equals(in.readUTF())) {
                return "the file is missing";
            }
            else{
                File file = new File("client" + File.separator + filename);
                if (!file.exists()) {
                    file.createNewFile();
                }
                long length = in.readLong();
                FileOutputStream fos = new FileOutputStream(file);
                byte[] buffer = new byte[256];
                for (int i = 0; i < (length + 255) / 256; i++) {
                    int read = in.read(buffer);
                    fos.write(buffer, 0, read);
                }
                fos.flush();
                return "DONE download";
                }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        return "SOME ERROR";
    }

    private String sendFile(String filename) {
        try {
            File file = new File("client" + File.separator + filename);
            if (file.exists()) {
                out.writeUTF("upload");
                out.writeUTF(filename);
                long length = file.length();
                out.writeLong(length);
                FileInputStream fis = new FileInputStream(file);
                int read = 0;
                byte[] buffer = new byte[256];
                while ((read = fis.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.flush();
                String status = in.readUTF();
                return status;

            } else {
                return "File is not exists";
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Something error";
    }

    private String removeFile(String filename) {
        try {
            File file = new File("client" + File.separator + filename);
            out.writeUTF("remove");
            out.writeUTF(filename);
            out.flush();
            String status = in.readUTF();
            return status;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Something error";
    }

    public static void main(String[] args) throws IOException {
        new Client();
    }
}
