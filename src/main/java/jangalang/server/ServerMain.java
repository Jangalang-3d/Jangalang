package jangalang.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jangalang.common.ApplicationProperties;
import jangalang.common.maps.MapData;
import jangalang.common.maps.MapLoader;

public class ServerMain {
    private static final String SERVER_SPLASH =
        "   ___                         _                   \n" +
        "  |_  |                       | |                  \n" +
        "    | | __ _ _ __   __ _  __ _| | __ _ _ __   __ _ \n" +
        "    | |/ _` | '_ \\ / _` |/ _` | |/ _` | '_ \\ / _` |\n" +
        "/\\__/ / (_| | | | | (_| | (_| | | (_| | | | | (_| |\n" +
        "\\____/ \\__,_|_| |_|\\__, |\\__,_|_|\\__,_|_| |_|\\__, |\n" +
        "                    __/ |                     __/ |\n" +
        "                   |___/                     |___/ \n";

    public static void main(String[] args) {
        int tcpPort = ApplicationProperties.getInt("server.tcp");
        int udpPort = ApplicationProperties.getInt("server.udp");

        System.out.println(SERVER_SPLASH);
        System.out.printf("Server starting (tcp=%d udp=%d)%n", tcpPort, udpPort);

        MapData map = MapLoader.parseMap("/maps/example.map");

        GameServer server = new GameServer(map, udpPort);
        server.start();

        try {
            ServerSocket ss = new ServerSocket(tcpPort);

            ExecutorService acceptPool = Executors.newCachedThreadPool();
            acceptPool.submit(() -> {
                while (true) {
                    Socket s = ss.accept();
                    acceptPool.submit(new TcpClientHandler(s, server));
                }
            });

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down server");
                try {
                    ss.close();
                } catch (Exception ignored) {}

                server.stop();
                acceptPool.shutdownNow();
            }));
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
