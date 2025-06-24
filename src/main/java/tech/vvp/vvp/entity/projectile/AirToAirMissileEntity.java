package tech.vvp.vvp.entity.projectile;

import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.entity.projectile.FastThrowableProjectile;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tools.CustomExplosion;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import tech.vvp.vvp.init.ModEntities;
import tech.vvp.vvp.init.ModItems;

public class AirToAirMissileEntity extends FastThrowableProjectile implements GeoEntity {

    public static final EntityDataAccessor<String> TARGET_UUID = SynchedEntityData.defineId(AirToAirMissileEntity.class, EntityDataSerializers.STRING);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private float damage = 500.0f;
    private float explosionDamage = 150f;
    private float explosionRadius = 7f;

    public AirToAirMissileEntity(EntityType<? extends AirToAirMissileEntity> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    public AirToAirMissileEntity(LivingEntity owner, Level level, String targetUUID) {
        super(ModEntities.AIR_TO_AIR_MISSILE.get(), owner, level);
        this.entityData.set(TARGET_UUID, targetUUID);
    }

    public AirToAirMissileEntity(PlayMessages.SpawnEntity spawnEntity, Level level) {
        this(ModEntities.AIR_TO_AIR_MISSILE.get(), level);
    }

    public void setTargetUuid(String uuid) {
        this.entityData.set(TARGET_UUID, uuid);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TARGET_UUID, "none");
    }

    @Override
    public void tick() {
        super.tick();
        Entity targetEntity = EntityFindUtil.findEntity(this.level(), entityData.get(TARGET_UUID));

        if (targetEntity != null && !targetEntity.isRemoved() && targetEntity.isAlive()) {
            if (this.level() instanceof ServerLevel) {
                if (targetEntity.tickCount % 10 == 0) {
                    targetEntity.level().playSound(null, targetEntity.getOnPos(), ModSounds.MISSILE_WARNING.get(), SoundSource.PLAYERS, 2.5F, 1.3F);
                }
            }
            if (this.tickCount > 3) {
                Vec3 predictedPos = targetEntity.position().add(targetEntity.getDeltaMovement().scale(0.9));
                Vec3 vectorToTarget = this.position().vectorTo(predictedPos).normalize();
                this.setDeltaMovement(this.getDeltaMovement().add(vectorToTarget.scale(0.1)).normalize());
            }
        }

        if (this.tickCount > 300 || this.isInWater() || (targetEntity != null && !targetEntity.isAlive())) {
            this.causeExplode(this.position());
            this.discard();
            return;
        }

        if (this.getDeltaMovement().length() < 3.8) {
            this.setDeltaMovement(this.getDeltaMovement().scale(1.06));
        }
        this.setDeltaMovement(this.getDeltaMovement().multiply(0.998, 0.998, 0.998));

        if (this.tickCount > 2 && !this.level().isClientSide()) {
            ((ServerLevel)this.level()).sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, this.xo, this.yo, this.zo, 2, 0.1, 0.1, 0.1, 0.02);
            ((ServerLevel)this.level()).sendParticles(ParticleTypes.FLAME, this.xo, this.yo, this.zo, 1, 0.0, 0.0, 0.0, 0.05);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity entity = result.getEntity();
        if (this.getOwner() != null && (entity == this.getOwner() || entity == this.getOwner().getVehicle())) return;
        if (!this.level().isClientSide()) {
            entity.hurt(ModDamageTypes.causeCannonFireDamage(this.level().registryAccess(), this, this.getOwner()), this.damage);
            causeExplode(result.getLocation());
            this.discard();
        }
    }

    @Override
    public void onHitBlock(@NotNull BlockHitResult blockHitResult) {
        if (!this.level().isClientSide()) {
            causeExplode(blockHitResult.getLocation());
            this.discard();
        }
    }

    private void causeExplode(Vec3 vec3) {
        if (this.level().isClientSide) return;
        CustomExplosion explosion = new CustomExplosion(this.level(), this,
                ModDamageTypes.causeProjectileBoomDamage(this.level().registryAccess(), this, this.getOwner()),
                explosionDamage, vec3.x, vec3.y, vec3.z, explosionRadius,
                ExplosionConfig.EXPLOSION_DESTROY.get() ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.KEEP, true);
        explosion.explode();
        net.minecraftforge.event.ForgeEventFactory.onExplosionStart(this.level(), explosion);
        explosion.finalizeExplosion(false);
        ParticleTool.spawnHugeExplosionParticles(this.level(), vec3);
    }
    
    @Override public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
    @Override protected @NotNull Item getDefaultItem() { return ModItems.AIR_TO_AIR_MISSILE_ITEM.get(); }
    @Override public boolean isNoGravity() { return true; }
    @Override protected float getGravity() { return 0F; }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar data) { data.add(new AnimationController<>(this, "controller", 0, event -> event.setAndContinue(RawAnimation.begin().thenLoop("animation.missile.fly")))); }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return this.cache; }
}