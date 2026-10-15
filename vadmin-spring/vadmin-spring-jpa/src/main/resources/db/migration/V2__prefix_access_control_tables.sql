alter table users rename to ${vadminTablePrefix}users;
alter table roles rename to ${vadminTablePrefix}roles;
alter table permissions rename to ${vadminTablePrefix}permissions;
alter table user_roles rename to ${vadminTablePrefix}user_roles;
alter table role_permissions rename to ${vadminTablePrefix}role_permissions;
alter table audit_entries rename to ${vadminTablePrefix}audit_entries;
