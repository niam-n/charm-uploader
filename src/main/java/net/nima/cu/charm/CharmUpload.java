package net.nima.cu.charm;

import com.google.gson.Gson;
import net.nima.cu.CharmUploader;
import net.nima.cu.CharmUploaderConfig;
import net.nima.cu.charm.dto.RequestDTO;
import net.nima.cu.charm.dto.ZenithCharmDTO;
import net.nima.cu.util.Chatter;
import net.nima.cu.util.ChatterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CharmUpload {

    private static final Logger LOGGER = LoggerFactory.getLogger(CharmUpload.class);
    private static final Chatter CHATTER = ChatterFactory.getChatter(CharmUpload.class);
    private static final Gson gson = new Gson();

    private final CharmUploaderConfig config;
    private final HttpClient client;

    public CharmUpload() {
        config = CharmUploader.getConfigHolder().getConfig();
        this.client = HttpClient.newHttpClient();
    }

    public void uploadCharms(
            List<ZenithCharmDTO> charms,
            String shard,
            List<Integer> at,
            String type
    ) {
        if (charms.isEmpty()) return;

        postCharms(charms, shard, at, type)
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        LOGGER.info("Charms were sent to the server");
                    } else {
                        CHATTER.error("Failed to upload charms. Status: " + response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    CHATTER.error("Error uploading charms: " + e.getMessage());
                    return null;
                });
    }

    private String getCharmUrl() {
        String serverAddress = config.web.charm_server_address;
        StringBuilder url = new StringBuilder();

        if (serverAddress.startsWith("https://") || serverAddress.startsWith("http://")) {
            url.append(serverAddress);
        } else {
            url.append("https://");
            url.append(serverAddress);
        }

        if (serverAddress.endsWith("/")) {
            url.append("api/v1/charms");
        } else {
            url.append("/api/v1/charms");
        }
        return url.toString();
    }

    private CompletableFuture<HttpResponse<String>> postCharms(
            List<ZenithCharmDTO> charms,
            String shard,
            List<Integer> at,
            String type
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sBody = gson.toJson(new RequestDTO(
                        charms,
                        null,
                        shard,
                        at,
                        type
                ));
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(getCharmUrl()))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(sBody))
                        .build();
                return client.send(req, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                throw new RuntimeException("Failed to post charms", e);
            }
        });
    }

    private String getConsentURL() {
        String serverAddress = config.web.charm_server_address;
        StringBuilder url = new StringBuilder();

        if (serverAddress.startsWith("https://") || serverAddress.startsWith("http://")) {
            url.append(serverAddress);
        } else {
            url.append("https://");
            url.append(serverAddress);
        }

        if (serverAddress.endsWith("/")) {
            url.append("api/v1/charms/allowed");
        } else {
            url.append("/api/v1/charms/allowed");
        }
        return url.toString();
    }

    public CompletableFuture<HttpResponse<String>> getConsentList() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(getConsentURL()))
                        .GET()
                        .build();
                return client.send(req, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                throw new RuntimeException("Failed to get consent list", e);
            }
        });
    }
}