package jp.ac.ritsumei.is0231iv.exp3net;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Main {

    public static void main(String[] args) throws Exception {
        // port, dir
        if  (args.length < 2) {
            throw new IllegalArgumentException("specify port, dir");
        }


        String publicDir = args[0];
        int port = Integer.parseInt(args[1]);
        new Main().startServer(port, publicDir);

    }


    private int curLine = 0;
    private String requestMethod;
    private String requestedPath;
    private List<String> lines = new ArrayList<String>();
    private HashMap<String, String> extContentTypeMap = new HashMap<>();
    private HashMap<String, String> headers = new HashMap<>();

    public Main() {
        extContentTypeMap.put("html", "text/html");
        extContentTypeMap.put("css", "text/css");
        extContentTypeMap.put("png", "image/png");
    }

    public void startServer(int port, String publicDir) throws IOException {

        ServerSocket serverSock = new ServerSocket(port);
        System.out.println("server started");
        while(true) {
            lines.clear();
            headers.clear();

            Socket sock = serverSock.accept();
            BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));

            String line;
            while((line = in.readLine()) != null) {
                if (line.isEmpty()) {
                    break;
                }

                lines.add(line);
            }

            readFirstLine();
            while(curLine < lines.size()) {
                readHeader();
            }


            // ファイルパスを解決する
            File file = new File(publicDir + requestedPath);
            Path path = Paths.get(file.getPath());
            if (file.isDirectory()) {
                path = path.resolve("./index.html");
                file = path.toFile();
            }

            try {
                sendFileContent(path, sock.getOutputStream());
            } catch(NoSuchFileException e) {
                String errorMessage = "not found";
                out.write("HTTP/1.1 404 Not Found\r\n");
                out.write("Content-Type: plain/text\r\n");
                out.write("Content-Length: " + errorMessage.length() + "\r\n");
                out.write("\r\n");
                out.write(errorMessage);
            }

            out.close();
            in.close();
            sock.close();
        }
    }

    public void readHeader() {
        String line = lines.get(curLine);
        int sepIdx = line.indexOf(":");
        if (sepIdx <= 0) {
            throw new Error("failed to parse header");
        }

        String key = line.substring(0, sepIdx).trim();
        String value = line.substring(sepIdx + 1).trim();
        headers.put(key, value);
        curLine++;
    }

    public void readFirstLine() {
        String firstLine = lines.get(0);
        String[] tokens = firstLine.split(" ");
        if (tokens.length != 3) {
            throw new Error("illegal first line");
        }

        String method = tokens[0].toLowerCase();
        if (!method.equals("get")) {
            throw new Error("unsupported method " + method);
        }

        requestMethod = method;
        requestedPath = tokens[1];
        curLine++;
    }

    public void sendFileContent(Path path, OutputStream outputStream) throws IOException {
        String filepath = path.toString();
        int dotIdx = filepath.lastIndexOf(".");
        String ext = filepath.substring(dotIdx + 1);
        String contentType = extContentTypeMap.containsKey(ext) ? extContentTypeMap.get(ext) : "plain/text";

        byte[] fileContent = Files.readAllBytes(path);
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(outputStream));

        out.write("HTTP/1.1 200 OK\r\n");
        out.write("Content-Type: " + contentType + "\r\n");
        out.write("Content-Length: " + fileContent.length + "\r\n");
        out.write("\r\n");
        out.flush();

        outputStream.write(fileContent, 0, fileContent.length);
        out.close();

    }
}
