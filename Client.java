import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JFrame;

public class Client {
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String username;
    private Map<String, JFrame> openPrivateChats = new HashMap<>();
    // GUI components
    private JFrame frame;
    private JTextArea textAreaMessages;
    private JTextField textFieldInput;
    private JButton buttonSend;
    private JButton buttonQuit;
 
    public Client(Socket socket, String username) {
        this.socket = socket;
        this.username = username;

        // Prompt for chat mode selection
        int choice = JOptionPane.showOptionDialog(null, 
                "Select your chat mode:", 
                "Chat Mode Selection",
                JOptionPane.YES_NO_OPTION, 
                JOptionPane.QUESTION_MESSAGE, 
                null, 
                new String[]{"Global Chat", "Private Chat"}, 
                "Global Chat");

        if (choice == JOptionPane.YES_OPTION) {
            // User chose to join Global Chat directly
            JOptionPane.showMessageDialog(null, "Welcome to the global chat!", "Global Chat", JOptionPane.INFORMATION_MESSAGE);
        } else if (choice == JOptionPane.NO_OPTION) {
            // User chose to use Private Chat
            JOptionPane.showMessageDialog(null, "To start a private chat with someone, send a message in the format '/private username message'.", "Private Chat Information", JOptionPane.INFORMATION_MESSAGE);
        }

        createClientGUI();

        try {
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            bufferedWriter.write(username);
            bufferedWriter.newLine();
            bufferedWriter.flush();

            // Start listening for messages
            listenForMessage();
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
            System.err.println("Client Constructor Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createClientGUI() {
        frame = new JFrame("Client: " + username);
        textAreaMessages = new JTextArea();
        textAreaMessages.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textAreaMessages);
        textFieldInput = new JTextField("Type your message here...");
        buttonSend = new JButton("Send");
        buttonQuit = new JButton("Quit");

        frame.setLayout(new BorderLayout());
        frame.add(scrollPane, BorderLayout.CENTER);

        // Input panel at the bottom
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(textFieldInput, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
        buttonPanel.add(buttonSend);
        buttonPanel.add(buttonQuit);
        inputPanel.add(buttonPanel, BorderLayout.EAST);
        frame.add(inputPanel, BorderLayout.SOUTH);

        // Placeholder text and clear on focus
        textFieldInput.setForeground(Color.GRAY);
        textFieldInput.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (textFieldInput.getText().equals("Type your message here...")) {
                    textFieldInput.setText("");
                    textFieldInput.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (textFieldInput.getText().isEmpty()) {
                    textFieldInput.setForeground(Color.GRAY);
                    textFieldInput.setText("Type your message here...");
                }
            }
        });

        textFieldInput.addActionListener(e -> {
            String messageToSend = textFieldInput.getText().trim();
            sendMessage(messageToSend); // Now passing the message as an argument
        });

        // Button actions
        buttonSend.addActionListener(e -> {
            String messageToSend = textFieldInput.getText().trim();
            sendMessage(messageToSend); // Pass the message as an argument
        });

        buttonQuit.addActionListener(e -> quitChat());

        // Finalize frame setup
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setVisible(true);
    }
    
    private void openPrivateChatWindow(String withUsername, String message) {
        String chatKey = username.compareTo(withUsername) < 0 ? username + withUsername : withUsername + username;
        JFrame privateChatFrame;
        JTextArea privateChatArea;

        if (!openPrivateChats.containsKey(chatKey)) {
            privateChatFrame = new JFrame("Private Chat with " + withUsername);
            privateChatArea = new JTextArea();
            privateChatArea.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(privateChatArea);
            JTextField privateChatInput = new JTextField();
            JButton sendButton = new JButton("Send");

            // Action for sending a message
            ActionListener sendAction = e -> {
                String messageToSend = privateChatInput.getText().trim();
                if (!messageToSend.isEmpty()) {
                    sendMessage("/private " + withUsername + " " + messageToSend);
                    privateChatArea.append("Me: " + messageToSend + "\n");
                    privateChatInput.setText("");
                }
            };

            privateChatInput.addActionListener(sendAction);
            sendButton.addActionListener(sendAction);

            JButton quitButton = new JButton("Quit");
            quitButton.addActionListener(e -> {
                privateChatFrame.dispose();
                openPrivateChats.remove(chatKey);
            });

            JPanel southPanel = new JPanel(new BorderLayout());
            southPanel.add(privateChatInput, BorderLayout.CENTER);
            JPanel buttonPanel = new JPanel(new FlowLayout());
            buttonPanel.add(sendButton);
            buttonPanel.add(quitButton);
            southPanel.add(buttonPanel, BorderLayout.EAST);

            privateChatFrame.setLayout(new BorderLayout());
            privateChatFrame.add(scrollPane, BorderLayout.CENTER);
            privateChatFrame.add(southPanel, BorderLayout.SOUTH);
            privateChatFrame.setSize(400, 300);
            privateChatFrame.setVisible(true);

            openPrivateChats.put(chatKey, privateChatFrame);
        } else {
            privateChatFrame = openPrivateChats.get(chatKey);
        }

        JScrollPane finalScrollPane = (JScrollPane) privateChatFrame.getContentPane().getComponent(0);
        JTextArea finalPrivateChatArea = (JTextArea) finalScrollPane.getViewport().getView();
        finalPrivateChatArea.append(message + "\n");

        privateChatFrame.toFront();
        privateChatFrame.repaint();
    }

    



