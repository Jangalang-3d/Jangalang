package jangalang.client;

import jangalang.common.ApplicationProperties;
import jangalang.client.ui.Window;

public class ClientMain {
    public static void main(String[] args) throws Exception {
        String host = ApplicationProperties.get("server.host");
        int tcpPort = ApplicationProperties.getInt("server.tcp");
        NetworkClient network = new NetworkClient(host, tcpPort);
        ClientGame game = new ClientGame(network);
        // Window + renderer use the client-side GameMode (PlayingState adapted)
        Window window = new Window(game);
        game.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                game.stop();
                network.disconnect();
            } catch (Exception ignored) {}
        }));
    }
}
