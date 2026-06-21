import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 俄罗斯方块联机中转服务器（独立运行）
 * 支持 Cloudflare Tunnel / WebSocket
 *
 * 启动: javac WebSocketUtil.java RelayServer.java && java RelayServer 3202
 */
public class RelayServer {

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        int port = 3202;
        if (args.length > 0) {
            try { port = Integer.parseInt(args[0]); } catch (NumberFormatException ignored) {}
        }
        new RelayServer().start(port);
    }

    public void start(int port) {
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("=================================");
            System.out.println("  俄罗斯方块联机服务器");
            System.out.println("  监听端口: " + port);
            System.out.println("  等待客户端连接...");
            System.out.println("=================================");

            while (true) {
                Socket socket = server.accept();
                String ip = socket.getInetAddress().getHostAddress();
                System.out.println("[连接] " + ip + " 已连接");
                new Thread(() -> handleClient(socket, ip)).start();
            }
        } catch (IOException e) {
            System.err.println("服务器异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleClient(Socket socket, String ip) {
        try {
            WebSocketUtil.acceptHandshake(socket);
            System.out.println("[WS] " + ip + " WebSocket 握手成功");

            WebSocketUtil.WebSocketFrame cmdFrame = WebSocketUtil.readFrame(socket.getInputStream());
            if (cmdFrame == null || !cmdFrame.isText()) {
                socket.close();
                return;
            }
            String command = cmdFrame.getText();
            System.out.println("[命令] " + ip + " -> " + command);

            String[] parts = command.split(" ", 2);
            if (parts.length < 2) {
                WebSocketUtil.sendText(socket.getOutputStream(), "ERROR 命令格式错误", false);
                socket.close();
                return;
            }

            String action = parts[0].toUpperCase();
            String roomName = parts[1].trim();

            if (roomName.isEmpty()) {
                WebSocketUtil.sendText(socket.getOutputStream(), "ERROR 房间名不能为空", false);
                socket.close();
                return;
            }

            switch (action) {
                case "CREATE" -> handleCreate(socket, roomName, ip);
                case "JOIN" -> handleJoin(socket, roomName, ip);
                default -> {
                    WebSocketUtil.sendText(socket.getOutputStream(), "ERROR 未知命令", false);
                    socket.close();
                }
            }
        } catch (Exception e) {
            System.out.println("[断开] " + ip + " 异常: " + e.getMessage());
        }
    }

    private void handleCreate(Socket socket, String roomName, String ip) throws IOException {
        if (rooms.containsKey(roomName)) {
            WebSocketUtil.sendText(socket.getOutputStream(), "ROOM_EXISTS 房间名已被占用", false);
            socket.close();
            System.out.println("[房间] " + roomName + " 创建失败(已存在)");
            return;
        }

        Room room = new Room(roomName);
        room.host = new ClientHandler(socket, room, true, ip);
        rooms.put(roomName, room);

        WebSocketUtil.sendText(socket.getOutputStream(), "WAITING 房间创建成功，等待对手...", false);
        System.out.println("[房间] " + roomName + " 已创建，等待挑战者...");
        room.host.start();
    }

    private void handleJoin(Socket socket, String roomName, String ip) throws IOException {
        Room room = rooms.get(roomName);

        if (room == null) {
            WebSocketUtil.sendText(socket.getOutputStream(), "ROOM_NOT_FOUND 房间不存在", false);
            socket.close();
            System.out.println("[房间] " + roomName + " 加入失败(不存在)");
            return;
        }

        if (room.guest != null) {
            WebSocketUtil.sendText(socket.getOutputStream(), "ROOM_FULL 房间已满", false);
            socket.close();
            System.out.println("[房间] " + roomName + " 加入失败(已满)");
            return;
        }

        room.guest = new ClientHandler(socket, room, false, ip);
        WebSocketUtil.sendText(socket.getOutputStream(), "START 成功加入，游戏开始！", false);
        System.out.println("[房间] " + roomName + " 配对成功！");

        room.host.sendCommand("START 对手已加入！");
        room.guest.start();
    }

    // ==================== 内部类 ====================

    static class Room {
        final String name;
        ClientHandler host;
        ClientHandler guest;

        Room(String name) { this.name = name; }

        ClientHandler opponent(ClientHandler me) {
            return (me == host) ? guest : host;
        }
    }

    static class ClientHandler extends Thread {
        private final Socket socket;
        private final Room room;
        private final boolean isHost;
        private final String ip;
        private volatile boolean running = true;

        ClientHandler(Socket socket, Room room, boolean isHost, String ip) {
            this.socket = socket;
            this.room = room;
            this.isHost = isHost;
            this.ip = ip;
        }

        void sendCommand(String msg) {
            try {
                WebSocketUtil.sendText(socket.getOutputStream(), msg, false);
            } catch (IOException e) {
                running = false;
            }
        }

        void close() {
            running = false;
            try { socket.close(); } catch (IOException ignored) {}
        }

        @Override
        public void run() {
            try {
                String role = isHost ? "房主" : "挑战者";
                System.out.println("[" + role + "] " + ip + " 数据通道就绪");
                InputStream in = socket.getInputStream();

                while (running) {
                    WebSocketUtil.WebSocketFrame frame = WebSocketUtil.readFrame(in);
                    if (frame == null || frame.isClose()) break;

                    // 只转发二进制帧（游戏数据），原始字节直接透传
                    if (frame.isBinary()) {
                        ClientHandler opp = room.opponent(this);
                        if (opp != null && opp.running) {
                            WebSocketUtil.sendBinary(opp.socket.getOutputStream(), frame.payload, false);
                        }
                    }
                }
            } catch (EOFException e) {
                System.out.println("[" + (isHost ? "房主" : "挑战者") + "] " + ip + " 正常断开");
            } catch (Exception e) {
                if (running) {
                    System.out.println("[" + (isHost ? "房主" : "挑战者") + "] " + ip + " 异常: " + e.getMessage());
                }
            } finally {
                running = false;
                ClientHandler opp = room.opponent(this);
                if (opp != null) {
                    opp.sendCommand("CLOSE 对手已断开连接");
                    opp.close();
                }
                try { socket.close(); } catch (IOException ignored) {}
            }
        }
    }
}