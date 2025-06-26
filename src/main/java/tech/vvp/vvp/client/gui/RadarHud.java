package tech.vvp.vvp.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import tech.vvp.vvp.VVP;
import tech.vvp.vvp.entity.vehicle.Mi24Entity;
import tech.vvp.vvp.entity.vehicle.CobrasharkEntity;
import tech.vvp.vvp.entity.vehicle.F35Entity;
import tech.vvp.vvp.entity.vehicle.Mi24ukrEntity;
import tech.vvp.vvp.entity.vehicle.Mi24polEntity;
import tech.vvp.vvp.entity.vehicle.CobraEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import java.util.*;

public class RadarHud {
    public static List<Vec3> radarTargets = new ArrayList<>();

    private static final ResourceLocation RADAR_BACKGROUND = new ResourceLocation(VVP.MOD_ID, "textures/gui/radar_bg.png");
    private static final ResourceLocation RADAR_TARGET = new ResourceLocation(VVP.MOD_ID, "textures/gui/radar_target.png");
    private static final ResourceLocation RADAR_SWEEP = new ResourceLocation(VVP.MOD_ID, "textures/gui/radar_sweep.png"); // Текстура 1x47
    
    private static final List<String> SUPPORTED_HELICOPTERS = Arrays.asList(
        "Mi24Entity",
        "СobraEntity",
        "F35Entity",
        "CobrasharkEntity",
        "Mi24ukrEntity", 
        "Mi24polEntity"
    );
    
    // Переменные для эффекта сканирования
    private static float sweepAngle = 0.0f;
    private static final float SWEEP_SPEED = 1.3f; // Увеличенная скорость вращения развертки
    private static final int TRAIL_LENGTH = 3; // Только 3 сегмента шлейфа
    
    // Карта для отслеживания времени обнаружения целей
    private static final Map<Vec3, Long> targetDetectionTime = new HashMap<>();
    private static final long TARGET_VISIBILITY_TIME = 1000; 
    private static final long FADE_TIME = 1000;

    public static final IGuiOverlay HUD_RADAR = (gui, guiGraphics, partialTick, screenWidth, screenHeight) -> {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null || !player.isAlive() || mc.level == null) {
            return;
        }

        Entity vehicle = player.getVehicle();

