package chatApp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Server {
    private ServerSocket serverSocket;
    private Map<String, PrintWriter> clientWriters;

    public Server(int port) {
        try {
            serverSocket = new ServerSocket(port);
            clientWriters = new HashMap<>();
            System.out.println("Server started on port " + port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        try {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                Thread clientThread = new Thread(new ClientHandler(clientSocket));
                clientThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void broadcast(String message, String sender) {
        for (PrintWriter writer : clientWriters.values()) {
            writer.println("[" + sender + "]: " + message);
        }
        System.out.println("[" + sender + "]: " + message); // Display the message in the server console
    }

    private void sendUserList(PrintWriter writer) {
        StringBuilder userList = new StringBuilder();
        synchronized (clientWriters) {
            userList.append("Connected users: ");
            for (String user : clientWriters.keySet()) {
                userList.append(user).append(", ");
            }
        }
        writer.println(userList.toString());
    }

    private class ClientHandler implements Runnable {
        private Socket clientSocket;
        private BufferedReader input;
        private PrintWriter output;
        private String clientName;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
            try {
                input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                output = new PrintWriter(clientSocket.getOutputStream(), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                clientName = input.readLine();
                System.out.println(clientName + " has connected.");

                synchronized (clientWriters) {
                    clientWriters.put(clientName, output);
                }

                output.println("Welcome, " + clientName + "! Available commands: /msg username message, /msg-all message, /users, /logout");

                String message;
                while ((message = input.readLine()) != null) {
                    if (message.startsWith("/msg ")) {
                        String[] parts = message.split(" ", 3);
                        String recipient = parts[1];
                        String msg = parts[2];
                        PrintWriter recipientWriter;
                        synchronized (clientWriters) {
                            recipientWriter = clientWriters.get(recipient);
                        }
                        if (recipientWriter != null) {
                            recipientWriter.println("Private message from " + clientName + ": " + msg);
                            output.println("Private message to " + recipient + ": " + msg);
                        } else {
                            output.println("User '" + recipient + "' not found.");
                        }
                    } else if (message.startsWith("/msg-all ")) {
                        String msg = message.substring(9);
                        broadcast(msg, clientName);
                    } else if (message.equalsIgnoreCase("/users")) {
                        sendUserList(output);
                    } else if (message.equalsIgnoreCase("/logout")) {
                        break;
                    } else {
                        output.println("Invalid command! Available commands: /msg username message, /msg-all message, /users, /logout");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                synchronized (clientWriters) {
                    clientWriters.remove(clientName);
                }
                System.out.println(clientName + " has disconnected.");
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        int serverPort = 12345;

        Server server = new Server(serverPort);
        server.start();
    }
}
