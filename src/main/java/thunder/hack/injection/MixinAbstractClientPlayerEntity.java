package thunder.hack.injection;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thunder.hack.core.manager.client.ModuleManager;

@Mixin(AbstractClientPlayerEntity.class)
public class MixinAbstractClientPlayerEntity {
    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void modifySkinTextures(CallbackInfoReturnable<SkinTextures> cir) {
        if (ModuleManager.cape != null && ModuleManager.cape.isEnabled()) {
            AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) (Object) this;
            Identifier customCape = ModuleManager.cape.getCapeTexture(player);
            if (customCape != null) {
                SkinTextures original = cir.getReturnValue();
                Identifier elytraTexture = ModuleManager.cape.elytraTexture.getValue() ? customCape : original.elytraTexture();
                SkinTextures modified = new SkinTextures(
                    original.texture(),
                    original.textureUrl(),
                    customCape,
                    elytraTexture,
                    original.model(),
                    original.secure()
                );
                cir.setReturnValue(modified);
            }
        }
    }
} 