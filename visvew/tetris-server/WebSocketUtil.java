import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * WebSocket 工具类（服务端专用，纯 Java 零依赖）
 */
public class WebSocketUtil {

    private static final String WS_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    // ==================== 服务端握手 ====================

    public static void acceptHandshake(Socket socket) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String key = null;
        String line;
        while ((line = br.readLine()) != null && !line.isEmpty()) {
            if (line.toLowerCase().startsWith("sec-websocket-key:")) {
                key = line.substring(line.indexOf(":") + 1).trim();
            }
        }
        if (key == null) {
            socket.close();
            throw new IOException("不是合法的 WebSocket 请求");
        }

        String acceptKey = computeAcceptKey(key);
        OutputStream os = socket.getOutputStream();
        String response =
                "HTTP/1.1 101 Switching Protocols\r\n" +
                "Upgrade: websocket\r\n" +
                "Connection: Upgrade\r\n" +
                "Sec-WebSocket-Accept: " + acceptKey + "\r\n" +
                "\r\n";
        os.write(response.getBytes(StandardCharsets.UTF_8));
        os.flush();
    }

    // ==================== 帧操作 ====================

    public static final int OP_TEXT = 0x1;
    public static final int OP_BINARY = 0x2;
    public static final int OP_CLOSE = 0x8;

    public static void sendFrame(OutputStream out, int opcode, byte[] payload, boolean mask) throws IOException {
        int len = payload.length;
        ByteArrayOutputStream frame = new ByteArrayOutputStream();
        frame.write(0x80 | opcode);

        byte[] maskKey = null;
        if (mask) {
            maskKey = generateRandomBytes(4);
            if (len < 126) {
                frame.write(0x80 | len);
            } else if (len <= 0xFFFF) {
                frame.write(0x80 | 126);
                frame.write((len >> 8) & 0xFF);
                frame.write(len & 0xFF);
            } else {
                frame.write(0x80 | 127);
                for (int i = 7; i >= 0; i--) frame.write((len >> (i * 8)) & 0xFF);
            }
            frame.write(maskKey);
        } else {
            if (len < 126) {
                frame.write(len);
            } else if (len <= 0xFFFF) {
                frame.write(126);
                frame.write((len >> 8) & 0xFF);
                frame.write(len & 0xFF);
            } else {
                frame.write(127);
                for (int i = 7; i >= 0; i--) frame.write((len >> (i * 8)) & 0xFF);
            }
        }

        if (mask) {
            byte[] masked = new byte[len];
            for (int i = 0; i < len; i++) {
                masked[i] = (byte) (payload[i] ^ maskKey[i % 4]);
            }
            payload = masked;
        }
        frame.write(payload);

        synchronized (out) {
            out.write(frame.toByteArray());
            out.flush();
        }
    }

    public static void sendText(OutputStream out, String text, boolean mask) throws IOException {
        sendFrame(out, OP_TEXT, text.getBytes(StandardCharsets.UTF_8), mask);
    }

    public static void sendBinary(OutputStream out, byte[] data, boolean mask) throws IOException {
        sendFrame(out, OP_BINARY, data, mask);
    }

    public static void sendClose(OutputStream out) throws IOException {
        sendFrame(out, OP_CLOSE, new byte[0], true);
    }

    public static WebSocketFrame readFrame(InputStream in) throws IOException {
        int b0 = in.read();
        if (b0 == -1) return null;
        int opcode = b0 & 0x0F;

        int b1 = in.read();
        if (b1 == -1) return null;
        boolean masked = (b1 & 0x80) != 0;
        long length = b1 & 0x7F;

        if (length == 126) {
            length = ((in.read() & 0xFF) << 8) | (in.read() & 0xFF);
        } else if (length == 127) {
            length = 0;
            for (int i = 0; i < 8; i++) {
                length = (length << 8) | (in.read() & 0xFF);
            }
        }

        byte[] maskKey = null;
        if (masked) {
            maskKey = new byte[4];
            in.read(maskKey);
        }

        byte[] payload = new byte[(int) length];
        int offset = 0;
        while (offset < length) {
            int read = in.read(payload, offset, (int) (length - offset));
            if (read == -1) return null;
            offset += read;
        }

        if (masked) {
            for (int i = 0; i < payload.length; i++) {
                payload[i] ^= maskKey[i % 4];
            }
        }

        return new WebSocketFrame(opcode, payload);
    }

    public static class WebSocketFrame {
        public final int opcode;
        public final byte[] payload;

        public WebSocketFrame(int opcode, byte[] payload) {
            this.opcode = opcode;
            this.payload = payload;
        }

        public String getText() {
            return new String(payload, StandardCharsets.UTF_8);
        }

        public boolean isText() { return opcode == OP_TEXT; }
        public boolean isBinary() { return opcode == OP_BINARY; }
        public boolean isClose() { return opcode == OP_CLOSE; }
    }

    private static byte[] generateRandomBytes(int len) {
        byte[] b = new byte[len];
        for (int i = 0; i < len; i++) b[i] = (byte) (Math.random() * 256);
        return b;
    }

    private static String computeAcceptKey(String key) throws NoSuchAlgorithmException {
        String combined = key + WS_GUID;
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        return Base64.getEncoder().encodeToString(sha1.digest(combined.getBytes(StandardCharsets.UTF_8)));
    }
}