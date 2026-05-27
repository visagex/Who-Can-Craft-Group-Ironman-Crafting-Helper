package com.WhoCanCraft;

import net.runelite.client.hiscore.HiscoreSkill;

class SkillRequirement
{
	final HiscoreSkill skill;
	final int level;
	final boolean boostable;

	SkillRequirement(HiscoreSkill skill, int level, boolean boostable)
	{
		this.skill = skill;
		this.level = level;
		this.boostable = boostable;
	}
}
