package restfulcraft.mod.init;


import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import restfulcraft.mod.api.BlockRouter;
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
				Spark.before("/:x/:y/:z", Validate.DIMENSION, Validate.BLOCK_POS);
				Spark.get("/:x/:y/:z", BlockRouter.GET);
				Spark.put("/:x/:y/:z", BlockRouter.PUT);
				Spark.post("/:x/:y/:z", BlockRouter.POST);
				Spark.patch("/:x/:y/:z", BlockRouter.PATCH);
				Spark.delete("/:x/:y/:z", BlockRouter.DELETE);
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
