package com.WhoCanCraft;

import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.http.api.item.ItemPrice;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

class WhoCanCraftPanel extends PluginPanel
{
	private final JTextField searchField;
	private final JPanel contentPanel;
	private final ItemManager itemManager;
	private final Consumer<String> onSearch;

	private Consumer<ItemRecipe> itemSelectCallback;
	private List<ItemRecipe> lastResults;

	WhoCanCraftPanel(Consumer<String> onSearch, ItemManager itemManager)
	{
		super(false);
		this.onSearch = onSearch;
		this.itemManager = itemManager;

		setLayout(new BorderLayout(0, 8));
		setBorder(new EmptyBorder(10, 10, 10, 10));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel searchPanel = new JPanel(new BorderLayout(5, 0));
		searchPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		searchField = new JTextField();
		searchField.setToolTipText("Item name or partial name (e.g. dragon, leather)");
		searchField.addActionListener(e -> triggerSearch());

		JButton searchButton = new JButton("Search");
		searchButton.addActionListener(e -> triggerSearch());

		searchPanel.add(searchField, BorderLayout.CENTER);
		searchPanel.add(searchButton, BorderLayout.EAST);

		contentPanel = new JPanel();
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		contentPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JScrollPane scrollPane = new JScrollPane(contentPanel);
		scrollPane.setBorder(null);
		scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollPane.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		add(searchPanel, BorderLayout.NORTH);
		add(scrollPane, BorderLayout.CENTER);
	}

	private void triggerSearch()
	{
		String query = searchField.getText().trim();
		if (!query.isEmpty())
		{
			showLoading();
			onSearch.accept(query);
		}
	}

	void showLoading()
	{
		SwingUtilities.invokeLater(() ->
		{
			contentPanel.removeAll();
			JLabel label = new JLabel("Searching...");
			label.setForeground(Color.WHITE);
			label.setAlignmentX(Component.LEFT_ALIGNMENT);
			contentPanel.add(label);
			contentPanel.revalidate();
			contentPanel.repaint();
		});
	}

	void showError(String message)
	{
		SwingUtilities.invokeLater(() ->
		{
			contentPanel.removeAll();
			JLabel label = new JLabel("<html>" + message + "</html>");
			label.setForeground(Color.RED);
			label.setAlignmentX(Component.LEFT_ALIGNMENT);
			contentPanel.add(label);
			contentPanel.revalidate();
			contentPanel.repaint();
		});
	}

	void showItemList(List<ItemRecipe> items, Consumer<ItemRecipe> onSelect)
	{
		this.lastResults = items;
		this.itemSelectCallback = onSelect;

		SwingUtilities.invokeLater(() ->
		{
			contentPanel.removeAll();

			JLabel header = new JLabel(items.size() + " craftable item" + (items.size() == 1 ? "" : "s") + " found:");
			header.setForeground(Color.LIGHT_GRAY);
			header.setAlignmentX(Component.LEFT_ALIGNMENT);
			contentPanel.add(header);
			contentPanel.add(Box.createVerticalStrut(6));

			for (ItemRecipe recipe : items)
			{
				JPanel row = buildItemRow(recipe, onSelect);
				row.setAlignmentX(Component.LEFT_ALIGNMENT);
				contentPanel.add(row);
				contentPanel.add(Box.createVerticalStrut(2));
			}

			contentPanel.revalidate();
			contentPanel.repaint();
		});
	}

