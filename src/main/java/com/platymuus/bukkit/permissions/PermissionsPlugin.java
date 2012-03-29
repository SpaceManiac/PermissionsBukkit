package com.platymuus.bukkit.permissions;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;


/**
 * Main class for PermissionsBukkit.
 */
public class PermissionsPlugin extends JavaPlugin {

	private PlayerListener							playerListener	= new PlayerListener( this );
	private PermissionsCommand						commandExecutor	= new PermissionsCommand( this );
	private HashMap<String, PermissionAttachment>	permissions		= new HashMap<String, PermissionAttachment>();

	private File									configFile;
	private YamlConfiguration						config;


	// -- Basic stuff
	@Override
	public void onEnable() {
		// Take care of configuration
		configFile = new File( getDataFolder(), "config.yml" );
		if ( !configFile.exists() ) {
			saveDefaultConfig();
		}
		reloadConfig();

		// Register stuff
		getCommand( "permissions" ).setExecutor( commandExecutor );
		getServer().getPluginManager().registerEvents( playerListener, this );

		// Register everyone online right now
		for ( Player p : getServer().getOnlinePlayers() ) {
			registerPlayer( p );
		}

		// How are you gentlemen
		getLogger().info( "Enabled successfully, " + getServer().getOnlinePlayers().length + " players registered" );
	}


	@Override
	public FileConfiguration getConfig() {
		return config;
	}


	@Override
	public void reloadConfig() {
		config = new YamlConfiguration();
		config.options().pathSeparator( '/' );
		try {
			config.load( configFile );
		}
		catch ( Exception e ) {
			getLogger().severe( "Unable to load configuration!" );
		}
	}


	@Override
	public void onDisable() {
		// Unregister everyone
		for ( Player p : getServer().getOnlinePlayers() ) {
			unregisterPlayer( p );
		}

		// Good day to you! I said good day!
		getLogger().info( "Disabled successfully, " + getServer().getOnlinePlayers().length + " players unregistered" );
	}


	// -- External API
	/**
	 * Get the group with the given name.
	 * 
	 * @param groupName The name of the group.
	 * @return A Group if it exists or null otherwise.
	 */
	public Group getGroup( String groupName ) {
		if ( getNode( "groups" ) != null ) {
			for ( String key : getNode( "groups" ).getKeys( false ) ) {
				if ( key.equalsIgnoreCase( groupName ) ) {
					return new Group( this, key );
				}
			}
		}
		return null;
	}


	/**
	 * Returns a list of groups a player is in.
	 * 
	 * @param playerName The name of the player.
	 * @return The groups this player is in. May be empty.
	 */
	public List<Group> getGroups( String playerName ) {
		ArrayList<Group> result = new ArrayList<Group>();
		if ( getNode( "users/" + playerName ) != null ) {
			for ( String key : getNode( "users/" + playerName ).getStringList( "groups" ) ) {
				result.add( new Group( this, key ) );
			}
		}
		else {
			result.add( new Group( this, "default" ) );
		}
		return result;
	}


	/**
	 * Returns permission info on the given player.
	 * 
	 * @param playerName The name of the player.
	 * @return A PermissionsInfo about this player.
	 */
	public PermissionInfo getPlayerInfo( String playerName ) {
		if ( getNode( "users/" + playerName ) == null ) {
			return null;
		}
		else {
			return new PermissionInfo( this, getNode( "users/" + playerName ), "groups" );
		}
	}


	/**
	 * Returns a list of all defined groups.
	 * 
	 * @return The list of groups.
	 */
	public List<Group> getAllGroups() {
		ArrayList<Group> result = new ArrayList<Group>();
		if ( getNode( "groups" ) != null ) {
			for ( String key : getNode( "groups" ).getKeys( false ) ) {
				result.add( new Group( this, key ) );
			}
		}
		return result;
	}


	// -- Plugin stuff

	protected void registerPlayer( Player player ) {
		if ( permissions.containsKey( player.getName() ) ) {
			debug( "Registering " + player.getName() + ": was already registered" );
			unregisterPlayer( player );
		}
		PermissionAttachment attachment = player.addAttachment( this );
		permissions.put( player.getName(), attachment );
		calculateAttachment( player );
	}


	protected void unregisterPlayer( Player player ) {
		if ( permissions.containsKey( player.getName() ) ) {
			try {
				player.removeAttachment( permissions.get( player.getName() ) );
			}
			catch ( IllegalArgumentException ex ) {
				debug( "Unregistering " + player.getName() + ": player did not have attachment" );
			}
			permissions.remove( player.getName() );
		}
		else {
			debug( "Unregistering " + player.getName() + ": was not registered" );
		}
	}


	protected void refreshPermissions() {
		try {
			getConfig().save( configFile );
		}
		catch ( IOException e ) {
			getLogger().warning( "Failed to write changed config.yml: " + e.getMessage() );
		}
		for ( String player : permissions.keySet() ) {
			PermissionAttachment attachment = permissions.get( player );
			for ( String key : attachment.getPermissions().keySet() ) {
				attachment.unsetPermission( key );
			}

			calculateAttachment( getServer().getPlayer( player ) );
		}
	}


