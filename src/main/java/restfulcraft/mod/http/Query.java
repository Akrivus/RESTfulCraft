package restfulcraft.mod.http;

import java.util.List;

import com.google.gson.JsonObject;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import spark.Request;

public class Query {
	public final World world;
	public JsonObject json;
	public BlockPos pos;
	public ListNBT entities;
	public BlockState blockState;
	public TileEntity tileEntity;
	
	public Query(World world) {
		this.world = world;
	}
	public void init(BlockPos pos) {
		this.tileEntity = this.world.getChunkAt(pos).getTileEntity(pos);
		this.blockState = this.world.getBlockState(pos);
		this.entities = getEntities(pos);
		this.pos = pos;
	}
	private ListNBT getEntities(BlockPos pos) {
		ListNBT list = new ListNBT();
		List<Entity> entities = this.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(pos, pos.add(1, 1, 2)));
		for (Entity entity : entities) {
			list.add(entity.serializeNBT());
		}
		return list;
	}
	public static Query get(Request req) {
		return (Query) req.attribute("query");
	}
}
