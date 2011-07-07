PermissionsBukkit
=================

A plugin providing groups and other permissions configuration for Bukkit's built-in permissions architecture.

Sample configuration file and more info:

```yaml
# PermissionsBukkit configuration file
# 
# A permission node is a string like 'permissions.build', usually starting
# with the name of the plugin. Refer to a plugin's documentation for what
# permissions it cares about. Each node should be followed by true to grant
# that permission or false to revoke it, as in 'permissions.build: true'.
# Some plugins provide permission nodes that map to a group of permissions -
# for example, PermissionsBukkit has 'permissions.*', which automatically
# grants all admin permissions, but you can't specify false for permissions
# of this type.
# Users inherit permissions from the groups they are a part of. If a user is
# not specified here, or does not have a 'groups' node, they will be in the
# group 'default'. Permissions for individual users may also be specified by
# using a 'permissions' node with a list of permission nodes, which will
# override their group permissions.
# 
# Groups can be assigned to players and all their permissions will also be
# assigned to those players. Groups can also inherit permissions from other
# groups. Like user permissions, groups may override the permissions of their
# parent group(s). Unlike users, groups do NOT automatically inherit from
# default.

users:
    SpaceManiac:
        permissions:
            permissions.example: true
        groups:
        - admin
groups:
    default:
        permissions:
            permissions.build: false
    admin:
        inherits:
        - user
        permissions:
            permissions.*: true
    user:
        inherits:
        - default
        permissions:
            permissions.build: true
```