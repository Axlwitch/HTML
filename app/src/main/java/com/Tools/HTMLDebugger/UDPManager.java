package com.Tools.HTMLDebugger;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UDPManager {
    private Context context;
    private WebView webView;
    private ConcurrentHashMap<String, UDPSession> sessions = new ConcurrentHashMap<String, UDPSession>();
    private ExecutorService executor = Executors.newCachedThreadPool();
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    public UDPManager(Context context, WebView webView) {
        this.context = context;
        this.webView = webView;
    }

    @JavascriptInterface
    public String udpSend(String ip, int port, String data, int timeoutMs) {
        try {
            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(timeoutMs > 0 ? timeoutMs : 3000);
            InetAddress address = InetAddress.getByName(ip);
            byte[] buffer = data.getBytes("UTF-8");
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
            socket.send(packet);
            socket.close();
            return "SUCCESS";
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @JavascriptInterface
    public String udpSendHex(String ip, int port, String hexData, int timeoutMs) {
        try {
            byte[] data = hexStringToByteArray(hexData);
            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(timeoutMs > 0 ? timeoutMs : 3000);
            InetAddress address = InetAddress.getByName(ip);
            DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
            socket.send(packet);
            socket.close();
            return "SUCCESS";
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @JavascriptInterface
    public String udpReceive(int port, int bufferSize, int timeoutMs) {
        try {
            DatagramSocket socket = new DatagramSocket(port);
            socket.setSoTimeout(timeoutMs > 0 ? timeoutMs : 5000);
            byte[] buffer = new byte[bufferSize > 0 ? bufferSize : 1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            String result = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
            socket.close();
            return result;
        } catch (SocketTimeoutException e) {
            return "TIMEOUT";
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @JavascriptInterface
    public String udpReceiveHex(int port, int bufferSize, int timeoutMs) {
        try {
            DatagramSocket socket = new DatagramSocket(port);
            socket.setSoTimeout(timeoutMs > 0 ? timeoutMs : 5000);
            byte[] buffer = new byte[bufferSize > 0 ? bufferSize : 1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            String hex = byteArrayToHexString(packet.getData(), packet.getLength());
            socket.close();
            return hex;
        } catch (SocketTimeoutException e) {
            return "TIMEOUT";
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @JavascriptInterface
    public String udpSendReceive(String ip, int port, String data, int bufferSize, int timeoutMs) {
        try {
            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(timeoutMs > 0 ? timeoutMs : 5000);
            InetAddress address = InetAddress.getByName(ip);
            byte[] sendData = data.getBytes("UTF-8");
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
            socket.send(sendPacket);

            byte[] recvBuffer = new byte[bufferSize > 0 ? bufferSize : 1024];
            DatagramPacket recvPacket = new DatagramPacket(recvBuffer, recvBuffer.length);
            socket.receive(recvPacket);
            String result = new String(recvPacket.getData(), 0, recvPacket.getLength(), "UTF-8");
            socket.close();
            return result;
        } catch (SocketTimeoutException e) {
            return "TIMEOUT";
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @JavascriptInterface
    public String udpSendReceiveHex(String ip, int port, String hexData, int bufferSize, int timeoutMs) {
        try {
            byte[] sendData = hexStringToByteArray(hexData);
            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(timeoutMs > 0 ? timeoutMs : 5000);
            InetAddress address = InetAddress.getByName(ip);
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
            socket.send(sendPacket);

            byte[] recvBuffer = new byte[bufferSize > 0 ? bufferSize : 1024];
            DatagramPacket recvPacket = new DatagramPacket(recvBuffer, recvBuffer.length);
            socket.receive(recvPacket);
            String hex = byteArrayToHexString(recvPacket.getData(), recvPacket.getLength());
            socket.close();
            return hex;
        } catch (SocketTimeoutException e) {
            return "TIMEOUT";
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @JavascriptInterface
    public String udpStartSession(String sessionId, String ip, int port, int bufferSize, int timeoutMs) {
        if (sessions.containsKey(sessionId)) {
            return "SESSION_EXISTS";
        }
        try {
            UDPSession session = new UDPSession(sessionId, ip, port, bufferSize, timeoutMs);
            sessions.put(sessionId, session);
            session.startListening();
            return "SESSION_STARTED";
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @JavascriptInterface
    public String udpSendToSession(String sessionId, String data) {
        UDPSession session = sessions.get(sessionId);
        if (session == null) return "SESSION_NOT_FOUND";
        return session.send(data);
    }

    @JavascriptInterface
    public String udpSendHexToSession(String sessionId, String hexData) {
        UDPSession session = sessions.get(sessionId);
        if (session == null) return "SESSION_NOT_FOUND";
        return session.sendHex(hexData);
    }

    @JavascriptInterface
    public String udpStopSession(String sessionId) {
        UDPSession session = sessions.remove(sessionId);
        if (session == null) return "SESSION_NOT_FOUND";
        session.stopListening();
        return "SESSION_STOPPED";
    }

    @JavascriptInterface
    public boolean udpIsSessionActive(String sessionId) {
        return sessions.containsKey(sessionId);
    }

    @JavascriptInterface
    public String udpGetSessionStats(String sessionId) {
        UDPSession session = sessions.get(sessionId);
        if (session == null) return "{}";
        return session.getStats();
    }

    @JavascriptInterface
    public String udpMulticastSend(String groupIp, int port, String data) {
        try {
            InetAddress group = InetAddress.getByName(groupIp);
            MulticastSocket socket = new MulticastSocket();
            byte[] buffer = data.getBytes("UTF-8");
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, port);
            socket.send(packet);
            socket.close();
            return "SUCCESS";
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @JavascriptInterface
    public String udpMulticastJoin(String groupIp, int port, int bufferSize, int timeoutMs) {
        try {
            InetAddress group = InetAddress.getByName(groupIp);
            MulticastSocket socket = new MulticastSocket(port);
            socket.setSoTimeout(timeoutMs > 0 ? timeoutMs : 5000);
            socket.joinGroup(group);

            byte[] buffer = new byte[bufferSize > 0 ? bufferSize : 1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            String result = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
            socket.leaveGroup(group);
            socket.close();
            return result;
        } catch (SocketTimeoutException e) {
            return "TIMEOUT";
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @JavascriptInterface
    public String udpMulticastJoinHex(String groupIp, int port, int bufferSize, int timeoutMs) {
        try {
            InetAddress group = InetAddress.getByName(groupIp);
            MulticastSocket socket = new MulticastSocket(port);
            socket.setSoTimeout(timeoutMs > 0 ? timeoutMs : 5000);
            socket.joinGroup(group);

            byte[] buffer = new byte[bufferSize > 0 ? bufferSize : 1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            String hex = byteArrayToHexString(packet.getData(), packet.getLength());
            socket.leaveGroup(group);
            socket.close();
            return hex;
        } catch (SocketTimeoutException e) {
            return "TIMEOUT";
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @JavascriptInterface
    public String getLocalIPs() {
        StringBuilder sb = new StringBuilder();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp()) continue;
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    String ip = addr.getHostAddress();
                    if (ip.contains(":") || ip.startsWith("127.")) continue;
                    sb.append(ip).append(";");
                }
            }
        } catch (SocketException e) {
            return "";
        }
        return sb.toString();
    }

    @JavascriptInterface
    public boolean pingHost(String ip, int timeoutMs) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            return address.isReachable(timeoutMs > 0 ? timeoutMs : 3000);
        } catch (Exception e) {
            return false;
        }
    }

    @JavascriptInterface
    public String resolveHost(String hostname) {
        try {
            InetAddress address = InetAddress.getByName(hostname);
            return address.getHostAddress();
        } catch (UnknownHostException e) {
            return "";
        }
    }

    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
				+ Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    private String byteArrayToHexString(byte[] bytes, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(String.format("%02X", bytes[i]));
        }
        return sb.toString();
    }

    private class UDPSession {
        private String sessionId;
        private String ip;
        private int port;
        private int bufferSize;
        private int timeoutMs;
        private DatagramSocket socket;
        private boolean isRunning = false;
        private long packetsSent = 0;
        private long packetsReceived = 0;
        private long bytesSent = 0;
        private long bytesReceived = 0;
        private String lastError = "";

        public UDPSession(String sessionId, String ip, int port, int bufferSize, int timeoutMs) 
		throws SocketException, UnknownHostException {
            this.sessionId = sessionId;
            this.ip = ip;
            this.port = port;
            this.bufferSize = bufferSize > 0 ? bufferSize : 1024;
            this.timeoutMs = timeoutMs > 0 ? timeoutMs : 5000;
            this.socket = new DatagramSocket();
            this.socket.setSoTimeout(timeoutMs > 0 ? timeoutMs : 5000);
        }

        public void startListening() {
            if (isRunning) return;
            isRunning = true;
            executor.execute(new Runnable() {
					@Override
					public void run() {
						listenLoop();
					}
				});
        }

        private void listenLoop() {
            byte[] buffer = new byte[bufferSize];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                InetAddress address = InetAddress.getByName(ip);
                while (isRunning && !socket.isClosed()) {
                    try {
                        socket.receive(packet);
                        if (packet.getLength() > 0) {
                            packetsReceived++;
                            bytesReceived += packet.getLength();
                            final String data = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
                            final String hex = byteArrayToHexString(packet.getData(), packet.getLength());
                            mainHandler.post(new Runnable() {
									@Override
									public void run() {
										if (webView != null) {
											String js = String.format(
												"if (typeof window.udpCallback === 'function') { "
												+ "window.udpCallback('%s', '%s', '%s', %d); "
												+ "}",
												sessionId, data.replace("'", "\\'"), hex, System.currentTimeMillis()
											);
											webView.loadUrl("javascript:" + js);
										}
									}
								});
                        }
                    } catch (SocketTimeoutException e) {
                    } catch (IOException e) {
                        lastError = e.getMessage();
                        if (isRunning) {
                            try { Thread.sleep(100); } catch (InterruptedException ie) {}
                        }
                    }
                }
            } catch (Exception e) {
                lastError = e.getMessage();
            }
        }

        public String send(String data) {
            try {
                byte[] sendData = data.getBytes("UTF-8");
                InetAddress address = InetAddress.getByName(ip);
                DatagramPacket packet = new DatagramPacket(sendData, sendData.length, address, port);
                socket.send(packet);
                packetsSent++;
                bytesSent += sendData.length;
                return "SENT";
            } catch (Exception e) {
                return "ERROR: " + e.getMessage();
            }
        }

        public String sendHex(String hexData) {
            try {
                byte[] sendData = hexStringToByteArray(hexData);
                InetAddress address = InetAddress.getByName(ip);
                DatagramPacket packet = new DatagramPacket(sendData, sendData.length, address, port);
                socket.send(packet);
                packetsSent++;
                bytesSent += sendData.length;
                return "SENT";
            } catch (Exception e) {
                return "ERROR: " + e.getMessage();
            }
        }

        public void stopListening() {
            isRunning = false;
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }

        public String getStats() {
            return "{" +
                "\"sessionId\":\"" + sessionId + "\"," +
                "\"ip\":\"" + ip + "\"," +
                "\"port\":" + port + "," +
                "\"packetsSent\":" + packetsSent + "," +
                "\"packetsReceived\":" + packetsReceived + "," +
                "\"bytesSent\":" + bytesSent + "," +
                "\"bytesReceived\":" + bytesReceived + "," +
                "\"isRunning\":" + isRunning + "," +
                "\"lastError\":\"" + lastError + "\"" +
                "}";
        }
    }
}
