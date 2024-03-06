import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedWriter;
import java.io.IOException;

public class PrivateChatWindow extends JFrame {
    private JTextArea chatTextArea;
    private JTextField inputField;
    private JButton sendButton;
    private BufferedWriter bufferedWriter;
    private String recipient;

    public PrivateChatWindow(String recipient, BufferedWriter bufferedWriter) {
        this.recipient = recipient;
        this.bufferedWriter = bufferedWriter;

        // Initialize the window
        setTitle("Private Chat with " + recipient);
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // Changed to DISPOSE_ON_CLOSE
        setLayout(new BorderLayout());

        // Create components
        chatTextArea = new JTextArea();
        chatTextArea.setEditable(false);
        inputField = new JTextField();
        sendButton = new JButton("Send");

        // Add components to the window
        add(new JScrollPane(chatTextArea), BorderLayout.CENTER);
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(inputField, BorderLayout.CENTER);
        southPanel.add(sendButton, BorderLayout.EAST);
        add(southPanel, BorderLayout.SOUTH);

        // Set up event listeners
        inputField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        this.setVisible(true); // Make the window visible
    }

    public void appendMessage(String message) {
        chatTextArea.append(message + "\n");
    }
    

    private void sendMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            try {
                // Update the message format to include the /private command
                String formattedMessage = "/private " + recipient + " " + message;
                bufferedWriter.write(formattedMessage);
                bufferedWriter.newLine();
                bufferedWriter.flush();
                // Append message to the chat text area
                chatTextArea.append("You: " + message + "\n");
                // Clear the input field
                inputField.setText("");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Failed to send message",
                        "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }
}

