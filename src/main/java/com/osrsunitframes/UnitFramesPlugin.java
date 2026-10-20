package com.osrsunitframes;

import com.google.gson.JsonObject;
import com.google.inject.Provides;
import com.google.gson.Gson;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import net.runelite.client.util.Filepath;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.InteractingChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@PluginDescriptor(name = "Taika Mal", description = "Player and NPC status frames", internalName = "osrs-unit-frames")
public class UnitFramesPlugin extends Plugin
{
    @Inject private Client client;
    @Inject private OverlayManager overlayManager;
    @Inject private PlayerFrameOverlay playerOverlay;
    @Inject private TargetFrameOverlay targetOverlay;
    @Inject private UnitFramesConfig config;
    @Inject private OkHttpClient httpClient;
    @Inject private Gson gson;

    private NPC target;
    private Instant targetSeenAt;
    private final Map<Integer, BufferedImage> npcPortraits = new ConcurrentHashMap<>();
    private final Set<Integer> portraitLoads = ConcurrentHashMap.newKeySet();
    private final Map<Integer, Call> portraitCalls = new ConcurrentHashMap<>();
    private ExecutorService portraitIo;

    @Override
    protected void startUp()
    {
        portraitIo = Executors.newSingleThreadExecutor(r ->
        {
            Thread t = new Thread(r, "unit-frame-portrait-cache");
            t.setDaemon(true);
            return t;
        });
        overlayManager.add(playerOverlay);
        overlayManager.add(targetOverlay);
    }

    @Override
    protected void shutDown()
    {
        overlayManager.remove(playerOverlay);
        overlayManager.remove(targetOverlay);
        target = null;
        targetSeenAt = null;
        for (Call call : portraitCalls.values()) call.cancel();
        portraitCalls.clear();
        if (portraitIo != null) portraitIo.shutdownNow();
        portraitLoads.clear();
        npcPortraits.clear();
    }

    @Subscribe
    public void onInteractingChanged(InteractingChanged event)
    {
        if (client.getLocalPlayer() == null || event.getSource() != client.getLocalPlayer())
        {
            return;
        }
        Actor actor = event.getTarget();
        if (actor instanceof NPC)
        {
            target = (NPC) actor;
            targetSeenAt = Instant.now();
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() != GameState.LOGGED_IN)
        {
            target = null;
            targetSeenAt = null;
        }
    }

    NPC getTarget()
    {
        if (target != null && targetSeenAt != null && targetSeenAt.plusSeconds(10).isBefore(Instant.now()))
        {
            target = null;
            targetSeenAt = null;
        }
        return target;
    }

    UnitFramesConfig getConfig() { return config; }

    BufferedImage getNpcPortrait(NPC npc)
    {
        if (!config.useWikiNpcPortraits() || npc == null || npc.getId() < 0)
        {
            return null;
        }
        int npcId = npc.getId();
        BufferedImage image = npcPortraits.get(npcId);
        if (image == null && portraitLoads.add(npcId) && portraitIo != null)
        {
            String npcName = npc.getName();
            portraitIo.execute(() -> loadNpcPortrait(npcId, npcName));
        }
        return image;
    }

    private void loadNpcPortrait(int npcId, String npcName)
    {
        try
        {
            Filepath file = getPluginDirectory().joinSegment("npc-" + npcId + ".png");
            if (file.exists())
            {
                try (java.io.InputStream in = file.openInputStream())
                {
                    BufferedImage cached = ImageIO.read(in);
                    if (cached != null)
                    {
                        npcPortraits.put(npcId, cached);
                        return;
                    }
                }
            }
            if (npcName == null || npcName.trim().isEmpty())
            {
                portraitLoads.remove(npcId);
                return;
            }

            HttpUrl api = HttpUrl.parse("https://oldschool.runescape.wiki/api.php").newBuilder()
                .addQueryParameter("action", "query")
                .addQueryParameter("format", "json")
                .addQueryParameter("prop", "pageimages")
                .addQueryParameter("piprop", "thumbnail")
                .addQueryParameter("pithumbsize", "180")
                .addQueryParameter("titles", npcName)
                .build();
            Request request = new Request.Builder().url(api).header("User-Agent", "OSRSUnitFrames/0.1 (RuneLite plugin)").build();
            Call call = httpClient.newCall(request);
            portraitCalls.put(npcId, call);
            call.enqueue(new okhttp3.Callback()
            {
                @Override
                public void onFailure(Call failed, IOException e)
                {
                    portraitCalls.remove(npcId, failed);
                    portraitLoads.remove(npcId);
                }

                @Override
                public void onResponse(Call responseCall, Response response) throws IOException
                {
                    portraitCalls.remove(npcId, responseCall);
                    String imageUrl = null;
                    try (Response r = response)
                    {
                        if (!r.isSuccessful() || r.body() == null)
                        {
                            portraitLoads.remove(npcId);
                            return;
                        }
                        JsonObject root = gson.fromJson(r.body().charStream(), JsonObject.class);
                        JsonObject pages = root == null ? null : root.getAsJsonObject("query").getAsJsonObject("pages");
                        if (pages == null)
                        {
                            portraitLoads.remove(npcId);
                            return;
                        }
                        for (Map.Entry<String, com.google.gson.JsonElement> entry : pages.entrySet())
                        {
                            JsonObject page = entry.getValue().getAsJsonObject();
                            if (page.has("thumbnail")) imageUrl = page.getAsJsonObject("thumbnail").get("source").getAsString();
                            break;
                        }
                    }
                    catch (RuntimeException e)
                    {
                        portraitLoads.remove(npcId);
                        return;
                    }
                    if (imageUrl == null) return;
                    Request imageRequest = new Request.Builder().url(imageUrl).header("User-Agent", "OSRSUnitFrames/0.1 (RuneLite plugin)").build();
                    Call imageCall = httpClient.newCall(imageRequest);
                    portraitCalls.put(npcId, imageCall);
                    imageCall.enqueue(new okhttp3.Callback()
                    {
                        @Override
                        public void onFailure(Call failed, IOException e)
                        {
                            portraitCalls.remove(npcId, failed);
                            portraitLoads.remove(npcId);
                        }

                        @Override
                        public void onResponse(Call downloaded, Response imageResponse) throws IOException
                        {
                            portraitCalls.remove(npcId, downloaded);
                            try (Response r = imageResponse)
                            {
                                if (!r.isSuccessful() || r.body() == null) return;
                                byte[] bytes = r.body().bytes();
                                BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
                                if (image == null) return;
                                npcPortraits.put(npcId, image);
                                Filepath destination = getPluginDirectory().joinSegment("npc-" + npcId + ".png");
                                destination.write(bytes);
                            }
                            finally
                            {
                                portraitLoads.remove(npcId);
                            }
                        }
                    });
                }
            });
        }
        catch (Exception e)
        {
            portraitLoads.remove(npcId);
        }
    }

    @Provides
    UnitFramesConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(UnitFramesConfig.class);
    }
}
