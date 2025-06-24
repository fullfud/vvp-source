package tech.vvp.vvp.init;

import com.atsuishio.superbwarfare.entity.projectile.*;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import tech.vvp.vvp.VVP;
import tech.vvp.vvp.entity.vehicle.VazikEntity;
import tech.vvp.vvp.entity.vehicle.BikeredEntity;
import tech.vvp.vvp.entity.vehicle.BikegreenEntity;
import tech.vvp.vvp.entity.vehicle.Mi24Entity;
import tech.vvp.vvp.entity.vehicle.Mi24polEntity;
import tech.vvp.vvp.entity.vehicle.Mi24ukrEntity;
import tech.vvp.vvp.entity.vehicle.M997Entity;
import tech.vvp.vvp.entity.vehicle.CobraEntity;
import tech.vvp.vvp.entity.vehicle.CobrasharkEntity;
import tech.vvp.vvp.entity.vehicle.F35Entity;
import tech.vvp.vvp.entity.vehicle.Btr80aEntity;
import tech.vvp.vvp.entity.vehicle.M997_greenEntity;
import tech.vvp.vvp.entity.vehicle.Btr80a_1Entity;
import tech.vvp.vvp.entity.vehicle.StrykerEntity;
import tech.vvp.vvp.entity.vehicle.Stryker_1Entity;
import tech.vvp.vvp.entity.vehicle.Stryker_hakiEntity;
import tech.vvp.vvp.entity.vehicle.Stryker_1_hakiEntity;
import tech.vvp.vvp.entity.projectile.AirToAirMissileEntity;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, VVP.MOD_ID);
                    
    public static final RegistryObject<EntityType<VazikEntity>> VAZIK = ENTITY_TYPES.register("vazik",
                    () -> EntityType.Builder.<VazikEntity>of(VazikEntity::new, MobCategory.MISC)
                        .setTrackingRange(64)
                        .setUpdateInterval(1)
                        .setCustomClientFactory(VazikEntity::clientSpawn)
                        .fireImmune()
                        .sized(2.7f, 2.3f)
                        .build("vazik"));

    public static final RegistryObject<EntityType<M997Entity>> M997 = ENTITY_TYPES.register("m997",
                        () -> EntityType.Builder.<M997Entity>of(M997Entity::new, MobCategory.MISC)
                            .setTrackingRange(64)
                            .setUpdateInterval(1)
                            .setCustomClientFactory(M997Entity::clientSpawn)
                            .fireImmune()
                            .sized(4.2f, 3.2f)
                            .build("m997"));
                            
    public static final RegistryObject<EntityType<M997_greenEntity>> M997_GREEN = ENTITY_TYPES.register("m997_green",
                        () -> EntityType.Builder.<M997_greenEntity>of(M997_greenEntity::new, MobCategory.MISC)
                            .setTrackingRange(64)
                            .setUpdateInterval(1)
                            .setCustomClientFactory(M997_greenEntity::clientSpawn)
                            .fireImmune()
                            .sized(4.2f, 3.2f)
                            .build("m997_green"));

    public static final RegistryObject<EntityType<Btr80aEntity>> BTR80A = ENTITY_TYPES.register("btr_80a",
                            () -> EntityType.Builder.<Btr80aEntity>of(Btr80aEntity::new, MobCategory.MISC)
                                .setTrackingRange(64)
                                .setUpdateInterval(1)
                                .setCustomClientFactory(Btr80aEntity::clientSpawn)
                                .fireImmune()
                                .sized(3.9f, 3.2f)
                                .build("btr_80a"));

    public static final RegistryObject<EntityType<Btr80a_1Entity>> BTR_80A_1 = ENTITY_TYPES.register("btr_80a_1",
                            () -> EntityType.Builder.<Btr80a_1Entity>of(Btr80a_1Entity::new, MobCategory.MISC)
                                .setTrackingRange(64)
                                .setUpdateInterval(1)
                                .setCustomClientFactory(Btr80a_1Entity::clientSpawn)
                                .fireImmune()
                                .sized(3.9f, 3.2f)
                                .build("btr_80a_1"));

    public static final RegistryObject<EntityType<StrykerEntity>> STRYKER = ENTITY_TYPES.register("stryker",
                            () -> EntityType.Builder.<StrykerEntity>of(StrykerEntity::new, MobCategory.MISC)
                                .setTrackingRange(64)
                                .setUpdateInterval(1)
                                .setCustomClientFactory(StrykerEntity::clientSpawn)
                                .fireImmune()
                                .sized(3.9f, 3.5f)
                                .build("stryker"));
                        
    public static final RegistryObject<EntityType<Stryker_1Entity>> STRYKER_1 = ENTITY_TYPES.register("stryker_m1296",
                            () -> EntityType.Builder.<Stryker_1Entity>of(Stryker_1Entity::new, MobCategory.MISC)
                                .setTrackingRange(64)
                                .setUpdateInterval(1)
                                .setCustomClientFactory(Stryker_1Entity::clientSpawn)
                                .fireImmune()
                                .sized(3.9f, 3.5f)
                                .build("stryker_1"));

    public static final RegistryObject<EntityType<Stryker_1_hakiEntity>> STRYKER_1_HAKI = ENTITY_TYPES.register("stryker_m1296_haki",
                                () -> EntityType.Builder.<Stryker_1_hakiEntity>of(Stryker_1_hakiEntity::new, MobCategory.MISC)
                                    .setTrackingRange(64)
                                    .setUpdateInterval(1)
                                    .setCustomClientFactory(Stryker_1_hakiEntity::clientSpawn)
                                    .fireImmune()
                                    .sized(3.9f, 3.5f)
                                    .build("stryker_1"));

    public static final RegistryObject<EntityType<Stryker_hakiEntity>> STRYKER_HAKI = ENTITY_TYPES.register("stryker_haki",
                                () -> EntityType.Builder.<Stryker_hakiEntity>of(Stryker_hakiEntity::new, MobCategory.MISC)
                                    .setTrackingRange(64)
                                    .setUpdateInterval(1)
                                    .setCustomClientFactory(Stryker_hakiEntity::clientSpawn)
                                    .fireImmune()
                                    .sized(3.9f, 3.5f)
                                    .build("stryker_haki"));

    public static final RegistryObject<EntityType<AirToAirMissileEntity>> AIR_TO_AIR_MISSILE = ENTITY_TYPES.register("air_to_air_missile",
        () -> EntityType.Builder.<AirToAirMissileEntity>of(AirToAirMissileEntity::new, MobCategory.MISC)
                .sized(0.5f, 0.5f)
                .setTrackingRange(256)
                .setUpdateInterval(1)
                .setCustomClientFactory(AirToAirMissileEntity::new)
                .fireImmune()
                .build("air_to_air_missile"));

    public static final RegistryObject<EntityType<BikegreenEntity>> BIKEGREEN = ENTITY_TYPES.register("bikegreen",
                        () -> EntityType.Builder.<BikegreenEntity>of(BikegreenEntity::new, MobCategory.MISC)
                        .setTrackingRange(64)
                        .setUpdateInterval(1)
                        .setCustomClientFactory(BikegreenEntity::clientSpawn)
                        .fireImmune()
                        .sized(0.9f, 1.2f)
                        .build("bikegreen"));

    public static final RegistryObject<EntityType<BikeredEntity>> BIKERED = ENTITY_TYPES.register("bikered",
                            () -> EntityType.Builder.<BikeredEntity>of(BikeredEntity::new, MobCategory.MISC)
                        .setTrackingRange(64)
                        .setUpdateInterval(1)
                        .setCustomClientFactory(BikeredEntity::clientSpawn)
                        .fireImmune()
                        .sized(0.9f, 1.2f)
                        .build("bikered"));

    public static final RegistryObject<EntityType<SmallCannonShellEntity>> SMALL_CANNON_SHELL = ENTITY_TYPES.register("small_cannon_shell",
                    () -> EntityType.Builder.<SmallCannonShellEntity>of(SmallCannonShellEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .build("small_cannon_shell"));
        
    public static final RegistryObject<EntityType<CannonShellEntity>> CANNON_SHELL = ENTITY_TYPES.register("cannon_shell",
                    () -> EntityType.Builder.<CannonShellEntity>of(CannonShellEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .build("cannon_shell"));
        
    public static final RegistryObject<EntityType<ProjectileEntity>> PROJECTILE = ENTITY_TYPES.register("projectile",
                    () -> EntityType.Builder.<ProjectileEntity>of(ProjectileEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .build("projectile"));
        
    public static final RegistryObject<EntityType<HeliRocketEntity>> HELI_ROCKET = ENTITY_TYPES.register("heli_rocket",
                    () -> EntityType.Builder.<HeliRocketEntity>of(HeliRocketEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .build("heli_rocket"));
        
    public static final RegistryObject<EntityType<Mk82Entity>> MK82 = ENTITY_TYPES.register("mk82",
                    () -> EntityType.Builder.<Mk82Entity>of(Mk82Entity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .build("mk82"));
        
    public static final RegistryObject<EntityType<Agm65Entity>> AGM65 = ENTITY_TYPES.register("agm65",
                    () -> EntityType.Builder.<Agm65Entity>of(Agm65Entity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .build("agm65"));
        
    public static final RegistryObject<EntityType<SwarmDroneEntity>> SWARM_DRONE = ENTITY_TYPES.register("swarm_drone",
                    () -> EntityType.Builder.<SwarmDroneEntity>of(SwarmDroneEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .build("swarm_drone"));

    public static final RegistryObject<EntityType<WgMissileEntity>> WGL_MISSILE = ENTITY_TYPES.register("wg_missile",
                            () -> EntityType.Builder.<WgMissileEntity>of(WgMissileEntity::new, MobCategory.MISC)
                                    .sized(0.5f, 0.5f)
                                    .build("wg_missile"));

    public static final RegistryObject<EntityType<Mi24Entity>> MI24 = register("mi24",
            EntityType.Builder.<Mi24Entity>of(Mi24Entity::new, MobCategory.MISC).setTrackingRange(64).setUpdateInterval(1).setCustomClientFactory(Mi24Entity::new).fireImmune().sized(4.5f, 4.8f));

    public static final RegistryObject<EntityType<Mi24ukrEntity>> MI24UKR = register("mi24ukr",
            EntityType.Builder.<Mi24ukrEntity>of(Mi24ukrEntity::new, MobCategory.MISC).setTrackingRange(64).setUpdateInterval(1).setCustomClientFactory(Mi24ukrEntity::new).fireImmune().sized(4.5f, 4.8f));

    public static final RegistryObject<EntityType<Mi24polEntity>> MI24POL = register("mi24polsha",
            EntityType.Builder.<Mi24polEntity>of(Mi24polEntity::new, MobCategory.MISC).setTrackingRange(64).setUpdateInterval(1).setCustomClientFactory(Mi24polEntity::new).fireImmune().sized(4.5f, 4.8f));
    
    public static final RegistryObject<EntityType<CobraEntity>> COBRA = register("cobra",
            EntityType.Builder.<CobraEntity>of(CobraEntity::new, MobCategory.MISC).setTrackingRange(64).setUpdateInterval(1).setCustomClientFactory(CobraEntity::new).fireImmune().sized(4.5f, 4.8f));

    public static final RegistryObject<EntityType<CobrasharkEntity>> COBRASHARK = register("cobrashark",
            EntityType.Builder.<CobrasharkEntity>of(CobrasharkEntity::new, MobCategory.MISC).setTrackingRange(64).setUpdateInterval(1).setCustomClientFactory(CobrasharkEntity::new).fireImmune().sized(4.5f, 4.8f));

    public static final RegistryObject<EntityType<F35Entity>> F35 = register("f35",
            EntityType.Builder.<F35Entity>of(F35Entity::new, MobCategory.MISC).setTrackingRange(64).setUpdateInterval(1).setCustomClientFactory(F35Entity::new).fireImmune().sized(4.5f, 4.8f));


            private static <T extends Entity> RegistryObject<EntityType<T>> register(String name, EntityType.Builder<T> entityTypeBuilder) {
                return ENTITY_TYPES.register(name, () -> entityTypeBuilder.build(name));
            }
        
        
            public static void register(IEventBus eventBus) {
                ENTITY_TYPES.register(eventBus);
            }
}
