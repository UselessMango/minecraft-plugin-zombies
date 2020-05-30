package com.mango.zombies.gameplay;

import com.mango.zombies.Main;
import com.mango.zombies.PluginCore;
import com.mango.zombies.Time;
import com.mango.zombies.entities.MapEntity;
import com.mango.zombies.gamemodes.base.ZombiesGamemode;
import com.mango.zombies.gameplay.base.BaseGameplayRegisterable;
import com.mango.zombies.gameplay.base.InventoryClickEventRegisterable;
import com.mango.zombies.gameplay.base.PlayerSwapHandsItemEventRegisterable;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GameplaySession extends BaseGameplayRegisterable implements InventoryClickEventRegisterable, PlayerSwapHandsItemEventRegisterable {

    //region Fields
    private boolean hasBegun;
    private boolean isCountdownScheduled;
    private boolean playerToPlayerDamageEnabled;

    private int countdownSeconds;
    private int countdownTaskReference;

    private final List<GameplayPlayer> players = new ArrayList<GameplayPlayer>();

    private final MapEntity map;

    private Scoreboard scoreboard;

    private final UUID uuid;

    private final ZombiesGamemode gamemode;
    //endregion

    //region Getters/Setters
    /**
     * Gets whether the session has begun.
     */
    public boolean hasBegun() {
        return hasBegun;
    }

    /**
     * Gets the gamemode.
     */
    public ZombiesGamemode getGamemode() {
        return gamemode;
    }

    /**
     * Gets the map.
     */
    public MapEntity getMap() {
        return map;
    }

    /**
     * Gets the players in the session.
     */
    public GameplayPlayer[] getPlayers() {
        return players.toArray(new GameplayPlayer[0]);
    }

    /**
     * Gets whether player-to-player damage is enabled.
     */
    public boolean isPlayerToPlayerDamageEnabled() {
        return playerToPlayerDamageEnabled;
    }

    /**
     * Sets whether player-to-player damage is enabled.
     */
    public void setPlayerToPlayerDamage(boolean playerToPlayerDamageEnabled) {
        this.playerToPlayerDamageEnabled = playerToPlayerDamageEnabled;
    }

    /**
     * Gets the session scoreboard.
     */
    public Scoreboard getScoreboard() {
        return scoreboard;
    }

    /**
     * Gets the UUID of this gameplay registerable.
     */
    public UUID getUUID() {
        return uuid;
    }
    //endregion

    //region Event Handlers
    /**
     * Called when an inventory is clicked.
     * @param event The event caused by the click.
     */
    public void onInventoryClick(InventoryClickEvent event) {

        if (!(event.getClickedInventory() instanceof PlayerInventory))
            return;

        PlayerInventory playerInventory = (PlayerInventory)event.getClickedInventory();

        GameplayPlayer gameplayPlayer = null;

        for (GameplayPlayer queryPlayer : players) {

            if (queryPlayer.getPlayer().getUniqueId().equals(playerInventory.getHolder().getUniqueId())) {
                gameplayPlayer = queryPlayer;
                break;
            }
        }

        if (gameplayPlayer != null)
            event.setCancelled(true);
    }

    /**
     * Called when a player swaps the items in their hands.
     * @param event The event caused by the swap.
     */
    public void onPlayerSwapHansItem(PlayerSwapHandItemsEvent event) {

        GameplayPlayer gameplayPlayer = null;

        for (GameplayPlayer queryPlayer : players) {

            if (queryPlayer.getPlayer().getUniqueId().equals(event.getPlayer().getUniqueId())) {
                gameplayPlayer = queryPlayer;
                break;
            }
        }

        if (gameplayPlayer != null)
            event.setCancelled(true);
    }

    /**
     * Called when this gameplay registerable is unregistered.
     */
    @Override
    public void onUnregistered() {

        super.onUnregistered();

        if (gamemode != null)
            gamemode.endGame();

        for (GameplayPlayer queryPlayer : players)
            removePlayer(queryPlayer);

        for (BaseGameplayRegisterable queryRegisterable : PluginCore.getGameplayService().getRegisterables()) {

            if (!(queryRegisterable instanceof GameplayEnemy))
                continue;

            GameplayEnemy queryEnemy = (GameplayEnemy)queryRegisterable;

            if (queryEnemy.getGameplaySession() == this)
                queryEnemy.despawn();
        }
    }
    //endregion

    //region Public Methods
    /**
     * Adds a player to the session.
     */
    public void addPlayer(Player player) {

        GameplayPlayer gameplayPlayer = new GameplayPlayer(null, player);

        gameplayPlayer.register();
        gameplayPlayer.cachePlayerState();

        int requiredPlayers = gamemode.getMinimumPlayers() - (players.size() + 1);

        String startingMessage;

        if (requiredPlayers > 0)
            startingMessage = "Waiting for " + requiredPlayers + " " + (requiredPlayers == 1 ? "player" : "players") + " to join.";

        else {

            scheduleCountdown();

            startingMessage = "Session starting in " + countdownSeconds + " seconds.";
        }

        for (GameplayPlayer existingPlayer : players)
            sendMessage(existingPlayer.getPlayer(), player.getDisplayName() + " has joined. " + startingMessage);

        players.add(gameplayPlayer);

        player.teleport(new Location(player.getWorld(), map.getLobbyPoint().getX(), map.getLobbyPoint().getY(), map.getLobbyPoint().getZ()));

        player.getInventory().clear();
        player.setGameMode(GameMode.SURVIVAL);

        sendMessage(player, startingMessage);
    }

    /**
     * Removes a player from the session.
     */
    public void removePlayer(GameplayPlayer gameplayPlayer) {

        players.remove(gameplayPlayer);

        gameplayPlayer.unregister();

        String leftMessage = gameplayPlayer.getPlayer().getDisplayName() + " has left.";
        int requiredPlayers = gamemode.getMinimumPlayers() - players.size();

        if (requiredPlayers > 0) {

            cancelCountdown();

            leftMessage += " Waiting for " + requiredPlayers + " " + (requiredPlayers == 1 ? "player" : "players") + " to join.";
        }

        for (GameplayPlayer remainingPlayer : players)
            sendMessage(remainingPlayer.getPlayer(), leftMessage);
    }

    /**
     * Starts the session.
     */
    public void startSession() {

        if (hasBegun)
            return;

        hasBegun = true;

        gamemode.prepare();
    }
    //endregion

    //region Runnables
    private void countdown_runnable() {

        if (players.size() < 1) {

            cancelCountdown();
            applyDefaultCountdown();

            return;
        }

        countdownSeconds--;

        if (countdownSeconds <= 10) {

            for (GameplayPlayer gameplayPlayer : players)
                gameplayPlayer.getPlayer().sendMessage(ChatColor.GREEN + "Session starting in: " + countdownSeconds + ".");
        }

        if (countdownSeconds > 0)
            return;

        cancelCountdown();

        startSession();
    }
    //endregion

    //region Constructors
    public GameplaySession(MapEntity map, ZombiesGamemode gamemode) {

        this.gamemode = gamemode;
        this.map = map;

        uuid = UUID.randomUUID();

        applyDefaultCountdown();
    }
    //endregion

    //region Private Methods
    private void applyDefaultCountdown() {
        countdownSeconds = (int)Math.round(Time.fromMinutes(PluginCore.getGameplayConfig().getLobbyCountdown()).totalSeconds());
    }

    private void cancelCountdown() {

        if (!isCountdownScheduled)
            return;

        isCountdownScheduled = false;

        Main.getInstance().getServer().getScheduler().cancelTask(countdownTaskReference);
    }

    private void scheduleCountdown() {

        if (isCountdownScheduled)
            return;

        isCountdownScheduled = true;

        Main instance = Main.getInstance();

        countdownTaskReference = instance.getServer().getScheduler().scheduleSyncRepeatingTask(instance, this::countdown_runnable, 0, Time.fromSeconds(1).totalTicks());
    }

    private void sendMessage(Player player, String message) {

        if (countdownSeconds > 10)
            player.sendMessage(ChatColor.GREEN + message);
    }
    //endregion
}
