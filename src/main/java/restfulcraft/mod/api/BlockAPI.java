package restfulcraft.mod.api;

import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.ListNBT;
import net.minecraft.state.IProperty;
import net.minecraft.state.IStateHolder;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import restfulcraft.mod.http.CreateJSON;
import spark.Request;
import spark.Route;
import spark.Spark;

public class BlockAPI {
	public static final String QUERY_URL = "/:x/:y/:z";
	
	public static final Route GET = (req, res) -> {
		JsonObject json = new JsonObject(), properties = new JsonObject();
		Instance inst = new Instance(req);
   		json.addProperty("id", inst.getName(inst.blockState.getBlock()));
   		ImmutableMap<IProperty<?>, Comparable<?>> map = inst.blockState.getValues();
   		for (Entry<IProperty<?>, Comparable<?>> entry : map.entrySet()) {
   			IProperty<?> property = entry.getKey();
   			properties.add(property.getName(), CreateJSON.fromObject(inst.blockState.get(property).toString()));
   		}
   		json.add("properties", properties);
   		if (inst.tileEntity != null) {
   			json.add("tileEntity", CreateJSON.fromNBT(inst.tileEntity.serializeNBT()));
   		} else {
   			json.add("tileEntity", null);
   		}
   		json.add("entities", CreateJSON.fromNBT(inst.entities));
   		return json.toString();
	};
	public static final Route PUT = (req, res) -> {
		return CreateJSON.fromMap("replaced", new Instance(req).placeBlock(true));
	};
	public static final Route POST = (req, res) -> {
		return CreateJSON.fromMap("placed", new Instance(req).placeBlock(false));
	};
	public static final Route PATCH = (req, res) -> {
		return CreateJSON.fromMap("updated", new Instance(req).updateBlock());
	};
	public static final Route DELETE = (req, res) -> {
		Instance inst = new Instance(req);
		if (inst.world.isAirBlock(inst.pos)) {
			return Spark.halt(404, CreateJSON.fromMap("error", "Block not found."));
		} else {
			return CreateJSON.fromMap("broken", inst.breakBlock());
		}
	};
	public static class Instance {
		public World world;
		public JsonObject json;
		public BlockPos pos;
		public BlockState blockState;
		public TileEntity tileEntity;
		public ListNBT entities = new ListNBT();
		
		public Instance(Request req) {
			this.world = (World) req.attribute("world");
			this.json = (JsonObject) req.attribute("json");
			this.pos = (BlockPos) req.attribute("pos");
			this.blockState = this.world.getBlockState(this.pos);
			this.tileEntity = this.world.getChunkAt(this.pos).getTileEntity(this.pos);
			List<Entity> entities = this.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(this.pos, this.pos.add(1, 2, 1)));
			for (Entity entity : entities) {
				this.entities.add(entity.serializeNBT());
			}
		}
		public boolean placeBlock(boolean replacing) {
			if (this.json != null) {
				try {
					Block block = getBlock(this.json.get("id").getAsString());
					if (replacing || this.world.isAirBlock(this.pos)) {
						this.json = this.json.getAsJsonObject("properties");
						return this.updateBlock(block.getDefaultState());
					} else {
						Spark.halt(405, CreateJSON.fromMap("error", "Block occupied."));
					}
				} catch (IllegalArgumentException e) {
					Spark.halt(422, CreateJSON.fromMap("error", e.getMessage()));
				} catch (NullPointerException e) {
					Spark.halt(405, CreateJSON.fromMap("error", "Block ID missing."));
				}
			} else {
				Spark.halt(406, CreateJSON.fromMap("error", "Request body missing."));
			}
			return false;
		}
		public boolean updateBlock(BlockState state) {
			state = this.setStateWithJSON(state.getBlock(), state, this.json);
			return this.world.setBlockState(this.pos, state);
		}
		public boolean updateBlock() {
			return this.updateBlock(this.world.getBlockState(this.pos));
		}
		public boolean breakBlock() {
			return this.world.destroyBlock(this.pos, true);
		}
		private BlockState setStateWithJSON(Block block, BlockState state, JsonObject json) {
			if (json != null) {
				StateContainer<Block, BlockState> container = block.getStateContainer();
				for (Entry<String, JsonElement> entry : json.entrySet()) {
					IProperty<?> property = container.getProperty(CreateJSON.withCamelCase(entry.getKey()));
					if (property == null) {
						throw new IllegalArgumentException(String.format("Invalid property '%s'.", entry.getKey()));
					} else {
						state = mutate(state, property, entry.getValue());
					}
				}
			}
			return state;
		}
		private <S extends IStateHolder<S>, T extends Comparable<T>> S mutate(S state, IProperty<T> property, JsonElement json) {
			Optional<T> value = property.parseValue(json.getAsString());
			if (value.isPresent()) {
				return (S)(state.with(property, (T)(value.get())));
			} else {
				return state;
			}
		}
		@SuppressWarnings("deprecation")
		private Block getBlock(String name) {
			return Registry.BLOCK.getOrDefault(new ResourceLocation(name));
		}
		@SuppressWarnings("deprecation")
		private String getName(Block block) {
			return Registry.BLOCK.getKey(block).toString();
		}
	}
}
