package restfulcraft.mod.api.v1;

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
import restfulcraft.mod.api.BlockWrapper;
import restfulcraft.mod.http.CreateJSON;
import spark.Route;

public class V1BlockRouter {
	/**
	 * GET request; returns the block state in a JSON representation along with tile entity and entities in and on.
	 */
	@SuppressWarnings("deprecation")
	public static final Route GET = (req, res) -> {
		// compared to other methods, this is the clunkiest because of the amount of unique code we're using
		// look at how POST, PUT, and to an extent PATCH use about the same methods
		JsonObject json = new JsonObject(), properties = new JsonObject();
		BlockWrapper wrapper = new BlockWrapper(req);
		BlockState blockState = wrapper.world.getBlockState(wrapper.pos);
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
        TileEntity tileEntity = wrapper.world.getChunkAt(wrapper.pos).getTileEntity(wrapper.pos);
        // reformatting "TileEntity" because depending on the configs
        // it should be "tile_entity" or "tileEntity"
        json.add(CreateJSON.reformat("TileEntity"), tileEntity == null ? null : CreateJSON.fromNBT(tileEntity.serializeNBT()));
        ListNBT entities = new ListNBT();
	    List<Entity> inAABB = wrapper.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(wrapper.pos, wrapper.pos.add(1, 2, 1)));
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
		return CreateJSON.fromMap("replaced", new BlockWrapper(req).replace(true));
	};
	/**
	 * POST request; like {@link #PUT} but returns a 405 if the target block is not air.
	 */
	public static final Route POST = (req, res) -> {
		return CreateJSON.fromMap("placed", new BlockWrapper(req).replace(false));
	};
	/**
	 * PATCH request; replaces JUST the target block's properties.
	 */
	public static final Route PATCH = (req, res) -> {
		return CreateJSON.fromMap("updated", new BlockWrapper(req).update());
	};
	/**
	 * DELETE request; breaks the target block and drops it.
	 */
	public static final Route DELETE = (req, res) -> {
		return CreateJSON.fromMap("broken", new BlockWrapper(req).remove());
	};
}
