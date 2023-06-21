package org.example;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ChatClientGUI extends JFrame {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean running;

    private JTextPane chatPane;
    private JTextField messageField;
    private JButton sendButton; // New button

    public ChatClientGUI(String serverAddress, int serverPort) {
        try {
            socket = new Socket(serverAddress, serverPort);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            running = true;

            // Add a shutdown hook to handle program termination
            Runtime.getRuntime().addShutdownHook(new Thread(this::close));

            // Set up the GUI
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setTitle("Chat Client");
            setSize(400, 300);
            setLayout(new BorderLayout());

            // Chat pane
            chatPane = new JTextPane();
            chatPane.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(chatPane);
            add(scrollPane, BorderLayout.CENTER);

            // Message input field
            JPanel inputPanel = new JPanel(new BorderLayout());
            messageField = new JTextField();
            messageField.addActionListener(e -> sendMessage());
            inputPanel.add(messageField, BorderLayout.CENTER);

            // Send button
            sendButton = new JButton("Send");
            sendButton.addActionListener(e -> sendMessage());
            inputPanel.add(sendButton, BorderLayout.EAST);

            add(inputPanel, BorderLayout.SOUTH);
            setVisible(true);

            // Start a separate thread to listen for server messages
            Thread messageThread = new Thread(this::listenForMessages);
            messageThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage() {
        String message = messageField.getText();
        new Thread(() -> {
            out.println(message);
            if (message.equalsIgnoreCase("bye")) {
                running = false;
                close();
            }
        }).start();
        appendToChatPane("You: " + message);
        messageField.setText("");
    }


    private void listenForMessages() {
        try {
            String message;
            while (running && (message = in.readLine()) != null) {
                appendToChatPane(message);
            }
        } catch (IOException e) {
            if (running) {
                e.printStackTrace();
            }
        } finally {
            close();
        }
    }

    private void appendToChatPane(String message) {
        SwingUtilities.invokeLater(() -> {
            try {
                Document doc = chatPane.getDocument();

                SimpleAttributeSet style = new SimpleAttributeSet();

                // Check if the message contains ":"
                int colonIndex = message.indexOf(":");
                if (colonIndex != -1) {
                    // Set purple color for letters before the ":"
                    StyleConstants.setForeground(style, Color.MAGENTA);
                    doc.insertString(doc.getLength(), message.substring(0, colonIndex) + ":", style);

                    // Reset the color for the remaining message after ":"
                    StyleConstants.setForeground(style, chatPane.getForeground());
                    doc.insertString(doc.getLength(), message.substring(colonIndex + 1) + "\n", style);
                } else if (message.contains("joined the chat")) {
                    StyleConstants.setForeground(style, Color.GREEN);
                    doc.insertString(doc.getLength(), message + "\n", style);
                } else if (message.contains("left the chat")) {
                    StyleConstants.setForeground(style, Color.RED);
                    doc.insertString(doc.getLength(), message + "\n", style);
                } else {
                    StyleConstants.setForeground(style, chatPane.getForeground());
                    doc.insertString(doc.getLength(), message + "\n", style);
                }

                chatPane.setCaretPosition(doc.getLength());
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
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
        SwingUtilities.invokeLater(() -> new ChatClientGUI(serverAddress, serverPort));
    }
}
