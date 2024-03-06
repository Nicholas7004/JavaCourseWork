import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String clientUsername;
    private Server server;

    public ClientHandler(Socket socket, Server server) {
        try {
            this.socket = socket;
            this.server = server;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.clientUsername = bufferedReader.readLine();

            server.addClient(clientUsername, this);
            server.broadcastMessage("SERVER: " + clientUsername + " has entered the chat!");
        } catch (IOException e) {
            closeEverything();
            System.err.println("ClientHandler Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        String messageFromClient;

        while (socket.isConnected()) {
            try {
                messageFromClient = bufferedReader.readLine();
                if (messageFromClient == null) {
                    throw new IOException("Client disconnected");
                }

                if ("QUIT".equals(messageFromClient.trim())) {
                    server.broadcastMessage("SERVER: " + clientUsername + " has left the chat.");
                    server.removeClient(clientUsername);
                    closeEverything();
                    break;
                } else if (messageFromClient.startsWith("/private ")) {
                    String[] parts = messageFromClient.split(" ", 3);
                    if (parts.length >= 3) {
                        String recipientUsername = parts[1];
                        String privateMessage = parts[2];
                        server.sendPrivateMessage(clientUsername, recipientUsername, privateMessage);
                    }
                } else {
                    server.broadcastMessage(clientUsername + ": " + messageFromClient);
                }
            } catch (IOException e) {
                server.broadcastMessage("SERVER: " + clientUsername + " has been disconnected due to an error.");
                server.removeClient(clientUsername);
                closeEverything();
                break;
            }
        }
    }



    public void closeEverything() {
        server.removeClient(clientUsername);
        try {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing resources: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean isAlive() {
        return !socket.isClosed();
    }

    public String getClientUsername() {
        return clientUsername;
    }

    public Socket getSocket() {
        return socket;
    }

    public BufferedWriter getBufferedWriter() {
        return bufferedWriter;
    }

    public void setCoordinator(boolean b) {
        // TODO Auto-generated method stub
    }
}