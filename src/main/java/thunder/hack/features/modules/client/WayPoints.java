package thunder.hack.features.modules.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector4d;
import thunder.hack.ThunderHack;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.world.WayPointManager;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.features.modules.Module;
import thunder.hack.utility.render.Render3DEngine;
import thunder.hack.utility.render.TextureStorage;

public final class WayPoints extends Module {
    public WayPoints() {
        super("WayPoints", Category.CLIENT);
    }

    @Override
    public void onEnable() {
        sendMessage(Managers.COMMAND.getPrefix() + "waypoint add x y z name");
    }

    public void onRender2D(DrawContext context) {
        if (!Managers.WAYPOINT.getWayPoints().isEmpty() && !fullNullCheck()) {
            for (WayPointManager.WayPoint wp : Managers.WAYPOINT.getWayPoints()) {
                if (wp.getName() == null) continue;
                if ((mc.isInSingleplayer() && wp.getServer().equals("SinglePlayer"))
                        || (mc.getNetworkHandler().getServerInfo() != null && !mc.getNetworkHandler().getServerInfo().address.contains(wp.getServer())))
                    continue;
                if (!mc.world.getRegistryKey().getValue().getPath().equals(wp.getDimension())) continue;

                Vec3d vector = new Vec3d(wp.getX(), wp.getY(), wp.getZ());
                Vec3d screenPos = Render3DEngine.worldSpaceToScreenSpace(vector);

                if (screenPos == null || screenPos.z < 0 || screenPos.z >= 1) continue;

                double posX = screenPos.x;
                double posY = screenPos.y;

                String coords = wp.getX() + " " + wp.getZ();
                String distance = String.format("%.0f", Math.sqrt(mc.player.squaredDistanceTo(wp.getX(), wp.getY(), wp.getZ()))) + "m";

                float diff = FontRenderers.sf_bold_mini.getStringWidth(wp.getName()) / 2.0f;

                float tagX = (float) (posX - diff);
                float tagX2 = (float) (posX - FontRenderers.sf_bold_mini.getStringWidth(coords) / 2.0f);
                float tagX3 = (float) (posX - FontRenderers.sf_bold_mini.getStringWidth(distance) / 2.0f);

                context.getMatrices().push();
                context.getMatrices().translate(posX - 10, posY - 35, 0);
                context.drawTexture(TextureStorage.waypoint, 0, 0, 20, 20, 0, 0, 20, 20, 20, 20);
                context.getMatrices().pop();

                FontRenderers.sf_bold_mini.drawString(context.getMatrices(), wp.getName(), tagX, (float) posY - 10, -1);
                FontRenderers.sf_bold_mini.drawString(context.getMatrices(), coords, tagX2, (float) posY - 2, -1);
                FontRenderers.sf_bold_mini.drawString(context.getMatrices(), distance, tagX3, (float) posY + 6, -1);
            }
        }
    }
}
