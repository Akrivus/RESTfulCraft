package restfulcraft.mod;

import com.google.gson.JsonParseException;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import restfulcraft.mod.http.BlockAPI;
import restfulcraft.mod.http.Query;
import spark.Spark;

@EventBusSubscriber
public class Server {
	static { Spark.port(RESTfulCraft.port); }
	public static int port = RESTfulCraft.port;
	private static boolean ONLINE = false;
	private MinecraftServer server;
	
	public Server(MinecraftServer server) {
		if (ONLINE) {
			throw new IllegalStateException("There is already a server running!");
		} else {
			this.server = server;
			ONLINE = true;
			Spark.init();
			Spark.internalServerError((req, res) -> {
				return CreateJSON.fromMap("error", "An error occured.");
			});
			Spark.notFound((req, res) -> {
				return CreateJSON.fromMap("error", "Route not found.");
			});
			Spark.before((req, res) -> {
				res.header("Content-Type", "application/json");
				if (RESTfulCraft.authKey.length() > 0 && !req.headers("Authorization").equals(RESTfulCraft.authKey)) {
					Spark.halt(401, CreateJSON.fromMap("error", "Authorization key invalid."));
				}
			});
			Spark.path("/api/v1/:mod/:dim", () -> {
				/**
				 * Validate dimension, world status, block position, and JSON.
				 */
				Spark.before(BlockAPI.QUERY_URL, (req, res) -> {
					// Validating dimension.
					ResourceLocation name = new ResourceLocation(req.params("mod"), req.params("dim"));
					DimensionType dimension = DimensionType.byName(name);
					if (dimension == null) {
						Spark.halt(404, CreateJSON.fromMap("error", String.format("Dimension '%s' not found.", name)));
					} else {
						// Validating world, a null world is an unloaded/able one.
						World world = RESTfulCraft.server.getWorld(dimension);
						if (world != null) {
							// Validating JSON.
							Query query = new Query(world);
							req.attribute("query", query);
							if (req.body().length() > 0) {
								try {
									query.json = CreateJSON.fromString(req.body());
								} catch (JsonParseException e) {
									Spark.halt(400, CreateJSON.fromMap("error", "Invalid JSON in response body."));
								}
							}
							// Validation block position.
							try {
								int x = Integer.parseInt(req.params("x"));
								int y = Integer.parseInt(req.params("y"));
								int z = Integer.parseInt(req.params("z"));
								BlockPos pos = new BlockPos(x, y, z);
								if (World.isValid(pos)) {
									Query.get(req).init(pos);
								} else {
									Spark.halt(403, CreateJSON.fromMap("error", "Invalid position."));
								}
							} catch (NumberFormatException e) {
								Spark.halt(422, CreateJSON.fromMap("error", "Invalid position parameters."));
							}
						} else {
							Spark.halt(503, CreateJSON.fromMap("error", "Dimension found but not currently loadable."));
						}
					}
				});
				/**
				 * Proceed with the usual CRUD.
				 */
				Spark.post(BlockAPI.QUERY_URL, BlockAPI.PLACE);
				Spark.put(BlockAPI.QUERY_URL, BlockAPI.REPLACE);
				Spark.patch(BlockAPI.QUERY_URL, BlockAPI.UPDATE);
				Spark.get(BlockAPI.QUERY_URL, BlockAPI.QUERY);
				Spark.delete(BlockAPI.QUERY_URL, BlockAPI.BREAK);
			});
			Spark.afterAfter((req, res) -> {
				RESTfulCraft.LOGGER.info("{} - {} {} {}", req.ip(), res.status(), req.requestMethod(), req.uri());
			});
		}
	}
	public void stop() {
		Spark.stop();
		ONLINE = false;
	}
	public boolean isOnline() {
		return ONLINE;
	}
	public World getWorld(DimensionType dimension) {
		return this.server.getWorld(dimension);
	}
}
