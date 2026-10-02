package com.theseeklab.bloodmoon.network;

import com.theseeklab.bloodmoon.BloodmoonEvents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Tells a client whether a Blood Moon is active in the level it is currently in. */
public record BloodMoonSyncPayload(boolean active) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<BloodMoonSyncPayload> TYPE = new CustomPacketPayload.Type<>(BloodmoonEvents.id("sync"));
	public static final StreamCodec<RegistryFriendlyByteBuf, BloodMoonSyncPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.BOOL, BloodMoonSyncPayload::active, BloodMoonSyncPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
