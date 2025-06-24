package tech.vvp.vvp.entity.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.config.server.VehicleConfig;
import com.atsuishio.superbwarfare.entity.vehicle.base.*;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.*;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.event.ClientMouseHandler;
import com.atsuishio.superbwarfare.init.*;
import com.atsuishio.superbwarfare.network.message.receive.ShakeClientMessage;
import com.atsuishio.superbwarfare.tools.*;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.PlayMessages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;
import tech.vvp.vvp.VVP;
import tech.vvp.vvp.entity.projectile.AirToAirMissileEntity;
import tech.vvp.vvp.entity.vehicle.weapon.AirToAirMissileWeapon;
import tech.vvp.vvp.network.ModMessages;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import static com.atsuishio.superbwarfare.event.ClientMouseHandler.freeCameraPitch;
import static com.atsuishio.superbwarfare.event.ClientMouseHandler.freeCameraYaw;
import static com.atsuishio.superbwarfare.tools.ParticleTool.sendParticle;

public class F35Entity extends ContainerMobileVehicleEntity implements GeoEntity, WeaponVehicleEntity, AircraftEntity {

    public static Consumer<MobileVehicleEntity> fireSound = vehicle -> {};

    public static final EntityDataAccessor<Integer> LOADED_ROCKET = SynchedEntityData.defineId(F35Entity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> LOADED_BOMB = SynchedEntityData.defineId(F35Entity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> LOADED_MISSILE = SynchedEntityData.defineId(F35Entity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> LOADED_AA_MISSILE = SynchedEntityData.defineId(F35Entity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> FIRE_TIME = SynchedEntityData.defineId(F35Entity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<String> TARGET_UUID = SynchedEntityData.defineId(F35Entity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Boolean> IS_ENGINE_RUNNING = SynchedEntityData.defineId(F35Entity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Float> PROPELLER_ROT = SynchedEntityData.defineId(F35Entity.class, EntityDataSerializers.FLOAT);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public int fireIndex;
    public int reloadCoolDownBomb;
    public int reloadCoolDownMissile;
    public String lockingTargetO = "none";
    public String lockingTarget = "none";
    public float destroyRot;
    public int lockTime;
    public boolean locked;
    private boolean wasFiring = false;
    public float delta_x;
    public float delta_y;
    public static final int RADAR_RANGE = 250;

    public F35Entity(PlayMessages.SpawnEntity packet, Level world) {
        this(tech.vvp.vvp.init.ModEntities.F35.get(), world);
    }

    public F35Entity(EntityType<F35Entity> type, Level world) {
        super(type, world);
        this.setMaxUpStep(1.5f);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.MOVEMENT_SPEED, 1.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 5.0D);
    }

    @SuppressWarnings("unchecked")
    public static F35Entity clientSpawn(PlayMessages.SpawnEntity packet, Level world) {
        EntityType<?> entityTypeFromPacket = BuiltInRegistries.ENTITY_TYPE.byId(packet.getTypeId());
        if (entityTypeFromPacket == null) {
            Mod.LOGGER.error("Failed to create entity from packet: Unknown entity type id: " + packet.getTypeId());
            return null;
        }
        if (!(entityTypeFromPacket instanceof EntityType<?>)) {
             Mod.LOGGER.error("Retrieved EntityType is not an instance of EntityType<?> for id: " + packet.getTypeId());
             return null;
        }
        EntityType<F35Entity> castedEntityType = (EntityType<F35Entity>) entityTypeFromPacket;
        F35Entity entity = new F35Entity(castedEntityType, world);
        return entity;
    }

    private void handleRadar() {
        if (this.level().isClientSide() || !(this.getFirstPassenger() instanceof ServerPlayer player)) {
            return;
        }
        List<Vec3> targetPositions = new ArrayList<>();
        List<Entity> potentialTargets = this.level().getEntities(this, this.getBoundingBox().inflate(RADAR_RANGE),
            entity -> (entity instanceof HelicopterEntity || entity instanceof AirEntity) && entity != this);

        if (!potentialTargets.isEmpty()) {
            for (Entity target : potentialTargets) {
                targetPositions.add(target.position());
            }
            ModMessages.sendToPlayer(new tech.vvp.vvp.network.message.S2CRadarSyncPacket(targetPositions), player);
        }
    }

    @Override
    public VehicleWeapon[][] initWeapons() {
        return new VehicleWeapon[][]{
                new VehicleWeapon[]{
                        new SmallCannonShellWeapon().damage(VehicleConfig.A_10_CANNON_DAMAGE.get()).explosionDamage(VehicleConfig.A_10_CANNON_EXPLOSION_DAMAGE.get()).explosionRadius(VehicleConfig.A_10_CANNON_EXPLOSION_RADIUS.get().floatValue()).sound(ModSounds.INTO_CANNON.get()).icon(Mod.loc("textures/screens/vehicle_weapon/cannon_30mm.png")),
                        new Agm65Weapon().sound(ModSounds.INTO_MISSILE.get()),
                        new AirToAirMissileWeapon().sound(ModSounds.INTO_MISSILE.get()).icon(VVP.loc("textures/screens/vehicle_weapon/air_to_air_missile.png"))
                }
        };
    }

    @Override
    public ThirdPersonCameraPosition getThirdPersonCameraPosition(int index) {
        return new ThirdPersonCameraPosition(17, 3, 0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(LOADED_ROCKET, 0);
        this.entityData.define(LOADED_BOMB, 0);
        this.entityData.define(LOADED_MISSILE, 0);
        this.entityData.define(LOADED_AA_MISSILE, 0);
        this.entityData.define(FIRE_TIME, 0);
        this.entityData.define(TARGET_UUID, "none");
        this.entityData.define(PROPELLER_ROT, 0.0f);
        this.entityData.define(IS_ENGINE_RUNNING, false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("LoadedRocket", this.entityData.get(LOADED_ROCKET));
        compound.putInt("LoadedBomb", this.entityData.get(LOADED_BOMB));
        compound.putInt("LoadedMissile", this.entityData.get(LOADED_MISSILE));
        compound.putInt("LoadedAAMissile", this.entityData.get(LOADED_AA_MISSILE));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(LOADED_ROCKET, compound.getInt("LoadedRocket"));
        this.entityData.set(LOADED_BOMB, compound.getInt("LoadedBomb"));
        this.entityData.set(LOADED_MISSILE, compound.getInt("LoadedMissile"));
        this.entityData.set(LOADED_AA_MISSILE, compound.getInt("LoadedAAMissile"));
    }

    @Override
    @ParametersAreNonnullByDefault
    protected void playStepSound(BlockPos pPos, BlockState pState) {
        this.playSound(ModSounds.WHEEL_STEP.get(), (float) (getDeltaMovement().length() * 0.2), random.nextFloat() * 0.1f + 1f);
    }

    @Override
    public boolean sendFireStarParticleOnHurt() {
        return false;
    }

    @Override
    public DamageModifier getDamageModifier() {
        return super.getDamageModifier()
                .custom((source, damage) -> getSourceAngle(source, 0.25f) * damage * (getHealth() > 0.1f ? 0.4f : 0.05f));
    }

    @Override
    public @NotNull InteractionResult interact(Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() == tech.vvp.vvp.init.ModItems.AIR_TO_AIR_MISSILE_ITEM.get() && this.entityData.get(LOADED_AA_MISSILE) < 4) {
            this.entityData.set(LOADED_AA_MISSILE, this.entityData.get(LOADED_AA_MISSILE) + 1);
            if (!player.isCreative()) {
                stack.shrink(1);
            }
            this.level().playSound(null, this, ModSounds.BOMB_RELOAD.get(), this.getSoundSource(), 2, 1);
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }
        if (stack.getItem() == ModItems.AGM.get() && this.entityData.get(LOADED_MISSILE) < 4) {
            this.entityData.set(LOADED_MISSILE, this.entityData.get(LOADED_MISSILE) + 1);
            if (!player.isCreative()) {
                stack.shrink(1);
            }
            this.level().playSound(null, this, ModSounds.BOMB_RELOAD.get(), this.getSoundSource(), 2, 1);
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }
        return super.interact(player, hand);
    }

    @Override
    public void baseTick() {
        if (!this.wasFiring && this.isFiring() && this.level().isClientSide()) {
            fireSound.accept(this);
        }
        this.wasFiring = this.isFiring();

        this.lockingTargetO = getTargetUuid();

        super.baseTick();
        float f = (float) Mth.clamp(Math.max((onGround() ? 0.819f : 0.82f) - 0.0035 * getDeltaMovement().length(), 0.5) + 0.001f * Mth.abs(90 - (float) calculateAngle(this.getDeltaMovement(), this.getViewVector(1))) / 90, 0.01, 0.99);

        if (this.tickCount % 20 == 0) {
            handleRadar();
        }

        boolean forward = getDeltaMovement().dot(getViewVector(1)) > 0;
        this.setDeltaMovement(this.getDeltaMovement().add(this.getViewVector(1).scale((forward ? 0.227 : 0.1) * getDeltaMovement().dot(getViewVector(1)))));
        this.setDeltaMovement(this.getDeltaMovement().multiply(f, f, f));

        if (this.isInWater() && this.tickCount % 4 == 0) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, 0.6, 0.6));
            if (lastTickSpeed > 0.4) {
                this.hurt(ModDamageTypes.causeVehicleStrikeDamage(this.level().registryAccess(), this, this.getFirstPassenger() == null ? this : this.getFirstPassenger()), (float) (20 * ((lastTickSpeed - 0.4) * (lastTickSpeed - 0.4))));
            }
        }

        if (this.level() instanceof ServerLevel) {
            this.handleAmmo();
        }

        if (this.getFirstPassenger() instanceof Player player && fireInputDown) {
            if (this.getWeaponIndex(0) == 0) {
                if ((this.entityData.get(AMMO) > 0 || InventoryTool.hasCreativeAmmoBox(player)) && !cannotFire) {
                    vehicleShoot(player, 0);
                }
            } else if (this.getWeaponIndex(0) == 1) {
                if (this.entityData.get(LOADED_MISSILE) > 0 && this.entityData.get(FIRE_TIME) == 0) {
                    vehicleShoot(player, 0);
                }
            } else if (this.getWeaponIndex(0) == 2) {
                if (this.entityData.get(LOADED_AA_MISSILE) > 0 && this.entityData.get(FIRE_TIME) == 0) {
                    vehicleShoot(player, 0);
                }
            }
        }
        if (onGround()) {
            terrainCompactA10();
        }

        if (entityData.get(FIRE_TIME) > 0) {
            entityData.set(FIRE_TIME, entityData.get(FIRE_TIME) - 1);
        }

        if (this.getWeaponIndex(0) == 1 || this.getWeaponIndex(0) == 2) {
            seekTarget();
        } else {
            if(this.getFirstPassenger() instanceof Player player) {
                resetSeek(player);
            }
        }

        lowHealthWarning();
        releaseDecoy();
        this.refreshDimensions();
    }

    @Override
    public void lowHealthWarning() {
        Matrix4f transform = this.getVehicleTransform(1);
        if (this.getHealth() <= 0.4 * this.getMaxHealth()) {
            List<Entity> entities = getPlayer(level());
            for (var e : entities) {
                if (e instanceof ServerPlayer player) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        Vector4f position = transformPosition(transform, -1.603125f, 0.875f, -5.0625f);
                        sendParticle(serverLevel, ParticleTypes.LARGE_SMOKE, position.x, position.y, position.z, 5, 0.25, 0.25, 0.25, 0, true);
                    }
                }
            }
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            if (this.getHealth() <= 0.25 * this.getMaxHealth()) {
                playLowHealthParticle(serverLevel);
            }
            if (this.getHealth() <= 0.15 * this.getMaxHealth()) {
                playLowHealthParticle(serverLevel);
            }
        }

        if (this.getHealth() <= 0.1 * this.getMaxHealth()) {
            List<Entity> entities = getPlayer(level());
            for (var e : entities) {
                if (e instanceof ServerPlayer player) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        Vector4f position = transformPosition(transform, -1.603125f, 0.875f, -5.0625f);
                        sendParticle(serverLevel, ParticleTypes.LARGE_SMOKE, position.x, position.y, position.z, 5, 0.25, 0.25, 0.25, 0, true);
                        sendParticle(serverLevel, ParticleTypes.CAMPFIRE_COSY_SMOKE, position.x, position.y, position.z, 5, 0.25, 0.25, 0.25, 0, true);
                        sendParticle(serverLevel, ParticleTypes.FLAME, position.x, position.y, position.z, 5, 0.25, 0.25, 0.25, 0, true);
                        sendParticle(serverLevel, ModParticleTypes.FIRE_STAR.get(), position.x, position.y, position.z, 5, 0.25, 0.25, 0.25, 0, true);
                        Vector4f position2 = transformPosition(transform, 1.603125f, 0.875f, -5.0625f);
                        sendParticle(serverLevel, ParticleTypes.LARGE_SMOKE, position2.x, position2.y, position2.z, 5, 0.25, 0.25, 0.25, 0, true);
                        sendParticle(serverLevel, ParticleTypes.CAMPFIRE_COSY_SMOKE, position2.x, position2.y, position2.z, 5, 0.25, 0.25, 0.25, 0, true);
                        sendParticle(serverLevel, ParticleTypes.FLAME, position2.x, position2.y, position2.z, 5, 0.25, 0.25, 0.25, 0, true);
                        sendParticle(serverLevel, ModParticleTypes.FIRE_STAR.get(), position2.x, position2.y, position2.z, 5, 0.25, 0.25, 0.25, 0, true);
                    }
                }
            }
            if (this.level() instanceof ServerLevel serverLevel) {
                sendParticle(serverLevel, ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 0.7f * getBbHeight(), this.getZ(), 2, 0.35 * this.getBbWidth(), 0.15 * this.getBbHeight(), 0.35 * this.getBbWidth(), 0.01, true);
                sendParticle(serverLevel, ParticleTypes.CAMPFIRE_COSY_SMOKE, this.getX(), this.getY() + 0.7f * getBbHeight(), this.getZ(), 2, 0.35 * this.getBbWidth(), 0.15 * this.getBbHeight(), 0.35 * this.getBbWidth(), 0.01, true);
                sendParticle(serverLevel, ParticleTypes.FLAME, this.getX(), this.getY() + 0.85f * getBbHeight(), this.getZ(), 4, 0.35 * this.getBbWidth(), 0.12 * this.getBbHeight(), 0.35 * this.getBbWidth(), 0.05, true);
                sendParticle(serverLevel, ModParticleTypes.FIRE_STAR.get(), this.getX(), this.getY() + 0.85f * getBbHeight(), this.getZ(), 4, 0.1 * this.getBbWidth(), 0.05 * this.getBbHeight(), 0.1 * this.getBbWidth(), 0.4, true);
            }
            if (this.tickCount % 15 == 0) {
                this.level().playSound(null, this.getOnPos(), SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS, 1, 1);
            }
        }

        if (this.getHealth() < 0.1f * this.getMaxHealth() && tickCount % 13 == 0) {
            this.level().playSound(null, this.getOnPos(), ModSounds.NO_HEALTH.get(), SoundSource.PLAYERS, 1, 1);
        } else if (this.getHealth() >= 0.1f && this.getHealth() < 0.4f * this.getMaxHealth() && tickCount % 10 == 0) {
            this.level().playSound(null, this.getOnPos(), ModSounds.LOW_HEALTH.get(), SoundSource.PLAYERS, 1, 1);
        }
    }

