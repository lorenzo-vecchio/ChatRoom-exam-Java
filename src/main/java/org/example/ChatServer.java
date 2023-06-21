package org.example;

import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private ServerSocket serverSocket;
    private List<ClientHandler> clients;

    public ChatServer(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        clients = new ArrayList<>();
    }

    public void start() {
        System.out.println("Chat Server started on port " + serverSocket.getLocalPort());
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                clientHandler.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
        System.out.println("Broadcast: " + message);
    }

    private void sendPrivateMessage(String message, String recipient, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client.getNickname().equals(recipient)) {
                client.sendMessage("[Private] " + sender.getNickname() + ": " + message);
                System.out.println("Private message to " + recipient + ": " + sender.getNickname() + ": " + message);
                break;
            }
        }
    }

    private void removeClient(ClientHandler client) {
        String nickname = client.getNickname();
        clients.remove(client);
        broadcast(nickname + " left the chat.", null);
    }

    public static void main(String[] args) {
        int port = 5000; // Change this to the desired port number
        try {
            ChatServer server = new ChatServer(port);
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ClientHandler extends Thread {
        private Socket clientSocket;
        private BufferedReader in;
        private PrintWriter out;
        private String nickname;
        private boolean connected;

        public ClientHandler(Socket socket) throws IOException {
            clientSocket = socket;
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            connected = true;
        }

        public String getNickname() {
            return nickname;
        }

        public void run() {
            try {
                out.println("Please enter your nickname: ");
                nickname = in.readLine();
                out.println("Welcome to the chat, " + nickname + "!");

                broadcast(nickname + " joined the chat.", this);

                String message;
                do {
                    message = in.readLine();
                    if (message == null) {
                        connected = false;
                        break;
                    }
                    if (message.startsWith("@")) {
                        int spaceIndex = message.indexOf(' ');
                        if (spaceIndex != -1) {
                            String recipient = message.substring(1, spaceIndex);
                            String privateMessage = message.substring(spaceIndex + 1);
                            sendPrivateMessage(privateMessage, recipient, this);
                        }
                    } else {
                        broadcast(nickname + ": " + message, this);
                    }
                } while (!message.equalsIgnoreCase("bye"));

                removeClient(this);
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void sendMessage(String message) {
            if (connected) {
                out.println(message);
                out.flush();
            }
        }
    }
}
