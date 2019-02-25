package team.creativecode.diamail.events;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import team.creativecode.diamail.Main;
import team.creativecode.diamail.manager.Menu;
import team.creativecode.diamail.manager.PlayerData;
import team.creativecode.diamail.manager.mail.Mail;
import team.creativecode.diamail.utils.DataConverter;
import team.creativecode.diamail.utils.Updater;

public class BasicEvent implements Listener {

	Main plugin = Main.getPlugin(Main.class);
	File folder = new File(plugin.getDataFolder() + "/PlayerData");
	
	@EventHandler
	public void join(PlayerJoinEvent event) {
		Player p = event.getPlayer();
		new PlayerData(event.getPlayer());
		
		if (event.getPlayer().hasPermission("diamail.admin")) {
			String newversion = Updater.getUpdateCheck(Main.rsid);
			Main.placeholder.inputData("version_new", newversion);
			if (newversion.equals("")) {
				Main.lang.sendMessage(p, Main.placeholder.useAsList(Main.lang.getMessages().get("alert.updater-failed")));
			}
			else if (Updater.isNewVersion()) {
				Main.lang.sendMessage(p, Main.placeholder.useAsList(Main.lang.getMessages().get("alert.updater")));
			}else{
				Main.lang.sendMessage(p,  Main.placeholder.useAsList(Main.lang.getMessages().get("alert.updater-latest")));
			}
		}
	}
	
	@EventHandler
	public void invclick(InventoryClickEvent event) {
		Player p = (Player) event.getWhoClicked();
		if (Menu.singleMenu.containsKey(p)) {
			Menu m = Menu.singleMenu.get(p);
			if (event.getClickedInventory().equals(m.getMenu())) {
				event.setCancelled(true);
				DataConverter.playSoundByString(p.getLocation(), plugin.getConfig().getString("settings.inventory-interact-sound"));
				m.action(p, event.getSlot(), event.getClick());
			}
		}
	}
	
	@SuppressWarnings("deprecation")
	@EventHandler
	public void chat(AsyncPlayerChatEvent event) {

		Player p = event.getPlayer();
		
		if (Mail.sending.containsKey(p)) {
			Mail m = Mail.sending.get(p);
			String msg = event.getMessage();
			event.setCancelled(true);
			
			if (msg.equalsIgnoreCase("SENDALL")) {
				if (p.hasPermission("diamail.access.sendall") || p.isOp()) {
					m.setSendAll(!m.isSendAll());
				}else {
					m.getPlayerData().getLanguage().sendMessage(p, m.getPlayerData().getLanguage().getMessages().get("alert.no-permission"));
				}
			}else if (msg.equalsIgnoreCase("SETITEM")) {
				m.setItem(p.getInventory().getItemInMainHand());
			}else if (msg.equalsIgnoreCase("SEND")) {
				m.send();
				return;
			}else if (msg.equalsIgnoreCase("EXIT")) {
				m.delete(true);
				return;
			}else {
				try {
					if (!m.getReceiver().equals(null)) {
						if (m.getReceiver().hasPlayedBefore()) {
							m.addMessage(msg);
						}else {
							m.setReceiver(null);
						}
					}
				}catch(Exception e) {
					m.setReceiver(Bukkit.getOfflinePlayer(msg));
				}
			}
			if (m.isSendAll() && !m.getItem().getType().equals(Material.AIR)) {
				if (!p.hasPermission("diamail.access.sendall") || !p.isOp()) {
					p.getInventory().addItem(m.getItem());
					m.setItem(new ItemStack(Material.AIR));
					m.getPlayerData().getLanguage().sendMessage(p, m.getPlayerData().getLanguage().getMessages().get("alert.no-permission"));
				}
			}
			
			m.showButton();
		}
		
	}

}