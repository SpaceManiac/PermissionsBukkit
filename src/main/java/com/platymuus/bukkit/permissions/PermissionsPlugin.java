package com.platymuus.bukkit.permissions;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.FileUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Main class for PermissionsBukkit.
 */
public class PermissionsPlugin extends JavaPlugin {

    private PlayerListener playerListener = new PlayerListener(this);
    private PermissionsCommand commandExecutor = new PermissionsCommand(this);
    private PermissionsTabComplete tabCompleter = new PermissionsTabComplete(this);
    private PermissionsMetrics metrics = new PermissionsMetrics(this);

    private HashMap<UUID, PermissionAttachment> permissions = new HashMap<UUID, PermissionAttachment>();
    
    private File configFile;
    private YamlConfiguration config;

    public boolean configLoadError = false;

    // -- Basic stuff
    @Override
    public void onEnable() {
        // Take care of configuration
        configFile = new File(getDataFolder(), "config.yml");
        saveDefaultConfig();
        reloadConfig();

        // Register stuff
        getCommand("permissions").setExecutor(commandExecutor);
        getCommand("permissions").setTabCompleter(tabCompleter);
        getServer().getPluginManager().registerEvents(playerListener, this);

        // Register everyone online right now
        for (Player p : getServer().getOnlinePlayers()) {
            registerPlayer(p);
        }

        // Metrics are fun!
        try {
            metrics.start();
        }
        catch (IOException ex) {
            getLogger().warning("Failed to connect to plugin metrics: " + ex.getMessage());
        }

        // How are you gentlemen
        int count = getServer().getOnlinePlayers().length;
        if (count > 0) {
            getLogger().info("Enabled successfully, " + count + " online players registered");
        } else {
            // "0 players registered" sounds too much like an error
            getLogger().info("Enabled successfully");
        }
    }

    @Override
    public FileConfiguration getConfig() {
        return config;
    }

