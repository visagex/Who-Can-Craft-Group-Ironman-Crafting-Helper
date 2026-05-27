# Who Can Craft

A RuneLite plugin for Group Ironman players that lets you search for craftable items and instantly see who in your
group has the skill levels and materials needed to make them.

## Features

- **Item search** — Type any item name (partial or full) to find craftable items that match
- **Skill requirements** — See the required skill levels for any item at a glance
- **Group member check** — Automatically detects your GIM group members and shows who can and cannot craft the item,
  including their current level and how many levels they still need
- **Material tracking** — Shows how many of each required material you have across your inventory, bank, and group
  storage
- **Bank & storage hints** — If you don't have a material on you but it's in your bank or group storage, the plugin
  highlights that so you know where to go

## How to Use

1. Open the **Who Can Craft** panel from the RuneLite sidebar (look for the "C" icon)
2. Type an item name in the search box and press Enter or click Search
3. If multiple items match, click one to view its details
4. The detail view shows:
    - Skill requirements
    - Materials needed with your current counts (green = sufficient, red = insufficient)
    - All group members with a ✔ or ✘ indicating whether they meet the skill requirements

## Notes

- Bank and group storage counts only appear after you have opened those containers during your current session
- Group members are detected automatically from your GIM clan. If you are not in a GIM group, you can manually enter
  member names (comma-separated) in the plugin settings
- Hiscore lookups are used for group members other than yourself. If a member does not appear on the configured
  hiscore type, the plugin will automatically fall back to the normal hiscores
- Crafting data is pulled live from the OSRS Wiki

## Configuration

| Setting | Description |
  |---|---|
| Group Members | Comma-separated list of member names (fallback if GIM clan is not detected) |
| Hiscore Type | Which hiscore leaderboard to use for member lookups (default: Ironman) |