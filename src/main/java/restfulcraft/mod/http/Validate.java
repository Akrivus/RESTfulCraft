package restfulcraft.mod.http;

import java.util.Base64;
import java.util.UUID;

import com.google.gson.JsonParseException;
import com.mojang.authlib.GameProfile;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import restfulcraft.mod.init.RESTfulCraft;
import spark.Filter;
import spark.Spark;

public class Validate {
	public static final Filter V1_AUTHORIZATION = (req, res) -> {
		if (RESTfulCraft.authKey.length() > 0 && !req.headers("Authorization").equals(RESTfulCraft.authKey)) {
			Spark.halt(401, CreateJSON.fromMap("error", "Authorization key invalid."));
		}
	};
	public static final Filter V2_AUTHORIZATION = (req, res) -> {
		byte[] auth = Base64.getDecoder().decode(req.headers("Authorization").substring(5, req.headers("Authorization").length()));
		String[] credentials = new String(auth).split(":");
		if (credentials.length == 2) {
			UUID uuid = UUID.fromString(credentials[1]);
			if (RESTfulCraft.authKey.equals(credentials[0])) {
				boolean admin = RESTfulCraft.server.getWorld(DimensionType.OVERWORLD).getServer().getPlayerList().canSendCommands(new GameProfile(uuid, null));
				if (!admin) {
					Spark.halt(403, CreateJSON.fromMap("error", "Access denied."));
				}
			} else {
				Spark.halt(401, CreateJSON.fromMap("error", "Incorrect authorization key."));
			}
		} else {
			Spark.halt(401, CreateJSON.fromMap("errors", "Malformed or missing authorization header."));
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
			if (world == null) { // you will get this if you call an unoccupied dimension
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
