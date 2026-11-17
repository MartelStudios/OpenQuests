package com.martelstudios.openquests.core.commands.subcommands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.auth.ProfileServiceClient;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.ParseResult;
import com.hypixel.hytale.server.core.command.system.arguments.system.FlagArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.permissions.provider.HytalePermissionsProvider;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.OpenQuestsServices;
import com.martelstudios.openquests.core.persistence.TeamInvitation;
import com.martelstudios.openquests.core.persistence.TeamRecord;
import com.martelstudios.openquests.core.teams.OpenQuestTeamType;
import com.martelstudios.openquests.core.teams.TeamService;
import com.martelstudios.openquests.core.teams.Teams;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

import static com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes.GAME_PROFILE_LOOKUP;
import static com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes.STRING;
import static com.martelstudios.openquests.core.commands.OpenQuestArgTypes.TEAM_TYPE;

/**
 * {@code /oquest team}: players manage their own team, admins anyone's under {@code admin}. Admin
 * forms live apart rather than as variants: a variant shares its command's permission.
 */
public class TeamCommand extends AbstractCommandCollection {

    public TeamCommand(@Nonnull OpenQuestsServices services) {
        super("team", "Manages the teams quests are shared in");

        // Named here: a command reads its parent's group one level up only
        setPermissionGroups(HytalePermissionsProvider.GROUP_ADVENTURER);
        Tools tools = new Tools(services.getTeams(), services.getTeamService(), services.getPlatform().clock());
        addSubCommand(new InviteCommand(tools));
        addSubCommand(new AcceptCommand(tools));
        addSubCommand(new RenameCommand(tools));
        addSubCommand(new LeaveCommand(tools));
        addSubCommand(new MembersCommand(tools));
        addSubCommand(new AdminCommand(tools));
    }

    /**
     * What every team command reads teams through, and how it names them.
     */
    private record Tools(@Nonnull Teams teams, @Nonnull TeamService service, @Nonnull Clock clock) {

        void run(CommandContext context, Runnable command) {
            service.execute(command).exceptionally(failure -> {
                context.sendMessage(Message.translation("openquests.commands.team.failed"));
                com.hypixel.hytale.logger.HytaleLogger.forEnclosingClass().atWarning().withCause(failure).log("Team command failed");
                return null;
            });
        }

        @Nonnull
        static Message name(@Nonnull TeamRecord team) {
            String name = team.getName();
            return name != null ? Message.raw(name) : Message.translation("openquests.team.default_name").param("player", team.getNamedAfter());
        }

        @Nonnull
        static String playerName(@Nonnull UUID playerId) {
            PlayerRef online = Universe.get().getPlayer(playerId);
            return online != null ? online.getUsername() : playerId.toString();
        }

        long hoursLeft(@Nonnull TeamInvitation invitation) {
            return Math.max(1, (invitation.expiresAt() - clock.millis() + 3_599_999) / 3_600_000);
        }

        /**
         * @return {@code true} when the change may go on: nothing would be lost, or the sender said so.
         */
        boolean confirmed(@Nonnull CommandContext context, @Nullable TeamRecord lost, boolean confirm) {
            if (lost == null || confirm) return true;

            context.sendMessage(Message.translation("openquests.commands.team.would_lose").param("team", name(lost)));
            return false;
        }

        /**
         * @return the team that id names, or that player's team of that type; {@code null} once the
         * sender was told none matches.
         */
        @Nullable
        TeamRecord resolve(@Nonnull CommandContext context, @Nonnull String input, @Nonnull String type) {
            TeamRecord team = null;
            try {
                team = teams.get(UUID.fromString(input));
            } catch (IllegalArgumentException e) {
                ParseResult result = new ParseResult();
                ProfileServiceClient.PublicGameProfile profile = GAME_PROFILE_LOOKUP.parse(input, result);
                UUID teamId = profile == null || result.failed() ? null : service.currentTeamOf(profile.getUuid(), type);
                if (teamId != null) team = teams.get(teamId);
            }

            if (team == null) {
                context.sendMessage(Message.translation("openquests.commands.team.unknown").param("team", input));
                return null;
            }
            if (!team.getType().equals(type)) {
                context.sendMessage(Message.translation("openquests.commands.team.wrong_type"));
                return null;
            }
            return team;
        }

