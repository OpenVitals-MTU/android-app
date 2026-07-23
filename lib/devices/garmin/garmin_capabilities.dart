import 'dart:typed_data';

/// The capabilities a Garmin watch declares in the CONFIGURATION handshake.
///
/// The watch sends a 15-byte bitmap — 120 flags, one per entry below, in this
/// exact order: bit `n` lives in byte `n ~/ 8` at bit `n % 8`. The order IS the
/// wire format, so entries must never be reordered or removed; unknown flags
/// keep their `unkNNN` names for that reason.
///
/// Ported from Gadgetbridge's `GarminCapability` (AGPLv3).
enum GarminCapability {
  connectMobileFitLink('CONNECT_MOBILE_FIT_LINK'),
  golfFitLink('GOLF_FIT_LINK'),
  vivokidJrFitLink('VIVOKID_JR_FIT_LINK'),
  sync('SYNC'),
  deviceInitiatesSync('DEVICE_INITIATES_SYNC'),
  hostInitiatedSyncRequests('HOST_INITIATED_SYNC_REQUESTS'),
  gncs('GNCS'),
  advancedMusicControls('ADVANCED_MUSIC_CONTROLS'),
  findMyPhone('FIND_MY_PHONE'),
  findMyWatch('FIND_MY_WATCH'),
  connectiqHttp('CONNECTIQ_HTTP'),
  connectiqSettings('CONNECTIQ_SETTINGS'),
  connectiqWatchAppDownload('CONNECTIQ_WATCH_APP_DOWNLOAD'),
  connectiqWidgetDownload('CONNECTIQ_WIDGET_DOWNLOAD'),
  connectiqWatchFaceDownload('CONNECTIQ_WATCH_FACE_DOWNLOAD'),
  connectiqDataFieldDownload('CONNECTIQ_DATA_FIELD_DOWNLOAD'),
  connectiqAppManagement('CONNECTIQ_APP_MANAGEMENT'),
  courseDownload('COURSE_DOWNLOAD'),
  workoutDownload('WORKOUT_DOWNLOAD'),
  golfCourseDownload('GOLF_COURSE_DOWNLOAD'),
  deltaSoftwareUpdateFiles('DELTA_SOFTWARE_UPDATE_FILES'),
  fitpay('FITPAY'),
  livetrack('LIVETRACK'),
  livetrackAutoStart('LIVETRACK_AUTO_START'),
  livetrackMessaging('LIVETRACK_MESSAGING'),
  groupLivetrack('GROUP_LIVETRACK'),
  weatherConditions('WEATHER_CONDITIONS'),
  weatherAlerts('WEATHER_ALERTS'),
  gpsEphemerisDownload('GPS_EPHEMERIS_DOWNLOAD'),
  explicitArchive('EXPLICIT_ARCHIVE'),
  swingSensor('SWING_SENSOR'),
  swingSensorRemote('SWING_SENSOR_REMOTE'),
  incidentDetection('INCIDENT_DETECTION'),
  trueup('TRUEUP'),
  instantInput('INSTANT_INPUT'),
  segments('SEGMENTS'),
  audioPromptLap('AUDIO_PROMPT_LAP'),
  audioPromptPaceSpeed('AUDIO_PROMPT_PACE_SPEED'),
  audioPromptHeartRate('AUDIO_PROMPT_HEART_RATE'),
  audioPromptPower('AUDIO_PROMPT_POWER'),
  audioPromptNavigation('AUDIO_PROMPT_NAVIGATION'),
  audioPromptCadence('AUDIO_PROMPT_CADENCE'),
  sportGeneric('SPORT_GENERIC'),
  sportRunning('SPORT_RUNNING'),
  sportCycling('SPORT_CYCLING'),
  sportTransition('SPORT_TRANSITION'),
  sportFitnessEquipment('SPORT_FITNESS_EQUIPMENT'),
  sportSwimming('SPORT_SWIMMING'),
  stopSyncAfterSoftwareUpdate('STOP_SYNC_AFTER_SOFTWARE_UPDATE'),
  calendar('CALENDAR'),
  wifiSetup('WIFI_SETUP'),
  smsNotifications('SMS_NOTIFICATIONS'),
  basicMusicControls('BASIC_MUSIC_CONTROLS'),
  audioPromptsSpeech('AUDIO_PROMPTS_SPEECH'),
  deltaSoftwareUpdates('DELTA_SOFTWARE_UPDATES'),
  garminDeviceInfoFileType('GARMIN_DEVICE_INFO_FILE_TYPE'),
  sportProfileSetup('SPORT_PROFILE_SETUP'),
  hsaSupport('HSA_SUPPORT'),
  sportStrength('SPORT_STRENGTH'),
  sportCardio('SPORT_CARDIO'),
  unionPay('UNION_PAY'),
  ipass('IPASS'),
  ciqAudioContentProvider('CIQ_AUDIO_CONTENT_PROVIDER'),
  unionPayInternational('UNION_PAY_INTERNATIONAL'),
  requestPairFlow('REQUEST_PAIR_FLOW'),
  locationUpdate('LOCATION_UPDATE'),
  lteSupport('LTE_SUPPORT'),
  deviceDrivenLivetrackSupport('DEVICE_DRIVEN_LIVETRACK_SUPPORT'),
  customCannedTextListSupport('CUSTOM_CANNED_TEXT_LIST_SUPPORT'),
  exploreSync('EXPLORE_SYNC'),
  incidentDetectAndAssistance('INCIDENT_DETECT_AND_ASSISTANCE'),
  currentTimeRequestSupport('CURRENT_TIME_REQUEST_SUPPORT'),
  contactsSupport('CONTACTS_SUPPORT'),
  launchRemoteCiqAppSupport('LAUNCH_REMOTE_CIQ_APP_SUPPORT'),
  deviceMessages('DEVICE_MESSAGES'),
  waypointTransfer('WAYPOINT_TRANSFER'),
  multiLinkService('MULTI_LINK_SERVICE'),
  oauthCredentials('OAUTH_CREDENTIALS'),
  golf9Plus9('GOLF_9_PLUS_9'),
  antiTheftAlarm('ANTI_THEFT_ALARM'),
  inreach('INREACH'),
  eventSharing('EVENT_SHARING'),
  unk82('UNK_82'),
  unk83('UNK_83'),
  unk84('UNK_84'),
  unk85('UNK_85'),
  unk86('UNK_86'),
  unk87('UNK_87'),
  unk88('UNK_88'),
  unk89('UNK_89'),
  unk90('UNK_90'),
  unk91('UNK_91'),
  realtimeSettings('REALTIME_SETTINGS'),
  unk93('UNK_93'),
  unk94('UNK_94'),
  unk95('UNK_95'),
  unk96('UNK_96'),
  unk97('UNK_97'),
  unk98('UNK_98'),
  unk99('UNK_99'),
  unk100('UNK_100'),
  unk101('UNK_101'),
  unk102('UNK_102'),
  unk103('UNK_103'),
  unk104('UNK_104'),
  unk105('UNK_105'),
  unk106('UNK_106'),
  unk107('UNK_107'),
  unk108('UNK_108'),
  unk109('UNK_109'),
  unk110('UNK_110'),
  unk111('UNK_111'),
  unk112('UNK_112'),
  unk113('UNK_113'),
  unk114('UNK_114'),
  unk115('UNK_115'),
  unk116('UNK_116'),
  unk117('UNK_117'),
  unk118('UNK_118'),
  unk119('UNK_119');

  const GarminCapability(this.wireName);

  /// Garmin's own name, for logs — the Dart identifier is a rendering of it.
  final String wireName;

  /// Index in the bitmap, which is simply declaration order.
  int get bit => index;
}

/// Decodes the CONFIGURATION bitmap into the capabilities it sets.
///
/// A short buffer is not an error: a future watch may send fewer bytes than we
/// know flags, and everything past the end is simply absent.
Set<GarminCapability> decodeGarminCapabilities(Uint8List bits) {
  final out = <GarminCapability>{};
  for (final capability in GarminCapability.values) {
    final byte = capability.bit ~/ 8;
    if (byte >= bits.length) break;
    if (bits[byte] & (1 << (capability.bit % 8)) != 0) out.add(capability);
  }
  return out;
}
