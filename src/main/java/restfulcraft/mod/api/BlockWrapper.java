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

public class BlockWrapper {
	public final World world;
	public JsonObject json;
	public BlockPos pos;
	
	public BlockWrapper(Request req) {
		this.world = (World) req.attribute("world");
		this.json = (JsonObject) req.attribute("json");
		this.pos = (BlockPos) req.attribute("pos");
	}
	@SuppressWarnings("deprecation")
	public boolean replace(boolean replacing) {
		if (this.json != null) {
			try {
				Block block = Registry.BLOCK.getOrDefault(new ResourceLocation(this.json.get("id").getAsString()));
				if (replacing || this.world.isAirBlock(this.pos)) {
					this.json = this.json.getAsJsonObject("properties");
					return this.update(block.getDefaultState());
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
	public boolean update(BlockState state) {
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
	public boolean update() {
		return this.update(this.world.getBlockState(this.pos));
	}
	public boolean remove() {
		if (this.world.isAirBlock(this.pos)) {
			Spark.halt(404, CreateJSON.fromMap("error", "Block not found."));
			return false;
		} else {
			return this.world.destroyBlock(this.pos, true);
		}
	}
	
	private <S extends IStateHolder<S>, T extends Comparable<T>> S mutate(S state, IProperty<T> property, JsonElement json) {
		Optional<T> value = property.parseValue(json.getAsString());
		if (value.isPresent()) {
			return (S)(state.with(property, (T)(value.get())));
		} else {
			return state;
		}
	}
}