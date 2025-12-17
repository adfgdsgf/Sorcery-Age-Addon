package com.jujutsuaddon.addon.damage.analysis;

import org.objectweb.asm.*;
import radon.jujutsu_kaisen.ability.base.Ability;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AbilityDamageData {

    private static final Map<Class<?>, CachedData> CACHE = new ConcurrentHashMap<>();

    private static final String[] PROJECTILE_PACKAGES = {
            "radon.jujutsu_kaisen.entity.projectile.",
            "radon.jujutsu_kaisen.entity.projectile.base.",
            "radon.jujutsu_kaisen.entity.effect.",
            "radon.jujutsu_kaisen.entity."
    };

    private static final String[] DAMAGE_FIELD_NAMES = {
            "DAMAGE", "BASE_DAMAGE", "damage", "baseDamage", "ATTACK_DAMAGE"
    };

    private static final String[] DAMAGE_METHOD_NAMES = {
            "getDamage", "getBaseDamage", "getAttackDamage"
    };

    public record CachedData(
            @Nullable Float baseDamage,
            float multiplier,
            @Nullable Class<?> projectileClass,
            boolean analyzed
    ) {
        public static CachedData empty() {
            return new CachedData(null, 1.0f, null, true);
        }

        @Nullable
        public Float getEffectiveDamage() {
            if (baseDamage == null || baseDamage <= 0) return null;
            return baseDamage * multiplier;
        }
    }

    public static CachedData get(Ability ability) {
        if (ability == null) return CachedData.empty();
        return get(ability.getClass());
    }

    public static CachedData get(Class<?> abilityClass) {
        return CACHE.computeIfAbsent(abilityClass, AbilityDamageData::analyze);
    }

    @Nullable
    public static Float getEffectiveDamage(Ability ability) {
        return get(ability).getEffectiveDamage();
    }

    private static CachedData analyze(Class<?> abilityClass) {
        AnalysisResult analysis = analyzeAbilityClass(abilityClass);

        Float baseDamage = null;
        Class<?> effectiveProjectileClass = null;

        // 1. 优先使用 ASM 找到的投射物
        if (analysis.projectileClass != null) {
            baseDamage = extractDamageFromClass(analysis.projectileClass);
            if (baseDamage == null) {
                baseDamage = extractDamageFromMethod(analysis.projectileClass);
            }
            if (baseDamage != null) {
                effectiveProjectileClass = analysis.projectileClass;
            }
        }

        // 2. 尝试从技能类本身提取
        if (baseDamage == null) {
            baseDamage = extractDamageFromClass(abilityClass);
        }

        // ★★★ 3. 只有当 run 方法有实际内容时，才尝试名称匹配 ★★★
        if (baseDamage == null && analysis.hasAnyContent) {
            Class<?> projectile = findProjectileByName(abilityClass);
            if (projectile != null) {
                baseDamage = extractDamageFromClass(projectile);
                if (baseDamage == null) {
                    baseDamage = extractDamageFromMethod(projectile);
                }
                if (baseDamage != null && effectiveProjectileClass == null) {
                    effectiveProjectileClass = projectile;
                }
            }
        }

        return new CachedData(
                baseDamage,
                analysis.damageMultiplier,
                effectiveProjectileClass,
                true
        );
    }

    // ==================== ASM 分析 ====================

    private record AnalysisResult(
            Class<?> projectileClass,
            float damageMultiplier,
            boolean hasAnyContent
    ) {}

    private static AnalysisResult analyzeAbilityClass(Class<?> abilityClass) {
        try {
            String className = abilityClass.getName().replace('.', '/');
            InputStream is = abilityClass.getClassLoader().getResourceAsStream(className + ".class");
            if (is == null) return new AnalysisResult(null, 1.0f, false);

            ClassReader reader = new ClassReader(is);
            AbilityAnalyzer visitor = new AbilityAnalyzer();
            reader.accept(visitor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            is.close();

            Class<?> projectileClass = null;
            if (visitor.projectileClassName != null) {
                try {
                    projectileClass = Class.forName(visitor.projectileClassName.replace('/', '.'));
                } catch (ClassNotFoundException ignored) {}
            }

            return new AnalysisResult(
                    projectileClass,
                    visitor.damageMultiplier,
                    visitor.hasAnyContent
            );
        } catch (Exception e) {
            return new AnalysisResult(null, 1.0f, false);
        }
    }

    private static class AbilityAnalyzer extends ClassVisitor {
        String projectileClassName = null;
        float damageMultiplier = 1.0f;
        boolean hasAnyContent = false;
        private boolean multiplierFound = false;

        AbilityAnalyzer() {
            super(Opcodes.ASM9);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            if (name.equals("run") || name.startsWith("lambda$run$")) {
                return new MultiplierDetector();
            }
            return null;
        }

        private class MultiplierDetector extends MethodVisitor {
            private int state = 0;
            private Float pendingFloat = null;
            private int instructionCount = 0;

            MultiplierDetector() {
                super(Opcodes.ASM9);
            }

            @Override
            public void visitMethodInsn(int opcode, String owner, String methodName,
                                        String desc, boolean isInterface) {
                instructionCount++;

                if (!methodName.equals("<init>")) {
                    hasAnyContent = true;
                }

                if (methodName.equals("getPower")) {
                    state = 1;
                    pendingFloat = null;
                } else {
                    resetState();
                }
            }

            @Override
            public void visitLdcInsn(Object value) {
                instructionCount++;
                hasAnyContent = true;

                if (state == 1 && value instanceof Float f) {
                    if (f > 1.0f && f <= 10.0f) {
                        pendingFloat = f;
                        state = 2;
                        return;
                    }
                }
                if (state != 0) resetState();
            }

            @Override
            public void visitInsn(int opcode) {
                instructionCount++;

                if (opcode == Opcodes.FMUL && state == 2 && pendingFloat != null) {
                    if (!multiplierFound) {
                        damageMultiplier = pendingFloat;
                        multiplierFound = true;
                    }
                }
                resetState();
            }

            @Override
            public void visitTypeInsn(int opcode, String type) {
                instructionCount++;

                if (opcode == Opcodes.NEW) {
                    hasAnyContent = true;

                    if (isProjectileType(type)) {
                        if (projectileClassName == null) {
                            projectileClassName = type;
                        }
                    }
                }
                if (state == 2) resetState();
            }

            @Override
            public void visitVarInsn(int opcode, int var) {
                instructionCount++;
                if (state == 2) resetState();
            }

            @Override
            public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                instructionCount++;
                hasAnyContent = true;
                if (state == 2) resetState();
            }

            @Override
            public void visitIntInsn(int opcode, int operand) {
                instructionCount++;
                if (state == 2) resetState();
            }

            @Override
            public void visitJumpInsn(int opcode, Label label) {
                instructionCount++;
                hasAnyContent = true;
                resetState();
            }

            @Override
            public void visitEnd() {
                // 如果指令数太少，说明是空方法
                if (instructionCount <= 1) {
                    hasAnyContent = false;
                }
            }

            private void resetState() {
                state = 0;
                pendingFloat = null;
            }
        }

        private boolean isProjectileType(String type) {
            return type.contains("Projectile") || type.contains("projectile/") ||
                    type.endsWith("Ball") || type.endsWith("Bolt") ||
                    type.endsWith("Arrow") || type.endsWith("Beam");
        }
    }

    // ==================== 从方法提取伤害 ====================

    @Nullable
    private static Float extractDamageFromMethod(Class<?> clazz) {
        for (String methodName : DAMAGE_METHOD_NAMES) {
            Float damage = analyzeMethodForDamage(clazz, methodName);
            if (damage != null && damage > 0) {
                return damage;
            }
        }

        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            return extractDamageFromMethod(superClass);
        }

        return null;
    }

    @Nullable
    private static Float analyzeMethodForDamage(Class<?> clazz, String targetMethodName) {
        try {
            String className = clazz.getName().replace('.', '/');
            InputStream is = clazz.getClassLoader().getResourceAsStream(className + ".class");
            if (is == null) return null;

            ClassReader reader = new ClassReader(is);
            DamageMethodAnalyzer visitor = new DamageMethodAnalyzer(targetMethodName);
            reader.accept(visitor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            is.close();

            return visitor.foundDamage;
        } catch (Exception e) {
            return null;
        }
    }

    private static class DamageMethodAnalyzer extends ClassVisitor {
        private final String targetMethodName;
        Float foundDamage = null;

        DamageMethodAnalyzer(String targetMethodName) {
            super(Opcodes.ASM9);
            this.targetMethodName = targetMethodName;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            if (name.equals(targetMethodName) && descriptor.endsWith(")F")) {
                return new MethodVisitor(Opcodes.ASM9) {
                    Float lastFloat = null;

                    @Override
                    public void visitLdcInsn(Object value) {
                        if (value instanceof Float f && f > 0) {
                            lastFloat = f;
                        }
                    }

                    @Override
                    public void visitInsn(int opcode) {
                        if (opcode == Opcodes.FRETURN && lastFloat != null) {
                            if (foundDamage == null || lastFloat > foundDamage) {
                                foundDamage = lastFloat;
                            }
                        }
                    }
                };
            }
            return null;
        }
    }

    // ==================== 字段提取 ====================

    @Nullable
    private static Float extractDamageFromClass(Class<?> clazz) {
        for (String fieldName : DAMAGE_FIELD_NAMES) {
            Float value = getStaticFloatField(clazz, fieldName);
            if (value != null && value > 0) return value;
        }

        for (Field field : clazz.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) continue;
            if (!isNumericType(field.getType())) continue;

            String name = field.getName().toUpperCase();
            if (name.contains("DAMAGE") && !name.contains("SCALE") &&
                    !name.contains("REDUCTION") && !name.contains("MULTIPLIER")) {
                Float value = getStaticFloatField(clazz, field.getName());
                if (value != null && value > 0) return value;
            }
        }

        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            return extractDamageFromClass(superClass);
        }

        return null;
    }

    @Nullable
    private static Float getStaticFloatField(Class<?> clazz, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            if (Modifier.isStatic(field.getModifiers()) && isNumericType(field.getType())) {
                field.setAccessible(true);
                Object value = field.get(null);
                if (value instanceof Number num) {
                    return num.floatValue();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    // ==================== 名称匹配 ====================

    @Nullable
    private static Class<?> findProjectileByName(Class<?> abilityClass) {
        String abilityName = abilityClass.getSimpleName();

        // ★★★ 功能性后缀 - 这些技能变体通常没有自己的伤害 ★★★
        String[] functionalSuffixes = {"Skating", "Flight", "Dash", "Movement", "Teleport"};
        for (String suffix : functionalSuffixes) {
            if (abilityName.endsWith(suffix)) {
                return null;  // 功能性技能，不做名称匹配
            }
        }

        String[] suffixesToStrip = {"Barrage", "Still", "Motion", "Attack",
                "Ability", "Net", "Wave", "Burst", "Storm", "Web", "web"};
        String[] suffixesToAdd = {"Projectile", "Entity", "Ball", "Bolt", "Arrow", "Beam"};

        for (String pkg : PROJECTILE_PACKAGES) {
            for (String suffix : suffixesToAdd) {
                Class<?> found = tryLoadClass(pkg + abilityName + suffix);
                if (found != null) return found;
            }

            for (String strip : suffixesToStrip) {
                if (abilityName.endsWith(strip)) {
                    String baseName = abilityName.substring(0, abilityName.length() - strip.length());
                    for (String suffix : suffixesToAdd) {
                        Class<?> found = tryLoadClass(pkg + baseName + suffix);
                        if (found != null) return found;
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    private static Class<?> tryLoadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static boolean isNumericType(Class<?> type) {
        return type == float.class || type == Float.class ||
                type == double.class || type == Double.class ||
                type == int.class || type == Integer.class;
    }

    public static void clearCache() {
        CACHE.clear();
    }
}
