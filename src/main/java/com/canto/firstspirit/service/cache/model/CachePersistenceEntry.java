package com.canto.firstspirit.service.cache.model;

import com.canto.firstspirit.api.model.CantoAsset;

public class CachePersistenceEntry {

  public CantoAsset asset;
  public long lastUsedTimestamp;
  public long lastUpdatedTimestamp;

  @SuppressWarnings("unused")
  public CachePersistenceEntry() {}

  public CachePersistenceEntry(CantoAsset asset, long lastUsedTimestamp, long lastUpdatedTimestamp) {
    this.asset = asset;
    this.lastUsedTimestamp = lastUsedTimestamp;
    this.lastUpdatedTimestamp = lastUpdatedTimestamp;
  }
}