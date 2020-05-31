package restfulcraft.mod.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.LockableLootTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import restfulcraft.mod.http.CreateJSON;
import spark.Request;
import spark.Spark;

public class ContainerWrapper {
	public final World world;
	public JsonObject json;
	public BlockPos pos;
	public LockableLootTileEntity container;
	public int slot;
	
	public ContainerWrapper(Request req) {
		this.world = (World) req.attribute("world");
		this.json = (JsonObject) req.attribute("json");
		this.pos = (BlockPos) req.attribute("pos");
		TileEntity entity = this.world.getTileEntity(this.pos);
		if (entity instanceof LockableLootTileEntity) {
			this.container = (LockableLootTileEntity) entity;
		} else if (entity == null) {
			Spark.halt(404, CreateJSON.fromMap("error", "Tile entity not found."));
		} else {
			Spark.halt(422, CreateJSON.fromMap("error", "Selected tile entity is missing an inventory."));
		}
		if (req.params("slot") != null) {
			this.slot = Integer.parseInt(req.params("slot"));
		} else {
			this.slot = 0;
		}
	}
	public JsonElement read(int slot) {
		if (slot < this.container.getSizeInventory()) {
			ItemStack stack = this.container.getStackInSlot(slot);
			String json = CreateJSON.fromMap("slot", slot, "id", stack.getItem().getRegistryName(), "count", stack.getCount(), "name", stack.getDisplayName().getString());
			return CreateJSON.fromString(json);
		} else {
			Spark.halt(406, CreateJSON.fromMap("error", "Slot outside of container maximum bounds. (%d)", this.container.getSizeInventory()));
			return null;
		}
	}
	public JsonElement read() {
		return this.read(this.slot);
	}
	@SuppressWarnings("deprecation")
	public boolean replace(boolean replacing) {
		if (this.slot < this.container.getSizeInventory()) {
			ItemStack original = this.container.getStackInSlot(this.slot);
			ResourceLocation id = original.getItem().getRegistryName();
			if (original.isEmpty() || !replacing) {
				ItemStack stack = new ItemStack(Registry.ITEM.getOrDefault(id));
				if (this.json.get("id") != null) {
					stack = new ItemStack(Registry.ITEM.getOrDefault(new ResourceLocation(this.json.get("id").getAsString()))); 
				} if (this.json.get("count") != null) {
					stack.setCount(this.json.get("count").getAsInt());
				} if (this.json.get("name") != null) {
					stack.setDisplayName(new StringTextComponent(this.json.get("name").getAsString()));
				}
				return true;
			} else {
				Spark.halt(405, CreateJSON.fromMap("error", "Slot occupied."));
				return false;
			}
		} else {
			Spark.halt(406, CreateJSON.fromMap("error", "Slot outside of container maximum bounds. (%d)", this.container.getSizeInventory()));
			return false;
		}
	}
}