package team.creativecode.diamail.manager.mail;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import team.creativecode.diamail.Main;
import team.creativecode.diamail.manager.PlayerData;
import team.creativecode.diamail.manager.event.mail.MailReceiveEvent;
import team.creativecode.diamail.manager.event.mail.MailSendEvent;
import team.creativecode.diamail.manager.menu.MailShow;
import team.creativecode.diamail.utils.ConfigManager;
import team.creativecode.diamail.utils.DataConverter;
import team.creativecode.diamail.utils.ItemManager;
import team.creativecode.diamail.utils.Language;
import team.creativecode.diamail.utils.Language.SendMode;
import team.creativecode.diamail.utils.Placeholder;

public class Mail{
	
	public enum MailType{
		INBOX, OUTBOX;
	}
	
	public enum MailMode{
		SENDALL, RECEIVER, ITEM, MESSAGE;
	}
	
	public static HashMap<Player, Mail> sending = new HashMap<Player, Mail>();
	
	Main plugin = Main.getPlugin(Main.class);
	
	private Placeholder plc = new Placeholder();
	private MailMode mailmode = MailMode.SENDALL;
	
	private Calendar date;
	private String displayDate = "00:00 - ?/?/????";
	private PlayerData pd = null;
	private String uuid = null;
	private OfflinePlayer sender;
	private OfflinePlayer receiver = null;
	private List<OfflinePlayer> multiReceiver = new ArrayList<OfflinePlayer>();
	private boolean isSendAll = false;
	private boolean isMultiSend = false;
	private List<String> msg = new ArrayList<String>();
	private ItemStack item = new ItemStack(Material.AIR);
	
	// For creating mail
	public Mail(PlayerData pd, boolean isSendAll) {
		this.uuid = UUID.randomUUID().toString();
		this.sender = pd.getPlayer();
		this.isSendAll = isSendAll;
		this.pd = pd;
		this.date = Calendar.getInstance();
		this.displayDate = date.get(Calendar.HOUR_OF_DAY) + ":" + date.get(Calendar.MINUTE) + " - " + date.get(Calendar.DATE) + "/" + date.get(Calendar.MONTH) + "/" + date.get(Calendar.YEAR);
		
	 	
		sending.put(pd.getPlayer().getPlayer(), this);
		String n = isSendAll ? pd.getLanguage().getMessages().get("alert.notification-send-pre").get(1) : pd.getLanguage().getMessages().get("alert.notification-send-pre").get(0);
		n = pd.getPlaceholder().use(n);
		initPlaceholder();
		if (pd.getPlayer().isOnline()) {
			try{
				pd.getPlayer().getPlayer().sendMessage(" ");
				pd.getLanguage().sendDirectMessage(pd.getPlayer().getPlayer(), n);
				if (isSendAll == true) {
					showButton();
				}
			}catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	// For getting mail data
	public Mail(PlayerData pd, String uuid, MailType mt) {
		this.uuid = uuid;
		this.pd = pd;
		String path = "mailbox." + mt.toString().toLowerCase() + "." + uuid;
		File file = pd.getFile();
		FileConfiguration config = YamlConfiguration.loadConfiguration(file);
		
		this.msg = ConfigManager.contains(file, path + ".message") ? config.getStringList(path + ".message") : new ArrayList<String>();
		this.item = ConfigManager.contains(file, path + ".item") ? config.getItemStack(path + ".item") : new ItemStack(Material.AIR);
		this.displayDate = ConfigManager.contains(file, path + ".date") ? config.getString(path + ".date") : "?/??/????";
		
		try {
			initPlaceholder();
			try {
				if (config.contains(path + ".sender")) {
					this.sender = Bukkit.getOfflinePlayer(UUID.fromString(config.getString(path + ".sender")));
				}
				if (config.contains(path + ".target")) {
					this.receiver = Bukkit.getOfflinePlayer(UUID.fromString(config.getString(path + ".target")));
				}
			}catch(Exception e) {
				System.out.println("Error when loading mail [" + uuid +"] (" + pd.getFile().getName() + ")");
			}
		}catch(Exception e) {e.printStackTrace();}
	}
	
	public void showButton() {
		try{
			boolean check = false;
			try {
				check = getReceiver() != null || isSendAll() || !getMultipleReceiver().isEmpty();
			}catch(Exception e) {
				check = isSendAll();
			}
			TextComponent send = new TextComponent(pd.getLanguage().getMessages().get("alert.notification-send-button").get(0)),
			exit = new TextComponent(pd.getLanguage().getMessages().get("alert.notification-send-button").get(1)),
			item = new TextComponent(pd.getLanguage().getMessages().get("alert.notification-send-button").get(2)),
			sendall = new TextComponent(pd.getLanguage().getMessages().get("alert.notification-send-button").get(3));
			
			send.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "SEND"));
			send.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
					new ComponentBuilder(ChatColor.translateAlternateColorCodes('&', (getMessage().size() > 0 && (check)) ? "&7[ &a&l✔ &7]" : "&7[ &c&l✘ &7]")).create()));
			
