package com.canto.firstspirit.api;

import com.canto.firstspirit.service.cache.ProjectBoundCacheAccess;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CentralCachingTest {

  @Test
  void testCacheHitsAndMisses() {

    CantoApi cantoApi = new CantoApi.Builder()
        .tenant("TENANT")
        .oAuthBaseUrl("OAUTHURL")
        .appId("APP_ID")
        .appSecret("APP_SECRET")
        .userId("USER_ID")
        .projectBoundCacheAccess(new ProjectBoundCacheAccess(null))
        .build();
    Assertions.assertNotNull(cantoApi);
  }


}
