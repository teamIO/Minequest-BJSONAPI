package com.theminequest.bjsonapi;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.alecgorge.minecraft.jsonapi.JSONAPI;
import com.alecgorge.minecraft.jsonapi.api.APIMethodName;
import com.alecgorge.minecraft.jsonapi.api.JSONAPICallHandler;
import com.alta189.simplesave.internal.TableRegistration;

import com.theminequest.MineQuest.API.Managers;
import com.theminequest.MineQuest.API.Tracker.QuestStatisticUtils;
import com.theminequest.MineQuest.API.Tracker.QuestStatisticUtils.QSException;
import com.theminequest.MineQuest.API.Tracker.QuestStatisticUtils.Status;
import com.theminequest.MineQuest.API.Tracker.StatisticManager.Statistic;

public class Main extends JavaPlugin implements JSONAPICallHandler {
	
	private JSONAPI jsonapi;

	@Override
	public void onDisable() {
		if (jsonapi!=null)
			jsonapi.deregisterAPICallHandler(this);
	}

	@Override
	public void onEnable() {
		Plugin checkplugin = getServer().getPluginManager().getPlugin("JSONAPI");
		if (getServer().getPluginManager().getPlugin("MineQuest") == null) {
			getServer().getLogger().severe("============= MineQuest-BJSONAPI =============");
			getServer().getLogger().severe("MineQuest is required for BJSONAPI to operate!");
			getServer().getLogger().severe("Please install MineQuest first!");
			getServer().getLogger().severe("You can find the latest version here:");
			getServer().getLogger().severe("http://dev.bukkit.org/server-mods/minequest/");
			getServer().getLogger().severe("==============================================");
			setEnabled(false);
			return;
		} else if (checkplugin == null) {
			getServer().getLogger().severe("============= MineQuest-BJSONAPI =============");
			getServer().getLogger().severe("JSONAPI is required for BJSONAPI to operate!");
			getServer().getLogger().severe("Please install JSONAPI first!");
			getServer().getLogger().severe("You can find the latest version here:");
			getServer().getLogger().severe("http://mcjsonapi.com/");
			getServer().getLogger().severe("==============================================");
			setEnabled(false);
			return;
		}
		
		jsonapi = (JSONAPI)checkplugin;
		jsonapi.registerAPICallHandler(this);
	}
	
	public boolean willHandle(APIMethodName arg0) {
		if (!arg0.getNamespace().equals("minequest"))
			return false;
		for (APIMethods a : APIMethods.values()){
			if (a.getValue().equals(arg0.getMethodName()))
				return true;
		}
		return false;
	}

	public Object handle(APIMethodName arg0, Object[] arg1) {
		if (!arg0.getNamespace().equals("minequest"))
			return null;
		for (APIMethods a : APIMethods.values()){
			if (a.getValue().equals(arg0.getMethodName()))
				return a.handle(arg1);
		}
		return null;
	}
	
	private static enum APIMethods {
		
		GETALLDETAILS("getalldetails", new ReturnHandler(){

			@Override
			public Object handle(Object[] args) {
				return Managers.getQuestManager().getListOfDetails();
			}
			
		}),
		GETDETAIL("getdetail", new ReturnHandler(){

			@Override
			public Object handle(Object[] args) {
				if (args.length!=1)
					return null;
				String questname = (String) args[0];
				return Managers.getQuestManager().getDetails(questname);
			}
			
		}),
		GETSTATISTIC("getstatistic", new ReturnHandler(){

			@Override
			public Object handle(Object[] args) {
				if (args.length!=2)
					return null;
				String playerName = (String) args[0];
				String tableClassName = (String) args[1];
				for (TableRegistration r : Managers.getStatisticManager().getStorageBackend().getTableRegistrations()){
					if (r.getTableClass().getName().equals(tableClassName))
						return Managers.getStatisticManager().getStatistic(playerName,(Class<? extends Statistic>) r.getTableClass());
				}
				return null;
			}
			
		}),
		
		// following from QuestStatisticUtils
		GETQUESTS("getquests", new ReturnHandler(){

			// 0 - given, 1 - completed, 2 - inprogress, 3 - unknown
			// this operation does not support 3 - unknown
			@Override
			public Object handle(Object[] args) {
				if (args.length!=2)
					return null;
				String playerName = (String) args[0];
				int status = (Integer) args[1];
				switch(status){
				case 0:
					return QuestStatisticUtils.getQuests(playerName,Status.GIVEN);
				case 1:
					return QuestStatisticUtils.getQuests(playerName,Status.COMPLETED);
				case 2:
					return QuestStatisticUtils.getQuests(playerName,Status.INPROGRESS);
				}
				return null;
			}
			
		}),
		GETCOMPLETEDDETAILS("getcompleteddetails", new ReturnHandler(){

			@Override
			public Object handle(Object[] args) {
				if (args.length!=1)
					return null;
				return QuestStatisticUtils.getCompletedDetails((String) args[0]);
			}
			
		}),
		HASQUEST("hasquest", new ReturnHandler(){

			// 0 - given, 1 - completed, 2 - inprogress, 3 - unknown
			// returns code above based on status
			@Override
			public Object handle(Object[] args) {
				if (args.length!=2)
					return null;
				String playerName = (String) args[0];
				String questName = (String) args[1];
				switch(QuestStatisticUtils.hasQuest(playerName,questName)){
				case COMPLETED:
					return 1;
				case GIVEN:
					return 0;
				case INPROGRESS:
					return 2;
				default:
					return 3;
				}
			}
			
		}),
		GIVEQUEST("givequest", new ReturnHandler(){

			@Override
			public Object handle(Object[] args) {
				if (args.length!=2)
					return null;
				String playerName = (String) args[0];
				String questName = (String) args[1];
				try {
					QuestStatisticUtils.giveQuest(playerName,questName);
					return "GOOD";
				} catch (QSException e) {
					return "FAIL:" + e.getMessage();
				}
			}
			
		}),
		DROPQUEST("dropquest", new ReturnHandler(){

			@Override
			public Object handle(Object[] args) {
				if (args.length!=2)
					return null;
				String playerName = (String) args[0];
				String questName = (String) args[1];
				try {
					QuestStatisticUtils.degiveQuest(playerName,questName);
					return "GOOD";
				} catch (QSException e) {
					return "FAIL:" + e.getMessage();
				}
			}
			
		}),
		COMPLETEQUEST("completequest", new ReturnHandler(){

			@Override
			public Object handle(Object[] args) {
				if (args.length!=2)
					return null;
				String playerName = (String) args[0];
				String questName = (String) args[1];
				try {
					QuestStatisticUtils.completeQuest(playerName,questName);
					return "GOOD";
				} catch (QSException e) {
					return "FAIL:" + e.getMessage();
				}
			}
			
		});
		
		private String methodname;
		private ReturnHandler rh;
		
		private APIMethods(String methodname, ReturnHandler rh){
			this.methodname = methodname;
			this.rh = rh;
		}
		
		public String getValue(){
			return methodname;
		}
		
		public Object handle(Object[] args){
			return rh.handle(args);
		}
	}
	
	private static abstract class ReturnHandler {
		public abstract Object handle(Object[] args);
	}

}
