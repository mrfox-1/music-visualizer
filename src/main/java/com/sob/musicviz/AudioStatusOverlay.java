package com.sob.musicviz;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;

class AudioStatusOverlay extends OverlayPanel
{
    private final MusicVizPlugin plugin;
    private final MusicVizConfig config;

    @Inject
    AudioStatusOverlay(MusicVizPlugin plugin, MusicVizConfig config)
    {
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.TOP_LEFT);
        panelComponent.setPreferredSize(new Dimension(300, 0));
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        panelComponent.getChildren().clear();
        PcAudioCapture capture = plugin.pcAudio();
        if (!config.showAudioStatus() || capture == null) return null;
        panelComponent.getChildren().add(LineComponent.builder().left("Music Visualizer — PC audio")
            .leftColor(Color.CYAN).build());
        String status = capture.status();
        panelComponent.getChildren().add(LineComponent.builder().left(status).build());
        if (status.startsWith("Listening"))
        {
            panelComponent.getChildren().add(LineComponent.builder().left("PC audio level")
                .right(String.format(java.util.Locale.ROOT, "%.1f%%", capture.levelPercent())).build());
        }
        return super.render(graphics);
    }
}
