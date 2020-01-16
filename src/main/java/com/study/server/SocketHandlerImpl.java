package com.study.server;

import com.study.server.http.HttpRequest;
import com.study.server.http.HttpRequestParser;
import com.study.server.http.Response;
import com.study.server.http.StatusCode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Map;

public class SocketHandlerImpl implements SocketHandler, Runnable {
    private Socket clientSocket;
    InputStream in = null;
    OutputStream out = null;

    public SocketHandlerImpl(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            in = clientSocket.getInputStream();
            out = clientSocket.getOutputStream();
            HttpRequest request = HttpRequestParser.parse(in);

//            if (!parser.parse(inputLine)) {
//                respond(500, "Unable to parse request", out);
//                return;
//            }

            RequestDispatcherImpl dispatcher = new RequestDispatcherImpl(request);
//            response = dispatcher.dispatch();
//            sendResponse(response);


//            File myFile = new File(sitesAndConfigDirectory + "\\www.food.com\\index.html");
//            byte[] myByteArray = new byte[(int) myFile.length()];
//
//            FileInputStream fis = new FileInputStream(myFile);
//            BufferedInputStream bis = new BufferedInputStream(fis);
//            bis.read(myByteArray);
//
//            out.write(("HTTP/1.1 200 OK" + "\r\n\r\n").getBytes());
//            out.write(myByteArray);
//
//            out.close();
//            System.out.println("File is transferred");
//            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void respond(int statusCode, String msg, OutputStream out) throws IOException {
        String responseLine = "HTTP/1.1 " + statusCode + " " + msg + "\r\n\r\n";
        out.write(responseLine.getBytes());
    }

    public void sendResponse(Response response) throws IOException {
        Map<String, String> headers = response.getHeaders();
        StatusCode statusCode = response.getStatusCode();
        String body = response.getBody();
        headers.put("Connection", "Close");
        out.write(("HTTP/1.1 " + statusCode + "\r\n").getBytes());

        for (String headerName : headers.keySet()) {
            out.write((headerName + ": " + headers.get(headerName) + "\r\n").getBytes());
        }

        out.write("\r\n".getBytes());

        if (body != null) {
            out.write(body.getBytes());
        }
    }
}
