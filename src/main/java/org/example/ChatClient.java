package org.example;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ChatClient {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean running;

    public ChatClient(String serverAddress, int serverPort) {
        try {
            socket = new Socket(serverAddress, serverPort);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            running = true;

            // Add a shutdown hook to handle program termination
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                running = false;
                close();
            }));

            // Start a separate thread to listen for server messages
            Thread messageThread = new Thread(this::listenForMessages);
            messageThread.start();

            // Read user input and send messages to the server
            Scanner scanner = new Scanner(System.in);
            while (running) {
                String message = scanner.nextLine();
                out.println(message);
                if (message.equalsIgnoreCase("bye")) {
                    running = false;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close();
            System.exit(0); // Close the program
        }
    }

    private void listenForMessages() {
        try {
            String message;
            while (running && (message = in.readLine()) != null) {
                String formattedMessage = formatMessage(message);
                System.out.println(formattedMessage);
            }
        } catch (IOException e) {
            if (running) {
                e.printStackTrace();
            }
        } finally {
            close();
            System.exit(0); // Close the program
        }
    }

    private String formatMessage(String message) {
        if (message.startsWith("[Private]")) {
            return message; // Private messages are not formatted
        } else if (message.contains("joined the chat.")) {
            return "\u001B[32m" + message + "\u001B[0m"; // Joined messages in green
        } else if (message.contains("left the chat.")) {
            return "\u001B[31m" + message + "\u001B[0m"; // Left messages in red
        } else {
            // Extract the username and format it in purple
            int colonIndex = message.indexOf(':');
            if (colonIndex != -1) {
                String username = message.substring(0, colonIndex);
                return "\u001B[35m" + username + "\u001B[0m" + message.substring(colonIndex);
            }
        }
        return message;
    }

    private void close() {
        try {
            running = false;
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String serverAddress = "localhost";  // Change this to the server IP address
        int serverPort = 5000;  // Change this to the server port
        new ChatClient(serverAddress, serverPort);
    }
}
