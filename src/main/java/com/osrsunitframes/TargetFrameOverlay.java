package com.osrsunitframes;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.NPC;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;

/** NPC target frame with one health bar and a square portrait slot. */
class TargetFrameOverlay extends Overlay
{
	private static final int PAD = 8;
	private static final int PORTRAIT = 86;
	private final UnitFramesPlugin plugin;
	private BufferedImage renderedPortrait;
	private int renderedNpcId = -1;
	private long portraitRenderedAt;

	@Inject
	TargetFrameOverlay(UnitFramesPlugin plugin)
	{
		this.plugin = plugin;
		setPosition(OverlayPosition.TOP_RIGHT);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		UnitFramesConfig c = plugin.getConfig();
		NPC npc = c.showTargetFrame() ? plugin.getTarget() : null;
		if (npc == null) return null;

		int width = Math.max(250, Math.min(420, c.frameWidth()));
		int height = PORTRAIT + PAD * 2;
		int portraitX = width - PAD - PORTRAIT;
		int bx = PAD;
		int bw = portraitX - PAD - 8;
		PlayerFrameOverlay.panel(g, c.targetPanelColor(), c.targetBorderColor(), width, height);
		BufferedImage portrait = plugin.getNpcPortrait(npc);
		if (portrait == null)
		{
			long now = System.currentTimeMillis();
			if (renderedPortrait == null || renderedNpcId != npc.getId() || now - portraitRenderedAt > 500)
			{
				renderedPortrait = ModelPortraitRenderer.render(npc, PORTRAIT);
				renderedNpcId = npc.getId();
				portraitRenderedAt = now;
			}
			portrait = renderedPortrait;
		}
		else
		{
			renderedPortrait = null;
			renderedNpcId = npc.getId();
		}
		PlayerFrameOverlay.portraitSlot(g, c.targetPortraitBackgroundColor(), c.targetBorderColor(), portraitX, PAD, portrait);

		String name = npc.getName() == null ? "NPC" : npc.getName();
		String title = npc.getCombatLevel() > 0 ? name + "  (Lvl " + npc.getCombatLevel() + ")" : name;
		g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
		FontMetrics fm = g.getFontMetrics();
		while (title.length() > 1 && fm.stringWidth(title) > bw - 8) title = title.substring(0, title.length() - 1);
		g.setColor(c.targetTextColor());
		g.drawString(title, bx + 4, PAD + 17);

		int ratio = npc.getHealthRatio();
		int scale = npc.getHealthScale();
		boolean healthKnown = scale > 0 && ratio >= 0;
		int percent = healthKnown ? Math.max(0, Math.min(100, (int) Math.round(ratio * 100.0 / scale))) : 0;
		int barY = (height - 38) / 2 + 10;
		g.setColor(c.targetTrackColor()); g.fillRect(bx, barY, bw, 38);
		g.setColor(c.targetHitpointsColor()); g.fillRect(bx, barY, bw * percent / 100, 38);
		g.setColor(c.targetBarBorderColor()); g.drawRect(bx, barY, bw - 1, 37);
		g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
		g.setColor(c.targetTextColor());
		g.drawString(healthKnown ? "Hitpoints   " + percent + "%" : "Hitpoints   --", bx + 6, barY + 23);
		return new Dimension(width, height);
	}
}
