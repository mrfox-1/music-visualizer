package com.sob.musicviz;

import java.awt.Shape;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Perspective;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;

/** A scene object or a ground tile, projected afresh as the camera moves. */
final class FlashTarget
{
    final GameObject object;
    final Tile floor;
    private final LocalPoint location;
    private final Scene scene;
    private final int plane, baseX, baseY;

    FlashTarget(GameObject object, Tile floor, LocalPoint location, WorldView view)
    {
        this.object = object;
        this.floor = floor;
        this.location = location;
        scene = view.getScene();
        plane = view.getPlane();
        baseX = view.getBaseX();
        baseY = view.getBaseY();
    }

    Shape shape(Client client)
    {
        WorldView view = client.getWorldView(location.getWorldView());
        if (view == null || view.getScene() != scene || view.getPlane() != plane
            || view.getBaseX() != baseX || view.getBaseY() != baseY) return null;
        return floor == null ? object.getConvexHull() : Perspective.getCanvasTilePoly(client, location);
    }
}
