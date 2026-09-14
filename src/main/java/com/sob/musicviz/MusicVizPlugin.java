package com.sob.musicviz;

import com.google.inject.Provides;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MidiRequest;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
    name = "Music Visualizer",
    description = "Flashes nearby scenery or floor tiles to OSRS music or Windows PC audio.",
    tags = {"music", "visualizer", "overlay", "midi", "audio", "tiles"}
)
public class MusicVizPlugin extends Plugin
{
    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private MusicVizConfig config;
    @Inject private OverlayManager overlayManager;
    @Inject private MusicVizOverlay overlay;
    @Inject private AudioStatusOverlay audioStatusOverlay;
    @Inject private MidiScheduler scheduler;
    @Inject private ObjectScanner scanner;
    @Inject private FlashStore flashes;
    @Inject private MidiCacheLoader cacheLoader;

    private ScheduledExecutorService pollExec;
    private ScheduledFuture<?> pollTask;
    private volatile int lastArchiveId = Integer.MIN_VALUE;
    private final Random rr = new Random();
    private int rrCursor = 0;
    private volatile boolean running;
    private volatile long sourceGeneration;
    private volatile PcAudioCapture pcAudio;

    PcAudioCapture pcAudio() { return pcAudio; }

    @Override
    protected void startUp()
    {
        overlayManager.add(overlay);
        overlayManager.add(audioStatusOverlay);
        running = true;
        restartSource();
    }

