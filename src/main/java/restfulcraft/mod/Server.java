package restfulcraft.mod;

import java.util.Map.Entry;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.state.IProperty;
import net.minecraft.state.IStateHolder;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import spark.Spark;

@EventBusSubscriber
public class Server {
	static { Spark.port(RESTfulCraft.port); }
	public static int port = RESTfulCraft.port;
	public static World world;
	public static BlockPos pos;
	public static BlockState blockState;
	public static TileEntity tileEntity;
	private static boolean ONLINE = false;
	public MinecraftServer server;
	
	public Server(MinecraftServer server) {
		if (ONLINE) {
			throw new IllegalStateException("There is already a server running!");
		} else {
			Spark.init();
			Spark.internalServerError((req, res) -> { return error("An error occured."); });
			Spark.notFound((req, res) -> { return error("Route not found."); });
			Spark.before("/*", (req, res) -> {
				res.header("Content-Type", "application/json");
				if (req.headers("Authentication").isEmpty() || req.headers("Authentication").equals(RESTfulCraft.authKey)) {
					RESTfulCraft.LOGGER.info("{} - {} {}", req.ip(), req.requestMethod(), req.uri());
				} else {
					Spark.halt(401, Server.error("Authentication key invalid."));
				}
			});
			Spark.path("/api/v1/:mod/:dim", () -> {
				Spark.before("/*", (req, res) -> {
					ResourceLocation location = new ResourceLocation(req.params("mod"), req.params("dim"));
					DimensionType dimension = DimensionType.byName(location);
					if (dimension == null) {
						Spark.halt(404, error("Dimension '%s' not found.", location));
					} else {
						world = this.server.getWorld(dimension);
					}
				});
				Spark.before("/:x/:y/:z", (req, res) -> {
					int x = Integer.parseInt(req.params("x"));
					int y = Integer.parseInt(req.params("y"));
					int z = Integer.parseInt(req.params("z"));
					pos = new BlockPos(x, y, z);
					if (!World.isValid(pos)) {
						throw Spark.halt(400, error("Invalid target block position."));
					} else {
						tileEntity = world.getChunkAt(pos).getTileEntity(pos, Chunk.CreateEntityType.IMMEDIATE);
						blockState = world.getBlockState(pos);
					}
				});
				Spark.put("/:x/:y/:z", (req, res) -> {
					JsonObject json = CreateJSON.fromString(req.body());
					BlockState state = getBlockFromName(json.get("id")).getDefaultState();
					if (json.get("properties") != null) {
						StateContainer<Block, BlockState> container = state.getBlock().getStateContainer();
						JsonObject properties = (JsonObject) json.get("properties");
						for (Entry<String, JsonElement> entry : properties.entrySet()) {
							IProperty<?> property = container.getProperty(CreateJSON.withCamelCase(entry.getKey()));
							if (property == null) {
								Spark.halt(400, Server.error("Invalid property '%s'.", entry.getKey()));
							} else {
								state = parse(state, property, entry.getValue());
							}
						}
					}
					return CreateJSON.fromMap("placed", world.setBlockState(pos, state));
				});
				Spark.get("/:x/:y/:z", (req, res) -> {
					JsonObject json = new JsonObject(), properties = new JsonObject();
			   		json.addProperty("id", getNameFromBlock(blockState));
			   		ImmutableMap<IProperty<?>, Comparable<?>> map = blockState.getValues();
			   		for (Entry<IProperty<?>, Comparable<?>> entry : map.entrySet()) {
			   			IProperty<?> property = entry.getKey();
			   			properties.add(property.getName(), CreateJSON.fromObject(blockState.get(property)));
			   		}
			   		json.add("properties", properties);
			   		if (tileEntity != null) {
			   			json.add("tileEntity", CreateJSON.fromNBT(tileEntity.serializeNBT()));
			   		} else {
			   			json.add("tileEntity", null);
			   		}
			   		return json.toString();
				});
				Spark.delete("/:x/:y/:z", (req, res) -> {
					if (world.isAirBlock(pos)) {
						return Spark.halt(409, Server.error("Target block position unoccupied."));
					} else {
						return CreateJSON.fromMap("destroyed", world.destroyBlock(pos, true));
					}
				});
			});
			this.server = server;
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
	public static String error(String message, Object...objects) {
		return CreateJSON.fromMap("error", String.format(message, objects));
	}
	private static Block getBlockFromName(JsonElement name) {
		return Registry.BLOCK.getOrDefault(new ResourceLocation(name.getAsString()));
	}
	private static String getNameFromBlock(BlockState block) {
		return Registry.BLOCK.getKey(block.getBlock()).toString();
	}
	private static <S extends IStateHolder<S>, T extends Comparable<T>> S parse(S state, IProperty<T> property, JsonElement json) {
		Optional<T> value = property.parseValue(json.getAsString());
		if (value.isPresent()) {
			return (S)(state.with(property, (T)(value.get())));
		} else {
			return state;
		}
	}
}
