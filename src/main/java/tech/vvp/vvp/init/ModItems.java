package tech.vvp.vvp.init;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.RecordItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.eventbus.api.IEventBus;
import tech.vvp.vvp.VVP;
import tech.vvp.vvp.item.armor.usahelmet;
import tech.vvp.vvp.item.armor.usachest;


public class ModItems {
    public static final DeferredRegister<Item> REGISTRY = 
            DeferredRegister.create(ForgeRegistries.ITEMS, VVP.MOD_ID);

    public static final RegistryObject<Item> ICON_SPAWN_ITEM = REGISTRY.register("icon_spawn_item",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ICON_CIVILIAN = REGISTRY.register("icon_civilian",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ARMOR_ICON = REGISTRY.register("armor_icon",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> USA_HELMET = REGISTRY.register("usahelmet",
            () -> new usahelmet());
            
    public static final RegistryObject<Item> USA_CHEST = REGISTRY.register("usachest",
            () -> new usachest());

    public static final RegistryObject<Item> RADIOHEAD = REGISTRY.register("music_disc_radiohead",
                        () -> new RecordItem(15, ModSounds.RADIOHEAD, // 15 = comparator signal strength
                                new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 4800)); // 4800 = длительность в тиках

    public static final RegistryObject<Item> AIR_TO_AIR_MISSILE_ITEM = REGISTRY.register("air_to_air_missile_item",
        () -> new Item(new Item.Properties()));


    public static void register(IEventBus eventBus) {
        REGISTRY.register(eventBus);
    }
}