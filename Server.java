import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    private ServerSocket serverSocket;
    private boolean isFirstClient = true;
    private ClientHandler coordinator = null;
    private final ConcurrentHashMap<String, ClientHandler> clientHandlers = new ConcurrentHashMap<>();
    private Set<String> usedUsernames = new HashSet<>();

    public Server(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public void startServer() {
        try {
            System.out.println("Server is now Online.");
            new Thread(this::checkClientStatus).start();

            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(socket, this);
                Thread thread = new Thread(clientHandler);
                thread.start();
            }
        } catch (IOException e) {
            System.err.println("Server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public synchronized void addClient(String clientUsername, ClientHandler clientHandler) {
        usedUsernames.add(clientUsername);
        System.out.println("Client joined: " + clientUsername);
        clientHandlers.put(clientUsername, clientHandler);

        if (isFirstClient || coordinator == null) {
            isFirstClient = false;
            coordinator = clientHandler;
            coordinator.setCoordinator(true);
            notifyCoordinator(coordinator.getSocket());
        } else {
            notifyClient(clientHandler.getSocket(), coordinator.getClientUsername());
        }

        sendOnlineUsersToCoordinator();
    }

    private void checkClientStatus() {
        while (!serverSocket.isClosed()) {
            try {
                for (ClientHandler clientHandler : clientHandlers.values()) {
                    if (!clientHandler.isAlive()) {
                        removeClient(clientHandler.getClientUsername());
                    }
                }
                Thread.sleep(20000);
            } catch (InterruptedException e) {
                System.err.println("Status check interrupted: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    public void sendPrivateMessage(String senderUsername, String recipientUsername, String message) {
        ClientHandler recipientHandler = clientHandlers.get(recipientUsername);
        if (recipientHandler != null) {
            try {
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(recipientHandler.getSocket().getOutputStream()));
                out.write("PRIVATE:" + senderUsername + ":" + message + "\n");
                out.flush();
            } catch (IOException e) {
                System.err.println("Error sending private message: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }


    private void notifyCoordinator(Socket socket) {
        try {
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            out.write("You are now the coordinator.\n");
            out.flush();
        } catch (IOException e) {
            System.err.println("Error notifying coordinator: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void notifyClient(Socket socket, String coordinatorUsername) {
        try {
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            out.write("The current coordinator is " + coordinatorUsername + "\n");
            out.flush();
        } catch (IOException e) {
            System.err.println("Error notifying client: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public synchronized void removeClient(String clientUsername) {
        clientHandlers.remove(clientUsername);
        usedUsernames.remove(clientUsername);
        System.out.println("Client disconnected: " + clientUsername);
        if (coordinator != null && clientUsername.equals(coordinator.getClientUsername())) {
            assignNewCoordinator();
        }
    }

    private void assignNewCoordinator() {
        if (!clientHandlers.isEmpty()) {
            String newCoordinatorUsername = clientHandlers.keySet().iterator().next();
            coordinator = clientHandlers.get(newCoordinatorUsername);
            coordinator.setCoordinator(true);
            notifyCoordinator(coordinator.getSocket());
            System.out.println("New coordinator assigned: " + newCoordinatorUsername);
            sendOnlineUsersToCoordinator();
        } else {
            coordinator = null;
            isFirstClient = true;
        }
    }

    private void sendOnlineUsersToCoordinator() {
        if (coordinator != null) {
            try {
                StringBuilder details = new StringBuilder("Coordinator Online Users:\n");
                details.append("Username\tIP\tPort\n");
                for (ClientHandler clientHandler : clientHandlers.values()) {
                    details.append(clientHandler.getClientUsername())
                           .append("\t")
                           .append(clientHandler.getSocket().getInetAddress().getHostAddress())
                           .append("\t")
                           .append(clientHandler.getSocket().getPort())
                           .append("\n");
                }
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(coordinator.getSocket().getOutputStream()));
                out.write(details.toString());
                out.flush();
            } catch (IOException e) {
                System.err.println("Error sending online users to coordinator: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public ConcurrentHashMap<String, ClientHandler> getClientHandlers() {
        return clientHandlers;
    }

    public ClientHandler getCoordinator() {
        return coordinator;
    }

    public void closeServerSocket() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(7004);
        Server server = new Server(serverSocket);
        server.startServer();
    }

    public void broadcastMessage(String message) {
        for (ClientHandler clientHandler : clientHandlers.values()) {
            try {
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientHandler.getSocket().getOutputStream()));
                out.write(message + "\n");
                out.flush();
            } catch (IOException e) {
                System.err.println("Error broadcasting message: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}