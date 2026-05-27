package com.WhoCanCraft;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Skill;
import net.runelite.api.clan.ClanID;
import net.runelite.api.clan.ClanMember;
import net.runelite.api.clan.ClanSettings;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.hiscore.HiscoreClient;
import net.runelite.client.hiscore.HiscoreEndpoint;
import net.runelite.client.hiscore.HiscoreResult;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.http.api.item.ItemPrice;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@PluginDescriptor(
	name = "Who Can Craft"
)
public class WhoCanCraftPlugin extends Plugin
{
	@Inject private Client client;
	@Inject private ClientThread clientThread;
	@Inject private ClientToolbar clientToolbar;
	@Inject private OkHttpClient okHttpClient;
	@Inject private Gson gson;
	@Inject private HiscoreClient hiscoreClient;
	@Inject private ItemManager itemManager;
	@Inject private WhoCanCraftConfig config;

	private WhoCanCraftPanel panel;
	private NavigationButton navButton;

	// Cached item containers — populated whenever the player opens them
	private volatile ItemContainer cachedBank;
	private volatile ItemContainer cachedGroupStorage;

	private static final Pattern SKILL_NAME = Pattern.compile("\\|\\s*skill(\\d+)\\s*=\\s*([^|\\n}]+)");
	private static final Pattern SKILL_LEVEL = Pattern.compile("\\|\\s*skill(\\d+)lvl\\s*=\\s*(\\d+)");
	private static final Pattern MAT_NAME = Pattern.compile("\\|\\s*mat(\\d+)\\s*=\\s*([^|\\n}]+)");
	private static final Pattern MAT_QTY = Pattern.compile("\\|\\s*mat(\\d+)(?:qty|quantity)\\s*=\\s*([\\d,]+)");
	private static final Pattern ITEM_ID = Pattern.compile("\\|\\s*ids?\\d*\\s*=\\s*([\\d,\\s]+)");
	private static final Pattern WIKILINK = Pattern.compile("\\[\\[(?:[^|\\]]*\\|)?([^\\]]+)\\]\\]");

	private static final Map<String, HiscoreSkill> WIKI_SKILL_NAMES = new LinkedHashMap<>();
	private static final Map<HiscoreSkill, Skill> HISCORE_TO_API_SKILL = new EnumMap<>(HiscoreSkill.class);

