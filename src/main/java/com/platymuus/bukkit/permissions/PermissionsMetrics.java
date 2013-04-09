package com.platymuus.bukkit.permissions;

import org.bukkit.configuration.ConfigurationSection;
import org.mcstats.Metrics;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Handles statistic tracking for PermissionsBukkit
 */
public class PermissionsMetrics {

    private final PermissionsPlugin plugin;
    private final Metrics metrics;

    public PermissionsMetrics(PermissionsPlugin plugin) throws IOException {
        this.plugin = plugin;
        metrics = new Metrics(plugin);

        // don't bother with the rest if it's off
        if (metrics.isOptOut()) return;

        setupFeaturesUsed();
        setupUsage();

        metrics.start();
    }

    private ConfigurationSection getSection(String name) {
        return plugin.getConfig().getConfigurationSection(name);
    }

    private void setupFeaturesUsed() {
        Metrics.Graph graph = metrics.createGraph("Features Used");

        // Whether any users except ConspiracyWizard have permissions set on them
        graph.addPlotter(new BooleanPlotter("Per-user permissions") {
            @Override
            protected boolean value() {
                ConfigurationSection sec = getSection("users");
                if (sec == null) return false;

                for (String key : sec.getKeys(false)) {
                    if (!key.equalsIgnoreCase("ConspiracyWizard") && (sec.isConfigurationSection(key + "/permissions") || sec.isConfigurationSection(key + "/worlds"))) {
                        return true;
                    }
                }
                return false;
            }
        });

        // Whether any users or groups have per-world permissions set on them
        graph.addPlotter(new BooleanPlotter("Per-world permissions") {
            @Override
            protected boolean value() {
                return found(getSection("users")) || found(getSection("groups"));
            }

            private boolean found(ConfigurationSection section) {
                if (section == null) return false;

                for (String key : section.getKeys(false)) {
                    if (section.isConfigurationSection(key + "/worlds")) {
                        return true;
                    }
                }
                return false;
            }
        });

        // Whether any groups inherit from other groups
        graph.addPlotter(new BooleanPlotter("Group inheritance") {
            @Override
            protected boolean value() {
                ConfigurationSection section = getSection("groups");

                for (String key : section.getKeys(false)) {
                    if (section.contains(key + "/inheritance")) {
                        return true;
                    }
                }
                return false;
            }
        });

        // Whether anti-build is used at all
        graph.addPlotter(new BooleanPlotter("Anti-build enabled") {
            @Override
            protected boolean value() {
                return antiBuildEnabled();
            }
        });

        // Whether an anti-build message is set
        graph.addPlotter(new BooleanPlotter("Anti-build message") {
            @Override
            protected boolean value() {
                return antiBuildEnabled() && plugin.getConfig().getString("messages.build", "").length() > 0;
            }
        });

        // Whether anti-build is set to true for the default group
        graph.addPlotter(new BooleanPlotter("Anti-build default") {
            @Override
            protected boolean value() {
                ConfigurationSection sec = getSection("groups");
                if (sec == null) return false;

                ConfigurationSection def = sec.getConfigurationSection("default");
                if (def == null) return false;

                ConfigurationSection perms = sec.getConfigurationSection("permissions");
                return perms != null && perms.isBoolean("permissions.build") && !perms.getBoolean("permissions.build");
            }
        });

        // Whether permissions.yml contains anything
        graph.addPlotter(new BooleanPlotter("Uses permissions.yml") {
            @Override
            protected boolean value() {
                // Note that this doesn't help if permissions.yml is in a nonstandard place, but c'est la vie
                File file = new File("permissions.yml");
                return file.isFile() && file.length() > 5;

            }
        });

        // Whether ConspiracyWizard is still in the configuration
        graph.addPlotter(new BooleanPlotter("Wizards") {
            @Override
            protected boolean value() {
                ConfigurationSection sec = getSection("users");
                return sec != null && sec.isConfigurationSection("ConspiracyWizard");
            }
        });

        // Whether debug mode is enabled
        graph.addPlotter(new BooleanPlotter("Debug mode") {
            @Override
            protected boolean value() {
                return plugin.getConfig().getBoolean("debug", false);
            }
        });
    }