    @Override
    protected void shutDown()
    {
        running = false;
        stopSource();
        overlayManager.remove(overlay);
        overlayManager.remove(audioStatusOverlay);
        flashes.clear();
        lastArchiveId = Integer.MIN_VALUE;
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!"musicviz".equals(event.getGroup())) return;
        String key = event.getKey();
        if ("targetType".equals(key) || "radius".equals(key))
        {
            clientThread.invoke(() -> {
                if (running) { flashes.clear(); scanner.refresh(config.radius(), config.targetType()); }
            });
        }
        if ("audioSource".equals(key)
            || "pollIntervalMs".equals(key) || "syncOffsetMs".equals(key))
        {
            clientThread.invoke(() -> { if (running) restartSource(); });
        }
    }

    private void stopSource()
    {
        sourceGeneration++;
        stopPolling();
        scheduler.stop();
        if (pcAudio != null) { pcAudio.stop(); pcAudio = null; }
        lastArchiveId = Integer.MIN_VALUE;
        flashes.clear();
        scanner.clear();
    }

    private void restartSource()
    {
        stopSource();
        long generation = sourceGeneration;
        clientThread.invoke(() -> {
            if (running && generation == sourceGeneration) scanner.refresh(config.radius(), config.targetType());
        });
        java.util.function.Consumer<NoteEvent> sink = event -> clientThread.invoke(() -> {
            if (running && generation == sourceGeneration) onNote(event);
        });
        if (config.audioSource() == MusicVizConfig.AudioSource.PC_AUDIO)
        {
            pcAudio = new PcAudioCapture();
            pcAudio.start(config::audioSensitivity, sink);
        }
        else
        {
            scheduler.start(sink);
            startPolling();
        }
    }

    @Provides
    MusicVizConfig provideConfig(ConfigManager cm)
    {
        return cm.getConfig(MusicVizConfig.class);
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged ev)
    {
        flashes.clear();
        scanner.clear();
    }

    @Subscribe
    public void onGameTick(GameTick ev)
    {
        scanner.refresh(config.radius(), config.targetType());
    }

    private void startPolling()
    {
        pollExec = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "musicviz-trackpoll");
            t.setDaemon(true);
            return t;
        });
        pollTask = pollExec.scheduleAtFixedRate(
            () -> clientThread.invoke(this::pollActiveTrack),
            500, config.pollIntervalMs(), TimeUnit.MILLISECONDS);
    }

    private void stopPolling()
    {
        if (pollTask != null) { pollTask.cancel(false); pollTask = null; }
        if (pollExec != null) { pollExec.shutdownNow(); pollExec = null; }
    }

    /**
     * Runs on client thread. Detects active-track change by reference
     * identity of the first MidiRequest. On change, kicks off off-thread
     * load + parse so we don't stall the client.
     */
    private void pollActiveTrack()
    {
        if (!running || config.audioSource() != MusicVizConfig.AudioSource.OSRS_MIDI) return;
        try
        {
            List<MidiRequest> active = client.getActiveMidiRequests();
            if (active == null || active.isEmpty())
            {
                if (lastArchiveId != Integer.MIN_VALUE)
                {
                    lastArchiveId = Integer.MIN_VALUE;
                    scheduler.loadTrack(java.util.Collections.emptyList(), System.nanoTime());
                }
                return;
            }
            MidiRequest req = active.get(0);
            int archiveId = req.getArchiveId();
            boolean jingle = req.isJingle();
            if (archiveId == lastArchiveId) return;
            lastArchiveId = archiveId;
            long startNanos = System.nanoTime() - TimeUnit.MILLISECONDS.toNanos(config.syncOffsetMs());
            // Cache read is fast (in-memory in the running client) so we do it on
            // the client thread; only the SMF conversion + note flattening get
            // pushed off-thread.
            byte[] cacheBytes = cacheLoader.load(archiveId, jingle);
            parseAsync(archiveId, cacheBytes, startNanos);
        }
        catch (Throwable t)
        {
            log.debug("pollActiveTrack failed", t);
        }
    }

    private void parseAsync(int archiveId, byte[] cacheBytes, long startNanos)
    {
        if (pollExec == null) return;
        long generation = sourceGeneration;
        if (cacheBytes == null)
        {
            if (config.logCacheMisses())
            {
                log.info("musicviz: no cache bytes for archive {} — idle", archiveId);
            }
            scheduler.loadTrack(java.util.Collections.emptyList(), startNanos);
            return;
        }
        pollExec.submit(() -> {
            try
            {
                List<NoteEvent> events = MidiTrackResolver.flatten(cacheBytes);
                log.debug("musicviz: parsed {} note events from archive {}", events.size(), archiveId);
                clientThread.invoke(() -> {
                    if (running && generation == sourceGeneration && archiveId == lastArchiveId)
                    {
                        scheduler.loadTrack(events, startNanos);
                        flashes.clear();
                    }
                });
            }
            catch (Throwable t)
            {
                log.warn("musicviz: failed to parse MIDI for archive {}", archiveId, t);
            }
        });
    }

    private void onNote(NoteEvent ev)
    {
        int wantedChannel = config.melodyChannel();
        if (config.audioSource() == MusicVizConfig.AudioSource.OSRS_MIDI
            && wantedChannel >= 0 && ev.channel != wantedChannel) return;
        int cursor = rrCursor++;
        if (config.targetType() != MusicVizConfig.TargetType.FLOOR_TILES)
            flashOne(scanner.scenery(), ev, cursor);
        if (config.targetType() != MusicVizConfig.TargetType.SCENERY)
            flashOne(scanner.floors(), ev, cursor);
    }

    private void flashOne(List<FlashTarget> targets, NoteEvent ev, int cursor)
    {
        if (targets.isEmpty()) return;

        int idx;
        switch (config.selectionMode())
        {
            case HASH_BY_NOTE:
                idx = (int) Math.floorMod((long) ev.note * 2654435761L, targets.size());
                break;
            case ROUND_ROBIN:
                idx = Math.floorMod(cursor, targets.size());
                break;
            case RANDOM:
            default:
                idx = rr.nextInt(targets.size());
                break;
        }
        FlashTarget target = targets.get(idx);
        flashes.add(new FlashState(target, System.currentTimeMillis(), NoteColor.forNote(ev.note)));
    }
}
