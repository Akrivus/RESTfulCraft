package restfulcraft.mod.api;

import java.util.Map.Entry;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;

import net.minecraft.state.IProperty;
import restfulcraft.mod.http.CreateJSON;
import spark.Route;
import spark.Spark;

public class BlockRouter {
	public static final String QUERY_URL = "/:x/:y/:z";
	public static final Route GET = (req, res) -> {
		JsonObject json = new JsonObject(), properties = new JsonObject();
		BlockConnection conn = new BlockConnection(req);
   		json.addProperty("id", conn.getBlockName(conn.blockState.getBlock()));
   		ImmutableMap<IProperty<?>, Comparable<?>> map = conn.blockState.getValues();
   		for (Entry<IProperty<?>, Comparable<?>> entry : map.entrySet()) {
   			IProperty<?> property = entry.getKey();
   			properties.add(property.getName(), CreateJSON.fromObject(conn.blockState.get(property).toString()));
   		}
   		json.add("properties", properties);
   		if (conn.tileEntity != null) {
   			json.add(CreateJSON.reformat("TileEntity"), CreateJSON.fromNBT(conn.tileEntity.serializeNBT()));
   		} else {
   			json.add(CreateJSON.reformat("TileEntity"), null);
   		}
   		json.add("entities", CreateJSON.fromNBT(conn.entities));
   		return json.toString();
	};
	public static final Route PUT = (req, res) -> {
		return CreateJSON.fromMap("replaced", new BlockConnection(req).placeBlock(true));
	};
	public static final Route POST = (req, res) -> {
		return CreateJSON.fromMap("placed", new BlockConnection(req).placeBlock(false));
	};
	public static final Route PATCH = (req, res) -> {
		return CreateJSON.fromMap("updated", new BlockConnection(req).updateBlock());
	};
	public static final Route DELETE = (req, res) -> {
		BlockConnection conn = new BlockConnection(req);
		if (conn.world.isAirBlock(conn.pos)) {
			return Spark.halt(404, CreateJSON.fromMap("error", "Block not found."));
		} else {
			return CreateJSON.fromMap("broken", conn.breakBlock());
		}
	};
}
