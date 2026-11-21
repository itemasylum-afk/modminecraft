package com.example.hollowland.entity;

import com.example.hollowland.HollowlandMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * The Watcher: a tall, pale thing that only moves while you are not looking at it.
 * If a player holds its gaze at close range for three seconds, it pulls them into the Hollowland.
 */
public class WatcherEntity extends Monster {
    private static final int GAZE_TICKS_TO_TAKE = 60;
    private static final double GAZE_RANGE_SQR = 6.0D * 6.0D;

    private int gazeTicks;

    public WatcherEntity(EntityType<? extends WatcherEntity> type, Level level) {
        super(type, level);
        this.xpReward = 20;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 60.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.26D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D);
    }

    /** Rare: only spawns in the dark, and only one time in eight when the normal rules allow it. */
    public static boolean checkWatcherSpawnRules(EntityType<WatcherEntity> type, LevelAccessor level,
                                                 MobSpawnType reason, BlockPos pos, RandomSource random) {
        return Monster.checkMonsterSpawnRules(type, level, reason, pos, random) && random.nextInt(8) == 0;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new StalkGoal(this));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 32.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    /** True if the player is looking roughly at this entity with a clear line of sight. */
    public boolean isObservedBy(Player player) {
        Vec3 view = player.getViewVector(1.0F).normalize();
        Vec3 toMe = new Vec3(getX() - player.getX(), getEyeY() - player.getEyeY(), getZ() - player.getZ());
        double dist = toMe.length();
        if (dist < 0.001D) {
            return true;
        }
        double dot = view.dot(toMe.scale(1.0D / dist));
        double threshold = Math.max(0.8D, 1.0D - 0.5D / dist);
        return dot > threshold && player.hasLineOfSight(this);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        LivingEntity target = getTarget();
        boolean gazing = target instanceof ServerPlayer p
                && level().dimension() != HollowlandMod.HOLLOWLAND
                && distanceToSqr(p) < GAZE_RANGE_SQR
                && isObservedBy(p);

        if (gazing) {
            ServerPlayer player = (ServerPlayer) target;
            gazeTicks++;
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false));
            if (gazeTicks % 20 == 1) {
                level().playSound(null, player.blockPosition(), SoundEvents.WARDEN_HEARTBEAT,
                        SoundSource.HOSTILE, 1.5F, 0.8F);
            }
            if (gazeTicks >= GAZE_TICKS_TO_TAKE) {
                HollowlandMod.pullIntoHollowland(player);
                this.discard();
            }
        } else if (gazeTicks > 0) {
            gazeTicks = Math.max(0, gazeTicks - 2);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // It can be hurt, but it won't fall down easily.
        return super.hurt(source, amount * 0.5F);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WARDEN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WARDEN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WARDEN_DEATH;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    /** Moves toward its target only while that target is not looking at it. */
    private static class StalkGoal extends Goal {
        private final WatcherEntity watcher;

        StalkGoal(WatcherEntity watcher) {
            this.watcher = watcher;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = watcher.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public void tick() {
            LivingEntity target = watcher.getTarget();
            if (target == null) {
                return;
            }
            if (target instanceof Player p && watcher.isObservedBy(p)) {
                watcher.getNavigation().stop();
            } else {
                watcher.getNavigation().moveTo(target, 1.0D);
            }
        }

        @Override
        public void stop() {
            watcher.getNavigation().stop();
        }
    }
}
