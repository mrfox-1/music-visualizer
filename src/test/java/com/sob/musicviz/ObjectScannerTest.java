package com.sob.musicviz;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Player;
import net.runelite.api.Scene;
import net.runelite.api.SceneTilePaint;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import org.junit.Test;
import static org.junit.Assert.*;

public class ObjectScannerTest
{
    @Test
    public void floorModeUsesOnlyPaintedGroundInsideCircularRadius()
    {
        Fixture fixture = new Fixture();
        fixture.scanner.refresh(1, MusicVizConfig.TargetType.FLOOR_TILES);
        assertEquals(2, fixture.scanner.floors().size());
        assertTrue(fixture.scanner.scenery().isEmpty());
        assertSame(fixture.tiles[0][4][4], fixture.scanner.floors().get(0).floor);
        assertSame(fixture.tiles[0][5][4], fixture.scanner.floors().get(1).floor);
    }

    @Test
    public void sceneryModeDeduplicatesObjectsSpanningTiles()
    {
        Fixture fixture = new Fixture();
        fixture.scanner.refresh(1, MusicVizConfig.TargetType.SCENERY);
        assertEquals(1, fixture.scanner.scenery().size());
        assertTrue(fixture.scanner.floors().isEmpty());
    }

    @Test
    public void bothModeRetainsBothTargetLists()
    {
        Fixture fixture = new Fixture();
        fixture.scanner.refresh(1, MusicVizConfig.TargetType.BOTH);
        assertEquals(1, fixture.scanner.scenery().size());
        assertEquals(2, fixture.scanner.floors().size());
        fixture.scanner.refresh(1, MusicVizConfig.TargetType.SCENERY);
        assertTrue(fixture.scanner.floors().isEmpty());
    }

    @Test
    public void logoutAndPlaneChangesDoNotLeaveOldFloorFlashes()
    {
        Fixture fixture = new Fixture();
        fixture.scanner.refresh(1, MusicVizConfig.TargetType.BOTH);
        FlashTarget oldFloor = fixture.scanner.floors().get(0);
        fixture.plane[0] = 1;
        assertNull(oldFloor.shape(fixture.client));
        fixture.scanner.refresh(1, MusicVizConfig.TargetType.FLOOR_TILES);
        assertEquals(1, fixture.scanner.floors().size());
        assertSame(fixture.tiles[1][4][4], fixture.scanner.floors().get(0).floor);
        fixture.player.set(null);
        fixture.scanner.refresh(1, MusicVizConfig.TargetType.BOTH);
        assertTrue(fixture.scanner.floors().isEmpty());
        assertTrue(fixture.scanner.scenery().isEmpty());
    }

    private static final class Fixture
    {
        final Tile[][][] tiles = new Tile[2][8][8];
        final int[] plane = {0};
        final AtomicReference<Player> player = new AtomicReference<>();
        final Client client;
        final ObjectScanner scanner;

        Fixture()
        {
            LocalPoint center = point(4, 4);
            GameObject object = stub(GameObject.class, Map.of("getLocalLocation", center));
            tiles[0][4][4] = tile(4, 4, true, object);
            tiles[0][5][4] = tile(5, 4, true, object);
            tiles[0][5][5] = tile(5, 5, true);
            tiles[0][7][4] = tile(7, 4, true);
            tiles[0][4][5] = tile(4, 5, false);
            tiles[1][4][4] = tile(4, 4, true);
            Scene scene = stub(Scene.class, Map.of("getTiles", tiles));
            WorldView view = proxy(WorldView.class, name -> name.equals("getPlane") ? plane[0]
                : name.equals("getScene") ? scene : null);
            player.set(stub(Player.class, Map.of("getLocalLocation", center)));
            client = proxy(Client.class, name -> name.equals("getLocalPlayer") ? player.get()
                : name.equals("getWorldView") ? view : null);
            scanner = new ObjectScanner(client);
        }
    }

    private static LocalPoint point(int x, int y) { return new LocalPoint(x * 128 + 64, y * 128 + 64); }

    private static Tile tile(int x, int y, boolean painted, GameObject... objects)
    {
        SceneTilePaint paint = painted ? stub(SceneTilePaint.class, Map.of()) : null;
        return proxy(Tile.class, name -> name.equals("getLocalLocation") ? point(x, y)
            : name.equals("getGameObjects") ? objects : name.equals("getSceneTilePaint") ? paint : null);
    }

    private static <T> T stub(Class<T> type, Map<String, Object> values) { return proxy(type, values::get); }

    private static <T> T proxy(Class<T> type, java.util.function.Function<String, Object> values)
    {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (object, method, args) -> {
            if (method.getName().equals("hashCode")) return System.identityHashCode(object);
            if (method.getName().equals("equals")) return object == args[0];
            Object value = values.apply(method.getName());
            if (value != null) return value;
            if (method.getReturnType() == int.class) return 0;
            if (method.getReturnType() == boolean.class) return false;
            return null;
        }));
    }
}
