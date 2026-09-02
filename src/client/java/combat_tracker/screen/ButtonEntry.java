package combat_tracker.screen;

import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ButtonEntry extends TooltipListEntry<Object> {
    private static final int MAX_WIDTH = 260;
    private static final int HEIGHT = 20;

    private final Button button;

    public ButtonEntry(Component label, Consumer<Button> onPress) {
        this(label, null, onPress);
    }

    public ButtonEntry(Component label, Component tooltip, Consumer<Button> onPress) {
        super(label, tooltipSupplier(tooltip));
        this.button = Button.builder(label, b -> onPress.accept(b))
                .bounds(0, 0, MAX_WIDTH, HEIGHT)
                .build();
    }

    private static Supplier<Optional<Component[]>> tooltipSupplier(Component tooltip) {
        Optional<Component[]> value = tooltip == null
                ? Optional.empty()
                : Optional.of(new Component[]{tooltip});
        return () -> value;
    }

    @Override
    public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight,
                       int mouseX, int mouseY, boolean isHovered, float delta) {
        super.render(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
        int width = Math.min(entryWidth, MAX_WIDTH);
        button.setX(x + (entryWidth - width) / 2);
        button.setY(y);
        button.setWidth(width);
        button.active = isEditable();
        button.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public int getItemHeight() {
        return HEIGHT + 4;
    }

    @Override
    public Object getValue() {
        return null;
    }

    @Override
    public Optional<Object> getDefaultValue() {
        return Optional.empty();
    }

    @Override
    public void save() {
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return List.of(button);
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        return List.of(button);
    }
}