        if (vehicle != null && isRadarHelicopter(vehicle)) {
            int radarSize = 96;
            int radarX = 10;
            int radarY = 10;
            int radarCenterX = radarX + radarSize / 2;
            int radarCenterY = radarY + radarSize / 2;
            // ИЗМЕНЕНО: Дальность отображения на радаре увеличена в два раза (было 150f)
            float radarDisplayRange = 300f;

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            // Рисуем фон радара
            guiGraphics.blit(RADAR_BACKGROUND, radarX, radarY, 0, 0, radarSize, radarSize, radarSize, radarSize);

            // Обновляем угол развертки
            sweepAngle = (sweepAngle + SWEEP_SPEED * partialTick) % 360;
            
            // Сохраняем текущую матрицу
            PoseStack poseStack = guiGraphics.pose();
            poseStack.pushPose();
            
            // Перемещаемся в центр радара
            poseStack.translate(radarCenterX, radarCenterY, 0);
            
            // Рисуем шлейф развертки (3 сегмента с разной прозрачностью)
            for (int i = TRAIL_LENGTH - 1; i >= 0; i--) {
                float trailAngle = sweepAngle - (i * 20); // Каждый сегмент отстает на 20 градусов
                float alpha = 0.15f - (i * 0.05f); // Прозрачность уменьшается: 0.15, 0.10, 0.05
                
                poseStack.pushPose();
                poseStack.mulPose(Axis.ZP.rotationDegrees(trailAngle));
                
                RenderSystem.setShaderColor(0.0f, 1.0f, 0.0f, alpha);
                guiGraphics.blit(RADAR_SWEEP, -1, -47, 0, 0, 1, 47, 1, 47);
                
                poseStack.popPose();
            }
            
            // Рисуем основную линию развертки
            poseStack.pushPose();
            poseStack.mulPose(Axis.ZP.rotationDegrees(sweepAngle));
            
            RenderSystem.setShaderColor(0.0f, 1.0f, 0.0f, 0.7f);
            guiGraphics.blit(RADAR_SWEEP, -1, -47, 0, 0, 1, 47, 1, 47);
            
            poseStack.popPose();
            
            // Восстанавливаем матрицу
            poseStack.popPose();
            
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

            // Текущее время
            long currentTime = System.currentTimeMillis();
            
            // Очищаем старые цели
            targetDetectionTime.entrySet().removeIf(entry -> 
                currentTime - entry.getValue() > TARGET_VISIBILITY_TIME + FADE_TIME);

            // Рисуем цели
            for (Vec3 targetPos : radarTargets) {
                Vec3 relativePos = targetPos.subtract(player.position());
                double distance = Math.sqrt(relativePos.x * relativePos.x + relativePos.z * relativePos.z);

                // Примечание: фактическая дальность обнаружения по-прежнему зависит от mi24Entity.RADAR_RANGE.
                // Если там стоит 150, то цели дальше 150 блоков не появятся, даже если радар показывает 300.
                if (distance > Mi24Entity.RADAR_RANGE) continue;

                float playerYaw = (player.getViewYRot(1.0f) % 360 + 360) % 360;
                double angleToTarget = Math.toDegrees(Math.atan2(relativePos.z, relativePos.x)) - 90;
                double rotatedAngle = Math.toRadians(angleToTarget - playerYaw - 90);

                double displayDist = (distance / radarDisplayRange) * (radarSize / 2.0 - 4);
                displayDist = Math.min(displayDist, radarSize / 2.0 - 4);

                int targetX = radarCenterX + (int) (Math.cos(rotatedAngle) * displayDist);
                int targetY = radarCenterY + (int) (Math.sin(rotatedAngle) * displayDist);

                // Проверяем угол между развёрткой и целью
                float targetAngle = (float) (Math.toDegrees(rotatedAngle) + 90) % 360;
                float angleDiff = Math.abs(normalizeAngle(targetAngle - sweepAngle));
                
                // Если развёртка проходит через цель, отмечаем время обнаружения
                if (angleDiff < 5 && !targetDetectionTime.containsKey(targetPos)) {
                    targetDetectionTime.put(targetPos, currentTime);
                }
                
                // Показываем цель только если она была обнаружена
                if (targetDetectionTime.containsKey(targetPos)) {
                    long detectionTime = targetDetectionTime.get(targetPos);
                    long timeSinceDetection = currentTime - detectionTime;
                    
                    float alpha = 1.0f;
                    
                    if (timeSinceDetection < TARGET_VISIBILITY_TIME) {
                        // Цель полностью видима
                        alpha = 1.0f;
                        
                        // Эффект пульсации в первую секунду после обнаружения
                        if (timeSinceDetection < 1000) {
                            float pulse = (float) (Math.sin(timeSinceDetection * 0.01) * 0.3 + 0.7);
                            alpha *= pulse;
                        }
                    } else if (timeSinceDetection < TARGET_VISIBILITY_TIME + FADE_TIME) {
                        // Цель затухает
                        alpha = 1.0f - ((float)(timeSinceDetection - TARGET_VISIBILITY_TIME) / FADE_TIME);
                    } else {
                        // Цель больше не видна
                        continue;
                    }
                    
                    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
                    // ИЗМЕНЕНО: Размер цели уменьшен ~в 1.5 раза (с 4x4 до 3x3) для лучшей читаемости
                    int targetSize = 3;
                    guiGraphics.blit(RADAR_TARGET, targetX - (targetSize / 2), targetY - (targetSize / 2), 0, 0, targetSize, targetSize, targetSize, targetSize);
                }
            }
            
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
        }
    };
    
    // Нормализация угла в диапазон 0-180
    private static float normalizeAngle(float angle) {
        angle = angle % 360;
        if (angle < 0) angle += 360;
        if (angle > 180) angle = 360 - angle;
        return angle;
    }
    
    private static boolean isRadarHelicopter(Entity vehicle) {
        String vehicleClassName = vehicle.getClass().getSimpleName();
        return SUPPORTED_HELICOPTERS.contains(vehicleClassName);
    }
}