	static
	{
		WIKI_SKILL_NAMES.put("attack", HiscoreSkill.ATTACK);
		WIKI_SKILL_NAMES.put("strength", HiscoreSkill.STRENGTH);
		WIKI_SKILL_NAMES.put("defence", HiscoreSkill.DEFENCE);
		WIKI_SKILL_NAMES.put("ranged", HiscoreSkill.RANGED);
		WIKI_SKILL_NAMES.put("prayer", HiscoreSkill.PRAYER);
		WIKI_SKILL_NAMES.put("magic", HiscoreSkill.MAGIC);
		WIKI_SKILL_NAMES.put("runecraft", HiscoreSkill.RUNECRAFT);
		WIKI_SKILL_NAMES.put("runecrafting", HiscoreSkill.RUNECRAFT);
		WIKI_SKILL_NAMES.put("hitpoints", HiscoreSkill.HITPOINTS);
		WIKI_SKILL_NAMES.put("crafting", HiscoreSkill.CRAFTING);
		WIKI_SKILL_NAMES.put("mining", HiscoreSkill.MINING);
		WIKI_SKILL_NAMES.put("smithing", HiscoreSkill.SMITHING);
		WIKI_SKILL_NAMES.put("fishing", HiscoreSkill.FISHING);
		WIKI_SKILL_NAMES.put("cooking", HiscoreSkill.COOKING);
		WIKI_SKILL_NAMES.put("firemaking", HiscoreSkill.FIREMAKING);
		WIKI_SKILL_NAMES.put("woodcutting", HiscoreSkill.WOODCUTTING);
		WIKI_SKILL_NAMES.put("agility", HiscoreSkill.AGILITY);
		WIKI_SKILL_NAMES.put("herblore", HiscoreSkill.HERBLORE);
		WIKI_SKILL_NAMES.put("thieving", HiscoreSkill.THIEVING);
		WIKI_SKILL_NAMES.put("fletching", HiscoreSkill.FLETCHING);
		WIKI_SKILL_NAMES.put("slayer", HiscoreSkill.SLAYER);
		WIKI_SKILL_NAMES.put("farming", HiscoreSkill.FARMING);
		WIKI_SKILL_NAMES.put("construction", HiscoreSkill.CONSTRUCTION);
		WIKI_SKILL_NAMES.put("hunter", HiscoreSkill.HUNTER);

		HISCORE_TO_API_SKILL.put(HiscoreSkill.ATTACK, Skill.ATTACK);
		HISCORE_TO_API_SKILL.put(HiscoreSkill.STRENGTH, Skill.STRENGTH);
		HISCORE_TO_API_SKILL.put(HiscoreSkill.DEFENCE, Skill.DEFENCE);
		HISCORE_TO_API_SKILL.put(HiscoreSkill.RANGED, Skill.RANGED);
		HISCORE_TO_API_SKILL.put(HiscoreSkill.PRAYER, Skill.PRAYER);
		HISCORE_TO_API_SKILL.put(HiscoreSkill.MAGIC, Skill.MAGIC);
		HISCORE_TO_API_SKILL.put(HiscoreSkill.RUNECRAFT, Skill.RUNECRAFT);
		HISCORE_TO_API_SKILL.put(HiscoreSkill.HITPOINTS, Skill.HITPOINTS);
		HISCORE_TO_API_SKILL.put(HiscoreSkill.CRAFTING, Skill.CRAFTING);
		HISCORE_TO_API_SKILL.put(HiscoreSkill.MINING, Skill.MINING);
		HISCORE_TO_API_SKILL.put(HiscoreSkill.SMITHING, Skill.SMITHING);
		HISCORE_TO_API_SKILL.put(HiscoreSkill.FISHING, Skill.FISHING);
		HISCORE_TO_API_SKILL.put(HiscoreSkill.COOKING, Skill.COOKING);
		HISCORE_TO_API_SKILL.put(HiscoreSkill.FIREMAKING, Skill.FIREMAKING);
		HISCORE_TO_API_SKILL.put(HiscoreSkill.WOODCUTTING, Skill.WOODCUTTING);
		HISCORE_TO_API_SKILL.put(HiscoreSkill.AGILITY, Skill.AGILITY);
		HISCORE_TO_API_SKILL.put(HiscoreSkill.HERBLORE, Skill.HERBLORE);
		HISCORE_TO_API_SKILL.put(HiscoreSkill.THIEVING, Skill.THIEVING);
		HISCORE_TO_API_SKILL.put(HiscoreSkill.FLETCHING, Skill.FLETCHING);
		HISCORE_TO_API_SKILL.put(HiscoreSkill.SLAYER, Skill.SLAYER);
		HISCORE_TO_API_SKILL.put(HiscoreSkill.FARMING, Skill.FARMING);
		HISCORE_TO_API_SKILL.put(HiscoreSkill.CONSTRUCTION, Skill.CONSTRUCTION);
		HISCORE_TO_API_SKILL.put(HiscoreSkill.HUNTER, Skill.HUNTER);
	}

