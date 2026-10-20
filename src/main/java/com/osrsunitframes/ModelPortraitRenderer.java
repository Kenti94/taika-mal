package com.osrsunitframes;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.runelite.api.Actor;
import net.runelite.api.Model;

/** Builds a small, cached 2D head image from a character's current in-game model. */
final class ModelPortraitRenderer
{
	private static final double HEAD_CUTOFF = 0.68;
	private static final double YAW = Math.PI / 7.0;

	private ModelPortraitRenderer() { }

	static BufferedImage render(Actor actor, int size)
	{
		if (actor == null) return null;
		Model model = actor.getModel();
		if (model == null || model.getVerticesCount() < 3 || model.getFaceCount() == 0) return null;

		float[] vx = model.getVerticesX();
		float[] vy = model.getVerticesY();
		float[] vz = model.getVerticesZ();
		int[] a = model.getFaceIndices1();
		int[] b = model.getFaceIndices2();
		int[] c = model.getFaceIndices3();
		int[] colA = model.getFaceColors1();
		int[] colB = model.getFaceColors2();
		int[] colC = model.getFaceColors3();
		if (vx == null || vy == null || vz == null || a == null || b == null || c == null || colA == null)
		{
			return null;
		}

		float minY = Float.MAX_VALUE;
		float maxY = -Float.MAX_VALUE;
		for (float y : vy) { minY = Math.min(minY, y); maxY = Math.max(maxY, y); }
		if (maxY <= minY) return null;
		float cutoff = minY + (float) ((maxY - minY) * HEAD_CUTOFF);
		double actorYaw = actor.getOrientation() * (Math.PI * 2.0 / 2048.0);
		double angle = actorYaw + YAW;
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);

		List<Face> faces = new ArrayList<>();
		double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
		double top = Double.MAX_VALUE, bottom = -Double.MAX_VALUE;
		int count = Math.min(model.getFaceCount(), Math.min(a.length, Math.min(b.length, c.length)));
		for (int i = 0; i < count; i++)
		{
			int i0 = a[i], i1 = b[i], i2 = c[i];
			if (!valid(i0, vx.length) || !valid(i1, vx.length) || !valid(i2, vx.length)) continue;
			if (vy[i0] < cutoff || vy[i1] < cutoff || vy[i2] < cutoff) continue;
			double x0 = vx[i0] * cos + vz[i0] * sin;
			double x1 = vx[i1] * cos + vz[i1] * sin;
			double x2 = vx[i2] * cos + vz[i2] * sin;
			double z0 = -vx[i0] * sin + vz[i0] * cos;
			double z1 = -vx[i1] * sin + vz[i1] * cos;
			double z2 = -vx[i2] * sin + vz[i2] * cos;
			minX = Math.min(minX, Math.min(x0, Math.min(x1, x2)));
			maxX = Math.max(maxX, Math.max(x0, Math.max(x1, x2)));
			top = Math.min(top, Math.min(vy[i0], Math.min(vy[i1], vy[i2])));
			bottom = Math.max(bottom, Math.max(vy[i0], Math.max(vy[i1], vy[i2])));
			Color color = faceColor(colA[i], valueAt(colB, i, colA[i]), valueAt(colC, i, colA[i]));
			faces.add(new Face(x0, vy[i0], z0, x1, vy[i1], z1, x2, vy[i2], z2, color));
		}
		if (faces.isEmpty() || maxX <= minX || bottom <= top) return null;

		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		try
		{
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			double scale = Math.min((size - 8.0) / (maxX - minX), (size - 8.0) / (bottom - top));
			double xOffset = (size - (maxX - minX) * scale) / 2.0 - minX * scale;
			double yOffset = (size - (bottom - top) * scale) / 2.0 + top * scale;
			faces.sort(Comparator.comparingDouble(f -> f.depth));
			for (Face f : faces)
			{
				int[] xs = {(int) Math.round(xOffset + f.x0 * scale), (int) Math.round(xOffset + f.x1 * scale), (int) Math.round(xOffset + f.x2 * scale)};
				int[] ys = {(int) Math.round(yOffset - f.y0 * scale), (int) Math.round(yOffset - f.y1 * scale), (int) Math.round(yOffset - f.y2 * scale)};
				g.setColor(f.color);
				g.fillPolygon(xs, ys, 3);
			}
		}
		finally { g.dispose(); }
		return image;
	}

	private static boolean valid(int index, int length) { return index >= 0 && index < length; }
	private static int valueAt(int[] values, int index, int fallback) { return values != null && index < values.length ? values[index] : fallback; }

	private static Color faceColor(int c1, int c2, int c3)
	{
		Color a = hsl(c1), b = hsl(c2), c = hsl(c3);
		return new Color((a.getRed() + b.getRed() + c.getRed()) / 3,
			(a.getGreen() + b.getGreen() + c.getGreen()) / 3,
			(a.getBlue() + b.getBlue() + c.getBlue()) / 3);
	}

	private static Color hsl(int packed)
	{
		packed = Math.max(0, packed);
		float hue = ((packed >> 10) & 63) / 64.0f;
		float saturation = ((packed >> 7) & 7) / 7.0f;
		float lightness = (packed & 127) / 127.0f;
		return Color.getHSBColor(hue, saturation, lightness);
	}

	private static final class Face
	{
		private final double x0, y0, z0, x1, y1, z1, x2, y2, z2;
		private final double depth;
		private final Color color;

		private Face(double x0, double y0, double z0, double x1, double y1, double z1, double x2, double y2, double z2, Color color)
		{
			this.x0=x0; this.y0=y0; this.z0=z0; this.x1=x1; this.y1=y1; this.z1=z1; this.x2=x2; this.y2=y2; this.z2=z2;
			this.depth = (z0 + z1 + z2) / 3.0;
			this.color = color;
		}
	}
}
