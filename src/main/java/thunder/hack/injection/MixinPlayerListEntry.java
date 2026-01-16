//package thunder.hack.injection;
//
//import com.mojang.authlib.GameProfile;
//import net.minecraft.client.MinecraftClient;
//import net.minecraft.client.network.PlayerListEntry;
//import net.minecraft.client.util.SkinTextures;
//import net.minecraft.util.Identifier;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.Unique;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//import thunder.hack.utility.ThunderUtility;
//
//@Mixin(PlayerListEntry.class)
//public class MixinPlayerListEntry {
//
//    @Unique
//    private boolean loadedCapeTexture;
//
//    @Unique
//    private Identifier customCapeTexture;
//
//    @Inject(method = "<init>(Lcom/mojang/authlib/GameProfile;Z)V", at = @At("TAIL"))
//    private void initHook(GameProfile profile, boolean secureChatEnforced, CallbackInfo ci) {
//        getTexture(profile);
//    }
//
//    @Inject(method = "getSkinTextures", at = @At("TAIL"), cancellable = true)
//    private void getCapeTexture(CallbackInfoReturnable<SkinTextures> cir) {
//        if (customCapeTexture != null) {
//            SkinTextures prev = cir.getReturnValue();
//            SkinTextures newTextures = new SkinTextures(prev.texture(), prev.textureUrl(), customCapeTexture, customCapeTexture, prev.model(), prev.secure());
//            cir.setReturnValue(newTextures);
//        }
//    }
//
//    @Unique
//    private void getTexture(GameProfile profile) {
//        if (loadedCapeTexture) return;
//        loadedCapeTexture = true;
//
//        MinecraftClient mc = MinecraftClient.getInstance();
//        if (mc.player != null && profile.getName().equalsIgnoreCase(mc.player.getGameProfile().getName())) {
//            customCapeTexture = Identifier.of("thunderhack", "textures/capes/starcape.png");
//            return;
//        }
//
//        for (String str : ThunderUtility.starGazer) {
//            if (profile.getName().equalsIgnoreCase(str)) {
//                customCapeTexture = Identifier.of("thunderhack", "textures/capes/starcape.png");
//                return;
//            }
//        }
//    }
//}