	@Override
	protected void startUp()
	{
		panel = new WhoCanCraftPanel(this::handleSearch, itemManager);
		navButton = NavigationButton.builder()
			.tooltip("Who Can Craft")
			.icon(buildIcon())
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navButton);
		panel = null;
		navButton = null;
		cachedBank = null;
		cachedGroupStorage = null;
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		int id = event.getContainerId();
		if (id == InventoryID.BANK.getId())
		{
			cachedBank = event.getItemContainer();
		}
		else if (id == InventoryID.GROUP_STORAGE.getId() || id == InventoryID.GROUP_STORAGE_INV.getId())
		{
			cachedGroupStorage = event.getItemContainer();
		}
	}

	private void handleSearch(String query)
	{
		searchCraftableItems(query)
			.thenAccept(results ->
			{
				if (results.isEmpty())
				{
					panel.showError("No craftable items found matching: <b>" + query + "</b>"
						+ "<br><small>Try a different spelling or more of the item name.</small>");
					return;
				}
				if (results.size() == 1)
				{
					handleItemSelect(results.get(0));
				}
				else
				{
					panel.showItemList(results, this::handleItemSelect);
				}
			})
			.exceptionally(ex ->
			{
				log.debug("Error searching for '{}'", query, ex);
				panel.showError("Error: " + ex.getMessage());
				return null;
			});
	}

	private void handleItemSelect(ItemRecipe recipe)
	{
		panel.showLoading();

		// Capture everything that needs the client thread before going async
		clientThread.invoke(() ->
		{
			String localName = client.getLocalPlayer() != null
				? client.getLocalPlayer().getName() : null;
			Map<HiscoreSkill, Integer> localLevels = captureLocalLevels();
			List<String> allMembers = resolveGroupMembers(localName);
			List<MaterialEntry> materials = checkMaterials(recipe);

			lookupAllMembers(recipe, allMembers, localName, localLevels)
				.thenAccept(memberResults ->
					panel.showMemberResults(recipe, memberResults, materials));
		});
	}

	/** Returns all group members to check: local player first, then GIM clan (or config fallback). */
	private List<String> resolveGroupMembers(String localName)
	{
		List<String> members = new ArrayList<>();
		if (localName != null) members.add(localName);

		ClanSettings gim = client.getClanSettings(ClanID.GROUP_IRONMAN);
		if (gim != null)
		{
			for (ClanMember m : gim.getMembers())
			{
				String name = m.getName();
				if (name != null && !name.equals(localName) && !members.contains(name))
				{
					members.add(name);
				}
			}
		}
		else
		{
			// Fall back to manually configured names
			String configValue = config.groupMembers().trim();
			if (!configValue.isEmpty())
			{
				for (String name : configValue.split(","))
				{
					String trimmed = name.trim();
					if (!trimmed.isEmpty() && !members.contains(trimmed))
					{
						members.add(trimmed);
					}
				}
			}
		}

		return members;
	}

	/** Checks how many of each required material the player has across all containers. */
	private List<MaterialEntry> checkMaterials(ItemRecipe recipe)
	{
		ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
		List<MaterialEntry> entries = new ArrayList<>();

		for (MaterialRequirement mat : recipe.materials)
		{
			int itemId = findItemId(mat.name);
			int inInv = countItem(inventory, itemId);
			int inBank = cachedBank != null ? countItem(cachedBank, itemId) : -1;
			int inStorage = cachedGroupStorage != null ? countItem(cachedGroupStorage, itemId) : -1;
			entries.add(new MaterialEntry(mat.name, mat.quantity, inInv, inBank, inStorage));
		}

		return entries;
	}

	private int findItemId(String name)
	{
		List<ItemPrice> matches = itemManager.search(name);
		for (ItemPrice p : matches)
		{
			if (p.getName().equalsIgnoreCase(name)) return p.getId();
		}
		return matches.isEmpty() ? -1 : matches.get(0).getId();
	}

	private int countItem(ItemContainer container, int targetId)
	{
		if (container == null || targetId < 0) return 0;
		int total = 0;
		for (Item item : container.getItems())
		{
			if (item.getId() < 0) continue;
			int canonical = itemManager.canonicalize(item.getId());
			if (canonical == targetId || item.getId() == targetId)
			{
				total += item.getQuantity();
			}
		}
		return total;
	}

	// ── Wiki API ─────────────────────────────────────────────────────────────

	private CompletableFuture<List<String>> searchPageTitles(String query)
	{
		String url;
		try
		{
			url = "https://oldschool.runescape.wiki/api.php?action=query&list=search"
				+ "&srsearch=" + URLEncoder.encode(query, StandardCharsets.UTF_8.name())
				+ "&srnamespace=0&srlimit=15&format=json";
		}
		catch (Exception e)
		{
			return CompletableFuture.completedFuture(Collections.emptyList());
		}

		CompletableFuture<List<String>> future = new CompletableFuture<>();
		okHttpClient.newCall(new Request.Builder()
			.url(url).header("User-Agent", "WhoCanCraft RuneLite Plugin").build())
			.enqueue(new Callback()
			{
				@Override
				public void onFailure(Call call, IOException e) { future.completeExceptionally(e); }

				@Override
				public void onResponse(Call call, Response response)
				{
					try (response)
					{
						if (!response.isSuccessful() || response.body() == null)
						{
							future.complete(Collections.emptyList());
							return;
						}
						List<String> titles = new ArrayList<>();
						JsonArray search = gson.fromJson(response.body().string(), JsonObject.class)
							.getAsJsonObject("query").getAsJsonArray("search");
						for (JsonElement el : search)
						{
							titles.add(el.getAsJsonObject().get("title").getAsString());
						}
						future.complete(titles);
					}
					catch (Exception e) { future.completeExceptionally(e); }
				}
			});

		return future;
	}

	private CompletableFuture<Map<String, String>> fetchWikitexts(List<String> titles)
	{
		String titlesParam = titles.stream()
			.map(t -> {
				try { return URLEncoder.encode(t, StandardCharsets.UTF_8.name()); }
				catch (Exception e) { return t; }
			})
			.collect(Collectors.joining("%7C"));

		String url = "https://oldschool.runescape.wiki/api.php?action=query"
			+ "&titles=" + titlesParam
			+ "&prop=revisions&rvprop=content&rvslots=main&format=json";

		CompletableFuture<Map<String, String>> future = new CompletableFuture<>();
		okHttpClient.newCall(new Request.Builder()
			.url(url).header("User-Agent", "WhoCanCraft RuneLite Plugin").build())
			.enqueue(new Callback()
			{
				@Override
				public void onFailure(Call call, IOException e) { future.completeExceptionally(e); }

				@Override
				public void onResponse(Call call, Response response)
				{
					try (response)
					{
						if (!response.isSuccessful() || response.body() == null)
						{
							future.complete(Collections.emptyMap());
							return;
						}
						Map<String, String> wikitexts = new LinkedHashMap<>();
						JsonObject pages = gson.fromJson(response.body().string(), JsonObject.class)
							.getAsJsonObject("query").getAsJsonObject("pages");
						for (Map.Entry<String, JsonElement> entry : pages.entrySet())
						{
							JsonObject page = entry.getValue().getAsJsonObject();
							if (!page.has("revisions")) continue;
							String title = page.get("title").getAsString();
							JsonObject slots = page.getAsJsonArray("revisions")
								.get(0).getAsJsonObject().getAsJsonObject("slots");
							if (slots == null) continue;
							JsonObject main = slots.getAsJsonObject("main");
							if (main == null || !main.has("*")) continue;
							wikitexts.put(title, main.get("*").getAsString());
						}
						future.complete(wikitexts);
					}
					catch (Exception e) { future.completeExceptionally(e); }
				}
			});

		return future;
	}

	private CompletableFuture<List<ItemRecipe>> searchCraftableItems(String query)
	{
		return searchPageTitles(query)
			.thenCompose(titles ->
			{
				if (titles.isEmpty())
				{
					return CompletableFuture.completedFuture(Collections.<ItemRecipe>emptyList());
				}
				return fetchWikitexts(titles).thenApply(this::parseWikitexts);
			});
	}

	// ── Wikitext parsing ──────────────────────────────────────────────────────

	private List<ItemRecipe> parseWikitexts(Map<String, String> wikitexts)
	{
		List<ItemRecipe> recipes = new ArrayList<>();
		for (Map.Entry<String, String> entry : wikitexts.entrySet())
		{
			ItemRecipe recipe = parsePageWikitext(entry.getKey(), entry.getValue());
			if (recipe != null) recipes.add(recipe);
		}
		return recipes;
	}

	private ItemRecipe parsePageWikitext(String title, String wikitext)
	{
		int itemId = -1;
		Matcher idMatcher = ITEM_ID.matcher(wikitext);
		if (idMatcher.find())
		{
			try { itemId = Integer.parseInt(idMatcher.group(1).trim().split("[,\\s]+")[0]); }
			catch (NumberFormatException ignored) {}
		}

		int recipeStart = wikitext.indexOf("{{Recipe");
		if (recipeStart < 0) return null;

		// Walk braces to find the end of the first Recipe template
		int depth = 0, end = recipeStart;
		for (int i = recipeStart; i < wikitext.length() - 1; i++)
		{
			char c = wikitext.charAt(i), n = wikitext.charAt(i + 1);
			if (c == '{' && n == '{') { depth++; i++; }
			else if (c == '}' && n == '}') { depth--; i++; if (depth == 0) { end = i + 1; break; } }
		}

		String block = wikitext.substring(recipeStart, end);

		// Skills
		Map<Integer, String> skillNames = new HashMap<>();
		Map<Integer, Integer> skillLevels = new HashMap<>();
		Matcher sn = SKILL_NAME.matcher(block);
		while (sn.find())
		{
			try { skillNames.put(Integer.parseInt(sn.group(1)), cleanWikilink(sn.group(2))); }
			catch (NumberFormatException ignored) {}
		}
		Matcher sl = SKILL_LEVEL.matcher(block);
		while (sl.find())
		{
			try { skillLevels.put(Integer.parseInt(sl.group(1)), Integer.parseInt(sl.group(2).trim())); }
			catch (NumberFormatException ignored) {}
		}

		List<SkillRequirement> requirements = new ArrayList<>();
		for (Map.Entry<Integer, String> e : skillNames.entrySet())
		{
			HiscoreSkill skill = WIKI_SKILL_NAMES.get(e.getValue().toLowerCase());
			if (skill == null) continue;
			requirements.add(new SkillRequirement(skill, skillLevels.getOrDefault(e.getKey(), 1), false));
		}

		// Materials
		Map<Integer, String> matNames = new HashMap<>();
		Map<Integer, Integer> matQtys = new HashMap<>();
		Matcher mn = MAT_NAME.matcher(block);
		while (mn.find())
		{
			try { matNames.put(Integer.parseInt(mn.group(1)), cleanWikilink(mn.group(2))); }
			catch (NumberFormatException ignored) {}
		}
		Matcher mq = MAT_QTY.matcher(block);
		while (mq.find())
		{
			try
			{
				String qtyStr = mq.group(2).trim().replaceAll(",", "");
				matQtys.put(Integer.parseInt(mq.group(1)), Integer.parseInt(qtyStr));
			}
			catch (NumberFormatException ignored) {}
		}

		List<MaterialRequirement> materials = new ArrayList<>();
		for (Map.Entry<Integer, String> e : matNames.entrySet())
		{
			materials.add(new MaterialRequirement(e.getValue(), matQtys.getOrDefault(e.getKey(), 1)));
		}

		if (requirements.isEmpty() && materials.isEmpty()) return null;

		return new ItemRecipe(itemId, title, requirements, materials);
	}

	private static String cleanWikilink(String s)
	{
		Matcher m = WIKILINK.matcher(s.trim());
		if (m.find()) return m.group(1).trim();
		return s.trim();
	}

	// ── Member skill lookup ───────────────────────────────────────────────────

	private CompletableFuture<Map<String, MemberResult>> lookupAllMembers(
		ItemRecipe recipe,
		List<String> members,
		String localName,
		Map<HiscoreSkill, Integer> localLevels)
	{
		Map<String, MemberResult> results = Collections.synchronizedMap(new LinkedHashMap<>());
		for (String m : members) results.put(m, MemberResult.unknown());

		List<CompletableFuture<Void>> futures = new ArrayList<>();
		HiscoreEndpoint endpoint = config.hiscoreEndpoint();

		for (String member : members)
		{
			if (member.equals(localName))
			{
				results.put(member, checkLevels(recipe, localLevels));
			}
			else
			{
				CompletableFuture<HiscoreResult> lookup = hiscoreClient.lookupAsync(member, endpoint)
					.exceptionally(ex -> { log.debug("Hiscore lookup failed for {} on {}", member, endpoint, ex); return null; });

				if (endpoint != HiscoreEndpoint.NORMAL)
				{
					lookup = lookup.thenCompose(hr ->
					{
						if (hr != null) return CompletableFuture.completedFuture(hr);
						return hiscoreClient.lookupAsync(member, HiscoreEndpoint.NORMAL)
							.exceptionally(ex -> { log.debug("Fallback hiscore lookup failed for {}", member, ex); return null; });
					});
				}

				CompletableFuture<Void> f = lookup.thenAccept(hr -> results.put(member, checkHiscore(recipe, hr)));
				futures.add(f);
			}
		}

		return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
			.thenApply(v -> results);
	}

	private MemberResult checkLevels(ItemRecipe recipe, Map<HiscoreSkill, Integer> levels)
	{
		for (SkillRequirement req : recipe.requirements)
		{
			if (levels.getOrDefault(req.skill, 0) < req.level)
				return MemberResult.cannotCraft(req.skill, req.level, levels.getOrDefault(req.skill, 0));
		}
		return MemberResult.canCraft();
	}

	private MemberResult checkHiscore(ItemRecipe recipe, HiscoreResult hr)
	{
		if (hr == null) return MemberResult.unknown();
		for (SkillRequirement req : recipe.requirements)
		{
			net.runelite.client.hiscore.Skill s = hr.getSkill(req.skill);
			if (s == null || s.getLevel() < req.level)
				return MemberResult.cannotCraft(req.skill, req.level, s.getLevel());
		}
		return MemberResult.canCraft();
	}

	private Map<HiscoreSkill, Integer> captureLocalLevels()
	{
		Map<HiscoreSkill, Integer> levels = new EnumMap<>(HiscoreSkill.class);
		for (Map.Entry<HiscoreSkill, Skill> e : HISCORE_TO_API_SKILL.entrySet())
			levels.put(e.getKey(), client.getRealSkillLevel(e.getValue()));
		return levels;
	}

	private static BufferedImage buildIcon()
	{
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setColor(new Color(197, 163, 90));
		g.fillRoundRect(0, 0, 15, 15, 4, 4);
		g.setColor(Color.BLACK);
		g.setFont(new Font("SansSerif", Font.BOLD, 10));
		g.drawString("C", 4, 12);
		g.dispose();
		return image;
	}

	@Provides
	WhoCanCraftConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(WhoCanCraftConfig.class);
	}
}
