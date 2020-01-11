package restfulcraft.mod.http;

import java.util.HashMap;
import java.util.UUID;

import com.google.common.base.CaseFormat;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import net.minecraft.nbt.CollectionNBT;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.DoubleNBT;
import net.minecraft.nbt.FloatNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.NumberNBT;
import restfulcraft.mod.init.RESTfulCraft;

public class CreateJSON {
	private static final JsonParser PARSER = new JsonParser();
    private static final Gson GSON = new Gson();
    
	/**
	 * Parses the provided <code>String</code> into a <code>JsonObject.</code>
	 * @param input
	 * @return
	 */
	public static JsonObject fromString(String input) {
		return (JsonObject) PARSER.parse(input);
	}
	/**
	 * Serializes the provided <code>Object</code> into a JSON string.
	 * @param input
	 * @return
	 */
	public static String toString(Object input) {
		return GSON.toJson(input);
	}
	/**
	 * Serializes the provided <code>Object</code> into JSON.
	 * @param input
	 * @return
	 */
	public static JsonElement fromObject(Object input) {
		return GSON.toJsonTree(input);
	}
	/**
	 * Converts an array of objects into a <code>Map</code>, and serializes it into JSON.
	 * Used as a convenience method for generating basic status responses.
	 * @param key
	 * @param value
	 * @param pairs
	 * @return
	 */
	public static String fromMap(String key, Object value, Object...pairs) {
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put(key, value);
		for (int i = 0; i < pairs.length; i += 2) {
			map.put(pairs[i].toString(), pairs[i + 1]);
		}
		return toString(map);
	}
	/**
	 * Converts NBT objects into proper JSON. <code>CompoundNBT#toString()</code> does not
	 * produce proper JSON, so we needed to make our own helper method for it.
	 * @param input
	 * @return
	 */
	public static JsonElement fromNBT(INBT input) {
		switch (INBT.NBT_TYPES[input.getId()]) {
		case "SHORT": case "INT": case "LONG": case "BYTE": case "FLOAT": case "DOUBLE":
			NumberNBT num = (NumberNBT) input;
			if (input instanceof FloatNBT && input instanceof DoubleNBT) {
				return new JsonPrimitive(num.getFloat());
			} else {
				return new JsonPrimitive(num.getInt());
			}
		case "BYTE[]": case "INT[]": case "LONG[]": case "LIST":
			CollectionNBT<?> col = (CollectionNBT<?>) input;
			JsonArray arr = new JsonArray();
			for (int i = 0; i < col.size(); ++i) {
				arr.add(fromNBT(col.get(i)));
			}
			return arr;
		case "COMPOUND":
			CompoundNBT com = (CompoundNBT) input;
			JsonObject obj = new JsonObject();
			for (String key : com.keySet()) {
				// uuids are stored in 2 values, so i'm combining them
				if (key.endsWith("Least") || key.endsWith("Most")) {
					String uuidKey = key.replaceAll("Least|Most$", "");
					UUID uuid = com.getUniqueId(uuidKey);
					obj.addProperty(reformat(uuidKey), uuid.toString());
				} else {
					obj.add(reformat(key), fromNBT(com.get(key)));
				}
			}
			return obj;
		default:
			return new JsonPrimitive(input.getString());
		}
	}
	/**
	 * Converts <code>PascalCase</code> into <code>camelCase</code> or <code>snake_case</code> for
	 * standardizing the atypical case format that NBT uses in Minecraft's mobile and tile entities.
	 * @param input
	 * @return
	 */
	public static String reformat(String input) {
		if (input.matches("^[A-Z]+$")) {
			return input.toLowerCase();
		} else if (RESTfulCraft.formatSnakeCase) {
			return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, input);
		} else {
			return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, input);
		}
	}
	/**
	 * Checks if JSON is valid and returns <code>true</code> if so.
	 * @param input
	 * @return
	 */
	public static boolean isValid(String input) {
		try {
			fromString(input);
			return true;
		} catch (JsonParseException e) {
			return false;
		}
	}
}