package chatApp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
    private Socket socket;
    private BufferedReader input;
    private BufferedReader serverInput;
    private PrintWriter output;

    public Client(String serverAddress, int serverPort) {
        try {
            socket = new Socket(serverAddress, serverPort);
            input = new BufferedReader(new InputStreamReader(System.in));
            serverInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        try {
            System.out.print("Enter your name: ");
            String name = input.readLine();
            output.println(name);

            Thread serverThread = new Thread(new ServerListener());
            serverThread.start();

            String message;
            while (true) {
                message = input.readLine();
                if (message.equalsIgnoreCase("/users")) {
                    output.println("/users");
                } else if (message.startsWith("/msg ")) {
                    output.println(message);
                } else if (message.startsWith("/msg-all ")) {
                    output.println(message);
                } else if (message.equalsIgnoreCase("/logout")) {
                    output.println("/logout");
                    break;
                } else {
                    output.println("/msg " + message);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class ServerListener implements Runnable {
        @Override
        public void run() {
            String serverMessage;
            try {
                while ((serverMessage = serverInput.readLine()) != null) {
                    System.out.println(serverMessage);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        String serverAddress = "localhost";
        int serverPort = 12345;

        Client client = new Client(serverAddress, serverPort);
        client.start();
    }
}
