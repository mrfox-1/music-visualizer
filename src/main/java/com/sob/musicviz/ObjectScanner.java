package com.sob.musicviz;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.IdentityHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Player;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;

@Singleton
class ObjectScanner
{
    private final Client client;

    private volatile List<FlashTarget> scenery = Collections.emptyList();
    private volatile List<FlashTarget> floors = Collections.emptyList();

    @Inject
    ObjectScanner(Client client)
    {
        this.client = client;
    }

    void clear()
    {
        scenery = Collections.emptyList();
        floors = Collections.emptyList();
    }

    void refresh(int radius, MusicVizConfig.TargetType targetType)
    {
        Player local = client.getLocalPlayer();
        if (local == null)
        {
            clear();
            return;
        }

        LocalPoint playerLoc = local.getLocalLocation();
        WorldView view = client.getWorldView(playerLoc.getWorldView());
        if (view == null) { clear(); return; }
        Scene scene = view.getScene();
        if (scene == null) { clear(); return; }
        Tile[][][] tiles = scene.getTiles();
        int plane = view.getPlane();
        if (tiles == null || plane < 0 || plane >= tiles.length || tiles[plane] == null)
        { clear(); return; }
        Tile[][] planeTiles = tiles[plane];

        int radiusUnits = radius * 128;
        long radSq = (long) radiusUnits * radiusUnits;

        List<FlashTarget> found = new ArrayList<>();
        List<FlashTarget> ground = new ArrayList<>();
        Set<GameObject> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (int sx = 0; sx < planeTiles.length; sx++)
        {
            Tile[] row = planeTiles[sx];
            if (row == null) continue;
            for (int sy = 0; sy < row.length; sy++)
            {
                Tile tile = row[sy];
                if (tile == null) continue;

                LocalPoint tileLocation = tile.getLocalLocation();
                if (targetType != MusicVizConfig.TargetType.SCENERY && tileLocation != null
                    && (tile.getSceneTilePaint() != null || tile.getSceneTileModel() != null)
                    && withinRadius(tileLocation, playerLoc, radSq))
                {
                    ground.add(new FlashTarget(null, tile, tileLocation, view));
                }
                if (targetType == MusicVizConfig.TargetType.FLOOR_TILES) continue;

                GameObject[] objs = tile.getGameObjects();
                if (objs == null) continue;

                for (GameObject obj : objs)
                {
                    if (obj == null || !seen.add(obj)) continue;
                    LocalPoint p = obj.getLocalLocation();
                    if (p == null) continue;
                    if (!withinRadius(p, playerLoc, radSq)) continue;
                    found.add(new FlashTarget(obj, null, p, view));
                }
            }
        }

        scenery = found;
        floors = ground;
    }

    private static boolean withinRadius(LocalPoint point, LocalPoint center, long radiusSquared)
    {
        long dx = point.getX() - center.getX(), dy = point.getY() - center.getY();
        return dx * dx + dy * dy <= radiusSquared;
    }

    List<FlashTarget> scenery() { return scenery; }
    List<FlashTarget> floors() { return floors; }
}