    public void terrainCompactA10() {
        if (onGround()) {
            Matrix4f transform = this.getWheelsTransform(1);
            Vector4f positionF = transformPosition(transform, 0.141675f, 0, 4.6315125f);
            Vector4f positionLB = transformPosition(transform, 2.5752f, 0, -0.7516125f);
            Vector4f positionRB = transformPosition(transform, -2.5752f, 0, -0.7516125f);
            Vec3 p1 = new Vec3(positionF.x, positionF.y, positionF.z);
            Vec3 p2 = new Vec3(positionLB.x, positionLB.y, positionLB.z);
            Vec3 p3 = new Vec3(positionRB.x, positionRB.y, positionRB.z);
            float p1y = (float) this.traceBlockY(p1, 3);
            float p2y = (float) this.traceBlockY(p2, 3);
            float p3y = (float) this.traceBlockY(p3, 3);
            p1 = new Vec3(positionF.x, p1y, positionF.z);
            p2 = new Vec3(positionLB.x, p2y, positionLB.z);
            p3 = new Vec3(positionRB.x, p3y, positionRB.z);
            Vec3 p4 = p2.add(p3).scale(0.5);
            Vec3 v1 = p2.vectorTo(p3);
            Vec3 v2 = p4.vectorTo(p1);
            double x = getXRotFromVector(v2);
            double z = getXRotFromVector(v1);
            float diffX = Math.clamp(-5f, 5f, Mth.wrapDegrees((float) (-2 * x) - getXRot()));
            setXRot(Mth.clamp(getXRot() + 0.05f * diffX, -45f, 45f));
            float diffZ = Math.clamp(-5f, 5f, Mth.wrapDegrees((float) (-2 * z) - getRoll()));
            setZRot(Mth.clamp(getRoll() + 0.05f * diffZ, -45f, 45f));
        } else if (isInWater()) {
            setXRot(getXRot() * 0.9f);
            setZRot(getRoll() * 0.9f);
        }
    }

