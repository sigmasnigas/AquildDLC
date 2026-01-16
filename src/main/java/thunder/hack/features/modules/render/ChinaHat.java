package thunder.hack.features.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine;

import java.awt.*;

public class ChinaHat extends Module {
    public ChinaHat() {
        super("ChinaHat", Category.RENDER);
    }

    private final Setting<Float> radius = new Setting<>("Radius", 0.8f, 0.1f, 2.0f);
    private final Setting<Float> height = new Setting<>("Height", 0.5f, 0.1f, 1.5f);
    private final Setting<Float> width = new Setting<>("Width", 1.0f, 0.1f, 3.0f);
    private final Setting<Float> length = new Setting<>("Length", 1.0f, 0.1f, 3.0f);
    private final Setting<Boolean> firstPerson = new Setting<>("FirstPerson", false);
    private final Setting<ColorSetting> color1 = new Setting<>("Color1", new ColorSetting(new Color(255, 0, 0, 150)));
    private final Setting<ColorSetting> color2 = new Setting<>("Color2", new ColorSetting(new Color(0, 255, 0, 150)));
    private final Setting<Boolean> gradient = new Setting<>("Gradient", true);

    private static final int SEGMENTS = 128; // daje to w zmienną jakby ktoś chciał zmienic

    @Override
    public void onRender3D(MatrixStack stack) {
        if (mc.player == null) return;
        
        if (!firstPerson.getValue() && mc.options.getPerspective().isFirstPerson()) {
            return;
        }

        double x = mc.player.prevX + (mc.player.getX() - mc.player.prevX) * Render3DEngine.getTickDelta() - mc.getEntityRenderDispatcher().camera.getPos().getX();
        double y = mc.player.prevY + (mc.player.getY() - mc.player.prevY) * Render3DEngine.getTickDelta() - mc.getEntityRenderDispatcher().camera.getPos().getY() + mc.player.getHeight();
        double z = mc.player.prevZ + (mc.player.getZ() - mc.player.prevZ) * Render3DEngine.getTickDelta() - mc.getEntityRenderDispatcher().camera.getPos().getZ();

        stack.push();
        stack.translate(x, y, z);

        Render3DEngine.setupRender();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = stack.peek().getPositionMatrix();

        long time = System.currentTimeMillis();
        
        Color centerColor;
        if (gradient.getValue()) {
            float centerProgress = (float) (Math.sin(time * 0.002) + 1.0) / 2.0f;
            centerColor = Render2DEngine.interpolateColorHue(color1.getValue().getColorObject(), color2.getValue().getColorObject(), centerProgress);
        } else {
            centerColor = color1.getValue().getColorObject();
        }
        
        bufferBuilder.vertex(matrix, 0f, height.getValue(), 0f).color(centerColor.getRGB());

        for (int i = 0; i <= SEGMENTS; i++) {
            double angle = (double) i / SEGMENTS * Math.PI * 2;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            
            float x1 = cos * radius.getValue() * width.getValue();
            float z1 = sin * radius.getValue() * length.getValue();

            Color segmentColor;
            if (gradient.getValue()) {
                float baseProgress = (float) i / SEGMENTS;
                float fadeWave1 = (float) (Math.sin(time * 0.004 + baseProgress * Math.PI * 2) + 1.0) / 2.0f;
                float fadeWave2 = (float) (Math.cos(time * 0.003 + baseProgress * Math.PI * 2) + 1.0) / 2.0f;
                float combinedWave = (fadeWave1 + fadeWave2) / 2.0f;
                segmentColor = Render2DEngine.interpolateColorHue(color1.getValue().getColorObject(), color2.getValue().getColorObject(), combinedWave);
            } else {
                segmentColor = color2.getValue().getColorObject();
            }

            bufferBuilder.vertex(matrix, x1, 0f, z1).color(segmentColor.getRGB());
        }

        Render2DEngine.endBuilding(bufferBuilder);

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        Render3DEngine.endRender();

        stack.pop();
    }
} 