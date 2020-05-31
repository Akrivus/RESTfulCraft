package restfulcraft.mod.api.v2;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import restfulcraft.mod.api.ContainerWrapper;
import restfulcraft.mod.http.CreateJSON;
import spark.Route;
import spark.Spark;

public class V2ItemRouter {
	public static final Route GET_INDEX = (req, res) -> {
		JsonObject json = new JsonObject(); JsonArray items = new JsonArray();
		ContainerWrapper conn = new ContainerWrapper(req);
		for (int slot = 0; slot < conn.container.getSizeInventory(); ++slot) {
			items.add(conn.getStack(slot));
		}
		json.add("items", items);
   		return json.toString();
	};
	public static final Route GET = (req, res) -> {
		ContainerWrapper conn = new ContainerWrapper(req);
		return conn.getStack(conn.slot);
	};
	public static final Route PUT_SELF = (req, res) -> {
		ContainerWrapper conn = new ContainerWrapper(req);
	};
	public static final Route PUT = (req, res) -> {
		return CreateJSON.fromMap("replaced", new ContainerWrapper(req).replaceSlot(true));
	};
	public static final Route POST = (req, res) -> {
		return CreateJSON.fromMap("placed", new ContainerWrapper(req).replaceSlot(false));
	};
	public static final Route POST_SELF = (req, res) -> {
		ContainerWrapper conn = new ContainerWrapper(req);
	};
	public static final Route PATCH = (req, res) -> {
		return CreateJSON.fromMap("updated", new ContainerWrapper(req).updateSlot());
	};
	public static final Route PATCH_SELF = (req, res) -> {
		ContainerWrapper conn = new ContainerWrapper(req);
	};
	public static final Route DELETE = (req, res) -> {
		ContainerWrapper conn = new ContainerWrapper(req);
		if (conn.container.getStackInSlot(conn.slot).isEmpty()) {
			return Spark.halt(404, CreateJSON.fromMap("error", "Slot not found."));
		} else {
			conn.container.removeStackFromSlot(conn.slot);
			return CreateJSON.fromMap("removed", true);
		}
	};
}