    private void handleAmmo() {
        boolean hasCreativeAmmoBox = this.getFirstPassenger() instanceof Player player && InventoryTool.hasCreativeAmmoBox(player);
        int ammoCount = countItem(ModItems.SMALL_SHELL.get());

        if ((hasItem(ModItems.AGM.get()) || hasCreativeAmmoBox) && reloadCoolDownMissile == 0 && this.getEntityData().get(LOADED_MISSILE) < 4) {
            this.entityData.set(LOADED_MISSILE, this.getEntityData().get(LOADED_MISSILE) + 1);
            reloadCoolDownMissile = 60;
            if (!hasCreativeAmmoBox) {
                this.getItemStacks().stream().filter(stack -> stack.is(ModItems.AGM.get())).findFirst().ifPresent(stack -> stack.shrink(1));
            }
            this.level().playSound(null, this, ModSounds.BOMB_RELOAD.get(), this.getSoundSource(), 2, 1);
        }

        if (this.getWeaponIndex(0) == 0) {
            this.entityData.set(AMMO, ammoCount);
        } else if (this.getWeaponIndex(0) == 1) {
            this.entityData.set(AMMO, this.entityData.get(LOADED_MISSILE));
        } else if (this.getWeaponIndex(0) == 2) {
            this.entityData.set(AMMO, this.entityData.get(LOADED_AA_MISSILE));
        }
    }

