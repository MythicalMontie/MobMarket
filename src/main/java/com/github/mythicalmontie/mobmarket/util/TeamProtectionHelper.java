package com.github.mythicalmontie.mobmarket.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

public class TeamProtectionHelper {

    public static boolean isFTBTeamsLoaded() {
        return ModList.get().isLoaded("ftbteams");
    }

    public static boolean canAccess(ServerPlayer player, UUID ownerUUID, boolean teamAccessOnly) {
        if (!teamAccessOnly || ownerUUID == null) return true;
        if (player.getUUID().equals(ownerUUID)) return true;
        if (isFTBTeamsLoaded()) {
            try {
                return checkFTBTeamAccessReflection(player, ownerUUID);
            } catch (Exception ignored) {}
        }
        return false;
    }

    private static boolean checkFTBTeamAccessReflection(ServerPlayer player, UUID ownerUUID) throws Exception {
        Class<?> apiClass = Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI");
        Method apiMethod = apiClass.getMethod("api");
        Object apiInstance = apiMethod.invoke(null);
        Method isManagerLoadedMethod = apiInstance.getClass().getMethod("isManagerLoaded");
        if (!(boolean) isManagerLoadedMethod.invoke(apiInstance)) return false;
        Method getManagerMethod = apiInstance.getClass().getMethod("getManager");
        Object managerInstance = getManagerMethod.invoke(apiInstance);

        Method getTeamForPlayerID = managerInstance.getClass().getMethod("getTeamForPlayerID", UUID.class);

        Optional<?> playerTeamOpt = (Optional<?>) getTeamForPlayerID.invoke(managerInstance, player.getUUID());
        Optional<?> ownerTeamOpt = (Optional<?>) getTeamForPlayerID.invoke(managerInstance, ownerUUID);

        if (playerTeamOpt.isPresent() && ownerTeamOpt.isPresent()) {
            Object playerTeam = playerTeamOpt.get();
            Object ownerTeam = ownerTeamOpt.get();

            Method getIdMethod = playerTeam.getClass().getMethod("getId");
            UUID playerTeamId = (UUID) getIdMethod.invoke(playerTeam);
            UUID ownerTeamId = (UUID) getIdMethod.invoke(ownerTeam);

            if (playerTeamId.equals(ownerTeamId)) return true;

            try {
                Method isAllyMethod = ownerTeam.getClass().getMethod("isAlly", UUID.class);
                return (boolean) isAllyMethod.invoke(ownerTeam, player.getUUID());
            } catch (NoSuchMethodException ignored) {}
        }

        return false;
    }
}