    @Override
    public void reloadConfig() {
        config = new YamlConfiguration();
        config.options().pathSeparator('/');
        try {
            config.load(configFile);
        } catch (InvalidConfigurationException ex) {
            configLoadError = true;

            // extract line numbers from the exception if we can
            ArrayList<String> lines = new ArrayList<String>();
            Pattern pattern = Pattern.compile("line (\\d+), column");
            Matcher matcher = pattern.matcher(ex.getMessage());
            while (matcher.find()) {
                String lineNo = matcher.group(1);
                if (!lines.contains(lineNo)) {
                    lines.add(lineNo);
                }
            }

            // make a nice message
            String msg = "Your configuration is invalid! ";
            if (lines.size() == 0) {
                msg += "Unable to find any line numbers.";
            } else {
                msg += "Take a look at line(s): " + lines.get(0);
                for (String lineNo : lines.subList(1, lines.size())) {
                    msg += ", " + lineNo;
                }
            }
            getLogger().severe(msg);

            // save the whole error to config_error.txt
            try {
                File outFile = new File(getDataFolder(), "config_error.txt");
                PrintStream out = new PrintStream(new FileOutputStream(outFile));
                out.println("Use the following website to help you find and fix configuration errors:");
                out.println("https://yaml-online-parser.appspot.com/");
                out.println();
                out.println(ex.toString());
                out.close();
                getLogger().info("Saved the full error message to " + outFile);
            } catch (IOException ex2) {
                getLogger().severe("Failed to save the full error message!");
            }

            // save a backup
            File backupFile = new File(getDataFolder(), "config_backup.yml");
            File sourceFile = new File(getDataFolder(), "config.yml");
            if (FileUtil.copy(sourceFile, backupFile)) {
                getLogger().info("Saved a backup of your configuration to " + backupFile);
            } else {
                getLogger().severe("Failed to save a configuration backup!");
            }
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, "Failed to load configuration", ex);
        }
    }

    @Override
    public void saveConfig() {
        // If there's no keys (such as in the event of a load failure) don't save
        if (config.getKeys(true).size() > 0) {
            try {
                config.save(configFile);
            } catch (IOException ex) {
                getLogger().log(Level.SEVERE, "Failed to save configuration", ex);
            }
        }
    }

    @Override
    public void onDisable() {
        // Unregister everyone
        for (Player p : getServer().getOnlinePlayers()) {
            unregisterPlayer(p);
        }

        // Good day to you! I said good day!
        int count = getServer().getOnlinePlayers().length;
        if (count > 0) {
            getLogger().info("Disabled successfully, " + count + " online players unregistered");
        }
    }

    // -- External API
    /**
     * Get the group with the given name.
     * @param groupName The name of the group.
     * @return A Group if it exists or null otherwise.
     */
    public Group getGroup(String groupName) {
        metrics.apiUsed();
        if (getNode("groups") != null) {
            for (String key : getNode("groups").getKeys(false)) {
                if (key.equalsIgnoreCase(groupName)) {
                    return new Group(this, key);
                }
            }
        }
        return null;
    }

    /**
     * Returns a list of groups a player is in.
     * @param playerName The name of the player.
     * @return The groups this player is in. May be empty.
     * @deprecated Use UUIDs instead.
     */
    @Deprecated
    public List<Group> getGroups(String playerName) {
        metrics.apiUsed();
        ArrayList<Group> result = new ArrayList<Group>();
        if (getNode("users/" + playerName) != null) {
            for (String key : getNode("users/" + playerName).getStringList("groups")) {
                result.add(new Group(this, key));
            }
        } else {
            result.add(new Group(this, "default"));
        }
        return result;
    }

    /**
     * Returns a list of groups a player is in.
     * @param player The uuid of the player.
     * @return The groups this player is in. May be empty.
     */
    public List<Group> getGroups(UUID player) {
        metrics.apiUsed();
        ArrayList<Group> result = new ArrayList<Group>();
        if (getNode("users/" + player) != null) {
            for (String key : getNode("users/" + player).getStringList("groups")) {
                result.add(new Group(this, key));
            }
        } else {
            result.add(new Group(this, "default"));
        }
        return result;
    }

    /**
     * Returns permission info on the given player.
     * @param playerName The name of the player.
     * @return A PermissionsInfo about this player.
     * @deprecated Use UUIDs instead.
     */
    @Deprecated
    public PermissionInfo getPlayerInfo(String playerName) {
        metrics.apiUsed();
        if (getNode("users/" + playerName) == null) {
            return null;
        } else {
            return new PermissionInfo(this, getNode("users/" + playerName), "groups");
        }
    }

    /**
     * Returns permission info on the given player.
     * @param player The uuid of the player.
     * @return A PermissionsInfo about this player.
     */
    public PermissionInfo getPlayerInfo(UUID player) {
        metrics.apiUsed();
        if (getNode("users/" + player) == null) {
            return null;
        } else {
            return new PermissionInfo(this, getNode("users/" + player), "groups");
        }
    }

    /**
     * Returns a list of all defined groups.
     * @return The list of groups.
     */
    public List<Group> getAllGroups() {
        metrics.apiUsed();
        ArrayList<Group> result = new ArrayList<Group>();
        if (getNode("groups") != null) {
            for (String key : getNode("groups").getKeys(false)) {
                result.add(new Group(this, key));
            }
        }
        return result;
    }

    // -- Plugin stuff

    protected PermissionsMetrics getMetrics() {
        return metrics;
    }
    
    protected void registerPlayer(Player player) {
        if (permissions.containsKey(player.getUniqueId())) {
            debug("Registering " + player.getName() + ": was already registered");
            unregisterPlayer(player);
        }
        PermissionAttachment attachment = player.addAttachment(this);
        permissions.put(player.getUniqueId(), attachment);
        calculateAttachment(player);
    }

    protected void unregisterPlayer(Player player) {
        if (permissions.containsKey(player.getUniqueId())) {
            try {
                player.removeAttachment(permissions.get(player.getUniqueId()));
            }
            catch (IllegalArgumentException ex) {
                debug("Unregistering " + player.getName() + ": player did not have attachment");
            }
            permissions.remove(player.getUniqueId());
        } else {
            debug("Unregistering " + player.getName() + ": was not registered");
        }
    }

    protected void refreshForPlayer(UUID player) {
        saveConfig();
        debug("Refreshing for player " + player);

        Player onlinePlayer = getServer().getPlayer(player);
        if (onlinePlayer != null) {
            calculateAttachment(onlinePlayer);
        }
    }

    private void fillChildGroups(HashSet<String> childGroups, String group) {
        if (childGroups.contains(group)) return;
        childGroups.add(group);

        for (String key : getNode("groups").getKeys(false)) {
            for (String parent : getNode("groups/" + key).getStringList("inheritance")) {
                if (parent.equalsIgnoreCase(group)) {
                    fillChildGroups(childGroups, key);
                }
            }
        }
    }

    protected void refreshForGroup(String group) {
        saveConfig();

        // build the set of groups which are children of "group"
        // e.g. if Bob is only a member of "expert" which inherits "user", he
        // must be updated if the permissions of "user" change
        HashSet<String> childGroups = new HashSet<String>();
        fillChildGroups(childGroups, group);
        debug("Refreshing for group " + group + " (total " + childGroups.size() + " subgroups)");

        for (UUID uuid : permissions.keySet()) {
            Player player = getServer().getPlayer(uuid);
            ConfigurationSection node = getUserNode(player);

            // if the player isn't in the config, act like they're in default
            List<String> groupList = (node != null) ? node.getStringList("groups") : Arrays.asList("default");
            for (String userGroup : groupList) {
                if (childGroups.contains(userGroup)) {
                    calculateAttachment(player);
                    break;
                }
            }
        }
    }

    protected void refreshPermissions() {
        debug("Refreshing all permissions (for " + permissions.size() + " players)");
        for (UUID player : permissions.keySet()) {
            calculateAttachment(getServer().getPlayer(player));
        }
    }
    
    protected ConfigurationSection getNode(String node) {
        for (String entry : getConfig().getKeys(true)) {
            if (node.equalsIgnoreCase(entry) && getConfig().isConfigurationSection(entry)) {
                return getConfig().getConfigurationSection(entry);
            }
        }
        return null;
    }

    protected ConfigurationSection getUserNode(Player player) {
        ConfigurationSection sec = getNode("users/" + player.getUniqueId());
        if (sec == null) {
            sec = getNode("users/" + player.getName());
            if (sec != null) {
                getConfig().set(sec.getCurrentPath(), null);
                getConfig().set("users/" + player.getUniqueId(), sec);
                debug("Migrated " + player.getName() + " to their UUID in config");
                saveConfig();
            }
        }
        return sec;
    }

    protected ConfigurationSection createNode(String node) {
        ConfigurationSection sec = getConfig();
        for (String piece : node.split("/")) {
            ConfigurationSection sec2 = getNode(sec == getConfig() ? piece : sec.getCurrentPath() + "/" + piece);
            if (sec2 == null) {
                sec2 = sec.createSection(piece);
            }
            sec = sec2;
        }
        return sec;
    }

    protected HashMap<String, Boolean> getAllPerms(String desc, String path) {
        ConfigurationSection node = getNode(path);
        
        int failures = 0;
        String firstFailure = "";

        // Make an attempt to autofix incorrect nesting
        boolean fixed = false, fixedNow = true;
        while (fixedNow) {
            fixedNow = false;
            for (String key : node.getKeys(true)) {
                if (node.isBoolean(key) && key.contains("/")) {
                    node.set(key.replace("/", "."), node.getBoolean(key));
                    node.set(key, null);
                    fixed = fixedNow = true;
                } else if (node.isConfigurationSection(key) && node.getConfigurationSection(key).getKeys(true).size() == 0) {
                    node.set(key, null);
                    fixed = fixedNow = true;
                }
            }
        }
        if (fixed) {
            getLogger().info("Fixed broken nesting in " + desc + ".");
            saveConfig();
        }

        LinkedHashMap<String, Boolean> result = new LinkedHashMap<String, Boolean>();
        // Do the actual getting of permissions
        for (String key : node.getKeys(false)) {
            if (node.isBoolean(key)) {
                result.put(key, node.getBoolean(key));
            } else {
                ++failures;
                if (firstFailure.length() == 0) {
                    firstFailure = key;
                }
            }
        }
        
        if (failures == 1) {
            getLogger().warning("In " + desc + ": " + firstFailure + " is non-boolean.");
        } else if (failures > 1) {
            getLogger().warning("In " + desc + ": " + firstFailure + " is non-boolean (+" + (failures-1) + " more).");
        }
        
        return result;
    }
    
    protected void debug(String message) {
        if (getConfig().getBoolean("debug", false)) {
            getLogger().info("Debug: " + message);
        }
    }

    protected void calculateAttachment(Player player) {
        if (player == null) {
            return;
        }
        PermissionAttachment attachment = permissions.get(player.getUniqueId());
        if (attachment == null) {
            debug("Calculating permissions on " + player.getName() + ": attachment was null");
            return;
        }

        Map<String, Boolean> values = calculatePlayerPermissions(player, player.getWorld().getName());

        // Fill the attachment reflectively so we don't recalculate for each permission
        // it turns out there's a lot of permissions!
        Map<String, Boolean> dest = reflectMap(attachment);
        dest.clear();
        dest.putAll(values);
        debug("Calculated permissions on " + player.getName() + ": " + dest.size() + " values");

        player.recalculatePermissions();
    }

    // -- Private stuff

    private Field pField;

    @SuppressWarnings("unchecked")
    private Map<String, Boolean> reflectMap(PermissionAttachment attachment) {
        try {
            if (pField == null) {
                pField = PermissionAttachment.class.getDeclaredField("permissions");
                pField.setAccessible(true);
            }
            return (Map<String, Boolean>) pField.get(attachment);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // normally, LinkedHashMap.put (and thus putAll) will not reorder the list
    // if that key is already in the map, which we don't want - later puts should
    // always be bumped to the end of the list
    private <K, V> void put(Map<K, V> dest, K key, V value) {
        dest.remove(key);
        dest.put(key, value);
    }

    private <K, V> void putAll(Map<K, V> dest, Map<K, V> src) {
        for (Map.Entry<K, V> entry : src.entrySet()) {
            put(dest, entry.getKey(), entry.getValue());
        }
    }

    private Map<String, Boolean> calculatePlayerPermissions(Player player, String world) {
        ConfigurationSection node = getUserNode(player);

        // if the player isn't in the config, act like they're in default
        if (node == null) {
            return calculateGroupPermissions("default", world);
        }

        String nodePath = node.getCurrentPath();
        Map<String, Boolean> perms = new LinkedHashMap<String, Boolean>();

        // first, apply the player's groups (getStringList returns an empty list if not found)
        // later groups override earlier groups
        for (String group : node.getStringList("groups")) {
            putAll(perms, calculateGroupPermissions(group, world));
        }

        // now apply user-specific permissions
        if (getNode(nodePath + "/permissions") != null) {
            putAll(perms, getAllPerms("user " + player, nodePath + "/permissions"));
        }

        // now apply world- and user-specific permissions
        if (getNode(nodePath + "/worlds/" + world) != null) {
            putAll(perms, getAllPerms("user " + player + " world " + world, nodePath + "/worlds/" + world));
        }

        return perms;
    }

    private Map<String, Boolean> calculateGroupPermissions(String group, String world) {
        return calculateGroupPermissions0(new HashSet<String>(), group, world);
    }

    private Map<String, Boolean> calculateGroupPermissions0(Set<String> recursionBuffer, String group, String world) {
        String groupNode = "groups/" + group;

        // if the group's not in the config, nothing
        if (getNode(groupNode) == null) {
            return new LinkedHashMap<String, Boolean>();
        }

        recursionBuffer.add(group);
        Map<String, Boolean> perms = new LinkedHashMap<String, Boolean>();

        // first apply any parent groups (see calculatePlayerPermissions for more)
        for (String parent : getNode(groupNode).getStringList("inheritance")) {
            if (recursionBuffer.contains(parent)) {
                getLogger().warning("In group " + group + ": recursive inheritance from " + parent);
                continue;
            }

            putAll(perms, calculateGroupPermissions0(recursionBuffer, parent, world));
        }

        // now apply the group's permissions
        if (getNode(groupNode + "/permissions") != null) {
            putAll(perms, getAllPerms("group " + group, groupNode + "/permissions"));
        }

        // now apply world-specific permissions
        if (getNode(groupNode + "/worlds/" + world) != null) {
            putAll(perms, getAllPerms("group " + group + " world " + world, groupNode + "/worlds/" + world));
        }

        return perms;
    }

}
