package restfulcraft.mod.api.v2;

import java.util.Map.Entry;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.block.BlockState;
import net.minecraft.state.IProperty;
import net.minecraft.util.registry.Registry;
import restfulcraft.mod.http.CreateJSON;
import spark.Route;
import spark.Spark;

public class V2EntityRouter {
	/**
	 * GET request; returns the entity in a JSON representation.
	 */
	@SuppressWarnings("deprecation")
	public static final Route GET_INDEX = (req, res) -> {
		// compared to other methods, this is the clunkiest because of the amount of unique code we're using
		// look at how POST, PUT, and to an extent PATCH use about the same methods
		JsonObject json = new JsonObject(), properties = new JsonObject();
		BlockWrapper conn = new BlockWrapper(req);
		BlockState blockState = conn.world.getBlockState(conn.pos);
		// will replace with a less deprecated method once i find one
   		json.addProperty("id", Registry.BLOCK.getKey(blockState.getBlock()).toString());
   		ImmutableMap<IProperty<?>, Comparable<?>> map = blockState.getValues();
   		for (Entry<IProperty<?>, Comparable<?>> entry : map.entrySet()) {
   			IProperty<?> property = entry.getKey();
   			JsonElement value = CreateJSON.fromObject(blockState.get(property).toString());
   			properties.add(property.getName(), value);
   		}
   		json.add("properties", properties);
   		return json.toString();
	};
	/**
	 * GET request; returns an entity in a JSON representation.
	 */
	@SuppressWarnings("deprecation")
	public static final Route GET = (req, res) -> {
		// compared to other methods, this is the clunkiest because of the amount of unique code we're using
		// look at how POST, PUT, and to an extent PATCH use about the same methods
		JsonObject json = new JsonObject(), properties = new JsonObject();
		BlockWrapper conn = new BlockWrapper(req);
		BlockState blockState = conn.world.getBlockState(conn.pos);
		// will replace with a less deprecated method once i find one
   		json.addProperty("id", Registry.BLOCK.getKey(blockState.getBlock()).toString());
   		ImmutableMap<IProperty<?>, Comparable<?>> map = blockState.getValues();
   		for (Entry<IProperty<?>, Comparable<?>> entry : map.entrySet()) {
   			IProperty<?> property = entry.getKey();
   			JsonElement value = CreateJSON.fromObject(blockState.get(property).toString());
   			properties.add(property.getName(), value);
   		}
   		json.add("properties", properties);
   		return json.toString();
	};
	/**
	 * PUT request; replaces target entity's item.
	 */
	public static final Route PUT = (req, res) -> {
		return CreateJSON.fromMap("replaced", new BlockWrapper(req).placeBlock(true));
	};
	/**
	 * POST request; like {@link #PUT} but returns a 405 if the target entity is not empty.
	 */
	public static final Route POST = (req, res) -> {
		return CreateJSON.fromMap("placed", new BlockWrapper(req).placeBlock(false));
	};
	/**
	 * PATCH request; impractical, re-routes to {@link #PUT}.
	 */
	public static final Route PATCH = PUT;
	/**
	 * DELETE request; deletes the target entity.
	 */
	public static final Route DELETE = (req, res) -> {
		BlockWrapper conn = new BlockWrapper(req);
		if (conn.world.isAirBlock(conn.pos)) {
			return Spark.halt(404, CreateJSON.fromMap("error", "Block not found."));
		} else {
			return CreateJSON.fromMap("broken", conn.world.destroyBlock(conn.pos, true));
		}
	};
}
