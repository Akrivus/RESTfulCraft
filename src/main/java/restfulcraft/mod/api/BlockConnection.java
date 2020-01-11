package restfulcraft.mod.api;

import java.util.Map.Entry;
import java.util.Optional;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.IProperty;
import net.minecraft.state.IStateHolder;
import net.minecraft.state.StateContainer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import restfulcraft.mod.http.CreateJSON;
import spark.Request;
import spark.Spark;

public class BlockConnection {
	/**
	 * The world the connection originates from.
	 */
	public final World world;
	/**
	 * The parsed JSON body of the request, unless null.
	 */
	public JsonObject json;
	/**
	 * The block position extracted from the URL.
	 */
	public BlockPos pos;
	
	public BlockConnection(Request req) {
		this.world = (World) req.attribute("world");
		this.json = (JsonObject) req.attribute("json");
		this.pos = (BlockPos) req.attribute("pos");
	}
	/**
	 * Helper method for POST and PUT requests, serializes request JSON into a block state.
	 * @param replacing
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public boolean placeBlock(boolean replacing) {
		if (this.json != null) {
			try {
				Block block = Registry.BLOCK.getOrDefault(new ResourceLocation(this.json.get("id").getAsString()));
				if (replacing || this.world.isAirBlock(this.pos)) {
					this.json = this.json.getAsJsonObject("properties");
					return this.updateBlock(block.getDefaultState());
				} else {
					Spark.halt(405, CreateJSON.fromMap("error", "Block occupied."));
				}
			} catch (IllegalArgumentException e) {
				Spark.halt(422, CreateJSON.fromMap("error", e.getMessage()));
			} catch (NullPointerException e) {
				Spark.halt(405, CreateJSON.fromMap("error", "Block ID missing."));
			}
		} else {
			Spark.halt(406, CreateJSON.fromMap("error", "Request body missing."));
		}
		return false;
	}
	/**
	 * Helper method for POST and PUT methods, serializes request JSON into individual block state properties.
	 * @param state
	 * @return
	 */
	public boolean updateBlock(BlockState state) {
		if (this.json != null) {
			StateContainer<Block, BlockState> container = state.getBlock().getStateContainer();
			for (Entry<String, JsonElement> entry : this.json.entrySet()) {
				IProperty<?> property = container.getProperty(CreateJSON.reformat(entry.getKey()));
				if (property == null) {
					throw new IllegalArgumentException(String.format("Invalid property '%s'.", entry.getKey()));
				} else {
					state = mutate(state, property, entry.getValue());
				}
			}
		}
		return this.world.setBlockState(this.pos, state);
	}
	/**
	 * Helper method for PATCH methods, serializes request JSON into individual block state properties.
	 * @return
	 */
	public boolean updateBlock() {
		return this.updateBlock(this.world.getBlockState(this.pos));
	}
	/**
	 * Private method used for the generic acrobatics performance that is returning the value of a block state property.
	 * @param <S>
	 * @param <T>
	 * @param state
	 * @param property
	 * @param json
	 * @return
	 */
	private <S extends IStateHolder<S>, T extends Comparable<T>> S mutate(S state, IProperty<T> property, JsonElement json) {
		Optional<T> value = property.parseValue(json.getAsString());
		if (value.isPresent()) {
			return (S)(state.with(property, (T)(value.get())));
		} else {
			return state;
		}
	}
}