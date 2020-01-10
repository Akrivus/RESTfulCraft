package restfulcraft.mod;

import java.util.HashMap;
import java.util.UUID;

import com.google.common.base.CaseFormat;
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

public class CreateJSON {
	private static final JsonParser PARSER = new JsonParser();
	public static JsonObject fromString(String input) {
		return (JsonObject) PARSER.parse(input);
	}
	public static String toString(Object object) {
		return RESTfulCraft.GSON.toJson(object);
	}
	public static JsonElement fromObject(Object object) {
		return RESTfulCraft.GSON.toJsonTree(object);
	}
	public static String fromMap(String key, Object value, Object...pairs) {
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put(key, value);
		for (int i = 0; i < pairs.length; i += 2) {
			map.put(pairs[i].toString(), pairs[i + 1]);
		}
		return toString(map);
	}
	public static JsonElement fromNBT(INBT object) {
		switch (INBT.NBT_TYPES[object.getId()]) {
		case "SHORT": case "INT": case "LONG": case "BYTE": case "FLOAT": case "DOUBLE":
			NumberNBT num = (NumberNBT) object;
			if (object instanceof FloatNBT && object instanceof DoubleNBT) {
				return new JsonPrimitive(num.getFloat());
			} else {
				return new JsonPrimitive(num.getInt());
			}
		case "BYTE[]": case "INT[]": case "LONG[]": case "LIST":
			CollectionNBT<?> col = (CollectionNBT<?>) object;
			JsonArray arr = new JsonArray();
			for (int i = 0; i < col.size(); ++i) {
				arr.add(fromNBT(col.get(i)));
			}
			return arr;
		case "COMPOUND":
			CompoundNBT com = (CompoundNBT) object;
			JsonObject obj = new JsonObject();
			for (String key : com.keySet()) {
				if (key.endsWith("Least") || key.endsWith("Most")) {
					String uuidKey = key.replaceAll("Least|Most$", "");
					UUID uuid = com.getUniqueId(uuidKey);
					obj.addProperty(withCamelCase(uuidKey), uuid.toString());
				} else {
					obj.add(withCamelCase(key), fromNBT(com.get(key)));
				}
			}
			return obj;
		default:
			return new JsonPrimitive(object.getString());
		}
	}
	public static String withCamelCase(String key) {
		return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, key);
	}
}