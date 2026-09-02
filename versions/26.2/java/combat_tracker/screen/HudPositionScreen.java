package combat_tracker.screen;

import combat_tracker.config.CtConfig;
import combat_tracker.hud.HudRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class HudPositionScreen extends Screen {
    private static final int LEFT_BUTTON = 0;

    private final Screen parent;
    private final CtConfig config = CtConfig.get();

    private boolean dragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    public HudPositionScreen(Screen parent) {
        super(Component.literal("Move HUD"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> this.onClose())
                .bounds(this.width / 2 - 100, this.height - 28, 200, 20).build());
        config.hudX = clampX(config.hudX);
        config.hudY = clampY(config.hudY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        String hint = "Drag the overlay to reposition it, then click Done";
        graphics.text(this.font, hint, this.width / 2 - this.font.width(hint) / 2, 24, 0xFFFFFFFF);

        HudRenderer.renderScaledAt(graphics, config.hudX, config.hudY);
        outline(graphics, config.hudX - 1, config.hudY - 1,
                HudRenderer.scaledWidth() + 2, HudRenderer.scaledHeight() + 2,
                dragging ? 0xFFFFFF55 : 0xFF55FF55);
    }

    private static void outline(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y + 1, x + 1, y + h - 1, color);
        graphics.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == LEFT_BUTTON && insideBox(event.x(), event.y())) {
            dragging = true;
            dragOffsetX = (int) (event.x() - config.hudX);
            dragOffsetY = (int) (event.y() - config.hudY);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragging && event.button() == LEFT_BUTTON) {
            config.hudX = clampX((int) (event.x() - dragOffsetX));
            config.hudY = clampY((int) (event.y() - dragOffsetY));
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == LEFT_BUTTON && dragging) {
            dragging = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    private boolean insideBox(double mx, double my) {
        int x = config.hudX;
        int y = config.hudY;
        int w = HudRenderer.scaledWidth();
        int h = HudRenderer.scaledHeight();
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private int clampX(int x) {
        return Math.max(1, Math.min(x, this.width - HudRenderer.scaledWidth()));
    }

    private int clampY(int y) {
        return Math.max(1, Math.min(y, this.height - HudRenderer.scaledHeight()));
    }

    @Override
    public void onClose() {
        CtConfig.save();
        Minecraft.getInstance().gui.setScreen(parent);
    }
}
