package com.jujutsuaddon.addon.client;

import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.util.PressureBypassChecker;
import com.jujutsuaddon.addon.network.InfinityFieldData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ClientMovementHandler {

    public static void modifyInput(Input input) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null) return;

        // ★★★ 新增：天逆鉾豁免 ★★★
        if (PressureBypassChecker.shouldBypassPressure(player)) {
            return;
        }

        List<InfinityFieldData> fields = InfinityFieldClientCache.getFields();
        if (fields.isEmpty()) return;

        Vec3 playerPos = player.position();

        for (InfinityFieldData field : fields) {
            modifyInputForField(field, player, input, playerPos);
        }
    }

    private static void modifyInputForField(InfinityFieldData field, LocalPlayer player,
                                            Input input, Vec3 playerPos) {
        double zenoMultiplier = field.calculateZenoMultiplier(playerPos);

        if (zenoMultiplier < 0.05) {
            return;
        }

        // ★★★ 3D推力方向（包含Y轴） ★★★
        Vec3 pushDir3D = field.getFullPushDirection(playerPos);
        Vec3 pushDirHorizontal = field.getHorizontalPushDirection(playerPos);

        float yaw = player.getYRot();
        double yawRad = Math.toRadians(yaw);

        double forwardIntent = input.forwardImpulse;
        double leftIntent = input.leftImpulse;

        if (Math.abs(forwardIntent) < 0.01 && Math.abs(leftIntent) < 0.01) {
            // ★★★ 处理跳跃/下蹲（Y轴） ★★★
            handleVerticalInput(input, pushDir3D, zenoMultiplier);
            return;
        }

        double sin = Math.sin(yawRad);
        double cos = Math.cos(yawRad);
        double worldX = -forwardIntent * sin + leftIntent * cos;
        double worldZ = forwardIntent * cos + leftIntent * sin;
        Vec3 moveIntent = new Vec3(worldX, 0, worldZ);

        if (moveIntent.length() < 0.01) {
            handleVerticalInput(input, pushDir3D, zenoMultiplier);
            return;
        }

        // 分解为径向和切向
        double radialComponent = moveIntent.dot(pushDirHorizontal);

        // 不是接近：只处理Y轴
        if (radialComponent >= -0.01) {
            handleVerticalInput(input, pushDir3D, zenoMultiplier);
            return;
        }

        double blockRatio = Math.min(zenoMultiplier, 1.0);

        Vec3 radialVec = pushDirHorizontal.scale(radialComponent);
        Vec3 tangentVec = moveIntent.subtract(radialVec);

        // 径向和切向都用相同的阻止比例
        Vec3 newRadial = radialVec.scale(1.0 - blockRatio);
        Vec3 newTangent = tangentVec.scale(1.0 - blockRatio);

        Vec3 newMoveIntent = newRadial.add(newTangent);

        if (newMoveIntent.length() < 0.01) {
            input.forwardImpulse = 0;
            input.leftImpulse = 0;
            handleVerticalInput(input, pushDir3D, zenoMultiplier);
            return;
        }

        // 逆变换
        double newWorldX = newMoveIntent.x;
        double newWorldZ = newMoveIntent.z;
        double newForward = -newWorldX * sin + newWorldZ * cos;
        double newLeft = newWorldX * cos + newWorldZ * sin;

        input.forwardImpulse = (float) newForward;
        input.leftImpulse = (float) newLeft;

        // ★★★ 同时处理Y轴 ★★★
        handleVerticalInput(input, pushDir3D, zenoMultiplier);
    }

    /**
     * ★★★ 处理垂直方向输入（跳跃） ★★★
     * 如果玩家在施术者上方并试图下降接近，阻止
     * 如果玩家在施术者下方并试图跳跃接近，阻止
     */
    private static void handleVerticalInput(Input input, Vec3 pushDir3D, double zenoMultiplier) {
        // pushDir3D.y > 0 表示玩家在施术者上方
        // pushDir3D.y < 0 表示玩家在施术者下方

        double blockRatio = Math.min(zenoMultiplier, 1.0);

        // 玩家在下方，跳跃会接近施术者
        if (pushDir3D.y > 0.3 && input.jumping) {
            // 高阻力时阻止跳跃
            if (blockRatio >= 0.7) {
                input.jumping = false;
            }
        }

        // 注意：下落是重力导致的，不是输入，需要在别处处理
    }
}
