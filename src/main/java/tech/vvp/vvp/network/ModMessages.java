package tech.vvp.vvp.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import tech.vvp.vvp.VVP;
import tech.vvp.vvp.network.message.S2CRadarSyncPacket;

public class ModMessages {
    private static SimpleChannel INSTANCE;

    private static int packetId = 0;
    private static int id() {
        return packetId++;
    }

    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(VVP.MOD_ID, "messages"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE = net;

        // Регистрируем наш пакет радара
        net.registerMessage(id(),
                S2CRadarSyncPacket.class,
                S2CRadarSyncPacket::buffer,
                S2CRadarSyncPacket::new,
                S2CRadarSyncPacket::handler,
                java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}