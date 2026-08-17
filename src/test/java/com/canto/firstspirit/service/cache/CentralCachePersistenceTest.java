package com.canto.firstspirit.service.cache;

import static org.junit.jupiter.api.Assertions.*;

import com.canto.firstspirit.api.model.CantoAsset;
import com.canto.firstspirit.service.cache.model.CacheElement;
import com.canto.firstspirit.service.cache.model.CachePersistenceEntry;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CentralCachePersistenceTest {

  private static final long IN_USE_TIMESPAN_MS = TimeUnit.HOURS.toMillis(1);
  private static final long VALID_TIMESPAN_MS = TimeUnit.HOURS.toMillis(1);

  private CentralCache centralCache;

  @BeforeEach
  void setUp() {
    centralCache = new CentralCache(null, 100, VALID_TIMESPAN_MS, TimeUnit.HOURS.toMillis(4), IN_USE_TIMESPAN_MS, 10);
    centralCache.cacheUpdater.shutdown(); // stop background thread — tests drive cache state manually
  }

  @AfterEach
  void tearDown() {
    centralCache.cacheMap.clear();
  }

  // --- getEntriesForPersistence ---

  @Test
  void getEntriesForPersistence_emptyCache_returnsEmptyList() {
    assertTrue(centralCache.getEntriesForPersistence().isEmpty());
  }

  @Test
  void getEntriesForPersistence_returnsEntryMatchingCachedElement() throws Exception {
    CantoAsset asset = createAsset("123", "image");
    centralCache.addElement(asset);
    CacheElement element = centralCache.cacheMap.get("image/123");

    List<CachePersistenceEntry> entries = centralCache.getEntriesForPersistence();

    assertEquals(1, entries.size());
    CachePersistenceEntry entry = entries.get(0);
    assertSame(asset, entry.asset);
    assertEquals(element.lastUsedTimestamp, entry.lastUsedTimestamp);
    assertEquals(element.lastUpdatedTimestamp, entry.lastUpdatedTimestamp);
  }

  @Test
  void getEntriesForPersistence_returnsOneEntryPerCachedElement() throws Exception {
    centralCache.addElement(createAsset("1", "image"));
    centralCache.addElement(createAsset("2", "image"));
    centralCache.addElement(createAsset("3", "document"));

    List<CachePersistenceEntry> entries = centralCache.getEntriesForPersistence();

    assertEquals(3, entries.size());
  }

  // --- loadPersistedEntries ---

  @Test
  void loadPersistedEntries_populatesCacheUnderAssetPath() throws Exception {
    CantoAsset asset = createAsset("123", "image");
    CachePersistenceEntry entry = new CachePersistenceEntry(asset, System.currentTimeMillis(), System.currentTimeMillis());

    centralCache.loadPersistedEntries(List.of(entry));

    CacheElement loaded = centralCache.cacheMap.get("image/123");
    assertNotNull(loaded);
    assertSame(asset, loaded.asset);
  }

  @Test
  void loadPersistedEntries_loadsMultipleEntries() throws Exception {
    CachePersistenceEntry entry1 = new CachePersistenceEntry(createAsset("1", "image"), System.currentTimeMillis(), System.currentTimeMillis());
    CachePersistenceEntry entry2 = new CachePersistenceEntry(createAsset("2", "document"), System.currentTimeMillis(), System.currentTimeMillis());

    centralCache.loadPersistedEntries(List.of(entry1, entry2));

    assertEquals(2, centralCache.cacheMap.size());
    assertTrue(centralCache.cacheMap.containsKey("image/1"));
    assertTrue(centralCache.cacheMap.containsKey("document/2"));
  }

  @Test
  void loadPersistedEntries_skipsEntriesWithNullAsset() {
    CachePersistenceEntry entryWithoutAsset = new CachePersistenceEntry(null, System.currentTimeMillis(), System.currentTimeMillis());

    centralCache.loadPersistedEntries(List.of(entryWithoutAsset));

    assertTrue(centralCache.cacheMap.isEmpty());
  }

  @Test
  void loadPersistedEntries_resetsLastUsedTimestampToNow() throws Exception {
    CantoAsset asset = createAsset("123", "image");
    CachePersistenceEntry entry = new CachePersistenceEntry(asset, 1L, System.currentTimeMillis());

    long before = System.currentTimeMillis();
    centralCache.loadPersistedEntries(List.of(entry));
    long after = System.currentTimeMillis();

    CacheElement loaded = centralCache.cacheMap.get("image/123");
    assertTrue(loaded.lastUsedTimestamp >= before && loaded.lastUsedTimestamp <= after,
        "lastUsedTimestamp of a loaded entry should be reset to now, so it survives the next cleanup");
  }

  @Test
  void loadPersistedEntries_preservesPersistedLastUpdatedTimestamp() throws Exception {
    CantoAsset asset = createAsset("123", "image");
    long staleLastUpdated = System.currentTimeMillis() - VALID_TIMESPAN_MS * 2;
    CachePersistenceEntry entry = new CachePersistenceEntry(asset, System.currentTimeMillis(), staleLastUpdated);

    centralCache.loadPersistedEntries(List.of(entry));

    CacheElement loaded = centralCache.cacheMap.get("image/123");
    assertEquals(staleLastUpdated, loaded.lastUpdatedTimestamp);
    assertFalse(loaded.isValid(), "an entry persisted with a stale lastUpdatedTimestamp is loaded as invalid, triggering revalidation on the next update batch");
  }

  @Test
  void loadPersistedEntries_loadedAssetIsRetrievableViaGetCantoAsset() throws Exception {
    CantoAsset asset = createAsset("123", "image");
    CachePersistenceEntry entry = new CachePersistenceEntry(asset, System.currentTimeMillis(), System.currentTimeMillis());
    centralCache.loadPersistedEntries(List.of(entry));

    CantoAsset result = centralCache.getCantoAsset(com.canto.firstspirit.api.CantoAssetIdentifierFactory.fromCantoAsset(asset));

    assertSame(asset, result);
  }

  // --- round trip ---

  @Test
  void roundTrip_persistedEntriesReproduceOriginalCacheContent() throws Exception {
    centralCache.addElement(createAsset("1", "image"));
    centralCache.addElement(createAsset("2", "document"));
    List<CachePersistenceEntry> persisted = centralCache.getEntriesForPersistence();

    centralCache.cacheMap.clear();
    centralCache.loadPersistedEntries(persisted);

    assertEquals(2, centralCache.cacheMap.size());
    assertTrue(centralCache.cacheMap.containsKey("image/1"));
    assertTrue(centralCache.cacheMap.containsKey("document/2"));
  }

  // --- helpers ---

  private CantoAsset createAsset(String id, String scheme) throws Exception {
    CantoAsset asset = new CantoAsset();
    setField(asset, "id", id);
    setField(asset, "scheme", scheme);
    return asset;
  }

  private void setField(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }
}