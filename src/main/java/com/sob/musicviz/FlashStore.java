package com.sob.musicviz;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import javax.inject.Singleton;

@Singleton
class FlashStore
{
    private final ConcurrentHashMap<Object, FlashState> active = new ConcurrentHashMap<>();

    void add(FlashState flash)
    {
        // Refresh each target instead of stacking opacity and work during dense sweeps.
        active.put(flash.target.floor != null ? flash.target.floor : flash.target.object, flash);
    }

    void clear()
    {
        active.clear();
    }

    void forEachActive(long nowMs, int decayMs, Consumer<FlashState> visitor)
    {
        Iterator<FlashState> it = active.values().iterator();
        while (it.hasNext())
        {
            FlashState f = it.next();
            if (f.expired(nowMs, decayMs))
            {
                it.remove();
                continue;
            }
            visitor.accept(f);
        }
    }
}