    private boolean antiBuildEnabled() {
        ConfigurationSection section = getSection("groups");
        if (section == null) return false;

        // check each group
        for (String key : section.getKeys(false)) {
            if (section.isConfigurationSection(key)) {
                ConfigurationSection group = section.getConfigurationSection(key);

                // check and see if it sets permissions.build false
                if (group.isConfigurationSection("permissions")) {
                    if (!group.getConfigurationSection("permissions").getBoolean("permissions.build", true)) {
                        return true;
                    }
                }

                // check each world
                if (group.isConfigurationSection("worlds")) {
                    ConfigurationSection worlds = group.getConfigurationSection("worlds");
                    for (String world : worlds.getKeys(false)) {
                        if (worlds.isConfigurationSection(world)) {
                            // ... if THAT sets permissions.build false
                            if (!worlds.getConfigurationSection(world).getBoolean("permissions.build", true)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    private void setupUsage() {
        Metrics.Graph graph = metrics.createGraph("Usage");

        // Users in the config
        graph.addPlotter(new Metrics.Plotter("Users") {
            @Override
            public int getValue() {
                ConfigurationSection sec = getSection("groups");
                return sec == null ? 0 : sec.getKeys(false).size();
            }
        });

        // Groups in the config
        graph.addPlotter(new Metrics.Plotter("Groups") {
            @Override
            public int getValue() {
                ConfigurationSection sec = getSection("users");
                return sec == null ? 0 : sec.getKeys(false).size();
            }
        });

        // Number of permission nodes set throughout config
        graph.addPlotter(new Metrics.Plotter("Permissions") {
            @Override
            public int getValue() {
                return count(getSection("groups")) + count(getSection("users"));
            }

            private int count(ConfigurationSection section) {
                if (section == null) return 0;

                int total = 0;
                for (String key : section.getKeys(false)) {
                    if (section.isConfigurationSection(key)) {
                        ConfigurationSection individual = section.getConfigurationSection(key);

                        if (individual.isConfigurationSection("permissions")) {
                            total += individual.getConfigurationSection("permissions").getKeys(false).size();
                        }

                        if (individual.isConfigurationSection("worlds")) {
                            ConfigurationSection worlds = individual.getConfigurationSection("worlds");
                            for (String world : worlds.getKeys(false)) {
                                if (worlds.isConfigurationSection(world)) {
                                    total += worlds.getConfigurationSection(world).getKeys(false).size();
                                }
                            }
                        }
                    }
                }
                return total;
            }
        });

        // Unique worlds with per-world permissions set
        graph.addPlotter(new Metrics.Plotter("Worlds") {
            @Override
            public int getValue() {
                Set<String> worlds = new HashSet<String>();
                fill(worlds, getSection("groups"));
                fill(worlds, getSection("users"));
                return worlds.size();
            }

            private void fill(Set<String> results, ConfigurationSection section) {
                for (String key : section.getKeys(false)) {
                    if (section.isConfigurationSection(key)) {
                        ConfigurationSection individual = section.getConfigurationSection(key);
                        if (individual.isConfigurationSection("worlds")) {
                            ConfigurationSection worlds = individual.getConfigurationSection("worlds");
                            results.addAll(worlds.getKeys(false));
                        }
                    }
                }
            }
        });
    }

    private abstract static class BooleanPlotter extends Metrics.Plotter {
        protected BooleanPlotter(String name) {
            super(name);
        }

        @Override
        public int getValue() {
            try {
                return value() ? 1 : 0;
            }
            catch (Throwable t) {
                return 0;
            }
        }

        protected abstract boolean value();
    }
}
