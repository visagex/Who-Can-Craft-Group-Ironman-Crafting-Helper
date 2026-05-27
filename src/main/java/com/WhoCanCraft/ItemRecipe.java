package com.WhoCanCraft;

import java.util.List;

class ItemRecipe
{
	final int itemId;
	final String itemName;
	final List<SkillRequirement> requirements;
	final List<MaterialRequirement> materials;

	ItemRecipe(int itemId, String itemName, List<SkillRequirement> requirements, List<MaterialRequirement> materials)
	{
		this.itemId = itemId;
		this.itemName = itemName;
		this.requirements = requirements;
		this.materials = materials;
	}
}