			exit.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "EXIT"));
			
			item.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "SETITEM"));
			item.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
					new ComponentBuilder(ChatColor.translateAlternateColorCodes('&',
							(check) ? 
									"&8[ &f" +  (this.getItem().getType().equals(Material.AIR) ? "None" : ((this.getItem().getItemMeta().hasDisplayName() ? this.getItem().getItemMeta().getDisplayName()
											: this.getItem().getType().name()))) + " &8]" : "&8[ &fNone &8]")).create()));
			
			sendall.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "SENDALL"));
			sendall.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
					new ComponentBuilder(ChatColor.translateAlternateColorCodes('&', (this.isSendAll()) ? "&7[ &a&l✔ &7]" : "&7[ &c&l✘ &7]")).create()));
			
			if (this.getPlayerData().getPlayer().isOnline()) {
				pd.getPlayer().getPlayer().sendMessage(" ");
				this.getPlayerData().getPlayer().getPlayer().spigot().sendMessage(send,exit,item,sendall);
				pd.getPlayer().getPlayer().sendMessage(" ");
			}
		}catch(Exception e) {e.printStackTrace();}
	}
	
	public void showInGui(Player targetplayer) {
		MailShow show = new MailShow();
		show.inputObject("player", targetplayer);
		show.inputObject("mail", this);
		show.open(targetplayer);
	}
	
	public void takeItem(Player p) {
		PlayerData rec = new PlayerData(this.getReceiver());
		try {
			ItemStack item = getItem();
			rec.getConfig().set("mailbox.inbox." + this.getMailUUID() + ".item", null);
			rec.getConfig().save(rec.getFile());
			p.getInventory().addItem(item);
			rec.getLanguage().sendMessage(receiver.getPlayer(), SendMode.valueOf(rec.getPlayerSetting().getSettings().get("notification-display").toString().toUpperCase()),
					getPlayerData().getPlaceholder().useAsList(
							getPlayerData().getLanguage().getMessages().get("alert.notification-take")));
		}catch(Exception e) {
			rec.getLanguage().sendMessage(receiver.getPlayer(), SendMode.valueOf(rec.getPlayerSetting().getSettings().get("notification-display").toString().toUpperCase()),
					getPlayerData().getPlaceholder().useAsList(
							getPlayerData().getLanguage().getMessages().get("alert.notification-take")));
		}
	}
	
	private void sendWriteData(PlayerData receiver) {
		
		OfflinePlayer receive = receiver.getPlayer();
		String inbox = "mailbox.inbox." + this.getMailUUID(), outbox = "mailbox.outbox." + this.getMailUUID();
		
		if (!this.getSender().getName().equals(receive.getName())) {
			ConfigManager.input(this.getPlayerData().getFile(), outbox + ".date", this.getDate());
			ConfigManager.input(this.getPlayerData().getFile(), outbox + ".target", receive.getUniqueId().toString());
			ConfigManager.input(this.getPlayerData().getFile(), outbox + ".sender",this.getPlayerData().getPlayer().getUniqueId().toString());
			ConfigManager.input(this.getPlayerData().getFile(), outbox + ".message",this.getMessage());
			if (!this.getItem().getType().equals(Material.AIR)) {
				ConfigManager.input(this.getPlayerData().getFile(), outbox + ".item",this.getItem().getType().equals(Material.AIR) ? null : this.getItem());
			}
		}
		
		ConfigManager.input(receiver.getFile(), inbox + ".date", this.getDate());
		ConfigManager.input(receiver.getFile(), inbox + ".sender", this.getPlayerData().getPlayer().getUniqueId().toString());
		ConfigManager.input(receiver.getFile(), inbox + ".target", receive.getUniqueId().toString());
		ConfigManager.input(receiver.getFile(), inbox + ".message", this.getMessage());
		if (!this.getItem().getType().equals(Material.AIR)) {
			ConfigManager.input(receiver.getFile(), inbox + ".item", this.getItem().getType().equals(Material.AIR) ? null : this.getItem());
		}
		
		if (receive.isOnline()) {
			DataConverter.playSoundByString(receive.getPlayer().getLocation(), plugin.getConfig().getString("settings.notification-mail-send"));
			receiver.getLanguage().sendMessage(receive.getPlayer(), SendMode.valueOf(receiver.getPlayerSetting().getSettings().get("notification-display").toString().toUpperCase()),
					receiver.getPlaceholder().useAsList(
							receiver.getLanguage().getMessages().get("alert.notification-send-receive")));
		}
		sending.remove(this.getPlayerData().getPlayer());
	}
	
	@SuppressWarnings("unused")
	private void sendWriteData(OfflinePlayer receive) {
		sendWriteData(new PlayerData(receive));
	}
	
	public void send() {
		Language lang = this.getPlayerData().getLanguage();
		if (this.getSender().isOnline()) {
			if (!this.getSender().getPlayer().hasPermission("diamail.access.send")) {
				lang.sendMessage(getSender().getPlayer(), this.getPlaceholder().useAsList(lang.getMessages().get("alert.no-permission")));
				return;
			}
		}
		
		List<OfflinePlayer> receivers = new ArrayList<>();
		if (getReceiver() != null && this.getMultipleReceiver().isEmpty()) {
			receivers.add(getReceiver());
		}else if (!this.getMultipleReceiver().isEmpty()) {
			receivers.addAll(this.getMultipleReceiver());
		}
		MailSendEvent sendEvent = new MailSendEvent(this, getSender(), receivers, isSendAll(), this.getPlayerData());
		if (this.getMessage().size() <= 0) {
			lang.sendMessage(pd.getPlayer().getPlayer(), SendMode.valueOf(getPlayerData().getPlayerSetting().getSettings().get("notification-display").toString().toUpperCase()),
					getPlayerData().getPlaceholder().useAsList(lang.getMessages().get("alert.notification-send-failed")));
			this.showButton();
			return;
		}
		else if (this.isSendAll() == true) {
			File[] file = PlayerData.folder.listFiles();
			String path = "mailbox.inbox." + this.getMailUUID();
			Bukkit.getServer().getPluginManager().callEvent(sendEvent);
			for (File f : file) {
				OfflinePlayer op = Bukkit.getOfflinePlayer(UUID.fromString(f.getName().split("[.]")[0]));
				if (plugin.getConfig().getBoolean("sendall-settings.online-only")) {
					if (!op.isOnline()) {
						continue;
					}
				}
				ConfigManager.input(f, path + ".date", this.getDate());
				ConfigManager.input(f, path + ".sender", this.getSender().getUniqueId().toString());
				ConfigManager.input(f, path + ".target", f.getName().split("[.]")[0]);
				ConfigManager.input(f, path + ".item", this.getItem().getType().equals(Material.AIR) ? null : this.getItem());
				ConfigManager.input(f, path + ".message", this.getMessage());
				PlayerData l = new PlayerData(op);
				MailReceiveEvent receiveEvent = new MailReceiveEvent(this, op, l);
				Bukkit.getServer().getPluginManager().callEvent(receiveEvent);
				sendWriteData(l);
				if (op.isOnline()) {
					DataConverter.playSoundByString(op.getPlayer().getPlayer().getLocation(), plugin.getConfig().getString("settings.notification-mail-send"));
					lang.sendMessage(op.getPlayer(), SendMode.valueOf(getPlayerData().getPlayerSetting().getSettings().get("notification-display").toString().toUpperCase()),
							this.getPlaceholder().useAsList(getPlayerData().getPlaceholder().useAsList(lang.getMessages().get("alert.notification-send-receive"))));
				}
			}
			this.plc.inputData("mail_sendall_size", file.length + "");
			sending.remove(this.getPlayerData().getPlayer());
			if (pd.getPlayer().isOnline()) {
				DataConverter.playSoundByString(pd.getPlayer().getPlayer().getLocation(), plugin.getConfig().getString("settings.notification-mail-send"));
				lang.sendMessage(pd.getPlayer().getPlayer(),SendMode.valueOf(getPlayerData().getPlayerSetting().getSettings().get("notification-display").toString().toUpperCase()),
						this.getPlaceholder().useAsList(getPlayerData().getPlaceholder().useAsList(lang.getMessages().get("alert.notification-sendall-try"))));
			}
		}
		else if (this.isSendAll() == false && (this.getReceiver() != null || this.getReceiver().hasPlayedBefore())) {
			PlayerData l = new PlayerData(getReceiver());
			MailReceiveEvent receiveEvent = new MailReceiveEvent(this, getReceiver(), l);
			Bukkit.getServer().getPluginManager().callEvent(receiveEvent);
			Bukkit.getServer().getPluginManager().callEvent(sendEvent);
			sendWriteData(l);
			if (pd.getPlayer().isOnline()) {
				DataConverter.playSoundByString(pd.getPlayer().getPlayer().getLocation(), plugin.getConfig().getString("settings.notification-mail-send"));
				lang.sendMessage(pd.getPlayer().getPlayer(), SendMode.valueOf(getPlayerData().getPlayerSetting().getSettings().get("notification-display").toString().toUpperCase()),
						getPlayerData().getPlaceholder().useAsList(
								lang.getMessages().get("alert.notification-send-try")));
			}
		}else if (this.isSendAll == false && !this.getMultipleReceiver().isEmpty()){
			Bukkit.getServer().getPluginManager().callEvent(sendEvent);
			for (OfflinePlayer op : this.getMultipleReceiver()) {
				PlayerData l = new PlayerData(op);
				MailReceiveEvent receiveEvent = new MailReceiveEvent(this, op, l);
				Bukkit.getServer().getPluginManager().callEvent(receiveEvent);
				sendWriteData(l);
			}
			if (pd.getPlayer().isOnline()) {
				DataConverter.playSoundByString(pd.getPlayer().getPlayer().getLocation(), plugin.getConfig().getString("settings.notification-mail-send"));
				lang.sendMessage(pd.getPlayer().getPlayer(), SendMode.valueOf(getPlayerData().getPlayerSetting().getSettings().get("notification-display").toString().toUpperCase()),
						getPlayerData().getPlaceholder().useAsList(lang.getMessages().get("alert.notification-send-try")));
			}
		}else {
			DataConverter.playSoundByString(pd.getPlayer().getPlayer().getLocation(), plugin.getConfig().getString("settings.notification-mail-send"));
			lang.sendMessage(pd.getPlayer().getPlayer(), SendMode.valueOf(getPlayerData().getPlayerSetting().getSettings().get("notification-display").toString().toUpperCase()),
					getPlayerData().getPlaceholder().useAsList(lang.getMessages().get("alert.notification-send-exit")));
		}
	}
	
	// isSender = true, it will be delete mail from sender
	// if else, it will be delete mail to receiver
	public void forceDelete(Player p) {
		boolean checked = false;
		PlayerData receiver = new PlayerData(p);
		try {
			if (this.getSender().getName().equals(this.getReceiver().getName())) {
				PlayerData sender = this.getPlayerData();
				ConfigManager.input(sender.getFile(), "mailbox.outbox." + this.getMailUUID(), null);
				ConfigManager.input(sender.getFile(), "mailbox.inbox." + this.getMailUUID(), null);
				if (sender.getPlayer().isOnline()) {
					DataConverter.playSoundByString(sender.getPlayer().getPlayer().getLocation(), plugin.getConfig().getString("settings.notification-mail-delete"));
					sender.getLanguage().sendMessage(sender.getPlayer().getPlayer(), SendMode.valueOf(sender.getPlayerSetting().getSettings().get("notification-display").toString().toUpperCase()), sender.getPlaceholder().useAsList(sender.getLanguage().getMessages().get("alert.notification-delete")));
				}
				checked = true;
			}
		}catch(Exception e) {}
		if (checked == false) {
			DataConverter.playSoundByString(receiver.getPlayer().getPlayer().getLocation(), plugin.getConfig().getString("settings.notification-mail-delete"));
			ConfigManager.input(receiver.getFile(), "mailbox.inbox." + this.getMailUUID(), null);
			receiver.getLanguage().sendMessage(receiver.getPlayer().getPlayer(), SendMode.valueOf(receiver.getPlayerSetting().getSettings().get("notification-display").toString().toUpperCase()), receiver.getPlaceholder().useAsList(receiver.getLanguage().getMessages().get("alert.notification-delete")));
		}
	}
	
	public void delete(boolean isSender) {
		if (sending.containsKey(this.getPlayerData().getPlayer())) {
			sending.remove(this.getPlayerData().getPlayer());
			if (pd.getPlayer().isOnline()) {
				DataConverter.playSoundByString(pd.getPlayer().getPlayer().getLocation(), plugin.getConfig().getString("settings.notification-mail-delete"));
				getPlayerData().getLanguage().sendMessage(pd.getPlayer().getPlayer(), SendMode.valueOf(getPlayerData().getPlayerSetting().getSettings().get("notification-display").toString().toUpperCase()),
						getPlayerData().getPlaceholder().useAsList(
								getPlayerData().getLanguage().getMessages().get("alert.notification-send-exit")));
			}
		}else {
			boolean checked = false;
			try {
				if (this.getSender().getName().equals(this.getReceiver().getName())) {
					PlayerData sender = this.getPlayerData();
					ConfigManager.input(sender.getFile(), "mailbox.outbox." + this.getMailUUID(), null);
					ConfigManager.input(sender.getFile(), "mailbox.inbox." + this.getMailUUID(), null);
					if (sender.getPlayer().isOnline()) {
						DataConverter.playSoundByString(sender.getPlayer().getPlayer().getLocation(), plugin.getConfig().getString("settings.notification-mail-delete"));
						sender.getLanguage().sendMessage(sender.getPlayer().getPlayer(), SendMode.valueOf(sender.getPlayerSetting().getSettings().get("notification-display").toString().toUpperCase()), sender.getPlaceholder().useAsList(sender.getLanguage().getMessages().get("alert.notification-delete")));
					}
					checked = true;
				}
			}catch(Exception e) {}
			if (checked == false) {
				if (isSender) {
					PlayerData sender = this.getPlayerData();
					ConfigManager.input(sender.getFile(), "mailbox.outbox." + this.getMailUUID(), null);
					ConfigManager.input(sender.getFile(), "mailbox.inbox." + this.getMailUUID(), null);
					if (sender.getPlayer().isOnline()) {
						DataConverter.playSoundByString(sender.getPlayer().getPlayer().getLocation(), plugin.getConfig().getString("settings.notification-mail-delete"));
						sender.getLanguage().sendMessage(sender.getPlayer().getPlayer(), SendMode.valueOf(sender.getPlayerSetting().getSettings().get("notification-display").toString().toUpperCase()), sender.getPlaceholder().useAsList(sender.getLanguage().getMessages().get("alert.notification-delete")));
					}
				}else {
					DataConverter.playSoundByString(receiver.getPlayer().getPlayer().getLocation(), plugin.getConfig().getString("settings.notification-mail-delete"));
					PlayerData receiver = new PlayerData(this.getReceiver());
					ConfigManager.input(receiver.getFile(), "mailbox.inbox." + this.getMailUUID(), null);
					receiver.getLanguage().sendMessage(receiver.getPlayer().getPlayer(), SendMode.valueOf(receiver.getPlayerSetting().getSettings().get("notification-display").toString().toUpperCase()), receiver.getPlaceholder().useAsList(receiver.getLanguage().getMessages().get("alert.notification-delete")));
				}
			}
		}
	}
	
	public void read(Player targetplayer) {
		String msg = "   ";
		for (String s : this.getMessage()) {
			msg = msg.concat(" " + s);
		}
		targetplayer.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
	}
	
	public void setMode(MailMode mm) {
		this.mailmode = mm;
	}
	
	public void addMessage(String text) {
		this.msg.add(text);
		
		this.getPlayerData().getPlaceholder().inputData("mail_message_size", this.msg.size() + "");
		OfflinePlayer op = getPlayerData().getPlayer();
		if (op.isOnline()) {
			this.getPlayerData().getLanguage().sendDirectMessage(op.getPlayer(), this.getPlayerData().getPlaceholder().use(this.getPlayerData().getLanguage().getMessages().get("alert.notification-send-input").get(3)));
		}
	}
	
	public void setSendAll(boolean sendall) {
		this.isSendAll = sendall;
		this.getPlayerData().getPlaceholder().inputData("mail_sendall", this.isSendAll + "");
		if (this.getPlayerData().getPlayer().isOnline()) {
			this.getPlayerData().getLanguage().sendDirectMessage(this.getPlayerData().getPlayer().getPlayer(),
					this.getPlayerData().getPlaceholder().use(this.getPlayerData().getLanguage().getMessages().get("alert.notification-send-input").get(0)));
		}
		
	}
	
	public void setItem(ItemStack item) {
		if (!item.getType().equals(Material.AIR)) {
			if (!this.getItem().getType().equals(Material.AIR)) {
				getPlayerData().getPlayer().getPlayer().getInventory().addItem(this.getItem());
			}
			this.item = item.clone();
			this.getPlayerData().getPlaceholder().inputData("mail_item_amount", this.getItem().getType().equals(Material.AIR) ? "0" : this.getItem().getAmount() + "");
			this.getPlayerData().getPlaceholder().inputData("mail_item", this.item.getItemMeta().hasDisplayName() ? this.getItem().getItemMeta().getDisplayName() : this.getItem().getType().toString());
			
			if (this.getPlayerData().getPlayer().isOnline()) {
				this.getPlayerData().getLanguage().sendDirectMessage(this.getPlayerData().getPlayer().getPlayer(),
						this.getPlayerData().getPlaceholder().use(this.getPlayerData().getLanguage().getMessages().get("alert.notification-send-input").get(2)));
			}
			ItemManager.removeItemFromInventory(this.getPlayerData().getPlayer().getPlayer().getInventory(), item);
		}else {
			returnItem();
		}
	}
	
	public void returnItem() {
		if (!this.getItem().getType().equals(Material.AIR)) {
			if (this.getPlayerData().getPlayer().isOnline()) {
				getPlayerData().getPlayer().getPlayer().getInventory().addItem(this.getItem());
				this.item = new ItemStack(Material.AIR);
			}
		}
	}
	
	public void addMultiReceiver(List<OfflinePlayer> list) {
		try {
			this.getMultipleReceiver().addAll(list);
			setMultiSend(true);
			List<String> name = new ArrayList<String>();
			for (OfflinePlayer op : this.getMultipleReceiver()) {
				name.add(op.getName());
			}
			
			this.getPlayerData().getPlaceholder().inputData("mail_receiver_size", this.getMultipleReceiver().size() + "");
			this.getPlayerData().getPlaceholder().inputData("mail_sender", this.getPlayerData().getPlayer().getName());
			this.getPlayerData().getPlaceholder().inputData("mail_receiver", name.toString());
			if (this.getPlayerData().getPlayer().isOnline()) {
				this.getPlayerData().getLanguage().sendDirectMessage(this.getPlayerData().getPlayer().getPlayer(),
						this.getPlayerData().getPlaceholder().use(this.getPlayerData().getLanguage().getMessages().get("alert.notification-send-input").get(1)));
			}
		}catch(Exception e) {
			if (this.getPlayerData().getPlayer().isOnline()) {
				this.getPlayerData().getLanguage().sendMessage(this.getPlayerData().getPlayer().getPlayer(),
						this.getPlayerData().getPlaceholder().useAsList(this.getPlayerData().getLanguage().getMessages().get("alert.player-not-found")));
			}
		}
	}
	
	public void addMultiReceiver(OfflinePlayer offlinePlayer) {	
		try {
			this.getMultipleReceiver().add(offlinePlayer);
			setMultiSend(true);
			List<String> name = new ArrayList<String>();
			for (OfflinePlayer op : this.getMultipleReceiver()) {
				name.add(op.getName());
			}
			
			this.getPlayerData().getPlaceholder().inputData("mail_receiver_size", this.getMultipleReceiver().size() + "");
			this.getPlayerData().getPlaceholder().inputData("mail_sender", this.getPlayerData().getPlayer().getName());
			this.getPlayerData().getPlaceholder().inputData("mail_receiver", name.toString());
			if (this.getPlayerData().getPlayer().isOnline()) {
				this.getPlayerData().getLanguage().sendDirectMessage(this.getPlayerData().getPlayer().getPlayer(),
						this.getPlayerData().getPlaceholder().use(this.getPlayerData().getLanguage().getMessages().get("alert.notification-send-input").get(1)));
			}
		}catch(Exception e) {
			if (this.getPlayerData().getPlayer().isOnline()) {
				this.getPlayerData().getLanguage().sendMessage(this.getPlayerData().getPlayer().getPlayer(),
						this.getPlayerData().getPlaceholder().useAsList(this.getPlayerData().getLanguage().getMessages().get("alert.player-not-found")));
			}
		}
	}
	
	public void setMultiSend(boolean bool) {
		this.isMultiSend = bool;
	}
	
	public void setReceiver(OfflinePlayer offlinePlayer) {
		try {
			this.receiver = offlinePlayer;
			
			this.getPlayerData().getPlaceholder().inputData("mail_sender", this.getPlayerData().getPlayer().getName());
			this.getPlayerData().getPlaceholder().inputData("mail_receiver", offlinePlayer.getName());
			if (this.getPlayerData().getPlayer().isOnline()) {
				this.getPlayerData().getLanguage().sendDirectMessage(this.getPlayerData().getPlayer().getPlayer(),
						this.getPlayerData().getPlaceholder().use(this.getPlayerData().getLanguage().getMessages().get("alert.notification-send-input").get(1)));
			}
		}catch(Exception e) {
			if (this.getPlayerData().getPlayer().isOnline()) {
				this.getPlayerData().getLanguage().sendMessage(this.getPlayerData().getPlayer().getPlayer(),
						this.getPlayerData().getPlaceholder().useAsList(this.getPlayerData().getLanguage().getMessages().get("alert.player-not-found")));
			}
		}
	}
	
	public void initPlaceholder() {
		try {
			plc.inputData("mail_sender", this.getSender().getName());
		}catch(Exception e) {}
		try {
			plc.inputData("mail_receiver", this.getReceiver().getName());
		}catch(Exception e) {}
		plc.inputData("mail_date", this.getDate());
		try {
			plc.inputData("mail_item_amount", this.getItem().getType().equals(Material.AIR) ? "0" : this.getItem().getAmount() + "");
			plc.inputData("mail_item", "&8[&a" + (this.getItem().getType().equals(Material.AIR) ? "" : this.getItem().getAmount() + "x&f") + " &7" + (this.item.getType().equals(Material.AIR) ? "AIR" : (this.item.getItemMeta().hasDisplayName() ? this.getItem().getItemMeta().getDisplayName() : this.getItem().getType().toString())) + " &8]");
		}catch(Exception e) {
			e.printStackTrace();
		}
		plc.inputListData("mail_message", this.getMessage());
	}
	
	public boolean isMultiSend() {
		return this.isMultiSend;
	}
	
	public String getMailUUID() {
		return this.uuid;
	}
	
	public OfflinePlayer getSender() {
		return this.sender;
	}
	
	public OfflinePlayer getReceiver() {
		return this.receiver;
	}
	
	public boolean isSendAll() {
		return this.isSendAll;
	}
	
	public PlayerData getPlayerData() {
		return this.pd;
	}
	
	public ItemStack getItem() {
		return this.item;
	}
	
	public Mail getMail() {
		return this;
	}
	
	public MailMode getMailMode() {
		return this.mailmode;
	}
	
	public List<String> getMessage(){
		return this.msg;
	}
	
	public boolean hasItem() {
		return !this.getItem().getType().equals(Material.AIR);
	}
	
	public Placeholder getPlaceholder() {
		return this.plc;
	}
	
	public String getDate() {
		return this.displayDate;
	}
	
	
	public List<OfflinePlayer> getMultipleReceiver(){
		return this.multiReceiver;
	}

}
