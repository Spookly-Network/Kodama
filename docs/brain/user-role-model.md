# User & Role Model

## Purpose
Define the core user/role data model used by the Brain module for RBAC and future auth flows.

## What changed
- Added `User`, `RoleEntity`, and `UserRole` entities to the Brain domain.
- Added repositories for users, roles, and user-role mappings.
- Added a Flyway migration to create `users`, `roles`, and `user_roles` tables.
- Defined `Role` as an enum for `ADMIN`, `OPERATOR`, and `VIEWER`.

## How to use / impact
- Create roles first, then assign them to users through `User.addRole`.
- Persist `User` to write `user_roles` via cascade.
- Role values are stored as strings matching the `Role` enum names.

## Edge cases / risks
- Duplicate user-role pairs will be rejected by the `user_roles` primary key.
- Roles are currently created via persistence; there is no seed data.

## Links
- Migration: `backend/brain/src/main/resources/db/migration/V10__create_user_role_tables.sql`
- Entities: `backend/brain/src/main/java/net/spookly/kodama/brain/domain/user/User.java`
- Entities: `backend/brain/src/main/java/net/spookly/kodama/brain/domain/user/RoleEntity.java`
- Entities: `backend/brain/src/main/java/net/spookly/kodama/brain/domain/user/UserRole.java`
