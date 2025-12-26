package com.jujutsuaddon.addon.vow.condition.types.permanent;

import com.jujutsuaddon.addon.api.vow.ICondition;
import com.jujutsuaddon.addon.util.helper.AbilityRetrievalHelper;
import com.jujutsuaddon.addon.util.helper.TechniqueHelper;
import com.jujutsuaddon.addon.vow.ParamDefinition;
import com.jujutsuaddon.addon.vow.VowType;
import com.jujutsuaddon.addon.vow.condition.ConditionParams;
import com.jujutsuaddon.addon.vow.validation.CheckContext;
import com.jujutsuaddon.addon.vow.validation.CheckTrigger;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.Nullable;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.capability.data.sorcerer.CursedTechnique;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

public class AbilityBanCondition implements ICondition {

    public static final ResourceLocation ID = new ResourceLocation("jujutsu_addon", "ability_ban");
    public static final String PARAM_ABILITY_ID = "abilityId";

    @Override
    public ResourceLocation getId() { return ID; }

    @Override
    public Component getDisplayName() {
        return Component.translatable("vow.condition.ability_ban");
    }

    @Override
    public Component getDescription(ConditionParams params) {
        String abilityId = params.getString(PARAM_ABILITY_ID, "unknown");
        return Component.translatable("vow.condition.ability_ban.desc",
                Component.translatable("ability.jujutsu_kaisen." + abilityId));
    }

    @Override
    public String getOccupancyKey(ConditionParams params) {
        String abilityId = params.getString(PARAM_ABILITY_ID, "");
        return getId().toString() + ":" + abilityId;
    }

    @Override
    public VowType getAllowedVowType() {
        return VowType.PERMANENT;
    }

    @Override
    public float calculateWeight(ConditionParams params) {
        return 10.0f;
    }

    @Override
    public boolean isViolated(LivingEntity owner, ConditionParams params, CheckContext context) {
        if (context.getAbility() == null) return false;
        String bannedId = params.getString(PARAM_ABILITY_ID, "");
        ResourceLocation key = JJKAbilities.getKey(context.getAbility());
        if (key == null) return false;
        return key.getPath().equals(bannedId);
    }

    @Override
    public CheckTrigger[] getTriggers() {
        return new CheckTrigger[] { CheckTrigger.ABILITY_ATTEMPT };
    }

    @Override
    public boolean requiresTickCheck() { return false; }

    @Override
    public boolean isConfigurable() { return true; }

    @Override
    public @Nullable ParamDefinition getConfigurableParams() {
        // 使用 DistExecutor 安全地执行客户端代码
        // 如果在服务端，直接返回默认列表（反正服务端不显示GUI）
        return DistExecutor.unsafeRunForDist(
                () -> this::getClientParams, // 客户端执行这个
                () -> this::getServerParams  // 服务端执行这个
        );
    }

    // 服务端逻辑：返回一个空的或者默认的列表，防止崩溃
    private ParamDefinition getServerParams() {
        List<String> defaultList = new ArrayList<>();
        defaultList.add("none");
        return new ParamDefinition()
                .addStringSelection(
                        PARAM_ABILITY_ID,
                        Component.translatable("vow.param.select_ability"),
                        defaultList,
                        "none"
                );
    }

    // 客户端逻辑：真正的获取玩家技能列表
    @OnlyIn(Dist.CLIENT)
    private ParamDefinition getClientParams() {
        List<String> userSkillIds = new ArrayList<>();

        try {
            // 在这里引用 Minecraft 类是安全的，因为这个方法只在客户端运行
            Player player = net.minecraft.client.Minecraft.getInstance().player;
            if (player != null) {
                // 1. 获取所有可用技能（含十影、原生、偷窃、复制）
                List<Ability> allAbilities = AbilityRetrievalHelper.getAllPlayerAbilities(player);

                // 2. 准备黑名单（要剔除的技能）
                Set<ResourceLocation> keysToExclude = new HashSet<>();

                // 判断玩家当前的主术式是不是 "复制" (Copy / Mimicry)
                boolean isCopyUser = player.getCapability(SorcererDataHandler.INSTANCE)
                        .map(data -> data.getTechnique() == CursedTechnique.MIMICRY)
                        .orElse(false);

                // 只有当玩家是 "复制术师" (乙骨) 时，我们才剔除额外术式。
                if (isCopyUser) {
                    for (CursedTechnique technique : TechniqueHelper.getAllExtraTechniques(player)) {
                        if (technique != null) {
                            for (Ability ability : technique.getAbilities()) {
                                ResourceLocation key = JJKAbilities.getKey(ability);
                                if (key != null) keysToExclude.add(key);
                            }
                        }
                    }
                }

                // 3. 筛选并填入列表
                for (Ability ability : allAbilities) {
                    ResourceLocation key = JJKAbilities.getKey(ability);

                    if (key != null) {
                        // 如果在黑名单里（即属于乙骨的复制技能），则跳过，不让玩家选
                        if (keysToExclude.contains(key)) {
                            continue;
                        }
                        userSkillIds.add(key.getPath());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (userSkillIds.isEmpty()) {
            userSkillIds.add("none");
        } else {
            Collections.sort(userSkillIds);
        }

        return new ParamDefinition()
                .addStringSelection(
                        PARAM_ABILITY_ID,
                        Component.translatable("vow.param.select_ability"),
                        userSkillIds,
                        userSkillIds.get(0)
                );
    }

    @Override
    public ConditionParams createDefaultParams() {
        return new ConditionParams().setString(PARAM_ABILITY_ID, "none");
    }

    @Override
    public CompoundTag serializeParams(ConditionParams params) { return params.serializeNBT(); }

    @Override
    public ConditionParams deserializeParams(CompoundTag nbt) { return ConditionParams.fromNBT(nbt); }
}
