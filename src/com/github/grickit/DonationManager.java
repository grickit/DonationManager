package com.github.grickit;

import java.util.Calendar;
import java.util.logging.Logger;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;


public class DonationManager extends JavaPlugin {
	public final JoinListener join_listener = new JoinListener(); // Defined later; Listens for people logging into the server.
	public Logger log = Logger.getLogger("Minecraft"); //Used to send messages to console.
	public String prefix = "[DonationManager] ";
	public static Economy econ = null; //The economy.

	
	public void onEnable() {
	    if (!testEconomy()) { //If there's no economy plugin, there's no point.
	      log.info(prefix+"Disabled because Vault is unavailable or no economy service is installed.");
	      getServer().getPluginManager().disablePlugin(this);
	      return;
	    }	  
	  
		getConfig().options().copyDefaults(true); //Copy over the default configuration values.
		saveConfig(); //Initialize our folder and config file if they don't exist.
		
		getServer().getPluginManager().registerEvents(this.join_listener,this); //Tell bukkit about the join listener.
		
		log.info(prefix+"Fully enabled.");
	}
	
	
	public void onDisable() {
		saveConfig(); // Save the config file before we leave.
		log.info(prefix+"Fully disabled.");
	}
	
	
  	private boolean testEconomy() {
  	  if (getServer().getPluginManager().getPlugin("Vault") == null) { return false; }
  	  RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
  	  return (econ = rsp.getProvider()) != null;
	}
  	
  	
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
      if (cmd.getName().equalsIgnoreCase("dmset")) {
        if(args.length != 2) {
          sender.sendMessage(ChatColor.RED+"Incorrect number of arguments.");
        }
        else {
          try {
            Integer amount = Integer.parseInt(args[1]);
            getConfig().set("donators."+args[0], amount);
            saveConfig();
            sender.sendMessage(ChatColor.GREEN+"Donator added or updated.");
          }
          catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED+"Donation amount must be a number.");
          }
        }
        return true;
      }
      return false;
    }
    
	
	//SUBCLASS!
	public class JoinListener implements Listener {
		@EventHandler
		public void onPlayerJoin(PlayerJoinEvent event) { //Run every time a player joins.
		    Calendar calendar = Calendar.getInstance();
		    
		    //We don't have to check if it has actually been a day since their last login. We just need to check if today is a different date.
		    String date = calendar.get(Calendar.YEAR) + "-" + calendar.get(Calendar.MONTH) + "-" + calendar.get(Calendar.DATE);
		    String name = event.getPlayer().getDisplayName();
		    Integer amount = getConfig().getInt("donators."+name);
		    if(amount != null && amount != 0) { //Is this person a donator? If not, we don't care to waste any more time on them.
		    	String last_login = getConfig().getString("login_dates."+name);
		    	if(last_login == null || !last_login.equals(date)) { //Is today a different day than last time?
		    		getConfig().set("login_dates."+name,date); //Save the new date.
		    		saveConfig();
		    		
		    		//Send all the thank you messages.
			    	Object[] thanks = getConfig().getList("thanks_messages").toArray();
			    	for(Object message: thanks) {
			    		event.getPlayer().sendMessage(ChatColor.GREEN+message.toString());
			    	}			    	
			    	econ.bankDeposit(name,amount*getConfig().getInt("multiplier"));
		    	}
		    }
		}
	}
}