        void describe(@Nonnull CommandContext context, @Nonnull TeamRecord team) {
            List<String> members = team.getMembers().stream().map(Tools::playerName).toList();
            context.sendMessage(Message.translation("openquests.commands.team.members")
                                       .param("team", name(team))
                                       .param("id", team.getId().toString())
                                       .param("owner", playerName(team.getOwner()))
                                       .param("members", String.join(", ", members)));
        }
    }

    /**
     * The sender's own team, run on their world thread where their components answer.
     */
    private abstract static class OwnTeamCommand extends AbstractPlayerCommand {

        @Nonnull
        protected final RequiredArg<OpenQuestTeamType> typeArg = withRequiredArg("type", "Team type", TEAM_TYPE);

        @Nonnull
        protected final Tools tools;

        OwnTeamCommand(@Nonnull String name, @Nonnull String description, @Nonnull Tools tools) {
            super(name, description);
            this.tools = tools;
        }

        @Override
        protected final void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                                     @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef sender, @Nonnull World world) {
            tools.run(context, () -> executeStored(context, sender));
        }

        protected abstract void executeStored(@Nonnull CommandContext context, @Nonnull PlayerRef sender);

        @Nullable
        protected UUID ownTeam(@Nonnull CommandContext context, @Nonnull PlayerRef sender) {
            UUID teamId = tools.service().currentTeamOf(sender.getUuid(), context.get(typeArg).getId());
            if (teamId == null) context.sendMessage(Message.translation("openquests.commands.team.none"));
            return teamId;
        }
    }

    private static class InviteCommand extends OwnTeamCommand {

        @Nonnull
        private final RequiredArg<ProfileServiceClient.PublicGameProfile> playerArg = withRequiredArg("player", "server.commands.argtype.player.desc", GAME_PROFILE_LOOKUP);

        InviteCommand(@Nonnull Tools tools) {
            super("invite", "Invites a player into your team", tools);
        }

        @Override
        protected void executeStored(@Nonnull CommandContext context, @Nonnull PlayerRef sender) {
            UUID teamId = ownTeam(context, sender);
            if (teamId == null) return;

            ProfileServiceClient.PublicGameProfile invited = context.get(playerArg);
            try {
                TeamRecord team = tools.teams().invite(teamId, sender.getUuid(), invited.getUuid());
                TeamInvitation invitation = team.getInvitations().stream().filter(it -> it.player().equals(invited.getUuid())).findFirst().orElseThrow();
                context.sendMessage(Message.translation("openquests.commands.team.invited").param("player", invited.getUsername()).param("team", Tools.name(team)).param("hours", tools.hoursLeft(invitation)));
            } catch (IllegalArgumentException e) {
                notOwner(context, tools, teamId);
            }
        }
    }

    private static class AcceptCommand extends OwnTeamCommand {

        @Nonnull
        private final OptionalArg<ProfileServiceClient.PublicGameProfile> inviterArg = withOptionalArg("inviter", "The player who invited you, when several did", GAME_PROFILE_LOOKUP);

        @Nonnull
        private final FlagArg confirmFlag = withFlagArg("confirm", "Goes on even if it disbands your current team and its quests");

        AcceptCommand(@Nonnull Tools tools) {
            super("accept", "Joins a team that invited you", tools);
        }

        @Override
        protected void executeStored(@Nonnull CommandContext context, @Nonnull PlayerRef sender) {
            String type = context.get(typeArg).getId();
            ProfileServiceClient.PublicGameProfile inviter = context.get(inviterArg);

            List<TeamRecord> inviting = tools.teams().invitationsOf(sender.getUuid(), type).stream()
                                             .filter(team -> inviter == null || invitationTo(team, sender.getUuid()).invitedBy().equals(inviter.getUuid()))
                                             .toList();
            if (inviting.isEmpty()) {
                context.sendMessage(Message.translation("openquests.commands.team.no_invitation"));
                return;
            }
            if (inviting.size() > 1) {
                context.sendMessage(Message.translation("openquests.commands.team.several_invitations"));
                for (TeamRecord team : inviting) {
                    TeamInvitation invitation = invitationTo(team, sender.getUuid());
                    context.sendMessage(Message.translation("openquests.commands.team.invitation").param("inviter", Tools.playerName(invitation.invitedBy())).param("team", Tools.name(team)).param("hours", tools.hoursLeft(invitation)));
                }
                return;
            }


            if (!tools.confirmed(context, tools.service().wouldLose(sender.getUuid(), type), confirmFlag.get(context))) return;

            TeamRecord team = inviting.get(0);
            try {
                tools.service().accept(sender.getUuid(), sender.getUsername(), type, team.getId());
            } catch (IllegalArgumentException e) {
                context.sendMessage(Message.translation("openquests.commands.team.unknown").param("team", Tools.name(team)));
                return;
            }
            context.sendMessage(Message.translation("openquests.commands.team.joined").param("player", sender.getUsername()).param("team", Tools.name(team)));
        }

        @Nonnull
        private static TeamInvitation invitationTo(@Nonnull TeamRecord team, @Nonnull UUID playerId) {
            return team.getInvitations().stream().filter(it -> it.player().equals(playerId)).findFirst().orElseThrow();
        }
    }

    private static class RenameCommand extends OwnTeamCommand {

        @Nonnull
        private final RequiredArg<String> nameArg = withRequiredArg("name", "The new name", STRING);

        RenameCommand(@Nonnull Tools tools) {
            super("rename", "Renames your team", tools);
        }

        @Override
        protected void executeStored(@Nonnull CommandContext context, @Nonnull PlayerRef sender) {
            UUID teamId = ownTeam(context, sender);
            if (teamId == null) return;

            try {
                TeamRecord team = tools.teams().rename(teamId, sender.getUuid(), context.get(nameArg));
                context.sendMessage(Message.translation("openquests.commands.team.renamed").param("team", Tools.name(team)));
            } catch (IllegalArgumentException e) {
                notOwner(context, tools, teamId);
            }
        }
    }

    private static class LeaveCommand extends OwnTeamCommand {

        LeaveCommand(@Nonnull Tools tools) {
            super("leave", "Leaves your team, keeping a copy of its quests in a party", tools);
        }

        @Override
        protected void executeStored(@Nonnull CommandContext context, @Nonnull PlayerRef sender) {
            if (ownTeam(context, sender) == null) return;

            tools.service().change(sender.getUuid(), sender.getUsername(), context.get(typeArg).getId(), null);
            context.sendMessage(Message.translation("openquests.commands.team.left").param("player", sender.getUsername()));
        }
    }

    private static class MembersCommand extends OwnTeamCommand {

        MembersCommand(@Nonnull Tools tools) {
            super("members", "Lists your team's members", tools);
        }

        @Override
        protected void executeStored(@Nonnull CommandContext context, @Nonnull PlayerRef sender) {
            UUID teamId = ownTeam(context, sender);
            TeamRecord team = teamId == null ? null : tools.teams().get(teamId);
            if (team != null) tools.describe(context, team);
        }
    }

    private static void notOwner(@Nonnull CommandContext context, @Nonnull Tools tools, @Nonnull UUID teamId) {
        TeamRecord team = tools.teams().get(teamId);
        context.sendMessage(Message.translation("openquests.commands.team.not_owner").param("team", team == null ? Message.raw(teamId.toString()) : Tools.name(team)));
    }

    /**
     * {@code /oquest team admin}: any player's team, online or not.
     */
    private static class AdminCommand extends AbstractCommandCollection {

        AdminCommand(@Nonnull Tools tools) {
            super("admin", "Manages any player's team");
            setPermissionGroups(HytalePermissionsProvider.GROUP_WORLD_EDITOR);
            addSubCommand(new AdminCreateCommand(tools));
            addSubCommand(new AdminJoinCommand(tools));
            addSubCommand(new AdminLeaveCommand(tools));
            addSubCommand(new AdminMembersCommand(tools));
        }
    }

    private abstract static class TeamAdminCommand extends CommandBase {
        private final Tools commandTools;

        TeamAdminCommand(String name, String description, Tools tools) {
            super(name, description);
            commandTools = tools;
        }

        @Override
        protected final void executeSync(@Nonnull CommandContext context) {
            commandTools.run(context, () -> executeStored(context));
        }

        protected abstract void executeStored(@Nonnull CommandContext context);
    }

    private static class AdminCreateCommand extends TeamAdminCommand {

        @Nonnull
        private final RequiredArg<OpenQuestTeamType> typeArg = withRequiredArg("type", "Team type", TEAM_TYPE);

        @Nonnull
        private final RequiredArg<ProfileServiceClient.PublicGameProfile> playerArg = withRequiredArg("player", "server.commands.argtype.player.desc", GAME_PROFILE_LOOKUP);

        @Nonnull
        private final OptionalArg<String> nameArg = withOptionalArg("name", "The team's name, the player's team when left out", STRING);

        @Nonnull
        private final FlagArg confirmFlag = withFlagArg("confirm", "Goes on even if it disbands the player's current team and its quests");

        @Nonnull
        private final Tools tools;

        AdminCreateCommand(@Nonnull Tools tools) {
            super("create", "Creates a team owned by a player", tools);
            this.tools = tools;
        }

        @Override
        protected void executeStored(@Nonnull CommandContext context) {
            String type = context.get(typeArg).getId();
            ProfileServiceClient.PublicGameProfile profile = context.get(playerArg);
            if (!tools.confirmed(context, tools.service().wouldLose(profile.getUuid(), type), confirmFlag.get(context))) return;

            TeamRecord team = tools.service().create(profile.getUuid(), profile.getUsername(), type, context.get(nameArg));
            context.sendMessage(Message.translation("openquests.commands.team.created").param("team", Tools.name(team)).param("id", team.getId().toString()).param("player", profile.getUsername()));
        }
    }

    private static class AdminJoinCommand extends TeamAdminCommand {

        @Nonnull
        private final RequiredArg<ProfileServiceClient.PublicGameProfile> playerArg = withRequiredArg("player", "server.commands.argtype.player.desc", GAME_PROFILE_LOOKUP);

        @Nonnull
        private final RequiredArg<OpenQuestTeamType> typeArg = withRequiredArg("type", "Team type", TEAM_TYPE);

        @Nonnull
        private final RequiredArg<String> teamArg = withRequiredArg("team", "A team id, or the name of one of its members", STRING);

        @Nonnull
        private final FlagArg confirmFlag = withFlagArg("confirm", "Goes on even if it disbands the player's current team and its quests");

        @Nonnull
        private final Tools tools;

        AdminJoinCommand(@Nonnull Tools tools) {
            super("join", "Puts a player in a team", tools);
            this.tools = tools;
        }

        @Override
        protected void executeStored(@Nonnull CommandContext context) {
            String type = context.get(typeArg).getId();
            ProfileServiceClient.PublicGameProfile profile = context.get(playerArg);
            TeamRecord team = tools.resolve(context, context.get(teamArg), type);
            if (team == null) return;
            if (!tools.confirmed(context, tools.service().wouldLose(profile.getUuid(), type), confirmFlag.get(context))) return;

            tools.service().change(profile.getUuid(), profile.getUsername(), type, team.getId());
            context.sendMessage(Message.translation("openquests.commands.team.joined").param("player", profile.getUsername()).param("team", Tools.name(team)));
        }
    }

    private static class AdminLeaveCommand extends TeamAdminCommand {

        @Nonnull
        private final RequiredArg<ProfileServiceClient.PublicGameProfile> playerArg = withRequiredArg("player", "server.commands.argtype.player.desc", GAME_PROFILE_LOOKUP);

        @Nonnull
        private final RequiredArg<OpenQuestTeamType> typeArg = withRequiredArg("type", "Team type", TEAM_TYPE);

        @Nonnull
        private final Tools tools;

        AdminLeaveCommand(@Nonnull Tools tools) {
            super("leave", "Takes a player out of their team", tools);
            this.tools = tools;
        }

        @Override
        protected void executeStored(@Nonnull CommandContext context) {
            String type = context.get(typeArg).getId();
            ProfileServiceClient.PublicGameProfile profile = context.get(playerArg);
            if (tools.service().currentTeamOf(profile.getUuid(), type) == null) {
                context.sendMessage(Message.translation("openquests.commands.team.player_none").param("player", profile.getUsername()));
                return;
            }

            tools.service().change(profile.getUuid(), profile.getUsername(), type, null);
            context.sendMessage(Message.translation("openquests.commands.team.left").param("player", profile.getUsername()));
        }
    }

    private static class AdminMembersCommand extends TeamAdminCommand {

        @Nonnull
        private final RequiredArg<OpenQuestTeamType> typeArg = withRequiredArg("type", "Team type", TEAM_TYPE);

        @Nonnull
        private final RequiredArg<String> teamArg = withRequiredArg("team", "A team id, or the name of one of its members", STRING);

        @Nonnull
        private final Tools tools;

        AdminMembersCommand(@Nonnull Tools tools) {
            super("members", "Lists a team's members", tools);
            this.tools = tools;
        }

        @Override
        protected void executeStored(@Nonnull CommandContext context) {
            TeamRecord team = tools.resolve(context, context.get(teamArg), context.get(typeArg).getId());
            if (team != null) tools.describe(context, team);
        }
    }
}
