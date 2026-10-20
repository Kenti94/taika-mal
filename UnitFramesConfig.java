package com.osrsunitframes;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("osrsunitframes")
public interface UnitFramesConfig extends Config
{
	@ConfigItem(keyName = "showPlayerFrame", name = "Show player frame", description = "Show your player status frame", position = 0)
	default boolean showPlayerFrame() { return true; }

	@ConfigItem(keyName = "showTargetFrame", name = "Show NPC target frame", description = "Show the last NPC you interacted with", position = 1)
	default boolean showTargetFrame() { return true; }

	@ConfigItem(keyName = "useWikiNpcPortraits", name = "Use OSRS Wiki NPC portraits", description = "Download and cache a portrait for the current NPC target", warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers", position = 3)
	default boolean useWikiNpcPortraits() { return false; }

	@Range(min = 250, max = 420)
	@ConfigItem(keyName = "frameWidth", name = "Frame width", description = "Width of both unit frames", position = 2)
	default int frameWidth() { return 300; }

	@Alpha @ConfigItem(keyName="playerPanelColor", name="Player panel color / opacity", description="Player frame background color and opacity", position=10)
	default Color playerPanelColor() { return new Color(19, 18, 17, 225); }
	@Alpha @ConfigItem(keyName="playerBorderColor", name="Player border color / opacity", description="Player frame and portrait border color and opacity", position=11)
	default Color playerBorderColor() { return new Color(184, 155, 93, 220); }
	@Alpha @ConfigItem(keyName="playerPortraitBackgroundColor", name="Player portrait background / opacity", description="Background behind the player portrait", position=12)
	default Color playerPortraitBackgroundColor() { return new Color(32, 29, 26, 255); }
	@Alpha @ConfigItem(keyName="playerLevelBackgroundColor", name="Player level label background / opacity", description="Background behind combat and total level", position=13)
	default Color playerLevelBackgroundColor() { return new Color(0, 0, 0, 190); }
	@Alpha @ConfigItem(keyName="playerHitpointsColor", name="Player hitpoints color / opacity", description="Player hitpoints bar fill", position=14)
	default Color playerHitpointsColor() { return new Color(159, 35, 35, 255); }
	@Alpha @ConfigItem(keyName="playerPrayerColor", name="Player prayer color / opacity", description="Player prayer bar fill", position=15)
	default Color playerPrayerColor() { return new Color(54, 91, 184, 255); }
	@Alpha @ConfigItem(keyName="playerTrackColor", name="Player empty bar color / opacity", description="Empty portion of player bars", position=16)
	default Color playerTrackColor() { return new Color(7, 7, 7, 230); }
	@Alpha @ConfigItem(keyName="playerBarBorderColor", name="Player bar border color / opacity", description="Player bar outline", position=17)
	default Color playerBarBorderColor() { return new Color(255, 255, 255, 120); }
	@Alpha @ConfigItem(keyName="playerTextColor", name="Player text color / opacity", description="Player frame text", position=18)
	default Color playerTextColor() { return Color.WHITE; }

	@Alpha @ConfigItem(keyName="targetPanelColor", name="NPC panel color / opacity", description="NPC frame background color and opacity", position=30)
	default Color targetPanelColor() { return new Color(19, 18, 17, 225); }
	@Alpha @ConfigItem(keyName="targetBorderColor", name="NPC border color / opacity", description="NPC frame and portrait border color and opacity", position=31)
	default Color targetBorderColor() { return new Color(184, 155, 93, 220); }
	@Alpha @ConfigItem(keyName="targetPortraitBackgroundColor", name="NPC portrait background / opacity", description="Background behind the NPC portrait", position=32)
	default Color targetPortraitBackgroundColor() { return new Color(32, 29, 26, 255); }
	@Alpha @ConfigItem(keyName="targetHitpointsColor", name="NPC hitpoints color / opacity", description="NPC health bar fill", position=33)
	default Color targetHitpointsColor() { return new Color(159, 35, 35, 255); }
	@Alpha @ConfigItem(keyName="targetTrackColor", name="NPC empty bar color / opacity", description="Empty portion of the NPC bar", position=34)
	default Color targetTrackColor() { return new Color(7, 7, 7, 230); }
	@Alpha @ConfigItem(keyName="targetBarBorderColor", name="NPC bar border color / opacity", description="NPC health bar outline", position=35)
	default Color targetBarBorderColor() { return new Color(255, 255, 255, 120); }
	@Alpha @ConfigItem(keyName="targetTextColor", name="NPC text color / opacity", description="NPC frame text", position=36)
	default Color targetTextColor() { return Color.WHITE; }
}
