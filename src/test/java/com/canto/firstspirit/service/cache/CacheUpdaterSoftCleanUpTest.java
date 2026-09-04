package com.canto.firstspirit.service.cache;

import com.canto.firstspirit.api.model.CantoAsset;
import com.canto.firstspirit.service.cache.model.CacheElement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class CacheUpdaterSoftCleanUpTest {

  private static final long IN_USE_TIMESPAN_MS = TimeUnit.HOURS.toMillis(1);
  private static final long VALID_TIMESPAN_MS = TimeUnit.HOURS.toMillis(1);

  private CentralCache centralCache;
  private CacheUpdater cacheUpdater;

  @BeforeEach
  void setUp() {
    centralCache = new CentralCache(null, 100, VALID_TIMESPAN_MS, TimeUnit.HOURS.toMillis(4), IN_USE_TIMESPAN_MS, 10);
    cacheUpdater = centralCache.cacheUpdater;
    cacheUpdater.shutdown(); // stop background thread — tests invoke cleanup manually
  }

  @AfterEach
  void tearDown() {
    centralCache.cacheMap.clear();
  }

  // --- performSoftCleanUp(false): only remove elements not in use ---

  @Test
  void softCleanUpWithoutInvalidRemoval_keepsInUseValidElements() throws Exception {
    putInCache("image/valid-inuse", true, true);

    performSoftCleanUp(false);

    assertTrue(centralCache.cacheMap.containsKey("image/valid-inuse"));
  }

  @Test
  void softCleanUpWithoutInvalidRemoval_keepsInUseInvalidElements() throws Exception {
    putInCache("image/invalid-inuse", true, false);

    performSoftCleanUp(false);

    assertTrue(centralCache.cacheMap.containsKey("image/invalid-inuse"),
        "Invalid but in-use elements must NOT be removed before batch update");
  }

  @Test
  void softCleanUpWithoutInvalidRemoval_removesNotInUseValidElements() throws Exception {
    putInCache("image/valid-notinuse", false, true);

    performSoftCleanUp(false);

    assertFalse(centralCache.cacheMap.containsKey("image/valid-notinuse"));
  }

  @Test
  void softCleanUpWithoutInvalidRemoval_removesNotInUseInvalidElements() throws Exception {
    putInCache("image/invalid-notinuse", false, false);

    performSoftCleanUp(false);

    assertFalse(centralCache.cacheMap.containsKey("image/invalid-notinuse"));
  }

  @Test
  void softCleanUpWithoutInvalidRemoval_returnsCorrectCount() throws Exception {
    putInCache("image/valid-inuse", true, true);
    putInCache("image/invalid-inuse", true, false);
    putInCache("image/valid-notinuse", false, true);
    putInCache("image/invalid-notinuse", false, false);

    int removed = performSoftCleanUp(false);

    assertEquals(2, removed, "Only the two not-in-use elements should be removed");
    assertEquals(2, centralCache.cacheMap.size());
  }

  // --- performSoftCleanUp(true): also remove invalid elements ---

  @Test
  void softCleanUpWithInvalidRemoval_keepsInUseValidElements() throws Exception {
    putInCache("image/valid-inuse", true, true);

    performSoftCleanUp(true);

    assertTrue(centralCache.cacheMap.containsKey("image/valid-inuse"));
  }

  @Test
  void softCleanUpWithInvalidRemoval_removesInvalidInUseElements() throws Exception {
    putInCache("image/invalid-inuse", true, false);

    performSoftCleanUp(true);

    assertFalse(centralCache.cacheMap.containsKey("image/invalid-inuse"),
        "Invalid elements must be removed during cache size cleanup");
  }

  @Test
  void softCleanUpWithInvalidRemoval_removesNotInUseValidElements() throws Exception {
    putInCache("image/valid-notinuse", false, true);

    performSoftCleanUp(true);

    assertFalse(centralCache.cacheMap.containsKey("image/valid-notinuse"));
  }

  @Test
  void softCleanUpWithInvalidRemoval_returnsCorrectCount() throws Exception {
    putInCache("image/valid-inuse", true, true);
    putInCache("image/invalid-inuse", true, false);
    putInCache("image/valid-notinuse", false, true);
    putInCache("image/invalid-notinuse", false, false);

    int removed = performSoftCleanUp(true);

    assertEquals(3, removed, "Not-in-use and invalid elements should all be removed");
    assertEquals(1, centralCache.cacheMap.size());
    assertTrue(centralCache.cacheMap.containsKey("image/valid-inuse"));
  }

  // --- helpers ---

  private void putInCache(String key, boolean inUse, boolean valid) throws Exception {
    String[] parts = key.split("/", 2);
    CantoAsset asset = createAsset(parts[1], parts[0]);
    CacheElement element = new CacheElement(asset, VALID_TIMESPAN_MS, IN_USE_TIMESPAN_MS);
    if (!inUse) {
      element.lastUsedTimestamp = 0;
    }
    if (!valid) {
      element.lastUpdatedTimestamp = 0;
    }
    centralCache.cacheMap.put(key, element);
  }

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

  private int performSoftCleanUp(boolean removeInvalidElements) throws Exception {
    Method method = CacheUpdater.class.getDeclaredMethod("performSoftCleanUp", boolean.class);
    method.setAccessible(true);
    return (int) method.invoke(cacheUpdater, removeInvalidElements);
  }
}
