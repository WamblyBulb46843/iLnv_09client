package dev.iLnv_09.mod.modules.impl.render;

import dev.iLnv_09.mod.modules.Module;
import dev.iLnv_09.mod.modules.settings.impl.BooleanSetting;
import dev.iLnv_09.mod.modules.settings.impl.ColorSetting;
import dev.iLnv_09.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.gui.DrawContext;
import java.awt.Color;

public class TipTapShow extends Module {
    private final SliderSetting x = add(new SliderSetting("X", 20, 0, 2000, 1));
    private final SliderSetting y = add(new SliderSetting("Y", 20, 0, 2000, 1));
    private final SliderSetting keyWidth = add(new SliderSetting("Width", 20, 10, 50, 1));
    private final SliderSetting keyHeight = add(new SliderSetting("Height", 20, 10, 50, 1));
    private final SliderSetting spacing = add(new SliderSetting("Spacing", 2, 0, 10, 1));
    private final SliderSetting radius = add(new SliderSetting("Radius", 40, 20, 100, 1));
    private final ColorSetting pressedColor = add(new ColorSetting("Pressed Color", new Color(255, 255, 255, 255)));
    private final ColorSetting unpressedColor = add(new ColorSetting("Unpressed Color", new Color(0, 0, 0, 120)));
    private final ColorSetting textColor = add(new ColorSetting("Text Color", new Color(255, 255, 255, 255)));
    private final ColorSetting backgroundColor = add(new ColorSetting("Background Color", new Color(0, 0, 0, 80)));
    private final BooleanSetting background = add(new BooleanSetting("Background", true));
    private final BooleanSetting wasd = add(new BooleanSetting("WASD", true));
    private final BooleanSetting mouse = add(new BooleanSetting("Mouse", true));
    private final BooleanSetting space = add(new BooleanSetting("Space", true));

    public TipTapShow() {
        super("TipTapShow", Category.Render);
        setChinese("按键显示");
    }

    @Override
    public void onRender2D(DrawContext context, float tickDelta) {
        int keyWidth = this.keyWidth.getValueInt();
        int keyHeight = this.keyHeight.getValueInt();
        int spacing = this.spacing.getValueInt();
        int radius = this.radius.getValueInt();

        int startX = x.getValueInt();
        int startY = y.getValueInt();

        // --- Layout Calculation ---
        
        // Center of the arc for A, S, D
        int arcCenterX = startX + radius;
        int arcCenterY = startY + keyHeight + spacing;

        // W key position (above the arc)
        int w_keyX = arcCenterX - keyWidth / 2;
        int w_keyY = startY;

        // A, S, D key positions on the arc using angles for a nice curve
        double angle_A = 5 * Math.PI / 6; // 150 degrees
        double angle_S = Math.PI / 2;     // 90 degrees (bottom)
        double angle_D = Math.PI / 6;     // 30 degrees

        int a_keyX = (int) (arcCenterX + radius * Math.cos(angle_A)) - keyWidth / 2;
        int a_keyY = (int) (arcCenterY + radius * Math.sin(angle_A)) - keyHeight / 2;

        int s_keyX = (int) (arcCenterX + radius * Math.cos(angle_S)) - keyWidth / 2;
        int s_keyY = (int) (arcCenterY + radius * Math.sin(angle_S)) - keyHeight / 2;

        int d_keyX = (int) (arcCenterX + radius * Math.cos(angle_D)) - keyWidth / 2;
        int d_keyY = (int) (arcCenterY + radius * Math.sin(angle_D)) - keyHeight / 2;

        // Bounding box for all elements
        int wasdLeft = a_keyX;
        int wasdRight = d_keyX + keyWidth;
        int wasdTop = w_keyY;
        int wasdBottom = s_keyY + keyHeight;
        
        int totalWidth = wasdRight - wasdLeft;
        int currentY = wasdBottom + spacing;

        int mouseTotalHeight = mouse.getValue() ? keyHeight + spacing : 0;
        int spaceTotalHeight = space.getValue() ? keyHeight : 0;
        int totalHeight = (wasdBottom - wasdTop) + mouseTotalHeight + spaceTotalHeight;

        // --- Drawing ---

        if (background.getValue()) {
            context.fill(wasdLeft - spacing, wasdTop - spacing, wasdRight + spacing, wasdTop + totalHeight + spacing, backgroundColor.getValue().getRGB());
        }

        if (wasd.getValue()) {
            drawKey(context, "W", w_keyX, w_keyY, keyWidth, keyHeight, mc.options.forwardKey.isPressed());
            drawKey(context, "A", a_keyX, a_keyY, keyWidth, keyHeight, mc.options.leftKey.isPressed());
            drawKey(context, "S", s_keyX, s_keyY, keyWidth, keyHeight, mc.options.backKey.isPressed());
            drawKey(context, "D", d_keyX, d_keyY, keyWidth, keyHeight, mc.options.rightKey.isPressed());
        }

        if (mouse.getValue()) {
            int lmbWidth = (int) (mc.textRenderer.getWidth("LMB") * 1.2);
            int rmbWidth = (int) (mc.textRenderer.getWidth("RMB") * 1.2);
            int mouseButtonsWidth = lmbWidth + spacing + rmbWidth;
            int mouseStartX = wasdLeft + (totalWidth - mouseButtonsWidth) / 2;
            drawKey(context, "LMB", mouseStartX, currentY, lmbWidth, keyHeight, mc.options.attackKey.isPressed());
            drawKey(context, "RMB", mouseStartX + lmbWidth + spacing, currentY, rmbWidth, keyHeight, mc.options.useKey.isPressed());
            currentY += keyHeight + spacing;
        }

        if (space.getValue()) {
            int spaceWidth = totalWidth;
            drawKey(context, "Space", wasdLeft, currentY, spaceWidth, keyHeight, mc.options.jumpKey.isPressed());
        }
    }

    private void drawKey(DrawContext context, String text, int x, int y, int width, int height, boolean pressed) {
        Color fillColor = pressed ? pressedColor.getValue() : unpressedColor.getValue();
        context.fill(x, y, x + width, y + height, fillColor.getRGB());
        context.drawTextWithShadow(mc.textRenderer, text, x + (width - mc.textRenderer.getWidth(text)) / 2, y + (height - mc.textRenderer.fontHeight) / 2, textColor.getValue().getRGB());
    }
}