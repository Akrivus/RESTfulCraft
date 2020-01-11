package restfulcraft.mod.init;


import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import restfulcraft.mod.api.BlockAPI;
import restfulcraft.mod.http.CreateJSON;
import restfulcraft.mod.http.Validate;
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
			Spark.internalServerError((req, res) -> { return CreateJSON.fromMap("error", "An error occured."); });
			Spark.notFound((req, res) -> { return CreateJSON.fromMap("error", "Route not found."); });
			Spark.before(Validate.AUTHORIZATION, Validate.JSON);
			Spark.path("/api/v1/:mod/:dim", () -> {
				Spark.path("/block", () -> {
					Spark.before(BlockAPI.QUERY_URL, Validate.DIMENSION, Validate.BLOCK_POS);
					Spark.post(BlockAPI.QUERY_URL, BlockAPI.POST);
					Spark.put(BlockAPI.QUERY_URL, BlockAPI.PUT);
					Spark.patch(BlockAPI.QUERY_URL, BlockAPI.PATCH);
					Spark.delete(BlockAPI.QUERY_URL, BlockAPI.DELETE);
					Spark.get(BlockAPI.QUERY_URL, BlockAPI.GET);
				});
				Spark.path("/command", () -> {
					
				});
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
