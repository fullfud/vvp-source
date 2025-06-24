package tech.vvp.vvp.entity.vehicle.weapon;

import com.atsuishio.superbwarfare.entity.vehicle.weapon.VehicleWeapon;
import net.minecraft.world.entity.player.Player;
import tech.vvp.vvp.entity.projectile.AirToAirMissileEntity;

public class AirToAirMissileWeapon extends VehicleWeapon {
    
    // Здесь НЕ НУЖЕН @Override, это просто метод класса
    public AirToAirMissileEntity create(Player player) {
        return new AirToAirMissileEntity(player, player.level(), "none");
    }
}