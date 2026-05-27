package com.WhoCanCraft;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.hiscore.HiscoreEndpoint;

@ConfigGroup("whocancraft")
public interface WhoCanCraftConfig extends Config
{
	@ConfigItem(
		keyName = "groupMembers",
		name = "Group Members",
		description = "Comma-separated list of group member names to check (besides yourself)"
	)
	default String groupMembers()
	{
		return "";
	}

	@ConfigItem(
		keyName = "hiscoreEndpoint",
		name = "Hiscore Type",
		description = "Which hiscore table to use when looking up member skill levels"
	)
	default HiscoreEndpoint hiscoreEndpoint()
	{
		return HiscoreEndpoint.IRONMAN;
	}
}
