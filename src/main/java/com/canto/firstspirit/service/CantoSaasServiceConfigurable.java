package com.canto.firstspirit.service;

import static com.canto.firstspirit.service.CantoSaasServiceConfigurable.ServiceConfiguration.defaultConfiguration;
import static com.canto.firstspirit.service.CantoSaasServiceImpl.SERVICE_NAME;

import com.espirit.ps.psci.genericconfiguration.EventListener;
import com.espirit.ps.psci.genericconfiguration.EventType;
import com.espirit.ps.psci.genericconfiguration.GenericConfigPanel;
import com.espirit.ps.psci.genericconfiguration.Values;
import de.espirit.common.base.Logging;
import de.espirit.firstspirit.agency.ModuleAdminAgent;
import de.espirit.firstspirit.agency.SpecialistsBroker;
import de.espirit.firstspirit.module.ServerEnvironment;

public class CantoSaasServiceConfigurable extends GenericConfigPanel<ServerEnvironment> {

  // 48h In use timespan
  static final long DEFAULT_IN_USE_TIMESPAN_MS = 48 * 60 * 60 * 1000L;
  // 5h Validity timespan
  static final long DEFAULT_VALIDITY_TIMESPAN_MS = 5 * 60 * 60 * 1000L;
  // 4h Update Interval
  static final long DEFAULT_UPDATE_INTERVAL_MS = 4 * 60 * 60 * 1000L;

  public static final String PARAM_USE_CACHE = "useCache";
  public static final String PARAM_RESTART_SERVICE_ON_SAVE = "restartServiceOnSave";
  public static final String PARAM_TENANT = "tenant";
  public static final String PARAM_APP_ID = "appId";
  public static final String PARAM_APP_SECRET = "appSecret";
  public static final String PARAM_USER_ID = "userId";
  public static final String PARAM_OAUTH_BASE_URL = "OAuthBaseUrl";
  public static final String PARAM_CACHE_SIZE = "cacheSize";
  public static final String PARAM_CACHE_ITEM_LIFESPAN_MS = "cacheItemLifeSpan";
  public static final String PARAM_CACHE_UPDATE_TIMESPAN_MS = "cacheAutoUpdateTimespan";
  public static final String PARAM_CACHE_IN_USE_TIMESPAN_MS = "cacheInUseTimespan";
  public static final String PARAM_BATCH_UPDATE_SIZE = "batchUpdateSize";
  public static final String PARAM_RATE_LIMIT_RETRY_COUNT = "rateLimitRetryCount";
  public static final String PARAM_TIME_BUFFER_IN_MS = "timeBufferInMs";

  public static final String PARAM_USE_SINGLE_FETCH_REQUEST_LIMITER = "useSingleFetchRequestLimiter";
  public static final String PARAM_SINGLE_FETCH_MAX_REQUESTS_PER_MINUTE = "singleFetchMaxRequestsPerMinute";
  public static final String PARAM_SINGLE_FETCH_REQUESTS_WITHOUT_DELAY = "singleFetchRequestsWithoutDelay";

  public static final String PARAM_USE_BATCH_FETCH_REQUEST_LIMITER = "useBatchFetchRequestLimiter";
  public static final String PARAM_BATCH_FETCH_MAX_REQUESTS_PER_MINUTE = "batchFetchMaxRequestsPerMinute";
  public static final String PARAM_BATCH_FETCH_REQUESTS_WITHOUT_DELAY = "batchFetchRequestsWithoutDelay";

  public static final String PARAM_USE_SEARCH_REQUEST_LIMITER = "useSearchRequestLimiter";
  public static final String PARAM_SEARCH_MAX_REQUESTS_PER_MINUTE = "searchMaxRequestsPerMinute";
  public static final String PARAM_SEARCH_REQUESTS_WITHOUT_DELAY = "searchRequestsWithoutDelay";

