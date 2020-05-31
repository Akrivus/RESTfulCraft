package restfulcraft.mod.init;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.block.Block;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.config.ModConfig.ModConfigEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import restfulcraft.mod.block.RequestBlock;
import restfulcraft.mod.block.RequestBlock.RequestMethod;

@Mod("restfulcraft")
public class RESTfulCraft {
    public static final Logger LOGGER = LogManager.getLogger();
    public static Server server;
	public static String authKey = "";
	public static boolean formatSnakeCase = false;
	public static int port = 56552;

    public RESTfulCraft() {
    	ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    	IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        Blocks.REGISTRY.register(bus);
    }
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerLoggedInEvent e) {
    	if (RESTfulCraft.server.isOnline()) {
    		e.getPlayer().sendMessage(new TranslationTextComponent("restfulcraft.commands.server.online", Server.port));
    	} else {
    		e.getPlayer().sendMessage(new TranslationTextComponent("restfulcraft.commands.server.offline"));
    	}
    }
    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent e) {
		RESTfulCraft.server = new Server(e.getServer());
    }
    @SubscribeEvent
    public void onServerStopped(FMLServerStoppedEvent e) {
    	RESTfulCraft.server.stop();
	}
    @EventBusSubscriber
    public static class Config {
    	public static final Pair<Config, ForgeConfigSpec> PAIR = new ForgeConfigSpec.Builder().configure(Config::new);
    	public static final ForgeConfigSpec SPEC = PAIR.getRight();
    	public static final Config INSTANCE = PAIR.getLeft();
    	
    	public final ForgeConfigSpec.ConfigValue<String> authKey;
    	public final ForgeConfigSpec.BooleanValue formatSnakeCase;
    	public final ForgeConfigSpec.IntValue port;
    	
    	public Config(ForgeConfigSpec.Builder builder) {
    		builder.push("server");
    		this.authKey = builder.translation("restfulcraft.config.authKey").define("authKey", "");
    		this.formatSnakeCase = builder.translation("restfulcraft.config.formatSnakeCase").define("formatSnakeCase", false);
    		this.port = builder.translation("restfulcraft.config.port").defineInRange("port", 25595, 1, 65535);
    		builder.pop();
    	}
        @SubscribeEvent
        public static void onModConfig(ModConfigEvent e) {
        	RESTfulCraft.authKey = Config.INSTANCE.authKey.get();
        	RESTfulCraft.formatSnakeCase = Config.INSTANCE.formatSnakeCase.get();
        	RESTfulCraft.port = Config.INSTANCE.port.get();
    	}
    }
    public static class Blocks {
    	public static final DeferredRegister<Block> REGISTRY = new DeferredRegister<Block>(ForgeRegistries.BLOCKS, "restfulcraft");
    	public static final RegistryObject<RequestBlock> POST = REGISTRY.register("post_request", () -> new RequestBlock(RequestMethod.POST));
    	public static final RegistryObject<RequestBlock> GET = REGISTRY.register("get_request", () -> new RequestBlock(RequestMethod.GET));
    	public static final RegistryObject<RequestBlock> PATCH = REGISTRY.register("patch_request", () -> new RequestBlock(RequestMethod.PATCH));
    	public static final RegistryObject<RequestBlock> PUT = REGISTRY.register("put_request", () -> new RequestBlock(RequestMethod.PUT));
    	public static final RegistryObject<RequestBlock> DELETE = REGISTRY.register("delete_request", () -> new RequestBlock(RequestMethod.DELETE));
    }
}
