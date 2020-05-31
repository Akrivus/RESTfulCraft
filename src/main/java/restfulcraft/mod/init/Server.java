package restfulcraft.mod.init;


import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import restfulcraft.mod.api.v1.V1BlockRouter;
import restfulcraft.mod.api.v2.V2BlockRouter;
import restfulcraft.mod.api.v2.V2ContainerRouter;
import restfulcraft.mod.api.v2.V2EntityRouter;
import restfulcraft.mod.http.CreateJSON;
import restfulcraft.mod.http.Validate;
import spark.Spark;

@EventBusSubscriber
public class Server {
	public static int port = RESTfulCraft.port;
	private static boolean ONLINE = false;
	private MinecraftServer server;
	
	public Server(MinecraftServer server) {
		if (ONLINE) {
			throw new IllegalStateException("There is already a server running!");
		} else {
			this.server = server;
			Spark.port(port = RESTfulCraft.port);
			Spark.init();
			Spark.internalServerError((req, res) -> { return CreateJSON.fromMap("error", "An error occured."); });
			Spark.notFound((req, res) -> { return CreateJSON.fromMap("error", "Route not found."); });
			Spark.path("/api/v1", () -> {
				Spark.before(Validate.V1_AUTHORIZATION, Validate.JSON);
				Spark.path("/:mod/:dim", () -> {
					Spark.before("/:x/:y/:z", Validate.DIMENSION, Validate.BLOCK_POS);
					Spark.post("/:x/:y/:z", V1BlockRouter.POST);
					Spark.get("/:x/:y/:z", V1BlockRouter.GET);
					Spark.patch("/:x/:y/:z", V1BlockRouter.PATCH);
					Spark.put("/:x/:y/:z", V1BlockRouter.PUT);
					Spark.delete("/:x/:y/:z", V1BlockRouter.DELETE);
				});
			});
			Spark.path("/api/v2", () -> {
				Spark.before(Validate.V2_AUTHORIZATION, Validate.JSON);
				Spark.path("/:mod/:dim", () -> {
					Spark.before("/:x/:y/:z", Validate.DIMENSION, Validate.BLOCK_POS);
					Spark.post("/:x/:y/:z", V2BlockRouter.POST);
					Spark.post("/:x/:y/:z/container", V2ContainerRouter.POST);
					Spark.post("/:x/:y/:z/entity", V2EntityRouter.POST);
					Spark.get("/:x/:y/:z", V2BlockRouter.GET);
					Spark.get("/:x/:y/:z/container", V2ContainerRouter.GET_INDEX);
					Spark.get("/:x/:y/:z/container/:slot", V2ContainerRouter.GET);
					Spark.get("/:x/:y/:z/entity", V2EntityRouter.GET_INDEX);
					Spark.get("/:x/:y/:z/entity/:id", V2EntityRouter.GET);
					Spark.patch("/:x/:y/:z", V2BlockRouter.PATCH);
					Spark.patch("/:x/:y/:z/container/:slot", V2ContainerRouter.PATCH);
					Spark.patch("/:x/:y/:z/entity/:id", V2EntityRouter.PATCH);
					Spark.put("/:x/:y/:z", V2BlockRouter.PUT);
					Spark.put("/:x/:y/:z/container/:slot", V2ContainerRouter.PUT);
					Spark.put("/:x/:y/:z/entity/:id", V2EntityRouter.PUT);
					Spark.delete("/:x/:y/:z", V2BlockRouter.DELETE);
					Spark.delete("/:x/:y/:z/container/:slot", V2ContainerRouter.DELETE);
					Spark.delete("/:x/:y/:z/entity/:id", V2EntityRouter.DELETE);
				});
			});
			Spark.afterAfter((req, res) -> {
				RESTfulCraft.LOGGER.info("{} - {} {} {}", req.ip(), res.status(), req.requestMethod(), req.uri());
			});
			ONLINE = true;
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