  @Override protected void configure() {

    // Restart Canto Service on Config Change only if Restart On Save Checkbox is checked
    EventListener restartServiceAfterStoreListener = new EventListener() {

      @Override public void handleEvent(EventType eventType) {
        if (eventType.equals(EventType.AFTER_STORE)) {
          try {
            Boolean restartOnSave = getFormValue(PARAM_RESTART_SERVICE_ON_SAVE);
            if (restartOnSave != null && restartOnSave) {

              Logging.logInfo("Service Configuration changed. Restarting Canto Service", this.getClass());
              SpecialistsBroker broker = getEnvironment().getBroker();

              ModuleAdminAgent moduleAdminAgent = broker.requireSpecialist(ModuleAdminAgent.TYPE);

              moduleAdminAgent.stopService(SERVICE_NAME);
              moduleAdminAgent.startService(SERVICE_NAME);
              Logging.logInfo("Restart Successful", this.getClass());
            }
          } catch (Exception e) {
            Logging.logError("Unable to restart CantoService after Config Change", e, this.getClass());
          }

        }
      }
    };

    addListener(restartServiceAfterStoreListener);

    builder()
        .checkbox("Use Single Fetch Request Limiter (recommended)", PARAM_USE_SINGLE_FETCH_REQUEST_LIMITER, defaultConfiguration.useSingleFetchRequestLimiter, "Limits single asset fetch requests per minute")
        .text("[Single Fetch Limiter] Max Requests per Minute", PARAM_SINGLE_FETCH_MAX_REQUESTS_PER_MINUTE, String.valueOf(defaultConfiguration.singleFetchMaxRequestsPerMinute), "use Canto Api Limit")
        .text("[Single Fetch Limiter] Requests per Minute without delay", PARAM_SINGLE_FETCH_REQUESTS_WITHOUT_DELAY, String.valueOf(defaultConfiguration.singleFetchRequestsWithoutDelay), "The first x requests are not delayed. All remaining requests are delayed as long as needed, to ensure staying within Max Requests per Minute")
        .checkbox("Use Batch Fetch Request Limiter (recommended)", PARAM_USE_BATCH_FETCH_REQUEST_LIMITER, defaultConfiguration.useBatchFetchRequestLimiter, "Limits batch asset fetch requests per minute")
        .text("[Batch Fetch Limiter] Max Requests per Minute", PARAM_BATCH_FETCH_MAX_REQUESTS_PER_MINUTE, String.valueOf(defaultConfiguration.batchFetchMaxRequestsPerMinute), "use Canto Api Limit")
        .text("[Batch Fetch Limiter] Requests per Minute without delay", PARAM_BATCH_FETCH_REQUESTS_WITHOUT_DELAY, String.valueOf(defaultConfiguration.batchFetchRequestsWithoutDelay), "The first x requests are not delayed. All remaining requests are delayed as long as needed, to ensure staying within Max Requests per Minute")
        .checkbox("Use Search Request Limiter (recommended)", PARAM_USE_SEARCH_REQUEST_LIMITER, defaultConfiguration.useSearchRequestLimiter, "Limits search requests per minute")
        .text("[Search Limiter] Max Requests per Minute", PARAM_SEARCH_MAX_REQUESTS_PER_MINUTE, String.valueOf(defaultConfiguration.searchMaxRequestsPerMinute), "use Canto Api Limit")
        .text("[Search Limiter] Requests per Minute without delay", PARAM_SEARCH_REQUESTS_WITHOUT_DELAY, String.valueOf(defaultConfiguration.searchRequestsWithoutDelay), "The first x requests are not delayed. All remaining requests are delayed as long as needed, to ensure staying within Max Requests per Minute")
        .hiddenString(PARAM_TIME_BUFFER_IN_MS, String.valueOf(defaultConfiguration.timeBufferInMs))
        .checkbox("Use Cache (recommended)", PARAM_USE_CACHE, defaultConfiguration.useCache, "Caching is crucial for performance")
        .text("Cache Element Size", PARAM_CACHE_SIZE, String.valueOf(defaultConfiguration.cacheSize), "Soft size limit for Cache, not strictly enforced.")
        .text("Cache Item Lifespan in ms", PARAM_CACHE_ITEM_LIFESPAN_MS, String.valueOf(defaultConfiguration.cacheItemLifespanMs), "Time until Cache Item is revalidated on use")
        .text("Cache auto-update timespan in ms", PARAM_CACHE_UPDATE_TIMESPAN_MS, String.valueOf(defaultConfiguration.cacheUpdateTimespanMs), "Timespan after which cache automatically revalidates Item")
        .text("Cache Item In use Timespan in ms", PARAM_CACHE_IN_USE_TIMESPAN_MS, String.valueOf(defaultConfiguration.cacheItemInUseTimespanMs), "Timespan after which cache elements are removed if not requested")
        .text("Batch Update Size", PARAM_BATCH_UPDATE_SIZE, String.valueOf(defaultConfiguration.batchUpdateSize), "Batch size for CacheUpdater")
        .text("Rate Limit Retry Count", PARAM_RATE_LIMIT_RETRY_COUNT, String.valueOf(defaultConfiguration.rateLimitRetryCount), "Number of retries on 429 Too Many Requests, each retry waits 60 seconds")
        .text("Tenant (without https://)", PARAM_TENANT, "", "Enter the name of the canto tenant, e.g. my-company.canto.global")
        .text("OAuth Base URL (com,de,global)", PARAM_OAUTH_BASE_URL, "https://oauth.canto.<region>", "region is one of com/de/global")
        .text("App Id", PARAM_APP_ID, "", "App Id")
        .text("App Secret", PARAM_APP_SECRET, "", "App Secret")
        .text("User", PARAM_USER_ID, "", "User Id bound to generated Access Token")
        .checkbox("Restart Service on Config Save", PARAM_RESTART_SERVICE_ON_SAVE, defaultConfiguration.restartServiceOnSave, "When activated, the Canto Service restarts after closing this window with 'OK'");

  }

