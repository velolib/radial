package velo.radial.render;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import velo.radial.config.RadialConfig;

public class DonutRenderer implements AutoCloseable {

    private final String idSuffix;
    private DynamicTexture texture;
    private Identifier textureId;
    // Cache trackers
    private int lastCount = -1;
    private float lastGap = -1;
    private float lastInner = -1;
    private float lastOuter = -1;
    private int lastBgColor = -1;
    private int lastHoverColor = -1;
    private boolean lastShowHover = true;
    private int lastHoverSlot = -2;

    public DonutRenderer(String idSuffix) {
        this.idSuffix = idSuffix;
    }

    public void render(GuiGraphicsExtractor graphics, int cx, int cy, float inner, float outer, int count, int hoveredSlot, float ease, float resScale) {
        RadialConfig config = RadialConfig.INSTANCE;
        int logicalSize = (int) outer * 2 + 2;
        int texSize = (int) (logicalSize * resScale);

        if (texSize <= 0) return;

        boolean needsRedraw = false;

        // Recreate texture if size changes
        if (texture == null || texture.getPixels().getWidth() != texSize) {
            if (texture != null) texture.close();
            NativeImage image = new NativeImage(texSize, texSize, false);
            texture = new DynamicTexture(() -> "", image);
            textureId = Identifier.fromNamespaceAndPath("radial", "donut_" + idSuffix);
            Minecraft.getInstance().getTextureManager().register(textureId, texture);
            needsRedraw = true;
        }

        // Check if ANY visual configuration changed
        if (needsRedraw || lastCount != count || lastGap != config.sectorGap ||
                lastInner != inner || lastOuter != outer ||
                lastBgColor != config.backgroundColor.getRGB() ||
                lastHoverColor != config.activationColor.getRGB() ||
                lastShowHover != config.showActivationZone ||
                lastHoverSlot != hoveredSlot) {

            generatePixels(texture.getPixels(), inner, outer, count, hoveredSlot, resScale, config);
            texture.upload();

            lastCount = count;
            lastGap = config.sectorGap;
            lastInner = inner;
            lastOuter = outer;
            lastBgColor = config.backgroundColor.getRGB();
            lastHoverColor = config.activationColor.getRGB();
            lastShowHover = config.showActivationZone;
            lastHoverSlot = hoveredSlot;
        }

        int alpha = (int) (ease * 255);
        int tintColor = (alpha << 24) | 0xFFFFFF;

        graphics.pose().pushMatrix();
        graphics.pose().translate(cx, cy);
        graphics.pose().scale(ease / resScale, ease / resScale);

        int offset = -texSize / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, textureId, offset, offset, 0, 0, texSize, texSize, texSize, texSize, tintColor);

        graphics.pose().popMatrix();
    }

    private void generatePixels(NativeImage image, float inner, float outer, int count, int hoveredSlot, float resScale, RadialConfig config) {
        int texSize = image.getWidth();
        float center = texSize / 2.0f;
        double sectorSize = (Math.PI * 2) / count;

        int base = config.backgroundColor.getRGB();
        int hover = config.activationColor.getRGB();
        boolean showHover = config.showActivationZone;

        float gapWidth = config.sectorGap * resScale;
        float scaledInner = inner * resScale;
        float scaledOuter = outer * resScale;
        float sharpness = 1.0f;

        for (int y = 0; y < texSize; y++) {
            for (int x = 0; x < texSize; x++) {
                image.setPixel(x, y, 0x00000000);
            }
        }

        for (int y = 0; y < texSize; y++) {
            for (int x = 0; x < texSize; x++) {
                float dx = x - center + 0.5f;
                float dy = y - center + 0.5f;
                float distSq = dx * dx + dy * dy;

                if (distSq < (scaledInner - 2) * (scaledInner - 2) || distSq > (scaledOuter + 2) * (scaledOuter + 2)) {
                    continue;
                }

                float dist = (float) Math.sqrt(distSq);
                double angle = Math.atan2(dy, dx) + Math.PI / 2;
                if (angle < 0) angle += Math.PI * 2;

                float edgeDist = Math.min(dist - scaledInner, scaledOuter - dist);
                float alphaMod = Math.max(0.0f, Math.min(1.0f, (edgeDist * sharpness) + 0.5f));

                if (alphaMod <= 0.0f) continue;

                if (gapWidth > 0.0f) {
                    double relativeAngle = (angle + sectorSize / 2.0) % sectorSize;
                    double angularDistToNearestEdge = Math.min(relativeAngle, sectorSize - relativeAngle);
                    double pixelDistToSectorEdge = angularDistToNearestEdge * dist;

                    float gapAlpha = Math.max(0.0f, Math.min(1.0f, (float) (((pixelDistToSectorEdge - (gapWidth / 2.0f)) * sharpness) + 0.5f)));
                    alphaMod *= gapAlpha;
                }

                int color = base;
                if (showHover && hoveredSlot != -1) {
                    double targetAngle = hoveredSlot * sectorSize;
                    double angleDiff = Math.abs(angle - targetAngle);
                    if (angleDiff > Math.PI) angleDiff = Math.PI * 2 - angleDiff;

                    double angularDistToHoverEdge = angleDiff - (sectorSize / 2.0);
                    double pixelDistToHoverEdge = angularDistToHoverEdge * dist;

                    float hoverRatio = Math.max(0.0f, Math.min(1.0f, (float) (0.5f - (pixelDistToHoverEdge * sharpness))));
                    if (hoverRatio > 0.0f) {
                        color = blendColors(base, hover, hoverRatio);
                    }
                }

                color = applyAlpha(color, alphaMod);
                image.setPixelABGR(x, y, toABGR(color));
            }
        }
    }

    private int toABGR(int argb) {
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    private int blendColors(int c1, int c2, float ratio) {
        float inv = 1.0f - ratio;
        return (int) (((c1 >> 24) & 0xFF) * inv + ((c2 >> 24) & 0xFF) * ratio) << 24 |
                (int) (((c1 >> 16) & 0xFF) * inv + ((c2 >> 16) & 0xFF) * ratio) << 16 |
                (int) (((c1 >> 8) & 0xFF) * inv + ((c2 >> 8) & 0xFF) * ratio) << 8 |
                (int) ((c1 & 0xFF) * inv + (c2 & 0xFF) * ratio);
    }

    private int applyAlpha(int color, float alpha) {
        return ((int) (((color >> 24) & 0xFF) * alpha) << 24) | (color & 0x00FFFFFF);
    }

    @Override
    public void close() {
        if (textureId != null) {
            Minecraft.getInstance().getTextureManager().release(textureId);
            texture.close();
            texture = null;
        }
    }
}