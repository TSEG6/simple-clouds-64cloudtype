package dev.nonamecrackers2.simpleclouds.client.packet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.nonamecrackers2.simpleclouds.client.cloud.ClientSideCloudTypeManager;
import dev.nonamecrackers2.simpleclouds.client.mesh.multiregion.MultiRegionCloudMeshGenerator;
import dev.nonamecrackers2.simpleclouds.client.renderer.SimpleCloudsRenderer;
import dev.nonamecrackers2.simpleclouds.client.world.ClientCloudManager;
import dev.nonamecrackers2.simpleclouds.common.config.SimpleCloudsConfig;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.SendCloudManagerPacket;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.SendCloudTypesPacket;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.UpdateCloudManagerPacket;
import dev.nonamecrackers2.simpleclouds.common.world.CloudManager;
import net.minecraft.client.Minecraft;

public class SimpleCloudsClientPacketHandler
{
	private static final Logger LOGGER = LogManager.getLogger();
	
	public static void handleUpdateCloudManagerPacket(UpdateCloudManagerPacket packet)
	{
		Minecraft mc = Minecraft.getInstance();
		CloudManager manager = CloudManager.get(mc.level);
		handleUpdateCloudManagerPacket(packet, manager);
		//LOGGER.debug("Updating client-side cloud manager");
	}
	
	public static void handleUpdateCloudManagerPacket(UpdateCloudManagerPacket packet, CloudManager manager)
	{
		manager.setScrollX(packet.scrollX);
		manager.setScrollY(packet.scrollY);
		manager.setScrollZ(packet.scrollZ);
		manager.setDirection(packet.direction);
		manager.setSpeed(packet.speed);
		manager.setCloudHeight(packet.cloudHeight);
		if (manager instanceof ClientCloudManager clientManager)
			clientManager.setReceivedSync();
	}
	
	public static void handleSendCloudManagerPacket(SendCloudManagerPacket packet)
	{
		Minecraft mc = Minecraft.getInstance();
		CloudManager manager = CloudManager.get(mc.level);
		handleUpdateCloudManagerPacket(packet, manager);
		manager.setSeed(packet.seed);
		manager.setRegionGenerator(packet.type);
		SimpleCloudsRenderer renderer = SimpleCloudsRenderer.getInstance();
		if (SimpleCloudsConfig.SERVER_SPEC.isLoaded())
		{
			if (SimpleCloudsConfig.SERVER.cloudMode.get() != renderer.getCurrentCloudMode() || packet.type != renderer.getCurrentRegionGenerator())
			{
				LOGGER.debug("Looks like the server cloud mode or region generator does not match with the client. Requesting a reload...");
				renderer.requestReload();
			}
		}
		else
		{
			LOGGER.warn("Server spec is not loaded");
		}
		LOGGER.debug("Received cloud manager info");
	}
	
	public static void handleCloudTypesPacket(SendCloudTypesPacket packet)
	{
		LOGGER.debug("Received {} synced cloud types", packet.types.size());
		ClientSideCloudTypeManager.getInstance().receiveSynced(packet.types, packet.indexed);
		if (SimpleCloudsRenderer.getInstance().getMeshGenerator() instanceof MultiRegionCloudMeshGenerator meshGenerator)
		{
			if (packet.types.size() > MultiRegionCloudMeshGenerator.MAX_CLOUD_TYPES)
				LOGGER.warn("The amount of loaded cloud types exceeds the maximum of {}. Please be aware that not all cloud types loaded will be used.", MultiRegionCloudMeshGenerator.MAX_CLOUD_TYPES);
			else
				meshGenerator.setCloudTypes(packet.indexed);
		}
	}
}
