package restfulcraft.mod;

import java.util.HashMap;
import java.util.UUID;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.CaseFormat;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import net.minecraft.nbt.CollectionNBT;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.DoubleNBT;
import net.minecraft.nbt.FloatNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.NumberNBT;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.config.ModConfig.ModConfigEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;

@Mod("restfulcraft")
public class RESTfulCraft {
    public static final Logger LOGGER = LogManager.getLogger();
    public static final Gson GSON = new Gson();
    public static Server server;

    public RESTfulCraft() {
    	ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        MinecraftForge.EVENT_BUS.register(this);
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
    	public static String AUTH_KEY = "";
    	public static int PORT = 56552;
    	
    	public final ForgeConfigSpec.ConfigValue<String> authKey;
    	public final ForgeConfigSpec.IntValue port;
    	
    	public Config(ForgeConfigSpec.Builder builder) {
    		builder.push("server");
    		this.authKey = builder.translation("restfulcraft.config.authKey").define("authKey", "");
    		this.port = builder.translation("restfulcraft.config.port").defineInRange("port", 56552, 1, 65535);
    		builder.pop();
    	}
        @SubscribeEvent
        public static void onModConfig(ModConfigEvent e) {
        	Config.AUTH_KEY = Config.INSTANCE.authKey.get();
        	Config.PORT = Config.INSTANCE.port.get();
    	}
    }
    public static class JsonHelper {
		private static final JsonParser PARSER = new JsonParser();
		public static HashMap<String, Object> map(String key, Object value, Object...pairs) {
			HashMap<String, Object> map = new HashMap<String, Object>();
			map.put(key, value);
			for (int i = 0; i < pairs.length; i += 2) {
				map.put(pairs[i].toString(), pairs[i + 1]);
			}
			return map;
		}
		public static String stringify(Object object) {
			return RESTfulCraft.GSON.toJson(object);
		}
		public static String post(String key, Object value, Object...pairs) {
			return stringify(map(key, value, pairs));
		}
		public static JsonObject parse(String input) {
			return (JsonObject) PARSER.parse(input);
		}
		public static JsonObject toJson(CompoundNBT compound) {
			JsonObject json = new JsonObject();
			for (String key : compound.keySet()) {
				if (key.endsWith("Least") || key.endsWith("Most")) {
					String uuidKey = key.replaceAll("Least|Most$", "");
					UUID uuid = compound.getUniqueId(uuidKey);
					json.addProperty(format(uuidKey), uuid.toString());
				} else {
					json.add(format(key), objectify(compound.get(key)));
				}
			}
			return json;
		}
		public static JsonArray toJson(CollectionNBT<?> list) {
			JsonArray json = new JsonArray();
			for (int i = 0; i < list.size(); ++i) {
				json.add(objectify((INBT) list.get(i)));
			}
			return json;
		}
		public static JsonElement objectify(Object object) {
			return RESTfulCraft.GSON.toJsonTree(object);
		}
		public static JsonElement objectify(INBT object) {
			switch (INBT.NBT_TYPES[object.getId()]) {
			case "SHORT": case "INT": case "LONG": case "BYTE": case "FLOAT": case "DOUBLE":
				NumberNBT number = (NumberNBT) object;
				if (object instanceof FloatNBT && object instanceof DoubleNBT) {
					return new JsonPrimitive(number.getFloat());
				} else {
					return new JsonPrimitive(number.getInt());
				}
			case "BYTE[]": case "INT[]": case "LONG[]": case "LIST":
				return toJson((CollectionNBT<?>) object);
			case "COMPOUND":
				return toJson((CompoundNBT) object);
			default:
				return new JsonPrimitive(object.getString());
			}
		}
		public static String format(String key) {
			return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, key);
		}
    }
}
