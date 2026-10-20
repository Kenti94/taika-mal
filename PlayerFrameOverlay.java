package com.osrsunitframes;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;

/** Player status frame with a portrait rendered from the local character model. */
class PlayerFrameOverlay extends Overlay
{
	private static final int PAD = 8;
	private static final int PORTRAIT = 86;
	private final Client client;
	private final UnitFramesPlugin plugin;
	private BufferedImage portrait;
	private long portraitRenderedAt;

	@Inject
	PlayerFrameOverlay(Client client, UnitFramesPlugin plugin)
	{
		this.client = client;
		this.plugin = plugin;
		setPosition(OverlayPosition.TOP_LEFT);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		UnitFramesConfig c = plugin.getConfig();
		if (!c.showPlayerFrame() || client.getGameState() != net.runelite.api.GameState.LOGGED_IN || client.getLocalPlayer() == null)
		{
			return null;
		}

		int width = Math.max(250, Math.min(420, c.frameWidth()));
		int height = PORTRAIT + PAD * 2;
		int bx = PAD * 2 + PORTRAIT + 8;
		int bw = width - bx - PAD;
		panel(g, c.playerPanelColor(), c.playerBorderColor(), width, height);
		long now = System.currentTimeMillis();
		if (portrait == null || now - portraitRenderedAt > 500)
		{
			portrait = ModelPortraitRenderer.render(client.getLocalPlayer(), PORTRAIT);
			portraitRenderedAt = now;
		}
		portraitSlot(g, c.playerPortraitBackgroundColor(), c.playerBorderColor(), PAD, PAD, portrait);
		int combat = client.getLocalPlayer().getCombatLevel();
		int total = client.getTotalLevel();
		g.setColor(c.playerLevelBackgroundColor());
		g.fillRect(PAD + 2, height - PAD - 25, PORTRAIT - 4, 23);
		g.setColor(c.playerTextColor());
		g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
		g.drawString("CB " + combat, PAD + 5, height - PAD - 14);
		g.drawString("Total " + total, PAD + 5, height - PAD - 4);

		int hp = client.getBoostedSkillLevel(Skill.HITPOINTS);
		int maxHp = client.getRealSkillLevel(Skill.HITPOINTS);
		int prayer = client.getBoostedSkillLevel(Skill.PRAYER);
		int maxPrayer = client.getRealSkillLevel(Skill.PRAYER);
		int hpHeight = 38;
		int prayerHeight = 30;
		int gap = 7;
		int barY = (height - hpHeight - prayerHeight - gap) / 2;
		bar(g, c, bx, barY, bw, hpHeight, "Hitpoints", hp, maxHp, c.playerHitpointsColor(), c.playerTrackColor());
		bar(g, c, bx, barY + hpHeight + gap, bw, prayerHeight, "Prayer", prayer, maxPrayer, c.playerPrayerColor(), c.playerTrackColor());
		return new Dimension(width, height);
	}

	static void panel(Graphics2D g, Color fill, Color border, int w, int h)
	{
		g.setColor(fill); g.fillRect(0, 0, w, h);
		g.setColor(border); g.setStroke(new BasicStroke(2)); g.drawRect(1, 1, w - 3, h - 3);
	}

	static void portraitSlot(Graphics2D g, Color fill, Color border, int x, int y, BufferedImage image)
	{
		g.setColor(fill); g.fillRect(x, y, PORTRAIT, PORTRAIT);
		if (image != null)
		{
			java.awt.Shape oldClip = g.getClip();
			g.clipRect(x + 3, y + 3, PORTRAIT - 6, PORTRAIT - 6);
			int sourceSize = Math.min(image.getWidth(), image.getHeight());
			int sourceX = (image.getWidth() - sourceSize) / 2;
			g.drawImage(image, x + 3, y + 3, x + PORTRAIT - 3, y + PORTRAIT - 3,
				sourceX, 0, sourceX + sourceSize, sourceSize, null);
			g.setClip(oldClip);
		}
		else
		{
			g.setColor(new Color(255, 255, 255, 140));
			g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
			String label = "Loading face";
			FontMetrics fm = g.getFontMetrics();
			g.drawString(label, x + (PORTRAIT - fm.stringWidth(label)) / 2, y + PORTRAIT / 2);
		}
		g.setColor(border); g.setStroke(new BasicStroke(2)); g.drawRect(x + 1, y + 1, PORTRAIT - 3, PORTRAIT - 3);
	}

	static void bar(Graphics2D g, UnitFramesConfig c, int x, int y, int w, int h, String label, int value, int max, Color fill, Color track)
	{
		g.setColor(track); g.fillRect(x, y, w, h);
		int pct = max <= 0 ? 0 : Math.max(0, Math.min(100, (int) Math.round(value * 100.0 / max)));
		g.setColor(fill); g.fillRect(x, y, w * pct / 100, h);
		g.setColor(c.playerBarBorderColor()); g.setStroke(new BasicStroke(1)); g.drawRect(x, y, w - 1, h - 1);
		g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
		String text = label + "   " + value + " / " + max;
		FontMetrics fm = g.getFontMetrics();
		int ty = y + (h - fm.getHeight()) / 2 + fm.getAscent();
		g.setColor(c.playerTextColor()); g.drawString(text, x + 6, ty);
	}
}
