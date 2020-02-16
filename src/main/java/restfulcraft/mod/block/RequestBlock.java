package restfulcraft.mod.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;

public class RequestBlock extends Block {
	public enum RequestMethod {
		GET(MaterialColor.GREEN), PUT(MaterialColor.BLUE), POST(MaterialColor.YELLOW), PATCH(MaterialColor.GRAY), DELETE(MaterialColor.RED);
		public final MaterialColor color;
		RequestMethod(MaterialColor color) {
			this.color = color;
		}
	}
	public final RequestMethod method;
	public RequestBlock(RequestMethod method) {
		super(Properties.create(Material.ROCK, method.color));
		this.method = method;
	}
}
