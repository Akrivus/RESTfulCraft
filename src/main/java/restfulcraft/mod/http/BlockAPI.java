package restfulcraft.mod.http;

import java.util.Map.Entry;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.IProperty;
import net.minecraft.state.IStateHolder;
import net.minecraft.state.StateContainer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import restfulcraft.mod.CreateJSON;
import spark.Route;
import spark.Spark;

public class BlockAPI {
	public static final String QUERY_URL = "/:x/:y/:z";
	
	/**
	 * POST - places block, requires position to be air.
	 */
	public static final Route PLACE = (req, res) -> {
		Query query = Query.get(req);
		if (query.json != null && query.world.isAirBlock(query.pos)) {
			Block block = getBlock(query.json.get("id").getAsString());
			BlockState state = setStateWithJSON(block, query.json.getAsJsonObject("properties"));
			return CreateJSON.fromMap("placed", query.world.setBlockState(query.pos, state));
		} else if (query.json != null) {
			return Spark.halt(409, CreateJSON.fromMap("error", "Block occupied."));
		} else {
			return Spark.halt(406, CreateJSON.fromMap("error", "Request body missing."));
		}
	};
	/**
	 * PUT - replaces block.
	 */
	public static final Route REPLACE = (req, res) -> {
		Query query = Query.get(req);
		if (query.json != null) {
			Block block = getBlock(query.json.get("id").getAsString());
			BlockState state = setStateWithJSON(block, query.json.getAsJsonObject("properties"));
			return CreateJSON.fromMap("replaced", query.world.setBlockState(query.pos, state));
		} else {
			return Spark.halt(406, CreateJSON.fromMap("error", "Request body missing."));
		}
	};
	/**
	 * PATCH - sets block properties.
	 */
	public static final Route UPDATE = (req, res) -> {
		Query query = Query.get(req);
		if (query.json != null) {
			BlockState state = setStateWithJSON(query.blockState.getBlock(), query.blockState, query.json);
			return CreateJSON.fromMap("updated", query.world.setBlockState(query.pos, state));
		} else {
			return Spark.halt(406, CreateJSON.fromMap("error", "Request body missing."));
		}
	};
	/**
	 * GET - gets block properties, tile entities, and entities.
	 */
	public static final Route QUERY = (req, res) -> {
		JsonObject json = new JsonObject(), properties = new JsonObject();
		Query query = Query.get(req);
   		json.addProperty("id", getName(query.blockState.getBlock()));
   		ImmutableMap<IProperty<?>, Comparable<?>> map = query.blockState.getValues();
   		for (Entry<IProperty<?>, Comparable<?>> entry : map.entrySet()) {
   			IProperty<?> property = entry.getKey();
   			properties.add(property.getName(), CreateJSON.fromObject(query.blockState.get(property)));
   		}
   		json.add("properties", properties);
   		if (query.tileEntity != null) {
   			json.add("tileEntity", CreateJSON.fromNBT(query.tileEntity.serializeNBT()));
   		} else {
   			json.add("tileEntity", null);
   		}
   		json.add("entities", CreateJSON.fromNBT(query.entities));
   		return json.toString();
	};
	/**
	 * DELETE - destroys blocks and drops them; only works on non-AIR blocks.
	 */
	public static final Route BREAK = (req, res) -> {
		Query query = Query.get(req);
		if (query.world.isAirBlock(query.pos)) {
			return Spark.halt(404, CreateJSON.fromMap("error", "Block not found."));
		} else {
			return CreateJSON.fromMap("broken", query.world.destroyBlock(query.pos, true));
		}
	};
	
	@SuppressWarnings("deprecation")
	private static Block getBlock(String name) {
		return Registry.BLOCK.getOrDefault(new ResourceLocation(name));
	}
	@SuppressWarnings("deprecation")
	private static String getName(Block block) {
		return Registry.BLOCK.getKey(block).toString();
	}
	private static BlockState setStateWithJSON(Block block, BlockState state, JsonObject json) {
		if (json != null) {
			StateContainer<Block, BlockState> container = block.getStateContainer();
			for (Entry<String, JsonElement> entry : json.entrySet()) {
				IProperty<?> property = container.getProperty(CreateJSON.withCamelCase(entry.getKey()));
				if (property == null) {
					throw new IllegalArgumentException(String.format("Invalid property '%s'.", entry.getKey()));
				} else {
					state = mutate(state, property, entry.getValue());
				}
			}
		}
		return state;
	}
	private static BlockState setStateWithJSON(Block block, JsonObject json) {
		return setStateWithJSON(block, block.getDefaultState(), json);
	}
	private static <S extends IStateHolder<S>, T extends Comparable<T>> S mutate(S state, IProperty<T> property, JsonElement json) {
		Optional<T> value = property.parseValue(json.getAsString());
		if (value.isPresent()) {
			return (S)(state.with(property, (T)(value.get())));
		} else {
			return state;
		}
	}
}