	private JPanel buildItemRow(ItemRecipe recipe, Consumer<ItemRecipe> onSelect)
	{
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(new EmptyBorder(4, 6, 4, 6));
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
		row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		JLabel iconLabel = new JLabel();
		iconLabel.setPreferredSize(new Dimension(36, 36));
		int itemId = recipe.itemId;
		if (itemId < 0)
		{
			List<ItemPrice> matches = itemManager.search(recipe.itemName);
			if (!matches.isEmpty())
			{
				itemId = matches.get(0).getId();
			}
		}
		if (itemId >= 0)
		{
			AsyncBufferedImage icon = itemManager.getImage(itemId, 1, false);
			icon.addTo(iconLabel);
		}

		JPanel textPanel = new JPanel();
		textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
		textPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JLabel nameLabel = new JLabel(recipe.itemName);
		nameLabel.setForeground(Color.WHITE);
		nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));

		JLabel reqLabel = new JLabel(buildReqSummary(recipe));
		reqLabel.setForeground(Color.LIGHT_GRAY);
		reqLabel.setFont(reqLabel.getFont().deriveFont(11f));

		textPanel.add(nameLabel);
		textPanel.add(reqLabel);

		row.add(iconLabel, BorderLayout.WEST);
		row.add(textPanel, BorderLayout.CENTER);

		row.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				row.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
				textPanel.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
				textPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			}

			@Override
			public void mouseClicked(MouseEvent e)
			{
				onSelect.accept(recipe);
			}
		});

		return row;
	}

	private String buildReqSummary(ItemRecipe recipe)
	{
		if (recipe.requirements.isEmpty())
		{
			return "No requirements";
		}
		StringBuilder sb = new StringBuilder();
		for (SkillRequirement req : recipe.requirements)
		{
			if (sb.length() > 0) sb.append(", ");
			sb.append(req.skill.getName()).append(" ").append(req.level);
		}
		return sb.toString();
	}

	void showMemberResults(ItemRecipe recipe, Map<String, MemberResult> memberResults, List<MaterialEntry> materials)
	{
		SwingUtilities.invokeLater(() ->
		{
			contentPanel.removeAll();

			if (lastResults != null && lastResults.size() > 1)
			{
				JButton backButton = new JButton("← Back to results");
				backButton.setAlignmentX(Component.LEFT_ALIGNMENT);
				backButton.addActionListener(e ->
				{
					if (lastResults != null && itemSelectCallback != null)
					{
						showItemList(lastResults, itemSelectCallback);
					}
				});
				contentPanel.add(backButton);
				contentPanel.add(Box.createVerticalStrut(8));
			}

			// Item name + icon
			JPanel titleRow = new JPanel(new BorderLayout(8, 0));
			titleRow.setBackground(ColorScheme.DARK_GRAY_COLOR);
			titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
			titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);

			JLabel iconLabel = new JLabel();
			iconLabel.setPreferredSize(new Dimension(36, 36));
			int itemId = recipe.itemId;
			if (itemId < 0)
			{
				List<ItemPrice> matches = itemManager.search(recipe.itemName);
				if (!matches.isEmpty())
				{
					itemId = matches.get(0).getId();
				}
			}
			if (itemId >= 0)
			{
				AsyncBufferedImage icon = itemManager.getImage(itemId, 1, false);
				icon.addTo(iconLabel);
			}

			JLabel nameLabel = new JLabel(recipe.itemName);
			nameLabel.setForeground(Color.WHITE);
			nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 14f));

			titleRow.add(iconLabel, BorderLayout.WEST);
			titleRow.add(nameLabel, BorderLayout.CENTER);
			contentPanel.add(titleRow);
			contentPanel.add(Box.createVerticalStrut(6));

			// Requirements
			if (recipe.requirements.isEmpty())
			{
				JLabel noReqs = new JLabel("No skill requirements");
				noReqs.setForeground(Color.LIGHT_GRAY);
				noReqs.setAlignmentX(Component.LEFT_ALIGNMENT);
				contentPanel.add(noReqs);
			}
			else
			{
				JLabel reqHeader = new JLabel("Requirements:");
				reqHeader.setForeground(Color.LIGHT_GRAY);
				reqHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
				contentPanel.add(reqHeader);

				for (SkillRequirement req : recipe.requirements)
				{
					String text = "  • " + req.skill.getName() + " " + req.level;
					if (req.boostable) text += " (boostable)";
					JLabel reqLabel = new JLabel(text);
					reqLabel.setForeground(Color.LIGHT_GRAY);
					reqLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
					contentPanel.add(reqLabel);
				}
			}

			// Materials
			if (!materials.isEmpty())
			{
				contentPanel.add(Box.createVerticalStrut(8));

				JSeparator matSep = new JSeparator();
				matSep.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
				matSep.setAlignmentX(Component.LEFT_ALIGNMENT);
				matSep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
				contentPanel.add(matSep);
				contentPanel.add(Box.createVerticalStrut(6));

				JLabel matHeader = new JLabel("Materials:");
				matHeader.setForeground(Color.WHITE);
				matHeader.setFont(matHeader.getFont().deriveFont(Font.BOLD));
				matHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
				contentPanel.add(matHeader);
				contentPanel.add(Box.createVerticalStrut(4));

				for (MaterialEntry mat : materials)
				{
					JPanel matRow = buildMaterialRow(mat);
					matRow.setAlignmentX(Component.LEFT_ALIGNMENT);
					contentPanel.add(matRow);
					contentPanel.add(Box.createVerticalStrut(2));
				}
			}

			contentPanel.add(Box.createVerticalStrut(10));

			JSeparator sep = new JSeparator();
			sep.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
			sep.setAlignmentX(Component.LEFT_ALIGNMENT);
			sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
			contentPanel.add(sep);
			contentPanel.add(Box.createVerticalStrut(8));

			JLabel membersHeader = new JLabel("Group Members:");
			membersHeader.setForeground(Color.WHITE);
			membersHeader.setFont(membersHeader.getFont().deriveFont(Font.BOLD));
			membersHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
			contentPanel.add(membersHeader);
			contentPanel.add(Box.createVerticalStrut(4));

			if (memberResults.isEmpty())
			{
				JLabel hint = new JLabel("<html>Log in or add member names<br>in the plugin settings.</html>");
				hint.setForeground(Color.GRAY);
				hint.setAlignmentX(Component.LEFT_ALIGNMENT);
				contentPanel.add(hint);
			}
			else
			{
				for (Map.Entry<String, MemberResult> entry : memberResults.entrySet())
				{
					JPanel row = buildMemberRow(entry.getKey(), entry.getValue());
					row.setAlignmentX(Component.LEFT_ALIGNMENT);
					contentPanel.add(row);
					contentPanel.add(Box.createVerticalStrut(2));
				}
			}

			contentPanel.revalidate();
			contentPanel.repaint();
		});
	}

	private JPanel buildMaterialRow(MaterialEntry mat)
	{
		boolean hasInInv = mat.inInventory >= mat.needed;
		boolean hasInBank = mat.inBank > 0;
		boolean hasInStorage = mat.inGroupStorage > 0;
		boolean showStorageHint = !hasInInv && (hasInBank || hasInStorage);

		JPanel panel = new JPanel(new BorderLayout(8, 0));
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		panel.setBorder(new EmptyBorder(3, 8, 3, 8));
		panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, showStorageHint ? 44 : 30));

		JLabel nameLabel = new JLabel(mat.name);
		nameLabel.setForeground(Color.LIGHT_GRAY);

		// Tooltip shows full breakdown
		StringBuilder tip = new StringBuilder("<html>");
		tip.append("Inventory: ").append(mat.inInventory).append("<br>");
		tip.append(mat.inBank >= 0 ? "Bank: " + mat.inBank : "Bank: not checked").append("<br>");
		tip.append(mat.inGroupStorage >= 0 ? "Group Storage: " + mat.inGroupStorage : "Group Storage: not checked");
		tip.append("</html>");
		nameLabel.setToolTipText(tip.toString());

		JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
		rightPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		String countText = mat.total() + " / " + mat.needed;
		JLabel countLabel = new JLabel(countText);
		countLabel.setForeground(mat.hasSufficient() ? new Color(0, 200, 0) : new Color(220, 50, 50));
		countLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
		countLabel.setToolTipText(tip.toString());
		rightPanel.add(countLabel);

		if (showStorageHint)
		{
			String hint = hasInBank && hasInStorage ? "In bank & storage"
				: hasInBank ? "In bank" : "In storage";
			JLabel hintLabel = new JLabel(hint);
			hintLabel.setForeground(new Color(100, 180, 255));
			hintLabel.setFont(hintLabel.getFont().deriveFont(10f));
			hintLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
			hintLabel.setToolTipText(tip.toString());
			rightPanel.add(hintLabel);
		}

		panel.add(nameLabel, BorderLayout.WEST);
		panel.add(rightPanel, BorderLayout.EAST);
		return panel;
	}

	private JPanel buildMemberRow(String name, MemberResult result)
	{
		JPanel panel = new JPanel(new BorderLayout(8, 0));
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		panel.setBorder(new EmptyBorder(4, 8, 4, 8));
		panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));

		JLabel nameLabel = new JLabel(name);
		nameLabel.setForeground(Color.WHITE);

		JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
		rightPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		switch (result.status)
		{
			case CAN_CRAFT:
			{
				JLabel statusLabel = new JLabel("✔ Can craft");
				statusLabel.setForeground(new Color(0, 200, 0));
				statusLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
				rightPanel.add(statusLabel);
				break;
			}
			case CANNOT_CRAFT:
			{
				JLabel statusLabel = new JLabel("✘ " + result.missingSkill.getName() + " " + result.requiredLevel);
				statusLabel.setForeground(new Color(220, 50, 50));
				statusLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

				JLabel subLabel = new JLabel("Current: " + result.currentLevel);
				subLabel.setForeground(Color.GRAY);
				subLabel.setFont(subLabel.getFont().deriveFont(14f));
				subLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

				rightPanel.add(statusLabel);
				rightPanel.add(subLabel);
				break;
			}
			default:
			{
				JLabel statusLabel = new JLabel("Unknown");
				statusLabel.setForeground(Color.GRAY);
				statusLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
				rightPanel.add(statusLabel);
				break;
			}
		}

		panel.add(nameLabel, BorderLayout.WEST);
		panel.add(rightPanel, BorderLayout.EAST);
		return panel;
	}
}
