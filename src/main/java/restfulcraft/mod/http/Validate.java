package restfulcraft.mod.http;

import com.google.gson.JsonParseException;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import restfulcraft.mod.init.RESTfulCraft;
import spark.Filter;
import spark.Spark;

public class Validate {
	public static final Filter AUTHORIZATION = (req, res) -> {
		if (RESTfulCraft.authKey.length() > 0 && !req.headers("Authorization").equals(RESTfulCraft.authKey)) {
			Spark.halt(401, CreateJSON.fromMap("error", "Authorization key invalid."));
		}
	};
	public static final Filter JSON = (req, res) -> {
		res.header("Content-Type", "application/json");
		if (req.body().length() > 0) {
			try {
				req.attribute("json", CreateJSON.fromString(req.body()));
			} catch (JsonParseException e) {
				Spark.halt(400, CreateJSON.fromMap("error", "Invalid JSON in response body."));
			}
		}
	};
	public static final Filter DIMENSION = (req, res) -> {
		ResourceLocation name = new ResourceLocation(req.params("mod"), req.params("dim"));
		DimensionType dimension = DimensionType.byName(name);
		if (dimension == null) {
			Spark.halt(404, CreateJSON.fromMap("error", String.format("Dimension '%s' not found.", name)));
		} else {
			World world = RESTfulCraft.server.getWorld(dimension);
			if (world == null) {
				Spark.halt(503, CreateJSON.fromMap("error", "Dimension found but not currently loadable."));
			} else {
				req.attribute("world", world);
			}
		}
	};
	public static final Filter BLOCK_POS = (req, res) -> {
		try {
			int x = Integer.parseInt(req.params("x"));
			int y = Integer.parseInt(req.params("y"));
			int z = Integer.parseInt(req.params("z"));
			BlockPos pos = new BlockPos(x, y, z);
			if (!World.isValid(pos)) {
				Spark.halt(403, CreateJSON.fromMap("error", "Invalid position."));
			} else {
				req.attribute("pos", pos);
			}
		} catch (NumberFormatException e) {
			Spark.halt(422, CreateJSON.fromMap("error", "Invalid position parameters."));
		}
	};
}
