package thunder.hack.features.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.BooleanSettingGroup;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine;

import java.awt.*;
import java.util.List;

import static thunder.hack.utility.render.Render2DEngine.applyOpacity;

public class AuraRageDisplay extends Module {
    public AuraRageDisplay() {
        super("AuraRageDisplay", Category.RENDER);
    }

    private final Setting<Boolean> onlySelf = new Setting<>("OnlySelf", false);
    private final Setting<ColorSetting> defaultColor = new Setting<>("DefaultColor", new ColorSetting(Color.MAGENTA));
    private final Setting<ColorSetting> enemyInRangeColor = new Setting<>("EnemyInRangeColor", new ColorSetting(Color.RED));
    private final Setting<ColorSetting> otherPlayerColor = new Setting<>("OtherPlayerColor", new ColorSetting(Color.YELLOW), v -> !onlySelf.getValue());
    
    private final Setting<BooleanSettingGroup> glowSettings = new Setting<>("Glow", new BooleanSettingGroup(true));
    private final Setting<Integer> glowRadius = new Setting<>("GlowRadius", 8, 1, 20).addToGroup(glowSettings);
    private final Setting<Float> glowIntensity = new Setting<>("GlowIntensity", 0.8f, 0.1f, 2.0f).addToGroup(glowSettings);
    
    private final Setting<Integer> circlePoints = new Setting<>("CirclePoints", 64, 8, 128);
    private final Setting<Float> lineWidth = new Setting<>("LineWidth", 2.0f, 0.5f, 10.0f);
    private final Setting<Boolean> hudColorMode = new Setting<>("HudColorMode", false);
    private final Setting<Integer> colorOffset = new Setting<>("ColorOffset", 10, 1, 50, v -> hudColorMode.getValue());
    private final Setting<Boolean> ignoreJump = new Setting<>("IgnoreJump", false);

    @Override
    public void onRender3D(MatrixStack stack) {
        if (!ModuleManager.aura.isEnabled() || mc.player == null || mc.world == null) {
            return;
        }

        List<AbstractClientPlayerEntity> players = mc.world.getPlayers();
        
        for (AbstractClientPlayerEntity player : players) {
            if (player == mc.player && !onlySelf.getValue()) continue;
            if (player != mc.player && onlySelf.getValue()) continue;
            if (player.isDead() || !player.isAlive()) continue;
            
            float range = getAuraRange();
            Color circleColor = CircleColor(player, range);
            
            if (glowSettings.getValue().isEnabled()) {
                renderGlowCircle(stack, player, range, circleColor);
            }
            
            renderCircle(stack, player, range, circleColor);
        }
    }

    private float getAuraRange() {
        if (mc.player.isFallFlying() && ModuleManager.aura.elytra.getValue()) {
            return ModuleManager.aura.elytraAttackRange.getValue();
        }
        return ModuleManager.aura.attackRange.getValue();
    }

    private Color CircleColor(AbstractClientPlayerEntity player, float range) {
        if (player == mc.player) {
            List<AbstractClientPlayerEntity> enemiesInRange = getEnemiesInRange(player, range);
            if (!enemiesInRange.isEmpty()) {
                return enemyInRangeColor.getValue().getColorObject();
            }
            return defaultColor.getValue().getColorObject();
        } else {
            return otherPlayerColor.getValue().getColorObject();
        }
    }

    private List<AbstractClientPlayerEntity> getEnemiesInRange(AbstractClientPlayerEntity centerPlayer, float range) {
        return mc.world.getPlayers().stream()
                .filter(p -> p != centerPlayer)
                .filter(p -> !p.isDead() && p.isAlive())
                .filter(p -> centerPlayer.squaredDistanceTo(p) <= range * range)
                .filter(this::isEnemy)
                .toList();
    }

    private boolean isEnemy(AbstractClientPlayerEntity player) {
        return player != mc.player 
                && !thunder.hack.core.Managers.FRIEND.isFriend(player)
                && !player.isCreative();
    }

    private void renderGlowCircle(MatrixStack stack, AbstractClientPlayerEntity player, float range, Color color) {
        double x = player.prevX + (player.getX() - player.prevX) * Render3DEngine.getTickDelta() - mc.getEntityRenderDispatcher().camera.getPos().getX();
        double y = ignoreJump.getValue() ? 
            player.getBlockY() + 0.1 - mc.getEntityRenderDispatcher().camera.getPos().getY() :
            player.prevY + (player.getY() - player.prevY) * Render3DEngine.getTickDelta() - mc.getEntityRenderDispatcher().camera.getPos().getY();
        double z = player.prevZ + (player.getZ() - player.prevZ) * Render3DEngine.getTickDelta() - mc.getEntityRenderDispatcher().camera.getPos().getZ();

        stack.push();
        stack.translate(x, y, z);
        
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(770, 1);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        
        for (int i = 0; i < glowRadius.getValue(); i++) {
            float glowRange = range + (i * 0.1f);
            float alpha = (1.0f - (float)i / glowRadius.getValue()) * glowIntensity.getValue() * 0.3f;
            Color glowColor = applyOpacity(color, alpha);
            
            renderSingleCircle(stack, glowRange, glowColor, circlePoints.getValue(), lineWidth.getValue() + i * 0.5f);
        }
        
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        stack.pop();
    }

    private void renderCircle(MatrixStack stack, AbstractClientPlayerEntity player, float range, Color color) {
        double x = player.prevX + (player.getX() - player.prevX) * Render3DEngine.getTickDelta() - mc.getEntityRenderDispatcher().camera.getPos().getX();
        double y = ignoreJump.getValue() ? 
            player.getBlockY() + 0.1 - mc.getEntityRenderDispatcher().camera.getPos().getY() :
            player.prevY + (player.getY() - player.prevY) * Render3DEngine.getTickDelta() - mc.getEntityRenderDispatcher().camera.getPos().getY();
        double z = player.prevZ + (player.getZ() - player.prevZ) * Render3DEngine.getTickDelta() - mc.getEntityRenderDispatcher().camera.getPos().getZ();

        stack.push();
        stack.translate(x, y, z);
        
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        
        renderSingleCircle(stack, range, color, circlePoints.getValue(), lineWidth.getValue());
        
        RenderSystem.disableBlend();
        stack.pop();
    }

    private void renderSingleCircle(MatrixStack stack, float radius, Color color, int points, float width) {
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = stack.peek().getPositionMatrix();
        
        for (int i = 0; i <= points; i++) {
            Color currentColor = color;
            if (hudColorMode.getValue()) {
                currentColor = HudEditor.getColor(i * colorOffset.getValue());
            }
            
            float angle = (float) (i * 2 * Math.PI / points);
            float x = (float) (radius * Math.cos(angle));
            float z = (float) (radius * Math.sin(angle));
            
            bufferBuilder.vertex(matrix, x, 0f, z).color(currentColor.getRGB());
        }
        
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }
} 