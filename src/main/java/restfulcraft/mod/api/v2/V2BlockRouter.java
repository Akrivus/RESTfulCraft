package restfulcraft.mod.api.v2;

import java.util.Map.Entry;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.block.BlockState;
import net.minecraft.state.IProperty;
import net.minecraft.util.registry.Registry;
import restfulcraft.mod.api.BlockWrapper;
import restfulcraft.mod.http.CreateJSON;
import spark.Route;

public class V2BlockRouter {
	@SuppressWarnings("deprecation")
	public static final Route GET = (req, res) -> {
		JsonObject json = new JsonObject(), properties = new JsonObject();
		BlockWrapper wrapper = new BlockWrapper(req);
		BlockState state = wrapper.world.getBlockState(wrapper.pos);
   		json.addProperty("id", Registry.BLOCK.getKey(state.getBlock()).toString());
   		ImmutableMap<IProperty<?>, Comparable<?>> map = state.getValues();
   		for (Entry<IProperty<?>, Comparable<?>> entry : map.entrySet()) {
   			IProperty<?> property = entry.getKey();
   			JsonElement value = CreateJSON.fromObject(state.get(property).toString());
   			properties.add(property.getName(), value);
   		}
   		json.add("properties", properties);
   		return json.toString();
	};
	public static final Route PUT = (req, res) -> {
		return CreateJSON.fromMap("replaced", new BlockWrapper(req).replace(true));
	};
	public static final Route POST = (req, res) -> {
		return CreateJSON.fromMap("placed", new BlockWrapper(req).replace(false));
	};
	public static final Route PATCH = (req, res) -> {
		return CreateJSON.fromMap("updated", new BlockWrapper(req).update());
	};
	public static final Route DELETE = (req, res) -> {
		return CreateJSON.fromMap("removed", new BlockWrapper(req).remove());
	};
}
