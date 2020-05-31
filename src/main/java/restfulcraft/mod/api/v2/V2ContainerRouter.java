package restfulcraft.mod.api.v2;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import restfulcraft.mod.api.ContainerWrapper;
import restfulcraft.mod.http.CreateJSON;
import spark.Route;
import spark.Spark;

public class V2ContainerRouter {
	public static final Route GET = (req, res) -> {
		JsonObject json = new JsonObject(); JsonArray items = new JsonArray();
		ContainerWrapper wrapper = new ContainerWrapper(req);
		json.add("id", CreateJSON.fromObject(wrapper.container.getBlockState().getBlock().getRegistryName().toString()));
		json.add("name", CreateJSON.fromObject(wrapper.container.getName().getString()));
		for (int slot = 0; slot < wrapper.container.getSizeInventory(); ++slot) {
			items.add(wrapper.read(slot));
		}
		return json.toString();
	};
	public static final Route PUT = (req, res) -> {
		return CreateJSON.fromMap("replaced", new ContainerWrapper(req).replace(true));
	};
	public static final Route POST = (req, res) -> {
		
	};
	public static final Route PATCH = (req, res) -> {
		
	};
	public static final Route DELETE = (req, res) -> {
		
	};
}
