package com.WhoCanCraft;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class WhoCanCraftPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(WhoCanCraftPlugin.class);
		RuneLite.main(args);
	}
}