  static class ServiceConfiguration {

    static final ServiceConfiguration defaultConfiguration = new ServiceConfiguration( // NOSONAR
        true,
        true, 200, 30,
        true, 200, 30,
        true, 200, 30,
        1000L,
        true, "", "", "", "", "",
        1000, DEFAULT_VALIDITY_TIMESPAN_MS, DEFAULT_UPDATE_INTERVAL_MS, DEFAULT_IN_USE_TIMESPAN_MS, 75, 3);

    final boolean useCache;

    final boolean useSingleFetchRequestLimiter;
    final int singleFetchMaxRequestsPerMinute;
    final int singleFetchRequestsWithoutDelay;

    final boolean useBatchFetchRequestLimiter;
    final int batchFetchMaxRequestsPerMinute;
    final int batchFetchRequestsWithoutDelay;

    final boolean useSearchRequestLimiter;
    final int searchMaxRequestsPerMinute;
    final int searchRequestsWithoutDelay;

    final long timeBufferInMs;

    final int batchUpdateSize;

    final boolean restartServiceOnSave;
    final String apiTenant;
    final String apiOAuthBaseUrl;
    final String apiAppId;
    final String apiAppSecret;
    final String apiUserId;
    final long cacheItemLifespanMs;
    final long cacheUpdateTimespanMs;
    final long cacheItemInUseTimespanMs;
    final int cacheSize;
    final int rateLimitRetryCount;

    private ServiceConfiguration( // NOSONAR
      boolean useCache,
      boolean useSingleFetchRequestLimiter,
      int singleFetchMaxRequestsPerMinute,
      int singleFetchRequestsWithoutDelay,
      boolean useBatchFetchRequestLimiter,
      int batchFetchMaxRequestsPerMinute,
      int batchFetchRequestsWithoutDelay,
      boolean useSearchRequestLimiter,
      int searchMaxRequestsPerMinute,
      int searchRequestsWithoutDelay,
      long timeBufferInMs,
      boolean restartServiceOnSave,
      String apiTenant,
      String apiOAuthBaseUrl,
      String apiAppId,
      String apiAppSecret,
      String apiUserId,
      int cacheSize,
      long cacheItemLifespanMs,
      long cacheUpdateTimespanMs,
      long cacheItemInUseTimespanMs,
      int batchUpdateSize,
      int rateLimitRetryCount) {

      this.useCache = useCache;
      this.useSingleFetchRequestLimiter = useSingleFetchRequestLimiter;
      this.singleFetchMaxRequestsPerMinute = singleFetchMaxRequestsPerMinute;
      this.singleFetchRequestsWithoutDelay = singleFetchRequestsWithoutDelay;
      this.useBatchFetchRequestLimiter = useBatchFetchRequestLimiter;
      this.batchFetchMaxRequestsPerMinute = batchFetchMaxRequestsPerMinute;
      this.batchFetchRequestsWithoutDelay = batchFetchRequestsWithoutDelay;
      this.useSearchRequestLimiter = useSearchRequestLimiter;
      this.searchMaxRequestsPerMinute = searchMaxRequestsPerMinute;
      this.searchRequestsWithoutDelay = searchRequestsWithoutDelay;
      this.timeBufferInMs = timeBufferInMs;
      this.batchUpdateSize = batchUpdateSize;
      this.restartServiceOnSave = restartServiceOnSave;
      this.apiTenant = apiTenant;
      this.apiOAuthBaseUrl = apiOAuthBaseUrl;
      this.apiAppId = apiAppId;
      this.apiAppSecret = apiAppSecret;
      this.apiUserId = apiUserId;
      this.cacheSize = cacheSize;
      this.cacheItemLifespanMs = cacheItemLifespanMs;
      this.cacheUpdateTimespanMs = cacheUpdateTimespanMs;
      this.cacheItemInUseTimespanMs = cacheItemInUseTimespanMs;
      this.rateLimitRetryCount = rateLimitRetryCount;
    }

