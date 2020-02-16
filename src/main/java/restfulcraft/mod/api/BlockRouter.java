package restfulcraft.mod.api;

import java.util.List;
import java.util.Map.Entry;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.ListNBT;
import net.minecraft.state.IProperty;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.registry.Registry;
import restfulcraft.mod.http.CreateJSON;
import spark.Route;
import spark.Spark;

public class BlockRouter {
	/**
	 * GET request; returns the block state in a JSON representation along with tile entity and entities in and on.
	 */
	@SuppressWarnings("deprecation")
	public static final Route GET = (req, res) -> {
		// compared to other methods, this is the clunkiest because of the amount of unique code we're using
		// look at how POST, PUT, and to an extent PATCH use about the same methods
		JsonObject json = new JsonObject(), properties = new JsonObject();
		BlockConnection conn = new BlockConnection(req);
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
   		// we need to ask the chunk for the tile entity because the world withholds it
   		// if we're on a separate thread (which we are)
   		TileEntity tileEntity = conn.world.getChunkAt(conn.pos).getTileEntity(conn.pos);
   		// reformatting "TileEntity" because depending on the configs
   		// it should be "tile_entity" or "tileEntity"
   		json.add(CreateJSON.reformat("TileEntity"), tileEntity == null ? null : CreateJSON.fromNBT(tileEntity.serializeNBT()));
   		ListNBT entities = new ListNBT();
		List<Entity> inAABB = conn.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(conn.pos, conn.pos.add(1, 2, 1)));
		for (Entity entity : inAABB) {
			entities.add(entity.serializeNBT());
		}
   		json.add("entities", CreateJSON.fromNBT(entities));
   		return json.toString();
	};
	/**
	 * PUT request; replaces target block's state.
	 */
	public static final Route PUT = (req, res) -> {
		return CreateJSON.fromMap("replaced", new BlockConnection(req).placeBlock(true));
	};
	/**
	 * POST request; like {@link #PUT} but returns a 405 if the target block is not air.
	 */
	public static final Route POST = (req, res) -> {
		return CreateJSON.fromMap("placed", new BlockConnection(req).placeBlock(false));
	};
	/**
	 * PATCH request; replaces JUST the target block's properties.
	 */
	public static final Route PATCH = (req, res) -> {
		return CreateJSON.fromMap("updated", new BlockConnection(req).updateBlock());
	};
	/**
	 * DELETE request; breaks the target block and drops it.
	 */
	public static final Route DELETE = (req, res) -> {
		BlockConnection conn = new BlockConnection(req);
		if (conn.world.isAirBlock(conn.pos)) {
			return Spark.halt(404, CreateJSON.fromMap("error", "Block not found."));
		} else {
			return CreateJSON.fromMap("broken", conn.world.destroyBlock(conn.pos, true));
		}
	};
}
