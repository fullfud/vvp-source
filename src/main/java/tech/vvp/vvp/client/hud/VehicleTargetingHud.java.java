package tech.vvp.vvp.client.hud;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import tech.vvp.vvp.VVP;
import tech.vvp.vvp.entity.vehicle.F35Entity;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;

@Mod.EventBusSubscriber(modid = VVP.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class VehicleTargetingHud { // <-- УБЕДИСЬ, ЧТО ИМЯ СОВПАДАЕТ С ИМЕНЕМ ФАЙЛА

    @SubscribeEvent
    public static void renderHud(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.getVehicle() == null) return;

        if (!(mc.player.getVehicle() instanceof F35Entity f35)) return;
        if (!f35.locked) return;
        
        Entity target = EntityFindUtil.findEntity(f35.level(), f35.getTargetUuid());
        if (target == null) return;

        GuiGraphics guiGraphics = event.getGuiGraphics();
        PoseStack poseStack = guiGraphics.pose();
        
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        Vec3 targetPos = target.getEyePosition(event.getPartialTick());
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
        
        Matrix4f modelViewMatrix = poseStack.last().pose();
        Matrix4f projectionMatrix = event.getProjectionMatrix();
        Matrix4f mvpMatrix = new Matrix4f(projectionMatrix).mul(modelViewMatrix);
        
        Vector4f screenPosVec = new Vector4f((float)(targetPos.x - cameraPos.x), (float)(targetPos.y - cameraPos.y), (float)(targetPos.z - cameraPos.z), 1.0f).mul(mvpMatrix);
        
        if (screenPosVec.w <= 0) return;

        float x = (screenPosVec.x / screenPosVec.w + 1.0f) / 2.0f * screenWidth;
        float y = (1.0f - screenPosVec.y / screenPosVec.w) / 2.0f * screenHeight;

        if (x < 0 || x > screenWidth || y < 0 || y > screenHeight) return;
        
        int size = 25;
        int color = 0xFFFF0000;
        int halfSize = size / 2;
        int cornerLength = size / 4;

        // Верхний левый
        guiGraphics.fill(x - halfSize, y - halfSize, x - halfSize + cornerLength, y - halfSize + 1, color);
        guiGraphics.fill(x - halfSize, y - halfSize, x - halfSize + 1, y - halfSize + cornerLength, color);
        // Верхний правый
        guiGraphics.fill(x + halfSize - cornerLength, y - halfSize, x + halfSize, y - halfSize + 1, color);
        guiGraphics.fill(x + halfSize, y - halfSize, x + halfSize - 1, y - halfSize + cornerLength, color);
        // Нижний левый
        guiGraphics.fill(x - halfSize, y + halfSize, x - halfSize + cornerLength, y + halfSize - 1, color);
        guiGraphics.fill(x - halfSize, y + halfSize, x - halfSize + 1, y + halfSize - cornerLength, color);
        // Нижний правый
        guiGraphics.fill(x + halfSize - cornerLength, y + halfSize, x + halfSize, y + halfSize - 1, color);
        guiGraphics.fill(x + halfSize, y + halfSize, x + halfSize - 1, y + halfSize - cornerLength, color);
    }
}