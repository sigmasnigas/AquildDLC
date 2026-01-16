package thunder.hack.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.EntityHitResult;
import thunder.hack.events.impl.EventTick;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

import java.util.Random;

public class AutoClicker extends Module {
    private final Setting<Integer> minCPS = new Setting<>("Min CPS", 8, 0, 20);
    private final Setting<Integer> maxCPS = new Setting<>("Max CPS", 17, 0, 20);
    private final Setting<Boolean> onlyClick = new Setting<>("Only Click", true);

    private final Random random = new Random();
    private long nextClick;
    private long lastClick;

    public AutoClicker() {
        super("AutoClicker", Category.COMBAT);
    }

    @EventHandler
    public void onTick(EventTick event) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.currentScreen != null) {
            return;
        }

        boolean isClicking = mc.options.attackKey.isPressed();
        if (onlyClick.getValue() && !isClicking) {
            return;
        }

        if (System.currentTimeMillis() - lastClick >= nextClick) {
            performClick(mc);
            lastClick = System.currentTimeMillis();
            nextClick = calculateNextClickDelay();
        }
    }

    private void performClick(MinecraftClient mc) {
        HitResult hitResult = mc.crosshairTarget;

        if (hitResult instanceof EntityHitResult entityHitResult) {
            mc.interactionManager.attackEntity(mc.player, entityHitResult.getEntity());
            mc.player.swingHand(mc.player.getActiveHand());
        }
    }

    private long calculateNextClickDelay() {
        int minDelay = 1000 / maxCPS.getValue();
        int maxDelay = 1000 / minCPS.getValue();
        return random.nextInt(maxDelay - minDelay + 1) + minDelay;
    }
}