	protected ConfigurationSection getNode( String node ) {
		for ( String entry : getConfig().getKeys( true ) ) {
			if ( node.equalsIgnoreCase( entry ) && getConfig().isConfigurationSection( entry ) ) {
				return getConfig().getConfigurationSection( entry );
			}
		}
		return null;
	}


	protected HashMap<String, Boolean> getAllPerms( String desc, String path ) {
		// Use *ordered* hash
		HashMap<String, Boolean> result = new LinkedHashMap<String, Boolean>();
		ConfigurationSection node = getNode( path );

		int failures = 0;
		String firstFailure = "";

		Set<String> keys = node.getKeys( false );
		for ( String key : keys ) {
			if ( node.isBoolean( key ) ) {
				result.put( key, node.getBoolean( key ) );
			}
			else {
				++failures;
				if ( firstFailure.length() == 0 ) {
					firstFailure = key;
				}
			}
		}

		if ( failures == 1 ) {
			getLogger().warning( "In " + desc + ": " + firstFailure + " is non-boolean." );
		}
		else if ( failures > 1 ) {
			getLogger().warning( "In " + desc + ": " + firstFailure + " is non-boolean (+" + (failures - 1) + " more)." );
		}

		return result;
	}


	protected void debug( String message ) {
		if ( getConfig().getBoolean( "debug", false ) ) {
			getLogger().info( "Debug: " + message );
		}
	}


	protected void calculateAttachment( Player player ) {
		if ( player == null ) {
			return;
		}
		PermissionAttachment attachment = permissions.get( player.getName() );
		if ( attachment == null ) {
			debug( "Calculating permissions on " + player.getName() + ": attachment was null" );
			return;
		}

		for ( String key : attachment.getPermissions().keySet() ) {
			attachment.unsetPermission( key );
		}

		for ( Map.Entry<String, Boolean> entry : calculatePlayerPermissions( player.getName().toLowerCase(),
				player.getWorld().getName() ).entrySet() ) {
			attachment.setPermission( entry.getKey(), entry.getValue() );
		}

		player.recalculatePermissions();
	}


	// -- Private stuff

	private Map<String, Boolean> calculatePlayerPermissions( String player, String world ) {
		// Make player node name once as a single edit point. While unlikely, it might change.
		String playerNode = "users/" + player;
		Map<String, Boolean> perms = new LinkedHashMap<String, Boolean>();

		if ( getNode( playerNode ) == null ) {
			perms = calculateGroupPermissions( "default", world );
		}
		else {
			/*
			 * Create a blank *ordered* hash and populate with group permissions.
			 */
			List<String> playerGroups = getNode( playerNode ).getStringList( "groups" );

			/*
			 * Current group precedence is from last to first. (i.e. permissions in last group in list overrides permissions
			 * in any predecessors). If precedence should be from first to last, uncomment the following line.
			 */
			// Collections.reverse( playerGroups );

			for ( String group : playerGroups ) {
				perms.putAll( calculateGroupPermissions( group, world ) );
			}

			/*
			 * Overlay any player-specific permissions onto permission list, if they exist.
			 */

			if ( getNode( playerNode + "/permissions" ) != null )
				perms.putAll( getAllPerms( playerNode, playerNode + "/permissions" ) );

			/*
			 * Overlay any user world-specific permissions onto permissions list, if they exist.
			 */

			if ( getNode( playerNode + "/worlds/" + world ) != null )
				perms.putAll( getAllPerms( playerNode, playerNode + "/worlds/" + world ) );
		}

		return perms;
	}


	private Map<String, Boolean> calculateGroupPermissions( String group, String world ) {
		// Make group node name once as a single edit point. While unlikely, it might change.
		String groupNode = "groups/" + group;

		// Blank *ordered* group permissions list
		HashMap<String, Boolean> perms = new LinkedHashMap<String, Boolean>();

		if ( getNode( groupNode ) == null ) {
			return perms;
		}

		List<String> groupAncestors = getNode( groupNode ).getStringList( "inheritance" );
		/*
		 * Current inheritance precedence is from last to first. (i.e. permissions in last inherited group overrides
		 * permissions in any predecessors). If precedence should be from first to last, uncomment the following line.
		 */
		// Collections.reverse( groupAncestors );

		for ( String ancestor : groupAncestors ) {
			perms.putAll( calculateGroupPermissions( ancestor, world ) );
		}

		/*
		 * Overlay any group-specific permissions onto permission list, if they exist.
		 */

		if ( getNode( groupNode + "/permissions" ) != null )
			perms.putAll( getAllPerms( groupNode, groupNode + "/permissions" ) );

		/*
		 * Overlay any group world-specific permissions onto permissions list, if they exist.
		 */
		if ( getNode( groupNode + "/worlds/" + world ) != null )
			perms.putAll( getAllPerms( groupNode, groupNode + "/worlds/" + world ) );

		return perms;
	}
}
