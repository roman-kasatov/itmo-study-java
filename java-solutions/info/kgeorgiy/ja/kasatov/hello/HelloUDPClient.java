package info.kgeorgiy.ja.kasatov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.*;

public class HelloUDPClient implements HelloClient {

    private static final int SOCKET_TIMEOUT = 100;

    /**
     * Runs {@link HelloUDPClient#run(String, int, String, int, int)} with arguments.
     * @param args must contain: <br>
     *             имя или ip-адрес компьютера, на котором запущен сервер; <br>
     *             номер порта, на который отсылать запросы; <br>
     *             префикс запросов (строка); <br>
     *             число параллельных потоков запросов; <br>
     *             число запросов в каждом потоке.
     */
    public static void main(String[] args) {
        if (args == null || Arrays.stream(args).anyMatch(Objects::isNull) || args.length != 5) {
            System.err.println("Incorrect arguments");
            return;
        }
        new HelloUDPClient().run(
                args[0],
                Integer.parseInt(args[1]),
                args[2],
                Integer.parseInt(args[3]),
                Integer.parseInt(args[4])
        );
    }

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        SocketAddress serverAddress;
        try {
            serverAddress = new InetSocketAddress(InetAddress.getByName(host), port);
        } catch (UnknownHostException e) {
            System.err.println("Can't resolve host: " + e.getMessage());
            return;
        }

        ExecutorService threadsPool = Executors.newFixedThreadPool(threads);

        for (int threadNmb = 0; threadNmb < threads; threadNmb++) {
            threadsPool.submit(new ClientThread(threadNmb, requests, serverAddress, prefix));
        }

        MyUDPUtils.closeThreadPool(threadsPool, "Client");
    }

    private static class ClientThread implements Runnable {

        private final int threadNmb;
        private final int requests;
        private final String prefix;
        private int requestNmb = 0;
        private final SocketAddress serverAddress;
        private int BUFF_SIZE;

        private String createRequestText() {
            return prefix + (threadNmb + 1) + "_" + (requestNmb + 1);
        }

        private String preprocessAnswer(String string) {
            StringBuilder sb = new StringBuilder();
            for (char c : string.toCharArray()) {
                if (Character.isDigit(c)) {
                    sb.append(Character.getNumericValue(c));
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        private boolean makeRequest(DatagramSocket socket) {
            String request = createRequestText();
            byte[] bytes = request.getBytes();
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length, serverAddress);
            System.out.println(request);
           if (!MyUDPUtils.sendPacket(socket, packet, "Client")) {
                return false;
            }
            packet.setData(new byte[BUFF_SIZE]);
            try {
                socket.receive(packet);
            } catch (IOException e) { // includes SocketTimeoutException
                // waiting for answer
                return false;
            }
            String answer = new String(packet.getData(), packet.getOffset(),
                    packet.getLength(), StandardCharsets.UTF_8);
            System.out.println(answer);
            return preprocessAnswer(answer).contains(request);
        }

        public ClientThread(int threadNmb, int requests, SocketAddress serverAddress, String prefix) {
            this.threadNmb = threadNmb;
            this.requests = requests;
            this.serverAddress = serverAddress;
            this.prefix = prefix;
        }

        @Override
        public void run() {
            try (DatagramSocket socket = new DatagramSocket()) {
                BUFF_SIZE = socket.getReceiveBufferSize();
                socket.setSoTimeout(SOCKET_TIMEOUT);
                for (requestNmb = 0; requestNmb < requests; requestNmb++) {
                    while (!makeRequest(socket)) {
                        // Continue making requests
                    }
                }

            } catch (SocketException e) {
                System.err.println("Can't connect to server: " + e.getMessage());
            }
        }
    }
}