    public void sendMessage(String messageToSend) {
        if (!messageToSend.isEmpty()) {
            if (messageToSend.startsWith("/private ")) {
                String[] parts = messageToSend.split(" ", 3);
                if (parts.length >= 2) {
                    String recipientUsername = parts[1];
                    String chatKey = getChatKey(username, recipientUsername);
                    if (!openPrivateChats.containsKey(chatKey)) {
                        String initialMessage = parts.length == 3 ? parts[2] : "has opened a private chat with you.";
                        openPrivateChatWindow(recipientUsername, username + "(private): " + initialMessage);
                    }
                    // Send the message in both cases (new or existing chat)
                    sendToServer(messageToSend);
                }
            } else {
                // Sending regular message to the server
                sendToServer(messageToSend);
            }
            textFieldInput.setText(""); // Clear input field after sending
        }
    }


    private void sendToServer(String message) {
        try {
            bufferedWriter.write(message);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            System.err.println("Error sending message: " + e.getMessage());
            // Handle reconnection or user notification here
        }
    }

    private String getChatKey(String user1, String user2) {
        return user1.compareTo(user2) < 0 ? user1 + user2 : user2 + user1;
    }

    private void quitChat() {
        try {
            if (socket != null) {
                bufferedWriter.write("QUIT");
                bufferedWriter.newLine();
                bufferedWriter.flush();
            }
        } catch (IOException e) {
            System.err.println("Error quitting chat: " + e.getMessage());
        } finally {
            closeEverything(socket, bufferedReader, bufferedWriter);
            System.exit(0);
        }
    }

    private void listenForMessage() {
        new Thread(() -> {
            try {
                String messageFromServer;
                while ((messageFromServer = bufferedReader.readLine()) != null) {
                    final String finalMessage = messageFromServer;
                    SwingUtilities.invokeLater(() -> {
                        if (finalMessage.startsWith("PRIVATE:")) {
                            // Extract the sender's username and the actual message
                            String[] parts = finalMessage.split(":", 3);
                            if (parts.length == 3) {
                                String senderUsername = parts[1].trim();
                                String actualMessage = parts[2].trim();
                                // Open or focus the private chat window with the sender
                                openPrivateChatWindow(senderUsername, senderUsername + ": " + actualMessage);
                            }
                        } else {
                            textAreaMessages.append(finalMessage + "\n");
                        }
                    });
                }
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
                System.err.println("Lost connection to the server.");
            }
        }).start();
    }
    private void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        try {
            if (bufferedReader != null) bufferedReader.close();
            if (bufferedWriter != null) bufferedWriter.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Error closing resources: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        String serverAddress = "localhost"; // Replace with your server's IP address
        int port = 7004; // Replace with your server's port

        String username = JOptionPane.showInputDialog(null, "Enter your username for the group chat:", "Login", JOptionPane.PLAIN_MESSAGE);

        if (username != null && !username.trim().isEmpty()) {
            try {
                Socket socket = new Socket(serverAddress, port);
                new Client(socket, username.trim());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Error connecting to the server: " + e.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        } else {
            JOptionPane.showMessageDialog(null, "You must enter a username to connect.", "Login Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}