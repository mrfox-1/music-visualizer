package com.sob.musicviz;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup("musicviz")
public interface MusicVizConfig extends Config
{
    @ConfigSection(name = "Audio source", description = "Choose OSRS music or PC playback", position = -1)
    String audioSection = "audioSection";

    @ConfigItem(keyName = "audioSource", name = "Source",
        description = "PC audio reacts to sound playing through your Windows default headphones or speakers. No input selection needed.",
        section = audioSection, position = 0)
    default AudioSource audioSource() { return AudioSource.OSRS_MIDI; }

    @Range(min = 1, max = 100)
    @ConfigItem(keyName = "audioSensitivity", name = "Audio sensitivity",
        description = "Higher values react to quieter audio. Applies only to PC audio.",
        section = audioSection, position = 2)
    default int audioSensitivity() { return 50; }

    @ConfigItem(keyName = "showAudioStatus", name = "Show audio status",
        description = "Show capture status and setup errors while using PC audio.",
        section = audioSection, position = 3)
    default boolean showAudioStatus() { return true; }

    enum AudioSource
    {
        OSRS_MIDI, PC_AUDIO;

        @Override public String toString() { return this == PC_AUDIO ? "PC audio" : "OSRS music"; }
    }

    @ConfigSection(name = "Display", description = "Visual options", position = 1)
    String displaySection = "displaySection";

    @ConfigSection(name = "Sync", description = "Audio sync tuning", position = 2)
    String syncSection = "syncSection";

    @ConfigSection(name = "Advanced", description = "Power-user settings", position = 99, closedByDefault = true)
    String advancedSection = "advancedSection";

    @Range(min = 0, max = 100)
    @ConfigItem(keyName = "visualActivity", name = "Visual activity",
        description = "0 disables flashes; 25 is the original density. Above 25, floor coverage grows to the entire nearby floor at 100; scenery grows to twelve objects per event. Opacity and decay stay independent.",
        section = displaySection, position = -1)
    default int visualActivity() { return 50; }

    @Range(min = 5, max = 50)
    @ConfigItem(
        keyName = "radius",
        name = "Radius (tiles)",
        description = "How far around your character to scan for objects to flash. 10 keeps the visualization close and dense; 20 spreads it out roughly one screen at default zoom.",
        section = displaySection, position = 0
    )
    default int radius() { return 10; }

    @Range(min = 100, max = 2000)
    @Units(Units.MILLISECONDS)
    @ConfigItem(
        keyName = "flashDecayMs",
        name = "Flash decay",
        description = "How long each flash lingers before fading out. Shorter = sharper beats; longer = smoother glow.",
        section = displaySection, position = 1
    )
    default int flashDecayMs() { return 400; }

    @Range(min = 0, max = 255)
    @ConfigItem(
        keyName = "colorAlpha",
        name = "Flash alpha",
        description = "Peak opacity of each flash (0 = invisible, 255 = solid). Lower it if the highlights overwhelm the scene.",
        section = displaySection, position = 2
    )
    default int colorAlpha() { return 160; }

    @ConfigItem(
        keyName = "selectionMode",
        name = "Target selection",
        description = "How each note picks which nearby object to flash:\n"
            + "• HASH_BY_NOTE — same pitch always picks the same object (consistent, pitch → direction).\n"
            + "• ROUND_ROBIN — cycles through objects in scene order, producing a wave-like sweep.\n"
            + "• RANDOM — picks a random object every note for maximum spatial spread.",
        section = displaySection, position = 3
    )
    default SelectionMode selectionMode() { return SelectionMode.HASH_BY_NOTE; }

    @ConfigItem(keyName = "targetType", name = "Flash targets",
        description = "Scenery, floor tiles, or both per note. Visual activity controls the number of targets of each type.",
        section = displaySection, position = 4)
    default TargetType targetType() { return TargetType.SCENERY; }

    @Range(min = 0, max = 255)
    @ConfigItem(keyName = "floorAlpha", name = "Floor tile opacity",
        description = "Peak floor-tile fill opacity. Lower values keep the ground easy to see.",
        section = displaySection, position = 5)
    default int floorAlpha() { return 60; }

    @ConfigItem(keyName = "colorScheme", name = "Color scheme",
        description = "Palette for floor and scenery flashes. Pitch selects a color within the palette.",
        section = displaySection, position = 6)
    default ColorScheme colorScheme() { return ColorScheme.RAINBOW; }

    @ConfigItem(keyName = "customColor", name = "Custom gradient start",
        description = "First color of the Custom gradient. Pitch selects shades between the endpoints. Opacity is controlled separately.",
        section = displaySection, position = 7)
    default java.awt.Color customColor() { return new java.awt.Color(0x38C8D9); }

    @ConfigItem(keyName = "customColorEnd", name = "Custom gradient end",
        description = "Last color of the Custom gradient. Set both endpoints to the same color for a solid color.",
        section = displaySection, position = 8)
    default java.awt.Color customColorEnd() { return new java.awt.Color(0xBF68DE); }

    enum TargetType
    {
        SCENERY, FLOOR_TILES, BOTH;

        @Override public String toString()
        {
            return this == SCENERY ? "Scenery" : this == FLOOR_TILES ? "Floor tiles" : "Both";
        }
    }

    @Range(min = -1, max = 15)
    @ConfigItem(
        keyName = "melodyChannel",
        name = "MIDI channel",
        description = "Which MIDI channel(s) trigger flashes:\n"
            + "• -1 = all 16 channels (densest visual, default).\n"
            + "• 9 = drum/percussion only (clean beat sync).\n"
            + "• 0–15 = a single melodic channel (try different values per track).",
        section = syncSection, position = 0
    )
    default int melodyChannel() { return -1; }

    @Range(min = -2000, max = 2000)
    @Units(Units.MILLISECONDS)
    @ConfigItem(
        keyName = "syncOffsetMs",
        name = "Sync offset",
        description = "Nudge the visualization earlier (-) or later (+) by milliseconds. One OSRS game tick is ~600ms. Useful when audio is delayed by your system but the visuals aren't (or vice-versa).",
        section = syncSection, position = 1
    )
    default int syncOffsetMs() { return 0; }

    @Range(min = 100, max = 1000)
    @Units(Units.MILLISECONDS)
    @ConfigItem(
        keyName = "pollIntervalMs",
        name = "Track poll",
        description = "How often to check whether the music track has changed. Lower = faster lock on a new track; higher = less CPU. The default 250ms is the worst-case offset on a track change.",
        section = advancedSection, position = 0
    )
    default int pollIntervalMs() { return 250; }

    @ConfigItem(
        keyName = "logCacheMisses",
        name = "Log cache misses",
        description = "When on, logs once if a track's MIDI cannot be loaded from your OSRS cache. Safe to leave on.",
        section = advancedSection, position = 1
    )
    default boolean logCacheMisses() { return true; }

    enum SelectionMode
    {
        RANDOM,
        HASH_BY_NOTE,
        ROUND_ROBIN
    }
}