    public void seekTarget() {
        // Если в самолете нет игрока, ничего не делаем, сбрасываем захват.
        if (!(this.getFirstPassenger() instanceof Player player)) {
            resetSeek(null); // ИСПРАВЛЕНО: передаем null, если игрока нет
            return;
        }

        // --- ЛОГИКА ДЛЯ РАКЕТЫ AGM (Воздух-Земля, индекс 1) ---
        if (getWeaponIndex(0) == 1) {
            if (getTargetUuid().equals(lockingTargetO) && !getTargetUuid().equals("none")) {
                lockTime++;
            } else {
                resetSeek(player);
            }

            Entity entity = SeekTool.seekCustomSizeEntity(this, this.level(), 384, 18, 0.9, true);
            if (entity != null) {
                if (lockTime == 0) {
                    setTargetUuid(String.valueOf(entity.getUUID()));
                }
                if (!String.valueOf(entity.getUUID()).equals(getTargetUuid())) {
                    resetSeek(player);
                    setTargetUuid(String.valueOf(entity.getUUID()));
                }
            } else {
                setTargetUuid("none");
            }

            if (lockTime == 1) {
                if (player instanceof ServerPlayer serverPlayer) {
                    SoundTool.playLocalSound(serverPlayer, ModSounds.JET_LOCK.get(), 2, 1);
                }
            }
            if (lockTime > 10) {
                if (!locked && player instanceof ServerPlayer serverPlayer) {
                    SoundTool.playLocalSound(serverPlayer, ModSounds.JET_LOCKON.get(), 2, 1);
                }
                locked = true;
            }
        }
        // --- НОВАЯ ПРОСТАЯ ЛОГИКА ДЛЯ РАКЕТЫ В-В (Воздух-Воздух, индекс 2) ---
        else if (getWeaponIndex(0) == 2) {
            if (getTargetUuid().equals(lockingTargetO) && !getTargetUuid().equals("none")) {
                lockTime++;
            } else {
                resetSeek(player);
            }

            final double range = 1024.0;
            Entity bestTarget = null;
            double minScore = Double.MAX_VALUE;

            List<Entity> potentialTargets = this.level().getEntities(this, this.getBoundingBox().inflate(range),
                e -> (e instanceof AirEntity || e instanceof HelicopterEntity) && e != this);

            for (Entity target : potentialTargets) {
                Vec3 toTarget = target.getEyePosition().subtract(this.getEyePosition());
                double dot = toTarget.normalize().dot(this.getViewVector(1.0f));

                if (dot > 0.9) {
                    double score = (1.0 - dot) * toTarget.lengthSqr();
                    if (score < minScore) {
                        minScore = score;
                        bestTarget = target;
                    }
                }
            }

            if (bestTarget != null) {
                if (lockTime == 0) {
                    setTargetUuid(String.valueOf(bestTarget.getUUID()));
                }
                if (!String.valueOf(bestTarget.getUUID()).equals(getTargetUuid())) {
                    resetSeek(player);
                    setTargetUuid(String.valueOf(bestTarget.getUUID()));
                }
            } else {
                setTargetUuid("none");
            }

            if (lockTime == 1) {
                if (player instanceof ServerPlayer serverPlayer) {
                    SoundTool.playLocalSound(serverPlayer, ModSounds.JET_LOCK.get(), 2, 1);
                }
            }
            if (lockTime > 30) {
                if (!locked && player instanceof ServerPlayer serverPlayer) {
                    SoundTool.playLocalSound(serverPlayer, ModSounds.JET_LOCKON.get(), 2, 1);
                }
                locked = true;
            }
        }
    }

    public void resetSeek(Player player) {
        lockTime = 0;
        locked = false;
        // Добавил сброс UUID цели, этого не было в твоем коде, но это важно
        setTargetUuid("none");
        if (player instanceof ServerPlayer serverPlayer) {
            var clientboundstopsoundpacket = new ClientboundStopSoundPacket(new ResourceLocation(Mod.MODID, "jet_lock"), SoundSource.PLAYERS);
            serverPlayer.connection.send(clientboundstopsoundpacket);
        }
    }

    public void setTargetUuid(String uuid) {
        this.lockingTarget = uuid;
    }

    public String getTargetUuid() {
        return this.lockingTarget;
    }

