package com.theseeklab.bloodmoon.block;

import com.theseeklab.bloodmoon.BloodmoonEvents;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** The mod's blocks. None of them has an item, so they can't be obtained in survival or creative. */
public final class ModBlocks {
	/**
	 * What horde zombies pillar and bridge with: cobblestone's hardness, blast resistance and sound, no drops, and it
	 * disappears after the Blood Moon. Only the physical properties are copied, so it keeps its own name.
	 */
	public static final Block HORDE_COBBLESTONE = register(
		"horde_cobblestone", HordeBlock::new, BlockBehaviour.Properties.ofLegacyCopy(Blocks.COBBLESTONE).noLootTable()
	);

	private ModBlocks() {
	}

	/** Forces the class to load, which registers the blocks. */
	public static void init() {
	}

	private static Block register(final String name, final Function<BlockBehaviour.Properties, Block> factory, final BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, BloodmoonEvents.id(name));
		return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
	}
}