    static ServiceConfiguration fromServerEnvironment(ServerEnvironment serverEnvironment) {
      try {
        Values configValues = values(serverEnvironment);
        return new ServiceConfiguration(
            configValues.getBoolean(PARAM_USE_CACHE, true),
            configValues.getBoolean(PARAM_USE_SINGLE_FETCH_REQUEST_LIMITER, true),
            Integer.parseInt(configValues.getString(PARAM_SINGLE_FETCH_MAX_REQUESTS_PER_MINUTE, "200")),
            Integer.parseInt(configValues.getString(PARAM_SINGLE_FETCH_REQUESTS_WITHOUT_DELAY, "30")),
            configValues.getBoolean(PARAM_USE_BATCH_FETCH_REQUEST_LIMITER, true),
            Integer.parseInt(configValues.getString(PARAM_BATCH_FETCH_MAX_REQUESTS_PER_MINUTE, "200")),
            Integer.parseInt(configValues.getString(PARAM_BATCH_FETCH_REQUESTS_WITHOUT_DELAY, "30")),
            configValues.getBoolean(PARAM_USE_SEARCH_REQUEST_LIMITER, true),
            Integer.parseInt(configValues.getString(PARAM_SEARCH_MAX_REQUESTS_PER_MINUTE, "200")),
            Integer.parseInt(configValues.getString(PARAM_SEARCH_REQUESTS_WITHOUT_DELAY, "30")),
            Long.parseLong(configValues.getString(PARAM_TIME_BUFFER_IN_MS, "1000")),
            configValues.getBoolean(PARAM_RESTART_SERVICE_ON_SAVE, true),
            configValues.getString(PARAM_TENANT, ""),
            configValues.getString(PARAM_OAUTH_BASE_URL, ""),
            configValues.getString(PARAM_APP_ID, ""),
            configValues.getString(PARAM_APP_SECRET, ""),
            configValues.getString(PARAM_USER_ID, ""),
            Integer.parseInt(configValues.getString(PARAM_CACHE_SIZE, "10000")),
            Long.parseLong(configValues.getString(PARAM_CACHE_ITEM_LIFESPAN_MS, DEFAULT_VALIDITY_TIMESPAN_MS + "")),
            Long.parseLong(configValues.getString(PARAM_CACHE_UPDATE_TIMESPAN_MS, DEFAULT_UPDATE_INTERVAL_MS + "")),
            Long.parseLong(configValues.getString(PARAM_CACHE_IN_USE_TIMESPAN_MS, DEFAULT_IN_USE_TIMESPAN_MS + "")),
            Integer.parseInt(configValues.getString(PARAM_BATCH_UPDATE_SIZE, "75")),
            Integer.parseInt(configValues.getString(PARAM_RATE_LIMIT_RETRY_COUNT, "3")));

      } catch (Exception e) {
        Logging.logError("Unable to parse Server Configuration! Check set values. Using default Config as fallback", e, ServiceConfiguration.class);
        return defaultConfiguration;
      }
    }

    @Override public String toString() {
      return "ServiceConfiguration{" +
          "useCache=" + useCache +
          ", useSingleFetchRequestLimiter=" + useSingleFetchRequestLimiter +
          ", singleFetchMaxRequestsPerMinute=" + singleFetchMaxRequestsPerMinute +
          ", singleFetchRequestsWithoutDelay=" + singleFetchRequestsWithoutDelay +
          ", useBatchFetchRequestLimiter=" + useBatchFetchRequestLimiter +
          ", batchFetchMaxRequestsPerMinute=" + batchFetchMaxRequestsPerMinute +
          ", batchFetchRequestsWithoutDelay=" + batchFetchRequestsWithoutDelay +
          ", useSearchRequestLimiter=" + useSearchRequestLimiter +
          ", searchMaxRequestsPerMinute=" + searchMaxRequestsPerMinute +
          ", searchRequestsWithoutDelay=" + searchRequestsWithoutDelay +
          ", timeBufferInMs=" + timeBufferInMs +
          ", batchUpdateSize=" + batchUpdateSize +
          ", restartServiceOnSave=" + restartServiceOnSave +
          ", apiTenant='" + apiTenant + '\'' +
          ", apiOAuthBaseUrl='" + apiOAuthBaseUrl + '\'' +
          ", apiAppId='" + (apiAppId.length() < 5 ? "EMPTY" : (apiAppId.substring(0, 5) + "...")) + '\'' +
          ", apiAppSecret='" + (apiAppSecret.length() < 5 ? "EMPTY" : (apiAppSecret.substring(0, 5) + "...")) + '\'' +
          ", apiUserId='" + apiUserId + '\'' +
          ", cacheItemLifespanMs=" + cacheItemLifespanMs +
          ", cacheUpdateTimespanMs=" + cacheUpdateTimespanMs +
          ", cacheItemInUseTimespanMs=" + cacheItemInUseTimespanMs +
          ", cacheSize=" + cacheSize +
          '}';
    }
  }

}
