package restfulcraft.mod.api;

import restfulcraft.mod.http.CreateJSON;
import spark.Route;
import spark.Spark;

public class CommandRouter {
	public static final Route GET = (req, res) -> {
		return Spark.halt(501, CreateJSON.fromMap("error", "Function not implemented."));
	};
	public static final Route PUT = (req, res) -> {
		return Spark.halt(501, CreateJSON.fromMap("error", "Function not implemented."));
	};
	public static final Route POST = (req, res) -> {
		return Spark.halt(501, CreateJSON.fromMap("error", "Function not implemented."));
	};
	public static final Route PATCH = (req, res) -> {
		return Spark.halt(501, CreateJSON.fromMap("error", "Function not implemented."));
	};
	public static final Route DELETE = (req, res) -> {
		return Spark.halt(501, CreateJSON.fromMap("error", "Function not implemented."));
	};
	public static final Route INDEX = (req, res) -> {
		return Spark.halt(501, CreateJSON.fromMap("error", "Function not implemented."));
	};
}
