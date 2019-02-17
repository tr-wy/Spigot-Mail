package team.creativecode.diamail;

import org.bukkit.plugin.java.JavaPlugin;

import team.creativecode.diamail.cmds.DiamailCmd;
import team.creativecode.diamail.events.BasicEvent;
import team.creativecode.diamail.manager.menu.Mailbox;
import team.creativecode.diamail.utils.ConfigManager;
import team.creativecode.diamail.utils.Language;
import team.creativecode.diamail.utils.Placeholder;

public class Main extends JavaPlugin {
	
	public static Language lang;
	public static Placeholder placeholder;
	
	public void onEnable() {
		loadFile();
		loadCmds();
		loadEvents();
		loadMenus();
	}

	private void loadMenus() {
		new Mailbox().createFile();
		
	}

	private void loadEvents() {
		this.getServer().getPluginManager().registerEvents(new BasicEvent(), this);
	}

	private void loadCmds() {
		this.getCommand("diamail").setExecutor(new DiamailCmd());
	}

	public void loadFile() {
		this.getConfig().options().copyDefaults(true);
		this.getConfig().options().copyHeader(true);
		saveConfig();
		
		ConfigManager.createFolder(this.getDataFolder() + "/PlayerData");
		
		Language.loadLanguages();
		lang = new Language(this.getConfig().getString("global-language"));
		placeholder = new Placeholder();
		
	}

}
