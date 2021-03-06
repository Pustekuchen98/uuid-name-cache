package de.bergwerklabs.uuidcache.server.cache.uuid;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import de.bergwerklabs.api.cache.pojo.PlayerNameToUuidMapping;
import de.bergwerklabs.atlantis.api.logging.AtlantisLogger;
import de.bergwerklabs.framework.commons.database.tablebuilder.Database;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Yannic Rieger on 10.03.2018.
 *
 * <p>Contains methods for resolving {@link UUID}s to names and vice versa. The results will be
 * cached in {@link LoadingCache}s improved performance.
 *
 * @author Yannic Rieger
 */
public class UuidCache {

  LoadingCache<UUID, PlayerNameToUuidMapping> uuidToName;
  LoadingCache<String, PlayerNameToUuidMapping> nameToUuid;

  private final AtlantisLogger LOGGER = AtlantisLogger.getLogger(getClass());
  private AbstractCacheLoader<UUID, PlayerNameToUuidMapping> uuidNameLoader;
  private AbstractCacheLoader<String, PlayerNameToUuidMapping> nameUuidLoader;

  /** @param database {@link Database} containg the {@code uuidcache} table. */
  public UuidCache(@NotNull Database database) {
    this.uuidNameLoader = new UuidToNameCacheLoader(this, database);
    this.nameUuidLoader = new NameToUuidCacheLoader(this, database);

    this.uuidToName =
        CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build(this.uuidNameLoader);
    this.nameToUuid =
        CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build(this.nameUuidLoader);
  }

  public void shutdown() {
    this.uuidNameLoader.shutdown();
    this.nameUuidLoader.shutdown();
  }

  public void addEntryIfNotPresent(@NotNull PlayerNameToUuidMapping mapping) {
    this.uuidToName.asMap().putIfAbsent(mapping.getUuid(), mapping);
    this.nameToUuid.asMap().putIfAbsent(mapping.getName(), mapping);
  }

  /**
   * Resolves the {@link UUID} to the corresponding Minecraft player name.
   *
   * @param uuid {@link UUID} of the player.
   * @return a {@link PlayerNameToUuidMapping} containing the {@link UUID} and name of the player.
   *     <b>NOTE:</b> The name of the player will have the correct spelling.
   */
  public PlayerNameToUuidMapping resolveUuidToName(@NotNull UUID uuid) {
    return this.getFromCache(uuid, this.uuidToName);
  }

  /**
   * Resolves the name to a {@link UUID}.
   *
   * @param name name of the player.
   * @return a {@link PlayerNameToUuidMapping} containing the {@link UUID} and name of the player.
   *     <b>NOTE:</b> The name of the player will have the correct spelling.
   */
  public PlayerNameToUuidMapping resolveNameToUuid(@NotNull String name) {
    return this.getFromCache(name, this.nameToUuid);
  }

  /**
   * Gets an entry from cache.
   *
   * @param key Key associated with the value
   * @param cache Cache to get the value from.
   * @param <K> Type of key
   * @param <V> Type of value
   * @return {@link Optional} containing the loaded if present.
   */
  private <K, V> V getFromCache(@NotNull K key, @NotNull LoadingCache<K, V> cache) {
    try {
      return cache.get(key);
    } catch (Exception e) {
      this.LOGGER.error("Could not retrieve value from cache for key " + key.toString(), e);
    }
    return null;
  }
}
