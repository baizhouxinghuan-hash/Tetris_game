package com.he.network;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * 网络服务：支持局域网直连和互联网中转两种模式
 */
public class NetworkService {

    public enum Mode { LAN, INTERNET }

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private BufferedReader textReader;
    private PrintWriter textWriter;
    private volatile boolean isRunning = false;
    private Mode mode;

    // ==================== 局域网模式（保留不动）====================

    /** 房主：监听端口等待对手直连 */
    public void hostGame(int port) throws Exception {
        this.mode = Mode.LAN;
        ServerSocket server = new ServerSocket(port);
        System.out.println("[局域网] 等待对手连接...");
        this.socket = server.accept();
        System.out.println("[局域网] 对手已连接！");
        server.close();
        setupStreams();
    }

    /** 挑战者：直连房主 IP */
    public void joinGame(String ip, int port) throws Exception {
        this.mode = Mode.LAN;
        System.out.println("[局域网] 正在连接房主 " + ip + ":" + port + " ...");
        this.socket = new Socket(ip, port);
        System.out.println("[局域网] 连接成功！");
        setupStreams();
    }

    // ==================== 互联网模式（WebSocket）====================

    /**
     * 连接中转服务器（通过 WebSocket）
     */
    public void connectToServer(String host, int port) throws Exception {
        this.mode = Mode.INTERNET;
        System.out.println("[互联网] 正在连接服务器 " + host + ":" + port + " (WebSocket)...");
        this.socket = WebSocketUtil.connect(host, port, "/");
        System.out.println("[互联网] WebSocket 握手成功");
    }

    /**
     * 在服务器上创建房间
     * @param roomName 房间名
     * @return 服务器响应 ("WAITING ..." / "ROOM_EXISTS ..." / "ERROR ...")
     */
    public String createRoom(String roomName) throws Exception {
        WebSocketUtil.sendText(socket.getOutputStream(), "CREATE " + roomName, true);

        WebSocketUtil.WebSocketFrame responseFrame = WebSocketUtil.readFrame(socket.getInputStream());
        if (responseFrame == null || !responseFrame.isText()) return "ERROR 无响应";
        String response = responseFrame.getText();
        System.out.println("[互联网] 服务器响应: " + response);

        if (response.startsWith("WAITING")) {
            // 等待配对，阻塞读取直到 "START"
            WebSocketUtil.WebSocketFrame matchFrame = WebSocketUtil.readFrame(socket.getInputStream());
            if (matchFrame == null || !matchFrame.isText()) return "ERROR 配对失败";
            String matchResponse = matchFrame.getText();
            System.out.println("[互联网] 配对结果: " + matchResponse);
            if (matchResponse.startsWith("START")) {
                setupStreams();
                return "START";
            }
            return matchResponse;
        }
        return response;
    }

    /**
     * 加入服务器上已有房间
     * @param roomName 房间名
     * @return 服务器响应 ("START ..." / "ROOM_NOT_FOUND ..." / "ROOM_FULL ...")
     */
    public String joinRoom(String roomName) throws Exception {
        WebSocketUtil.sendText(socket.getOutputStream(), "JOIN " + roomName, true);

        WebSocketUtil.WebSocketFrame responseFrame = WebSocketUtil.readFrame(socket.getInputStream());
        if (responseFrame == null || !responseFrame.isText()) return "ERROR 无响应";
        String response = responseFrame.getText();
        System.out.println("[互联网] 服务器响应: " + response);

        if (response.startsWith("START")) {
            setupStreams();
        }
        return response;
    }

    // ==================== 共用方法 ====================

    /** 初始化流 */
    private void setupStreams() throws Exception {
        if (mode == Mode.INTERNET) {
            // WebSocket 模式：直接用 Socket 原始流，数据通过 WebSocketUtil 帧化
            isRunning = true;
        } else {
            // 局域网模式：ObjectStream
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
            isRunning = true;
        }
    }

    /** 发送游戏状态快照给对手 */
    public void sendData(SyncData data) {
        if (!isRunning) return;
        try {
            if (mode == Mode.INTERNET) {
                // WebSocket 模式：序列化为二进制帧发送
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos);
                oos.writeObject(data);
                oos.flush();
                WebSocketUtil.sendBinary(socket.getOutputStream(), bos.toByteArray(), true);
                oos.close();
            } else {
                out.reset();
                out.writeObject(data);
                out.flush();
            }
        } catch (Exception e) {
            System.out.println("[网络] 发送失败，连接断开");
            isRunning = false;
        }
    }

    /** 开启后台线程持续接收对手数据 */
    public void startListening(Consumer<SyncData> onDataReceived) {
        new Thread(() -> {
            while (isRunning) {
                try {
                    SyncData data;

                    if (mode == Mode.INTERNET) {
                        // WebSocket 模式：读取二进制帧 → 反序列化
                        WebSocketUtil.WebSocketFrame frame = WebSocketUtil.readFrame(socket.getInputStream());
                        if (frame == null || frame.isClose()) {
                            System.out.println("[网络] 对手已断开连接");
                            isRunning = false;
                            break;
                        }
                        if (frame.isText()) {
                            // 服务器发来的文本命令（如 CLOSE）
                            String text = frame.getText();
                            System.out.println("[网络] 服务器消息: " + text);
                            if (text.startsWith("CLOSE")) {
                                System.out.println("[网络] 对手已断开连接");
                                isRunning = false;
                                break;
                            }
                            continue;
                        }
                        if (!frame.isBinary()) continue;
                        ByteArrayInputStream bis = new ByteArrayInputStream(frame.payload);
                        ObjectInputStream ois = new ObjectInputStream(bis);
                        data = (SyncData) ois.readObject();
                        ois.close();
                    } else {
                        data = (SyncData) in.readObject();
                    }

                    onDataReceived.accept(data);
                } catch (EOFException e) {
                    System.out.println("[网络] 对手已断开连接");
                    isRunning = false;
                    break;
                } catch (Exception e) {
                    if (isRunning) {
                        System.out.println("[网络] 连接异常: " + e.getMessage());
                    }
                    isRunning = false;
                    break;
                }
            }
        }).start();
    }

    /** 关闭连接 */
    public void close() {
        isRunning = false;
        try {
            if (mode == Mode.INTERNET) {
                WebSocketUtil.sendClose(socket.getOutputStream());
            }
        } catch (Exception ignored) {}
        try { if (out != null) out.close(); } catch (Exception e) {}
        try { if (in != null) in.close(); } catch (Exception e) {}
        try { if (textReader != null) textReader.close(); } catch (Exception e) {}
        try { if (textWriter != null) textWriter.close(); } catch (Exception e) {}
        try { if (socket != null) socket.close(); } catch (Exception e) {}
    }

    public boolean isRunning() { return isRunning; }
}