    @Override
    public void travel() {
        Entity passenger = this.getFirstPassenger();

        if (getHealth() > 0.1f * getMaxHealth()) {
            if (passenger == null || isInWater()) {
                this.leftInputDown = false;
                this.rightInputDown = false;
                this.forwardInputDown = false;
                this.backInputDown = false;
                this.entityData.set(POWER, this.entityData.get(POWER) * 0.95f);
                if (onGround()) {
                    this.setDeltaMovement(this.getDeltaMovement().multiply(0.94, 1, 0.94));
                } else {
                    this.setXRot(Mth.clamp(this.getXRot() + 0.1f, -89, 89));
                }
            } else if (passenger instanceof Player) {
                if (getEnergy() > 0) {
                    if (forwardInputDown) {
                        this.entityData.set(POWER, Math.min(this.entityData.get(POWER) + 0.004f, sprintInputDown ? 1f : 0.0575f));
                    }

                    if (backInputDown) {
                        this.entityData.set(POWER, Math.max(this.entityData.get(POWER) - 0.002f, -0.2f));
                    }
                }

                if (!onGround()) {
                    if (rightInputDown) {
                        this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) - 1.2f);
                    } else if (this.leftInputDown) {
                        this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) + 1.2f);
                    }
                }

                if (downInputDown) {
                    if (onGround()) {
                        this.entityData.set(POWER, this.entityData.get(POWER) * 0.8f);
                        this.setDeltaMovement(this.getDeltaMovement().multiply(0.97, 1, 0.97));
                    } else {
                        this.entityData.set(POWER, this.entityData.get(POWER) * 0.97f);
                        this.setDeltaMovement(this.getDeltaMovement().multiply(0.994, 1, 0.994));
                    }
                    this.entityData.set(PLANE_BREAK, Math.min(this.entityData.get(PLANE_BREAK) + 10, 60f));
                }
            }

            if (getEnergy() > 0 && !this.level().isClientSide) {
                this.consumeEnergy((int) (Mth.abs(this.entityData.get(POWER)) * VehicleConfig.A_10_MAX_ENERGY_COST.get()));
            }

            float rotSpeed = 1.5f + 2 * Mth.abs(VectorTool.calculateY(getRoll()));

            float addY = Mth.clamp(Math.max((this.onGround() ? 0.1f : 0.2f) * (float) getDeltaMovement().length(), 0f) * entityData.get(MOUSE_SPEED_X), -rotSpeed, rotSpeed);
            float addX = Mth.clamp(Math.min((float) Math.max(getDeltaMovement().dot(getViewVector(1)) - 0.24, 0.03), 0.4f) * entityData.get(MOUSE_SPEED_Y), -3.5f, 3.5f);
            float addZ = this.entityData.get(DELTA_ROT) - (this.onGround() ? 0 : 0.004f) * entityData.get(MOUSE_SPEED_X) * (float) getDeltaMovement().dot(getViewVector(1));

            float i = getXRot() / 80;

            delta_x = addX;
            delta_y = addY - VectorTool.calculateY(getXRot()) * addZ;

            this.setYRot(this.getYRot() + delta_y);
            if (!onGround()) {
                this.setXRot(this.getXRot() + delta_x);
                this.setZRot(this.getRoll() - addZ * (1 - Mth.abs(i)));
            }

            if (!onGround()) {
                float speed = Mth.clamp(Mth.abs(roll) / 90, 0, 1);
                if (this.roll > 0) {
                    setZRot(roll - Math.min(speed, roll));
                } else if (this.roll < 0){
                    setZRot(roll + Math.min(speed, -roll));
                }
            }

            this.setPropellerRot(this.getPropellerRot() + 30 * this.entityData.get(POWER));

            if (upInputDown) {
                upInputDown = false;
                if (!onGround()) {
                    if (entityData.get(GEAR_UP)) {
                        entityData.set(GEAR_UP, false);
                    } else if (entityData.get(GEAR_ROT) == 0) {
                        entityData.set(GEAR_UP, true);
                    }
                }
            }

            if (onGround()) {
                entityData.set(GEAR_UP, false);
            }

            if (entityData.get(GEAR_UP)) {
                entityData.set(GEAR_ROT, Math.min(entityData.get(GEAR_ROT) + 5, 85));
            } else {
                entityData.set(GEAR_ROT, Math.max(entityData.get(GEAR_ROT) - 5, 0));
            }

            float flapX = (1 - (Mth.abs(getRoll())) / 90) * Mth.clamp(entityData.get(MOUSE_SPEED_Y), -22.5f, 22.5f) - VectorTool.calculateY(getRoll()) * Mth.clamp(entityData.get(MOUSE_SPEED_X), -22.5f, 22.5f);

            setFlap1LRot(Mth.clamp(-flapX - 4 * addZ - this.entityData.get(PLANE_BREAK), -22.5f, 22.5f));
            setFlap1RRot(Mth.clamp(-flapX + 4 * addZ - this.entityData.get(PLANE_BREAK), -22.5f, 22.5f));
            setFlap1L2Rot(Mth.clamp(-flapX - 4 * addZ + this.entityData.get(PLANE_BREAK), -22.5f, 22.5f));
            setFlap1R2Rot(Mth.clamp(-flapX + 4 * addZ + this.entityData.get(PLANE_BREAK), -22.5f, 22.5f));

            setFlap2LRot(Mth.clamp(flapX - 4 * addZ, -22.5f, 22.5f));
            setFlap2RRot(Mth.clamp(flapX + 4 * addZ, -22.5f, 22.5f));

            float flapY = (1 - (Mth.abs(getRoll())) / 90) * Mth.clamp(entityData.get(MOUSE_SPEED_X), -22.5f, 22.5f) + VectorTool.calculateY(getRoll()) * Mth.clamp(entityData.get(MOUSE_SPEED_Y), -22.5f, 22.5f);

            setFlap3Rot(flapY * 5);

        } else if (!onGround()) {
            float diffX;
            this.entityData.set(POWER, Math.max(this.entityData.get(POWER) - 0.0003f, 0.02f));
            destroyRot += 0.1f;
            diffX = 90 - this.getXRot();
            this.setXRot(this.getXRot() + diffX * 0.001f * destroyRot);
            this.setZRot(this.getRoll() - destroyRot);
            setDeltaMovement(getDeltaMovement().add(0, -0.03, 0));
            setDeltaMovement(getDeltaMovement().add(0, -destroyRot * 0.005, 0));
        }

        this.entityData.set(POWER, this.entityData.get(POWER) * 0.99f);
        this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) * 0.85f);
        this.entityData.set(PLANE_BREAK, this.entityData.get(PLANE_BREAK) * 0.8f);

        Matrix4f transform = getVehicleTransform(1);
        double flapAngle = (getFlap1LRot() + getFlap1RRot() + getFlap1L2Rot() + getFlap1R2Rot()) / 4;

        Vector4f force0 = transformPosition(transform, 0, 0, 0);
        Vector4f force1 = transformPosition(transform, 0, 1, 0);

        Vec3 force = new Vec3(force0.x, force0.y, force0.z).vectorTo(new Vec3(force1.x, force1.y, force1.z));

        setDeltaMovement(getDeltaMovement().add(force.scale(getDeltaMovement().dot(getViewVector(1)) * 0.022 * (1 + Math.sin((onGround() ? 25 : flapAngle + 25) * Mth.DEG_TO_RAD)))));

        this.setDeltaMovement(this.getDeltaMovement().add(getViewVector(1).scale(0.2 * this.entityData.get(POWER))));
    }

    @Override
    public void move(@NotNull MoverType movementType, @NotNull Vec3 movement) {
        if (!this.level().isClientSide()) {
            MobileVehicleEntity.IGNORE_ENTITY_GROUND_CHECK_STEPPING = true;
        }
        if (level() instanceof ServerLevel && canCollideBlockBeastly()) {
            collideBlockBeastly();
        }

        super.move(movementType, movement);
        if (level() instanceof ServerLevel) {
            if (this.horizontalCollision) {
                collideBlock();
                if (canCollideHardBlock()) {
                    collideHardBlock();
                }
            }

            if (lastTickSpeed < 0.3 || collisionCoolDown > 0) return;
            Entity driver = EntityFindUtil.findEntity(this.level(), this.entityData.get(LAST_DRIVER_UUID));

            if ((verticalCollision)) {
                if (entityData.get(GEAR_ROT) > 10 || (Mth.abs(getRoll()) > 20)) {
                    this.hurt(ModDamageTypes.causeVehicleStrikeDamage(this.level().registryAccess(), this, driver == null ? this : driver), (float) ((8 + Mth.abs(getRoll() * 0.2f)) * (lastTickSpeed - 0.3) * (lastTickSpeed - 0.3)));
                    if (!this.level().isClientSide) {
                        this.level().playSound(null, this, ModSounds.VEHICLE_STRIKE.get(), this.getSoundSource(), 1, 1);
                    }
                    this.bounceVertical(Direction.getNearest(this.getDeltaMovement().x(), this.getDeltaMovement().y(), this.getDeltaMovement().z()).getOpposite());
                } else {
                    if (Mth.abs((float) lastTickVerticalSpeed) > 0.4) {
                        this.hurt(ModDamageTypes.causeVehicleStrikeDamage(this.level().registryAccess(), this, driver == null ? this : driver), (float) (96 * ((Mth.abs((float) lastTickVerticalSpeed) - 0.4) * (lastTickSpeed - 0.3) * (lastTickSpeed - 0.3))));
                        if (!this.level().isClientSide) {
                            this.level().playSound(null, this, ModSounds.VEHICLE_STRIKE.get(), this.getSoundSource(), 1, 1);
                        }
                        this.bounceVertical(Direction.getNearest(this.getDeltaMovement().x(), this.getDeltaMovement().y(), this.getDeltaMovement().z()).getOpposite());
                    }
                }

            }

            if (this.horizontalCollision) {
                this.hurt(ModDamageTypes.causeVehicleStrikeDamage(this.level().registryAccess(), this, driver == null ? this : driver), (float) (126 * ((lastTickSpeed - 0.4) * (lastTickSpeed - 0.4))));
                this.bounceHorizontal(Direction.getNearest(this.getDeltaMovement().x(), this.getDeltaMovement().y(), this.getDeltaMovement().z()).getOpposite());
                if (!this.level().isClientSide) {
                    this.level().playSound(null, this, ModSounds.VEHICLE_STRIKE.get(), this.getSoundSource(), 1, 1);
                }
                collisionCoolDown = 4;
                crash = true;
            }
        }
    }
    @Override
    public SoundEvent getEngineSound() {
        return tech.vvp.vvp.init.ModSounds.F35_ENGINE.get();
    }

    @Override
    public float getEngineSoundVolume() {
        return entityData.get(POWER) * (sprintInputDown ? 5.5f : 3f);
    }

    @Override
    public void positionRider(@NotNull Entity passenger, @NotNull MoveFunction callback) {
        if (!this.hasPassenger(passenger)) {
            return;
        }

        Matrix4f transform = getVehicleTransform(1);

        float x = 0f;
        float y = -0.7f;
        float z = 0.5f;
        
        y += (float) passenger.getMyRidingOffset();

        Vector4f worldPosition = transformPosition(transform, x, y, z);
        passenger.setPos(worldPosition.x, worldPosition.y, worldPosition.z);
        callback.accept(passenger, worldPosition.x, worldPosition.y, worldPosition.z);

        copyEntityData(passenger);
    }

    public Vec3 driverPos(float ticks) {
        Matrix4f transform = getVehicleTransform(ticks);
        Vector4f worldPosition = transformPosition(transform, 0, 1.35f, 4f);
        return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
    }

    @Override
    public Vec3 driverZoomPos(float ticks) {
        Matrix4f transform = getVehicleTransform(ticks);
        Vector4f worldPosition = transformPosition(transform, 0, 1.35f, 4.15f);
        return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
    }

    public void copyEntityData(Entity entity) {
        entity.setYHeadRot(entity.getYHeadRot() + delta_y);
        entity.setYRot(entity.getYRot() + delta_y);
        entity.setYBodyRot(this.getYRot());
    }

    @Override
    public Matrix4f getVehicleTransform(float ticks) {
        Matrix4f transform = new Matrix4f();
        transform.translate((float) Mth.lerp(ticks, xo, getX()), (float) Mth.lerp(ticks, yo + 2.375f, getY() + 2.375f), (float) Mth.lerp(ticks, zo, getZ()));
        transform.rotate(Axis.YP.rotationDegrees(-Mth.lerp(ticks, yRotO, getYRot())));
        transform.rotate(Axis.XP.rotationDegrees(Mth.lerp(ticks, xRotO, getXRot())));
        transform.rotate(Axis.ZP.rotationDegrees(Mth.lerp(ticks, prevRoll, getRoll())));
        return transform;
    }

    @Override
    public void destroy() {
        if (this.crash) {
            crashPassengers();
        } else {
            explodePassengers();
        }

        if (level() instanceof ServerLevel) {
            CustomExplosion explosion = new CustomExplosion(this.level(), this,
                    ModDamageTypes.causeCustomExplosionDamage(this.level().registryAccess(), this, getAttacker()), 300.0f,
                    this.getX(), this.getY(), this.getZ(), 8f, ExplosionConfig.EXPLOSION_DESTROY.get() ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.KEEP, true).setDamageMultiplier(1);
            explosion.explode();
            ForgeEventFactory.onExplosionStart(this.level(), explosion);
            explosion.finalizeExplosion(false);
            ParticleTool.spawnHugeExplosionParticles(this.level(), this.position());
        }
        super.destroy();
    }
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public ResourceLocation getVehicleIcon() {
        return VVP.loc("textures/vehicle_icon/f35_icon.png");
    }

    @Override
    public Vec3 shootPos(float tickDelta) {
        Matrix4f transform = getVehicleTransform(tickDelta);
        Vector4f worldPosition;
        if (getWeaponIndex(0) == 0) {
            worldPosition = transformPosition(transform, 0.1321625f, -0.56446875f, 7.85210625f);
        } else if (getWeaponIndex(0) == 1) {
            worldPosition = transformPosition(transform, 0f, -1.443f, 0.13f);
        } else {
            worldPosition = transformPosition(transform, 0f, -1.203125f, 0.0625f);
        }
        return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
    }

    @Override
    public Vec3 shootVec(float tickDelta) {
        Matrix4f transform = getVehicleTransform(tickDelta);
        Vector4f worldPosition;
        Vector4f worldPosition2;
        if (getWeaponIndex(0) == 3) {
            worldPosition = transformPosition(transform, 0, 0, 0);
            worldPosition2 = transformPosition(transform, 0, 0f, 1);
        } else {
            worldPosition = transformPosition(transform, 0, 0, 0);
            worldPosition2 = transformPosition(transform, 0, -0.03f, 1);
        }
        return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z).vectorTo(new Vec3(worldPosition2.x, worldPosition2.y, worldPosition2.z)).normalize();
    }

    @Override
    public Float gearRot(float tickDelta) {
        return Mth.lerp(tickDelta, gearRotO, entityData.get(GEAR_ROT));
    }

    @Override
    public void vehicleShoot(Player player, int type) {
        Matrix4f transform = getVehicleTransform(1);

        if (getWeaponIndex(0) == 0) {
            if (this.cannotFire) return;
            boolean hasCreativeAmmo = getFirstPassenger() instanceof Player pPlayer && InventoryTool.hasCreativeAmmoBox(pPlayer);
            Vector4f worldPosition = transformPosition(transform, 0.1321625f, -0.56446875f, 7.85210625f);
            Vector4f worldPosition2 = transformPosition(transform, 0.1321625f + 0.01f, -0.56446875f - 0.03f, 8.85210625f);
            Vec3 shootVec = new Vec3(worldPosition.x, worldPosition.y, worldPosition.z).vectorTo(new Vec3(worldPosition2.x, worldPosition2.y, worldPosition2.z)).normalize();

            if (this.entityData.get(AMMO) > 0 || hasCreativeAmmo) {
                entityData.set(FIRE_TIME, Math.min(entityData.get(FIRE_TIME) + 6, 6));
                var entityToSpawn = ((SmallCannonShellWeapon) getWeapon(0)).create(player);
                entityToSpawn.setPos(worldPosition.x, worldPosition.y, worldPosition.z);
                entityToSpawn.shoot(shootVec.x, shootVec.y, shootVec.z, 30, 0.5f);
                level().addFreshEntity(entityToSpawn);
                sendParticle((ServerLevel) this.level(), ParticleTypes.LARGE_SMOKE, worldPosition.x, worldPosition.y, worldPosition.z, 1, 0.2, 0.2, 0.2, 0.001, true);
                sendParticle((ServerLevel) this.level(), ParticleTypes.CLOUD, worldPosition.x, worldPosition.y, worldPosition.z, 2, 0.5, 0.5, 0.5, 0.005, true);
                if (!hasCreativeAmmo) {
                    this.getItemStacks().stream().filter(stack -> stack.is(ModItems.SMALL_SHELL.get())).findFirst().ifPresent(stack -> stack.shrink(1));
                }
            }
            Level level = player.level();
            final Vec3 center = new Vec3(this.getX(), this.getEyeY(), this.getZ());
            for (Entity target : level.getEntitiesOfClass(Entity.class, new AABB(center, center).inflate(5), e -> true).stream().sorted(Comparator.comparingDouble(e -> e.distanceToSqr(center))).toList()) {
                if (target instanceof ServerPlayer serverPlayer) {
                    Mod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new ShakeClientMessage(6, 5, 12, this.getX(), this.getEyeY(), this.getZ()));
                }
            }
            this.entityData.set(HEAT, this.entityData.get(HEAT) + 2);
        } else if (getWeaponIndex(0) == 1 && this.getEntityData().get(LOADED_MISSILE) > 0) {
            var Agm65Entity = ((Agm65Weapon) getWeapon(0)).create(player);
            Vector4f worldPosition;
            if (this.getEntityData().get(LOADED_MISSILE) == 4) {
                worldPosition = transformPosition(transform, 6.63f, -1.55f, 1.83f);
            } else {
                worldPosition = transformPosition(transform, -6.63f, -1.55f, 1.83f);
            }
            if (locked) {
                Agm65Entity.setTargetUuid(getTargetUuid());
            }
            Agm65Entity.setPos(worldPosition.x, worldPosition.y, worldPosition.z);
            Agm65Entity.shoot(shootVec(1).x, shootVec(1).y, shootVec(1).z, (float) getDeltaMovement().length() + 1, 1);
            player.level().addFreshEntity(Agm65Entity);
            BlockPos pos = BlockPos.containing(new Vec3(worldPosition.x, worldPosition.y, worldPosition.z));
            this.level().playSound(null, pos, ModSounds.BOMB_RELEASE.get(), SoundSource.PLAYERS, 3, 1);
            if (this.getEntityData().get(LOADED_MISSILE) == 4) {
                reloadCoolDownMissile = 60;
            }
            this.entityData.set(LOADED_MISSILE, this.entityData.get(LOADED_MISSILE) - 1);
        } else if (getWeaponIndex(0) == 2 && this.getEntityData().get(LOADED_AA_MISSILE) > 0) {
            if (locked) {
                var missile = new AirToAirMissileEntity(player, player.level(), getTargetUuid());
                Vector4f worldPosition = transformPosition(transform, 0f, -1.2f, 2.5f);
                missile.setPos(worldPosition.x, worldPosition.y, worldPosition.z);
                missile.shoot(shootVec(1).x, shootVec(1).y, shootVec(1).z, (float) getDeltaMovement().length() + 1.5f, 0);
                player.level().addFreshEntity(missile);
                this.level().playSound(null, this.blockPosition(), ModSounds.BOMB_RELEASE.get(), SoundSource.PLAYERS, 3, 1);
                this.entityData.set(LOADED_AA_MISSILE, this.entityData.get(LOADED_AA_MISSILE) - 1);
            }
        }
    }

    public float shootingVolume() {
        return entityData.get(FIRE_TIME) * 0.3f;
    }

    public float shootingPitch() {
        return 0.7f + entityData.get(FIRE_TIME) * 0.05f;
    }

    @Override
    public int mainGunRpm(Player player) {
        if (getWeaponIndex(0) == 1) {
            return 120;
        }
        return 0;
    }

    public boolean isFiring() {
        return this.entityData.get(FIRE_TIME) > 0;
    }

    @Override
    public boolean canShoot(Player player) {
        if (getWeaponIndex(0) == 1) {
            return this.entityData.get(LOADED_ROCKET) > 0;
        }
        return false;
    }

    @Override
    public int getAmmoCount(Player player) {
        return this.entityData.get(LOADED_ROCKET);
    }

    @Override
    public boolean banHand(Player player) {
        return true;
    }

    @Override
    public boolean hidePassenger(Entity entity) {
        return false;
    }

    @Override
    public int zoomFov() {
        return 3;
    }

    @Override
    public int getWeaponHeat(Player player) {
        return entityData.get(HEAT);
    }

    @Override
    public float getRotX(float tickDelta) {
        return this.getPitch(tickDelta);
    }

    @Override
    public float getRotY(float tickDelta) {
        return this.getYaw(tickDelta);
    }

    @Override
    public float getRotZ(float tickDelta) {
        return this.getRoll(tickDelta);
    }

    @Override
    public float getPower() {
        return this.entityData.get(POWER);
    }

    @Override
    public int getDecoy() {
        return this.entityData.get(DECOY_COUNT);
    }

    @Override
    public double getSensitivity(double original, boolean zoom, int seatIndex, boolean isOnGround) {
        return 0;
    }

    @Override
    public double getMouseSensitivity() {
        return ClientEventHandler.zoomVehicle ? 0.03 : 0.07;
    }

    @Override
    public double getMouseSpeedX() {
        return 0.3;
    }

    @Override
    public double getMouseSpeedY() {
        return 0.3;
    }

    @Override
    public boolean isEnclosed(int index) {
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    public Pair<Quaternionf, Quaternionf> getPassengerRotation(Entity entity, float tickDelta) {
        return Pair.of(Axis.XP.rotationDegrees(-this.getViewXRot(tickDelta)), Axis.ZP.rotationDegrees(-this.getRoll(tickDelta)));
    }

    public Matrix4f getClientVehicleTransform(float ticks) {
        Matrix4f transform = new Matrix4f();
        transform.translate((float) Mth.lerp(ticks, xo, getX()), (float) Mth.lerp(ticks, yo + 2.375f, getY() + 2.375f), (float) Mth.lerp(ticks, zo, getZ()));
        transform.rotate(Axis.YP.rotationDegrees((float) (-Mth.lerp(ticks, yRotO, getYRot()) + freeCameraYaw)));
        transform.rotate(Axis.XP.rotationDegrees((float) (Mth.lerp(ticks, xRotO, getXRot()) + freeCameraPitch)));
        transform.rotate(Axis.ZP.rotationDegrees(Mth.lerp(ticks, prevRoll, getRoll())));
        return transform;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public @Nullable Vec2 getCameraRotation(float partialTicks, Player player, boolean zoom, boolean isFirstPerson) {
        if (this.getSeatIndex(player) == 0) {
            return new Vec2((float) (getRotY(partialTicks) - freeCameraYaw), (float) (getRotX(partialTicks) + freeCameraPitch));
        }
        return super.getCameraRotation(partialTicks, player, false, false);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public Vec3 getCameraPosition(float partialTicks, Player player, boolean zoom, boolean isFirstPerson) {
        if (this.getSeatIndex(player) == 0) {
            Matrix4f transform = getClientVehicleTransform(partialTicks);
            Vector4f maxCameraPosition = transformPosition(transform, 0, 4, -14 - (float) ClientMouseHandler.custom3pDistanceLerp);
            Vec3 finalPos = CameraTool.getMaxZoom(transform, maxCameraPosition);
            if (isFirstPerson) {
                return new Vec3(Mth.lerp(partialTicks, player.xo, player.getX()), Mth.lerp(partialTicks, player.yo + player.getEyeHeight(), player.getEyeY()), Mth.lerp(partialTicks, player.zo, player.getZ()));
            } else {
                return finalPos;
            }
        }
        return super.getCameraPosition(partialTicks, player, false, false);
    }

    @Override
    public @Nullable ResourceLocation getVehicleItemIcon() {
        return Mod.loc("textures/gui/vehicle/type/aircraft.png");
    }
}