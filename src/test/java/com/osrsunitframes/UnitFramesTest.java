package com.osrsunitframes;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class UnitFramesTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(UnitFramesPlugin.class);
        RuneLite.main(args);
    }
}
