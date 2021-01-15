package io.github.haykam821.dependencyoverridegenerator;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModDependency;
import net.fabricmc.loader.api.metadata.ModMetadata;

public class Main implements ModInitializer {
	private static final String MOD_ID = "dependencyoverridegenerator";
	private static final Logger LOGGER = LogManager.getLogger("Dependency Override Generator");
	private static final Path OUTPUT_PATH = Paths.get("generated/fabric_loader_dependencies.json");
	private static final Path MOVE_PATH = Paths.get("config/fabric_loader_dependencies.json");
	private static final Gson GSON = new GsonBuilder().create();

	public static boolean isOverrideNecessary(ModMetadata metadata) {
		// Prevent self from being included in generated overrides
		if (metadata.getId().equals(Main.MOD_ID)) return false;

		for (ModDependency dependency : metadata.getDepends()) {
			if (dependency.getModId().equals("minecraft")) {
				return true;
			}
		}
		return false;
	}

	public static JsonObject getOverride(ModMetadata metadata) {
		if (!Main.isOverrideNecessary(metadata)) return null;

		JsonObject dependsRemovals = new JsonObject();
		dependsRemovals.addProperty("minecraft", "");

		JsonObject override = new JsonObject();
		override.add("-depends", dependsRemovals);
		return override;
	}

	public static JsonObject getOverrides(FabricLoader loader) {
		JsonObject overrides = new JsonObject();
		for (ModContainer container : loader.getAllMods()) {
			ModMetadata metadata = container.getMetadata();
			JsonObject override = Main.getOverride(metadata);
			if (override != null) {
				overrides.add(metadata.getId(), override);
			}
		}

		return overrides;
	}

	public static JsonObject getDependencies(FabricLoader loader) {
		JsonObject dependencies = new JsonObject();

		dependencies.addProperty("version", 1);
		dependencies.add("overrides", Main.getOverrides(loader));

		return dependencies;
	}

	@Override
	public void onInitialize() {
		JsonObject dependencies = Main.getDependencies(FabricLoader.getInstance());
		String json = GSON.toJson(dependencies);

		try {
			Files.createDirectories(OUTPUT_PATH.getParent());

			BufferedWriter writer = Files.newBufferedWriter(OUTPUT_PATH);
			writer.write(json);
			writer.close();

			LOGGER.info("Wrote dependencies file at {}; move it to {} to use it", OUTPUT_PATH, MOVE_PATH);
		} catch (IOException exception) {
			LOGGER.warn("Failed to write dependencies file at {}:", OUTPUT_PATH, exception);
		}
	}
}