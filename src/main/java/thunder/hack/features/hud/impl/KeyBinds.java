package thunder.hack.features.hud.impl;

import net.minecraft.client.gui.DrawContext;
import org.jetbrains.annotations.NotNull;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.features.hud.HudElement;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.animation.AnimationUtility;

import java.awt.*;
import java.util.Objects;

public class KeyBinds extends HudElement {
    public final Setting<ColorSetting> oncolor = new Setting<>("OnColor", new ColorSetting(new Color(255, 255, 255, 255).getRGB()));
    public final Setting<ColorSetting> offcolor = new Setting<>("OffColor", new ColorSetting(new Color(200, 200, 200, 255).getRGB()));
    public final Setting<Boolean> onlyEnabled = new Setting<>("OnlyEnabled", false);

    public KeyBinds() {
        super("KeyBinds", 100, 100);
    }

    private float hAnimation;

    @Override
    public void onRender2D(DrawContext context) {
        super.onRender2D(context);

        float currentY = getPosY();
        float targetWidth = 75; 
        float headerHeight = 16f; 
        float bindHeight = 13f;   

        // 1. Obliczanie szerokości (szukamy najdłuższego rzędu)
        for (Module feature : Managers.MODULE.modules) {
            if (feature.isDisabled() && onlyEnabled.getValue()) continue;
            if (!Objects.equals(feature.getBind().getBind(), "None") && feature != ModuleManager.clickGui && feature != ModuleManager.thunderHackGui) {
                float totalW = FontRenderers.sf_bold_mini.getStringWidth(feature.getName() + getShortKeyName(feature)) + 22;
                if (totalW > targetWidth) targetWidth = totalW;
            }
        }

        hAnimation = AnimationUtility.fast(hAnimation, targetWidth, 15);

        // 2. RENDEROWANIE NAGŁÓWKA "KeyBinds"
        Render2DEngine.drawHudBase(context.getMatrices(), getPosX(), currentY, hAnimation, headerHeight, HudEditor.hudRound.getValue());
        
        // WYŚRODKOWANIE Y DLA NAGŁÓWKA (z poprawką +2.5f żeby nie był za wysoko)
        float headerTextY = currentY + (headerHeight / 2f) - (FontRenderers.sf_bold.getFontHeight("KeyBinds") / 2f) + 2.5f;
        
        if (HudEditor.hudStyle.is(HudEditor.HudStyle.Glowing)) {
            FontRenderers.sf_bold.drawString(context.getMatrices(), "KeyBinds", getPosX() + 6, headerTextY, 
                HudEditor.textColor.getValue().getColorObject().getRGB());
        } else {
            FontRenderers.sf_bold.drawGradientString(context.getMatrices(), "KeyBinds", getPosX() + 6, headerTextY, 10);
        }

        currentY += headerHeight + 2f;

        // 3. RENDEROWANIE BINDÓW (Osobne paski)
        for (Module feature : Managers.MODULE.modules) {
            if (feature.isDisabled() && onlyEnabled.getValue()) continue;

            if (!Objects.equals(feature.getBind().getBind(), "None") && feature != ModuleManager.clickGui && feature != ModuleManager.thunderHackGui) {
                String bindName = getShortKeyName(feature);
                int color = feature.isOn() ? oncolor.getValue().getColor() : offcolor.getValue().getColor();

                Render2DEngine.drawHudBase(context.getMatrices(), getPosX(), currentY, hAnimation, bindHeight, HudEditor.hudRound.getValue());

                // --- KLUCZOWA POPRAWKA CENTROWANIA Y ---
                // Dodajemy +2.7f offsetu, co przesunie tekst niżej, idealnie na środek czarnego paska
                float textY = currentY + (bindHeight / 2f) - (9f / 2f) + 2.7f;

                // Nazwa (Lewo)
                FontRenderers.sf_bold_mini.drawString(context.getMatrices(), feature.getName(), 
                    getPosX() + 6, textY, color);

                // Bind (Prawo)
                float bindWidth = FontRenderers.sf_bold_mini.getStringWidth(bindName);
                FontRenderers.sf_bold_mini.drawString(context.getMatrices(), bindName, 
                    getPosX() + hAnimation - bindWidth - 6, textY, color);

                currentY += bindHeight + 2f;
            }
        }

        // Draggable
        setWidth(hAnimation);
        setHeight(currentY - getPosY());
        setBounds(getPosX(), getPosY(), getWidth(), getHeight());
    }

    @NotNull
    public static String getShortKeyName(Module feature) {
        String sbind = feature.getBind().getBind();
        if (sbind == null || sbind.equals("None")) return "NONE";
        return switch (sbind) {
            case "LEFT_CONTROL" -> "LCtrl";
            case "RIGHT_CONTROL" -> "RCtrl";
            case "LEFT_SHIFT" -> "LShift";
            case "RIGHT_SHIFT" -> "RShift";
            case "LEFT_ALT" -> "LAlt";
            case "RIGHT_ALT" -> "RAlt";
            default -> sbind.toUpperCase();
        };
    }
}
