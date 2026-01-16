package thunder.hack.features.modules.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import thunder.hack.features.modules.Module;

public class RadyNotification extends Module {

    public RadyNotification() {
        super("RedyNotification", Category.MISC);
    }

    @EventHandler
    public void onRender2D(DrawContext context) {
        if (mc.player == null || mc.world == null) return;

        if (mc.player.getMainHandStack().getItem() == Items.TRIDENT) {
            int maxCharge = 15;
            int useTime = mc.player.getItemUseTime();

            float charge = mc.player.isUsingItem() ? (useTime / (float) maxCharge) : 0f;
            int level = MathHelper.clamp((int) (charge * 3), 0, 4);
            boolean isReady = level >= 4;

            String message = isReady
                    ? String.format("Trident ready [Max]")
                    : String.format("Trident [%d/3]", level);
            int color = isReady ? 0xFF00FF00 : 0xFFFFFF00;

            renderMessage(context, message, color);
        }
    }

    private void renderMessage(DrawContext context, String message, int color) {
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();

        float x = screenWidth / 2.0f - mc.textRenderer.getWidth(message) / 2.0f;
        float y = screenHeight / 2.0f - 20;

        context.drawText(mc.textRenderer, message, (int) x, (int) y, color, true);
        matrices.pop();
    }
}
