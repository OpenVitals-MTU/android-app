# Flutter → Kotlin test-parity matrix: devices / ui / goldens / integration / fixtures

Legend: PORTED = same logic + equivalent assertions (byte fixtures identical unless noted). DIVERGED = covered but weaker/different. MISSING = no Kotlin coverage and JVM-portable. N/A-WIDGET = widget/golden rendering. N/A-FRAMEWORK = Flutter-only plumbing. BLOCKED = the Kotlin behaviour genuinely differs, so porting the case would mean changing production code; see "Blocked on a behavior decision" at the end.

## test/devices/garmin/garmin_capabilities_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/devices/garmin/GarminCapabilitiesTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| the enum order IS the wire order | PORTED | GarminCapabilitiesTest: `the enum order IS the wire order` | Pins bits 3/9/92 and 120 entries, identical |
| decodes a flag from its byte and bit | PORTED | GarminCapabilitiesTest: `decodes a flag from its byte and bit` | Identical byte fixture (byte 11, bit 4) |
| an all-ones bitmap sets everything | PORTED | GarminCapabilitiesTest: `an all-ones bitmap sets everything` | - |
| an empty bitmap sets nothing | PORTED | GarminCapabilitiesTest: `an empty bitmap sets nothing` | - |
| a short buffer is not an error | PORTED | GarminCapabilitiesTest: `a short buffer is not an error` | Identical 2-byte 0xFF fixture, same 3 assertions |

## test/devices/garmin/garmin_counter_watermark_store_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/devices/garmin/GarminCounterWatermarkStoreTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| starts empty and round-trips through storage | PORTED | GarminCounterWatermarkStoreTest: `starts empty and round-trips through storage` | Instant epoch-millis instead of local DateTime; same round-trip via a second store |
| a later sync of the same day moves the watermark forward | PORTED | GarminCounterWatermarkStoreTest: `a later sync of the same day moves the watermark forward` | - |
| saving one day does not forget the others | PORTED | GarminCounterWatermarkStoreTest: `saving one day does not forget the others` | - |
| keeps the most recent days and drops the oldest | PORTED | GarminCounterWatermarkStoreTest: `keeps the most recent days and drops the oldest` | Same 70-day/60-cap fixture |
| an unreadable entry is dropped rather than guessed at | PORTED | GarminCounterWatermarkStoreTest: `an unreadable entry is dropped rather than guessed at` | Identical raw stored lines |
| a watermark written before the legacy flag reads as not retired | PORTED | GarminCounterWatermarkStoreTest: `a watermark written before the legacy flag reads as not retired` | Identical raw lines |
| the legacy flag survives a save and reload | PORTED | GarminCounterWatermarkStoreTest: `the legacy flag survives a save and reload` | - |
| the per-type maps survive a round trip | PORTED | GarminCounterWatermarkStoreTest: `the per-type maps survive a round trip` | Same maps {0:400,6:3000} etc. |
| the open-bucket seed values survive a round trip | PORTED | GarminCounterWatermarkStoreTest: `the open-bucket seed values survive a round trip` | - |
| a line from before the open-bucket seeds loads them as zero | PORTED | GarminCounterWatermarkStoreTest: `a line from before the open-bucket seeds loads them as zero` | Identical stored line |
| a line from before the maps loads with them null, and stays null | PORTED | GarminCounterWatermarkStoreTest: `a line from before the maps loads with them null, and stays null` | - |
| an unreadable type map drops the line, not just the map | PORTED | GarminCounterWatermarkStoreTest: `an unreadable type map drops the line, not just the map` | Identical stored line |
| clear forgets everything | PORTED | GarminCounterWatermarkStoreTest: `clear forgets everything` | - |

## test/devices/garmin/garmin_device_names_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/devices/garmin/GarminDeviceNamesTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| isGarminSyncDeviceName > matches the accented names the watches actually advertise | PORTED | GarminDeviceNamesTest: `matches the accented names the watches actually advertise` | Same strings |
| isGarminSyncDeviceName > matches the unaccented spellings some firmware uses | PORTED | GarminDeviceNamesTest: `matches the unaccented spellings some firmware uses` | - |
| isGarminSyncDeviceName > matches by family, so an unreleased model still onboards | PORTED | GarminDeviceNamesTest: `matches by family so an unreleased model still onboards` | - |
| isGarminSyncDeviceName > strips the Garmin prefix some models advertise with | PORTED | GarminDeviceNamesTest: `strips the Garmin prefix some models advertise with` | - |
| isGarminSyncDeviceName > does NOT match HRM chest straps | PORTED | GarminDeviceNamesTest: `does NOT match HRM chest straps` | Same 3 strap names |
| isGarminSyncDeviceName > does not match other vendors, blanks or null | PORTED | GarminDeviceNamesTest: `does not match other vendors, blanks or null` | - |
| isGarminSyncDeviceName > also matches Edge bike computers (they sync FIT files too) | PORTED | GarminDeviceNamesTest: `also matches Edge bike computers, they sync FIT files too` | - |
| isGarminWatchName > matches the watch families, not the Edge | PORTED | GarminDeviceNamesTest: `watch name matches the watch families, not the Edge` | - |
| isGarminWatchName > does not match straps, other vendors, blanks or null | PORTED | GarminDeviceNamesTest: `watch name does not match straps, other vendors, blanks or null` | - |
| isGarminBikeComputerName > matches the Edge family, including sub-models and the prefix | PORTED | GarminDeviceNamesTest: `bike computer name matches the Edge family, including sub-models and the prefix` | - |
| isGarminBikeComputerName > is disjoint from the watch families | PORTED | GarminDeviceNamesTest: `bike computer name is disjoint from the watch families` | - |
| isGarminBikeComputerName > does not match straps, other vendors, blanks or null | PORTED | GarminDeviceNamesTest: `bike computer name does not match straps, other vendors, blanks or null` | - |

## test/devices/garmin/garmin_device_state_store_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/devices/garmin/GarminDeviceStateStoreTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| synced file keys > starts empty and round-trips through storage | PORTED | GarminDeviceStateStoreTest: `synced file keys start empty and round-trip through storage` | Same round-trip via second store over same prefs |
| synced file keys > merges without duplicating across runs | PORTED | GarminDeviceStateStoreTest: `synced file keys merge without duplicating across runs` | - |
| synced file keys > keys are scoped per device | PORTED | GarminDeviceStateStoreTest: `synced file keys are scoped per device` | - |
| synced file keys > an empty write is a no-op | PORTED | GarminDeviceStateStoreTest: `an empty synced-keys write is a no-op` | - |
| synced file keys > the set is capped, dropping the oldest keys first | PORTED | GarminDeviceStateStoreTest: `the synced-keys set is capped, dropping the oldest keys first` | Same 3999+2 / 4000-cap fixture |
| capabilities > round-trip through storage by wire name | PORTED | GarminDeviceStateStoreTest: `capabilities round-trip through storage by wire name` | - |
| capabilities > an empty write is a no-op | PORTED | GarminDeviceStateStoreTest: `an empty capabilities write is a no-op` | - |
| clear drops both capabilities and synced-file history | PORTED | GarminDeviceStateStoreTest: `clear drops both capabilities and synced-file history` | Includes reload assertion |

## test/devices/garmin/garmin_file_store_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/devices/garmin/GarminFileStoreTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| writes the raw bytes, creating the directory | PORTED | GarminFileStoreTest: `writes the raw bytes creating the directory` | Same [9,8,7] fixture |
| names files by type and index, not the 65535 file number | PORTED | GarminFileStoreTest: `names files by type and index not the 65535 file number` | Same `sleep_113_` / `.fit` assertions |
| a re-download does not clobber the earlier copy | PORTED | GarminFileStoreTest: `a re-download does not clobber the earlier copy` | - |
| prune removes files past the retention window, keeping recent ones | PORTED | GarminFileStoreTest: `prune removes files past the retention window keeping recent ones` | Same 60/2-day mtimes |
| prune leaves non-FIT files alone | PORTED | GarminFileStoreTest: `prune leaves non-FIT files alone` | - |
| prune on a directory that does not exist is a no-op | PORTED | GarminFileStoreTest: `prune on a directory that does not exist is a no-op` | - |

## test/devices/garmin/garmin_gatt_report_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/devices/garmin/GarminGattReportTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| isSupported > true for the transports this app can drive | PORTED | GarminGattReportTest: `true for the transports this app can drive` | - |
| isSupported > false when the verdict says nothing usable | PORTED | GarminGattReportTest: `false when the verdict says nothing usable` | - |
| describe > renders every service and characteristic under one grep-able tag | PORTED | GarminGattReportTest: `renders every service and characteristic under one grep-able tag` | Identical UUID fixtures and tag assertions |
| describe > an unknown device still dumps what it found | PORTED | GarminGattReportTest: `an unknown device still dumps what it found` | Kotlin adds 2 extra classify() tests beyond the Dart file |

## test/devices/garmin/garmin_messages_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/devices/garmin/GarminMessagesTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| GarminTime > the Garmin epoch maps to 1989-12-31T00:00:00Z | PORTED | GarminMessagesTest: `the Garmin epoch maps to 1989-12-31T000000Z` | - |
| GarminTime > round-trips a real recording instant | PORTED | GarminMessagesTest: `round-trips a real recording instant` | - |
| GarminFileType > maps the FIT sub-types the importer consumes | PORTED | GarminMessagesTest: `maps the FIT sub-types the importer consumes` | Same 4 code pairs |
| GarminFileType > an unmapped sub-type is null, not an error | PORTED | GarminMessagesTest: `an unmapped sub-type is null not an error` | - |
| GarminFileType > virtual types are not wanted by the downloader | PORTED | GarminMessagesTest: `virtual types are not wanted by the downloader` | - |
| GarminDirectory.parse > keeps wanted FIT files and resolves their fields | PORTED | GarminMessagesTest: `keeps wanted FIT files and resolves their fields` | Identical 16-byte record builder |
| GarminDirectory.parse > drops unmapped types and the all-zero sentinel | PORTED | GarminMessagesTest: `drops unmapped types and the all-zero sentinel` | - |
| GarminDirectory.parse > a zero wire timestamp becomes a null date, not the Garmin epoch | PORTED | GarminMessagesTest: `a zero wire timestamp becomes a null date not the Garmin epoch` | - |
| GarminDirectory.parse > an unset file number yields NO dedup key | PORTED | GarminMessagesTest: `an unset file number yields NO dedup key` | Same 113/116/121 fixture |
| GarminDirectory.parse > diagnostics distinguish empty from filtered-out | PORTED | GarminMessagesTest: `diagnostics distinguish empty from filtered-out` | Same skipped strings `6:128/55?`, `7:deviceXml!` |
| GarminDirectory.parse > a trailing partial record is ignored | PORTED | GarminMessagesTest: `a trailing partial record is ignored` | - |
| outbound messages round-trip through the frame layer > download request carries the file index and fresh type | PORTED | GarminMessagesTest: `download request carries the file index and fresh type` | - |
| outbound messages round-trip through the frame layer > file-transfer ack names FILE_TRANSFER_DATA with ACK/OK | PORTED | GarminMessagesTest: `file-transfer ack names FILE_TRANSFER_DATA with ACK and OK` | - |
| outbound messages round-trip through the frame layer > archive flag is 0x10 | PORTED | GarminMessagesTest: `archive flag is 0x10` | - |
| outbound messages round-trip through the frame layer > SYNC_READY encodes as system-event ordinal 8 | PORTED | GarminMessagesTest: `SYNC_READY encodes as system-event ordinal 8` | - |
| inbound message decoding > a proceed-able download status carries the file size | PORTED | GarminMessagesTest: `a proceed-able download status carries the file size` | - |
| inbound message decoding > a non-OK download status does not proceed | PORTED | GarminMessagesTest: `a non-OK download status does not proceed` | - |
| inbound message decoding > a file-transfer data chunk exposes offset, crc and payload | PORTED | GarminMessagesTest: `a file-transfer data chunk exposes offset crc and payload` | Same 0xBEEF/2048 fixture |
| inbound message decoding > an out-of-vocabulary message decodes to unhandled, not an error | PORTED | GarminMessagesTest: `an out-of-vocabulary message decodes to unhandled not an error` | Same 5041 fixture |

## test/devices/garmin/garmin_ml_transport_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/devices/garmin/GarminMlTransportTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| opening the GFDI channel > closes stale handles before registering | PORTED | GarminMlTransportTest: `closes stale handles before registering` | Same 13-byte control packet assertions |
| opening the GFDI channel > the register request names GFDI and asks for plain ML | PORTED | GarminMlTransportTest: `the register request names GFDI and asks for plain ML` | - |
| opening the GFDI channel > becomes ready when the watch grants a handle | PORTED | GarminMlTransportTest: `becomes ready when the watch grants a handle` | - |
| opening the GFDI channel > a refused registration surfaces as an error, not a hang | PORTED | GarminMlTransportTest: `a refused registration surfaces as an error not a hang` | StateError → IllegalStateException |
| opening the GFDI channel > ignores control traffic belonging to another client | PORTED | GarminMlTransportTest: `ignores control traffic belonging to another client` | - |
| opening the GFDI channel > sending before the channel opens is a StateError, not a silent drop | PORTED | GarminMlTransportTest: `sending before the channel opens is an error not a silent drop` | - |
| sending frames > prefixes every write with the granted handle | PORTED | GarminMlTransportTest: `prefixes every write with the granted handle` | - |
| sending frames > a small frame fits one write and round-trips through COBS | PORTED | GarminMlTransportTest: `a small frame fits one write and round-trips through COBS` | - |
| sending frames > a frame larger than the MTU is split, and reassembles exactly | PORTED | GarminMlTransportTest: `a frame larger than the MTU is split and reassembles exactly` | Same 200-byte (i*7) fixture |
| sending frames > a negotiated MTU widens the writes | PORTED | GarminMlTransportTest: `a negotiated MTU widens the writes` | - |
| sending frames > MTU is clamped to the spec floor and ceiling | PORTED | GarminMlTransportTest: `MTU is clamped to the spec floor and ceiling` | - |
| receiving frames > reassembles a frame split across several packets | PORTED | GarminMlTransportTest: `reassembles a frame split across several packets` | - |
| receiving frames > emits two frames delivered back to back | PORTED | GarminMlTransportTest: `emits two frames delivered back to back` | - |
| receiving frames > a packet for an unknown handle is dropped, not misrouted | PORTED | GarminMlTransportTest: `a packet for an unknown handle is dropped not misrouted` | - |
| receiving frames > a corrupt frame is dropped and the stream keeps running | PORTED | GarminMlTransportTest: `a corrupt frame is dropped and the stream keeps running` | - |
| receiving frames > an empty packet is ignored | PORTED | GarminMlTransportTest: `an empty packet is ignored` | Kotlin runs it without opening the channel first; identical assertion |
| a full send/receive loop survives the real chunking both ways | PORTED | GarminMlTransportTest: `a full send-receive loop survives the real chunking both ways` | Same 500-byte (i*11) fixture |

## test/devices/garmin/garmin_primitives_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/devices/garmin/GarminPrimitivesTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| GarminCrc > empty data is 0 | PORTED | GarminPrimitivesTest: `crc of empty data is 0` | - |
| GarminCrc > is deterministic and stays within 16 bits | PORTED | GarminPrimitivesTest: `crc is deterministic and stays within 16 bits` | Same (i*37)&0xFF fixture |
| GarminCrc > respects offset and length | PORTED | GarminPrimitivesTest: `crc respects offset and length` | - |
| GarminByteReader/Writer round-trip > little-endian across every width | PORTED | GarminPrimitivesTest: `little-endian across every width` | Identical values incl. 64-bit |
| GarminByteReader/Writer round-trip > the writer grows past its initial capacity | PORTED | GarminPrimitivesTest: `the writer grows past its initial capacity` | - |
| GarminByteReader/Writer round-trip > patchShort backfills a placeholder in place | PORTED | GarminPrimitivesTest: `patchShort backfills a placeholder in place` | - |
| GarminByteReader/Writer round-trip > readNullTerminatedString consumes its terminator, so the next field reads correctly | PORTED | GarminPrimitivesTest: `readNullTerminatedString consumes its terminator so the next field reads correctly` | Identical bytes |
| GarminByteReader/Writer round-trip > an unterminated string returns the rest of the buffer rather than throwing | PORTED | GarminPrimitivesTest: `an unterminated string returns the rest of the buffer rather than throwing` | - |
| GarminByteReader/Writer round-trip > an empty null-terminated string is empty, not a skipped field | PORTED | GarminPrimitivesTest: `an empty null-terminated string is empty not a skipped field` | - |
| GarminCobs round-trip > data with no zeros | PORTED | GarminPrimitivesTest: `cobs data with no zeros` | Shared expectRoundTrip incl. 0x00 bracket asserts |
| GarminCobs round-trip > data containing zeros | PORTED | GarminPrimitivesTest: `cobs data containing zeros` | - |
| GarminCobs round-trip > a payload that ends in zero | PORTED | GarminPrimitivesTest: `cobs a payload that ends in zero` | - |
| GarminCobs round-trip > a payload that starts in zero | PORTED | GarminPrimitivesTest: `cobs a payload that starts in zero` | - |
| GarminCobs round-trip > a run longer than one max group (>254 bytes) | PORTED | GarminPrimitivesTest: `cobs a run longer than one max group` | Same 600-byte fixture |
| GarminCobs round-trip > a 254-byte zero-free run at the group boundary | PORTED | GarminPrimitivesTest: `cobs a 254-byte zero-free run at the group boundary` | - |
| GarminCobsDecoder streaming > reassembles a frame split across arbitrary chunks | PORTED | GarminPrimitivesTest: `reassembles a frame split across arbitrary chunks` | Byte-at-a-time feed preserved |
| GarminCobsDecoder streaming > pulls two frames concatenated in one buffer | PORTED | GarminPrimitivesTest: `pulls two frames concatenated in one buffer` | - |
| GarminCobsDecoder streaming > resynchronises when the buffer does not start with a pad | PORTED | GarminPrimitivesTest: `resynchronises when the buffer does not start with a pad` | - |
| GarminGfdiFrame > build then parse preserves type and payload | PORTED | GarminPrimitivesTest: `build then parse preserves type and payload` | - |
| GarminGfdiFrame > the length field equals the whole frame | PORTED | GarminPrimitivesTest: `the length field equals the whole frame` | - |
| GarminGfdiFrame > a flipped payload byte fails the CRC check | PORTED | GarminPrimitivesTest: `a flipped payload byte fails the CRC check` | - |
| GarminGfdiFrame > a wrong length field is rejected | PORTED | GarminPrimitivesTest: `a wrong length field is rejected` | - |
| GarminGfdiFrame > an incoming status type has its high bit remapped to the 5000 range | PORTED | GarminPrimitivesTest: `an incoming status type has its high bit remapped to the 5000 range` | Identical hand-built frame |
| GarminGfdiFrame > survives a COBS round-trip (the real transport path) | PORTED | GarminPrimitivesTest: `survives a COBS round-trip (the real transport path)` | - |

## test/devices/garmin/garmin_protobuf_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/devices/garmin/GarminProtobufTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| protobuf encoding > a varint field encodes key then value | PORTED | GarminProtobufTest: `a varint field encodes key then value` | Identical bytes 0x08 0x3C |
| protobuf encoding > a multi-byte varint is little-endian base-128 | PORTED | GarminProtobufTest: `a multi-byte varint is little-endian base-128` | - |
| protobuf encoding > an empty nested message is not the same as an absent one | PORTED | GarminProtobufTest: `an empty nested message is not the same as an absent one` | - |
| protobuf encoding > round-trips through the reader | PORTED | GarminProtobufTest: `round-trips through the reader` | - |
| protobuf encoding > a truncated message yields what was readable, not a crash | PORTED | GarminProtobufTest: `a truncated message yields what was readable not a crash` | - |
| find my watch > start carries a 60-second timeout under the find service | PORTED | GarminProtobufTest: `start carries a 60-second timeout under the find service` | Identical wire bytes |
| find my watch > cancel is an empty message, not a missing one | PORTED | GarminProtobufTest: `cancel is an empty message not a missing one` | - |
| find my watch > OK is 100 — a zero status is NOT success | PORTED | GarminProtobufTest: `OK is 100 — a zero status is NOT success` | - |
| find my watch > an EMPTY response is acceptance — the real watch sends no status | PORTED | GarminProtobufTest: `an EMPTY response is acceptance — the real watch sends no status` | Same captured bytes 62 02 12 00 / 62 02 22 00 |
| find my watch > an unreadable reply is UNKNOWN, never a refusal | PORTED | GarminProtobufTest: `an unreadable reply is UNKNOWN never a refusal` | - |
| protobuf transport > matches a reply to its request by id | PORTED | GarminProtobufTest: `matches a reply to its request by id` | Same real-envelope reply helper |
| protobuf transport > request ids advance, so two requests cannot be confused | PORTED | GarminProtobufTest: `request ids advance so two requests cannot be confused` | - |
| protobuf transport > a COMPLETE message is acknowledged by request id, not generically | PORTED | GarminProtobufTest: `a COMPLETE message is acknowledged by request id not generically` | Same 11-byte ack layout asserts |
| protobuf transport > a reply for an unknown id is consumed, not mistaken for ours | PORTED | GarminProtobufTest: `a reply for an unknown id is consumed not mistaken for ours` | - |
| protobuf transport > reassembles a chunked reply | PORTED | GarminProtobufTest: `reassembles a chunked reply` | - |
| protobuf transport > a dropped link fails the request instead of hanging on it | PORTED | GarminProtobufTest: `a dropped link fails the request instead of hanging on it` | StateError → IllegalStateException via launch/yield |
| protobuf transport > an oversized payload is refused rather than truncated | PORTED | GarminProtobufTest: `an oversized payload is refused rather than truncated` | ArgumentError → IllegalArgumentException |
| unsolicited chunking > reassembles a message the watch sent under its OWN id | PORTED | GarminProtobufTest: `reassembles a message the watch sent under its OWN id` | - |
| unsolicited chunking > acknowledges a chunk with the offset IT declared | PORTED | GarminProtobufTest: `acknowledges a chunk with the offset IT declared` | Same offset=3 assertion |

## test/devices/garmin/garmin_session_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/devices/garmin/GarminSessionTest.kt (sync suites) and /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/devices/garmin/GarminSessionNotificationsTest.kt (notification suites)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| GarminSession happy path > downloads every wanted file, byte-exact across chunks | PORTED | GarminSessionTest: `downloads every wanted file byte-exact across chunks` | Identical FakeWatch wire-format fixture, 8-byte chunks |
| GarminSession happy path > answers the introduction and the auth challenge | PORTED | GarminSessionTest: `answers the introduction and the auth challenge` | - |
| GarminSession happy path > records what the watch said about itself | PORTED | GarminSessionTest: `records what the watch said about itself` | - |
| GarminSession happy path > archives each downloaded file so it is not re-offered | PORTED | GarminSessionTest: `archives each downloaded file so it is not re-offered` | - |
| GarminSession happy path > brackets the sync with SYNC_READY and SYNC_COMPLETE | PORTED | GarminSessionTest: `brackets the sync with SYNC_READY and SYNC_COMPLETE` | - |
| GarminSession happy path > acknowledges every data chunk with the offset reached | PORTED | GarminSessionTest: `acknowledges every data chunk with the offset reached` | Same 12-ack count |
| GarminSession happy path > reports progress through every phase | PORTED | GarminSessionTest: `reports progress through every phase` | - |
| GarminSession resilience > skips a file the watch refuses, and still gets the others | PORTED | GarminSessionTest: `skips a file the watch refuses and still gets the others` | - |
| GarminSession resilience > skips a file whose chunk CRC is wrong | PORTED | GarminSessionTest: `skips a file whose chunk CRC is wrong` | Same CorruptingWatch 0xDEAD CRC |
| GarminSession resilience > files with no dedup key are always fetched, never skipped | PORTED | GarminSessionTest: `files with no dedup key are always fetched never skipped` | - |
| GarminSession resilience > a directory with nothing new completes without downloading | PORTED | GarminSessionTest: `a directory with nothing new completes without downloading` | - |
| GarminSession resilience > an empty directory completes cleanly | PORTED | GarminSessionTest: `an empty directory completes cleanly` | - |
| GarminSession resilience > unmapped file types are never requested | PORTED | GarminSessionTest: `unmapped file types are never requested` | - |
| GarminSession resilience > the capabilities exchange is answered with our own bitmap | PORTED | GarminSessionTest: `the capabilities exchange is answered with our own bitmap` | Same ChattyWatch chatter |
| GarminSession resilience > notification subscription gets a full status, not a bare ACK | PORTED | GarminSessionTest: `notification subscription gets a full status not a bare ACK` | Same 6-byte payload + DISABLED asserts |
| GarminSession resilience > every unanswered inbound message gets a generic ACK | PORTED | GarminSessionTest: `every unanswered inbound message gets a generic ACK` | - |
| GarminSession resilience > an ACK is never itself ACKed | PORTED | GarminSessionTest: `an ACK is never itself ACKed` | - |
| GarminSession resilience > messages with their own response are not double-acked | PORTED | GarminSessionTest: `messages with their own response are not double-acked` | - |
| GarminSession resilience > a FILTER is sent before the directory is requested | PORTED | GarminSessionTest: `a FILTER is sent before the directory is requested` | - |
| GarminSession resilience > a synchronization announcement re-reads the listing | PORTED | GarminSessionTest: `a synchronization announcement re-reads the listing` | Same AnnouncingWatch bit-26 bitmask |
| GarminSession resilience > an announcement with nothing we want does not re-read | PORTED | GarminSessionTest: `an announcement with nothing we want does not re-read` | - |
| GarminSession resilience > a link that dies during the empty grace still settles the sync | PORTED | GarminSessionTest: `a link that dies during the empty grace still settles the sync` | 20ms grace, link cut before timer; no explicit 2s timeout needed under runTest |
| GarminSession resilience > a file is kept before it is archived | PORTED | GarminSessionTest: `a file is kept before it is archived` | Same kept/archive ordering |
| GarminSession resilience > a file that could not be kept is NOT archived | PORTED | GarminSessionTest: `a file that could not be kept is NOT archived` | FileSystemException → IOException |
| GarminSession resilience > abort keeps what was already downloaded | PORTED | GarminSessionTest: `abort keeps what was already downloaded` | - |
| GarminSession resilience > ignores frames that arrive after completion | PORTED | GarminSessionTest: `ignores frames that arrive after completion` | - |
| GarminSession resilience > keeps acknowledging after completion when listening | PORTED | GarminSessionTest: `keeps acknowledging after completion when listening` | - |
| notification subscription > a session carrying a notifications handler replies ENABLED | PORTED | GarminSessionNotificationsTest: `a session carrying a notifications handler replies ENABLED` | Handler class renamed GarminNotificationsHandler → GarminGncsHandler |
| notification subscription > a session with NO handler still replies DISABLED, so sync, find and settings sessions are unchanged | PORTED | GarminSessionNotificationsTest: `a session with NO handler still replies DISABLED, so sync find and settings sessions are unchanged` | Also duplicated in GarminSessionTest |
| notification subscription > a watch that is not yet accepting notifications is STILL told the phone is willing | PORTED | GarminSessionNotificationsTest: `a watch that is not yet accepting notifications is STILL told the phone is willing` | - |
| notification subscription > the watch's own flag drives whether anything is announced | PORTED | GarminSessionNotificationsTest: `the watch's own flag drives whether anything is announced` | - |
| notification subscription > the subscription gets its purpose-built status and no generic ACK | PORTED | GarminSessionNotificationsTest: `the subscription gets its purpose-built status and no generic ACK` | - |
| the notification conversation end to end > announce, answer the request, and acknowledge the whole blob | PORTED | GarminSessionNotificationsTest: `announce, answer the request, and acknowledge the whole blob` | Same 9-byte announcement + Ada/On-my-way blob asserts |
| the notification conversation end to end > a control request is answered with a control status BEFORE the first chunk | PORTED | GarminSessionNotificationsTest: `a control request is answered with a control status BEFORE the first chunk` | - |
| the notification conversation end to end > a multi-chunk body arrives in order and reassembles exactly | PORTED | GarminSessionNotificationsTest: `a multi-chunk body arrives in order and reassembles exactly` | Same 700-char body, contiguous-offset asserts |
| the notification conversation end to end > a held notification is announced AFTER the subscription status, never before | PORTED | GarminSessionNotificationsTest: `a held notification is announced AFTER the subscription status, never before` | - |
| the notification conversation end to end > a control request that arrives before any notification sends only the status | PORTED | GarminSessionNotificationsTest: `a control request that arrives before any notification sends only the status` | - |

## test/devices/garmin/onboard_garmin_watch_use_case_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/devices/garmin/OnboardGarminWatchUseCaseTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| registers a bonded watch with no capabilities | PORTED | OnboardGarminWatchUseCaseTest: `registers a bonded watch with no capabilities` | Same E0:48:24:D5:F7:10 / vívoactive 5 fixture; real BleDeviceRepository over FakeSharedPreferences |
| registers an Edge as a bike computer with no capabilities | PORTED | OnboardGarminWatchUseCaseTest: `registers an Edge as a bike computer with no capabilities` | - |
| a registered watch never takes part in capability assignment | PORTED | OnboardGarminWatchUseCaseTest: `a registered watch never takes part in capability assignment` | Same reason string on the assert |
| a refused pairing writes nothing to the registry | PORTED | OnboardGarminWatchUseCaseTest: `a refused pairing writes nothing to the registry` | Ordered pairing-call list asserted |
| an unreachable watch writes nothing to the registry | PORTED | OnboardGarminWatchUseCaseTest: `an unreachable watch writes nothing to the registry` | - |
| a declined companion association still onboards the watch | PORTED | OnboardGarminWatchUseCaseTest: `a declined companion association still onboards the watch` | - |
| an already-bonded watch is registered without re-prompting | PORTED | OnboardGarminWatchUseCaseTest: `an already-bonded watch is registered without re-prompting` | - |
| reports each platform step before it shows its dialog | PORTED | OnboardGarminWatchUseCaseTest: `reports each platform step before it shows its dialog` | BONDING/ASSOCIATING/PROBING order |
| the probe runs only after bonding succeeds | PORTED | OnboardGarminWatchUseCaseTest: `the probe runs only after bonding succeeds` | - |
| an unsupported transport still onboards, and is reported | PORTED | OnboardGarminWatchUseCaseTest: `an unsupported transport still onboards, and is reported` | - |
| a v2 watch reports as supported | PORTED | OnboardGarminWatchUseCaseTest: `a v2 watch reports as supported` | - |
| forget drops the association and the bond, in that order | PORTED | OnboardGarminWatchUseCaseTest: `forget drops the association and the bond, in that order` | - |

## test/devices/core/ble/ble_aggregators_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/sensors/ble/BleAggregatorsTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| heartRateAggregator returns latest value | PORTED | `heartRateAggregator_returnsLatestValue` | - |
| cyclingCadenceAggregator computes rpm from crank delta | PORTED | `cyclingCadenceAggregator_computesRpmFromCrankDelta` | identical fixtures (10→11 revs, 1024 ticks, 60 rpm) |
| cyclingCadenceAggregator returns zero when crank stops | PORTED | `cyclingCadenceAggregator_returnsZeroWhenCrankStops` | - |
| cyclingSpeedAggregator computes meters per second | PORTED | `cyclingSpeedAggregator_computesMetersPerSecond` | same 2.1 m circumference, 4.2 ± 0.01 |
| cyclingSpeedAggregator returns zero when wheel stops | PORTED | `cyclingSpeedAggregator_returnsZeroWhenWheelStops` | - |
| powerAggregator returns instantaneous power | PORTED | `powerAggregator_returnsInstantaneousPower` | - |
| runningAggregator returns latest speed and cadence | PORTED | `runningAggregator_returnsLatestSpeedAndCadence` | - |
| aggregator clears stale values | PORTED | `aggregator_clearsStaleValues` | - |
| speed and cadence aggregators return zero when stale | PORTED | `speedAndCadenceAggregators_returnZeroWhenStale` | - |

## test/devices/core/ble/ble_parsers_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/sensors/ble/BleParsersTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| parseHeartRate uint8 | PORTED | `parseHeartRate_uint8` | identical bytes `[0x00,0x4A]` |
| parseHeartRate uint16 | PORTED | `parseHeartRate_uint16` | identical bytes `[0x01,0x2C,0x01]` |
| parseCyclingPower basic | PORTED | `parseCyclingPower_basic` | identical 8-byte payload |
| parseCyclingSpeedCadence wheel and crank | PORTED | `parseCyclingSpeedCadence_wheelAndCrank` | identical 11-byte payload |
| parseRunningSpeedCadence | PORTED | `parseRunningSpeedCadence` | identical payload + "Stryd" |
| parseRunningSpeedCadence tickrX adjusts cadence | PORTED | `parseRunningSpeedCadence_tickrXAdjustsCadence` | identical payload + "TICKR X 1234" |
| parseHeartRate zero-signal payload | PORTED | `parseHeartRate_zeroSignalPayload` | - |
| parseHeartRate single byte | PORTED | `parseHeartRate_singleByte` | - |
| parseHeartRate empty payload returns null | PORTED | `parseHeartRate_emptyPayloadReturnsNull` | - |

## test/devices/core/ble/ble_uuids_garmin_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/sensors/ble/BleUuidsTest.kt (the filter) and /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/devices/garmin/GarminDeviceNamesTest.kt (the name matching)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| the Garmin scan filter > filters on the ADVERTISED member service, not GFDI | PORTED | BleUuidsTest: `the Garmin scan filter filters on the ADVERTISED member service, not GFDI` | Same 0xFE1F-in / GFDI-out assertions and the same reason string |
| the Garmin scan filter > the member service grants no sensor capabilities | PORTED | BleUuidsTest: `the member service grants no sensor capabilities` | - |
| the Garmin scan filter > the standard sensor services are still in the filter | PORTED | BleUuidsTest: `the standard sensor services are still in the filter` | Same four service UUIDs |
| classifying the scan result > the member service surfaces a device but does not name its kind | PORTED | GarminDeviceNamesTest: `does not match other vendors, blanks or null` | asserts `isGarminSyncDeviceName(null)` false |
| classifying the scan result > a watch found via "Show all devices" is caught by its name | PORTED | GarminDeviceNamesTest: `watch name matches the watch families, not the Edge` + `matches the accented names the watches actually advertise` | both `isGarminWatchName` and `isGarminSyncDeviceName` on "vívoactive 5" |

## test/devices/core/ble/device_scan_classifier_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/devices/garmin/GarminScanClassifierTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| GarminScanClassifier > claims an advertisement carrying the member service | PORTED | `claims an advertisement carrying the member service` | - |
| GarminScanClassifier > claims it alongside unrelated advertised services | PORTED | `claims it alongside unrelated advertised services` | identical UUID strings |
| GarminScanClassifier > does not claim a live sensor advertisement | PORTED | `does not claim a live sensor advertisement` | identical UUID strings |
| GarminScanClassifier > does not claim an empty advertisement | PORTED | `does not claim an empty advertisement` | - |
| GarminScanClassifier > does not key on the connect-only GFDI service | PORTED | `does not key on the connect-only GFDI service` | - |

## test/devices/core/registry/ble_device_repository_impl_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/data/repository/BleDeviceRepositoryTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| updateBatteryLevel stores a changed value | PORTED | BleDeviceRepositoryTest: `updateBatteryLevel stores a changed value` | - |
| updateBatteryLevel clamps out-of-range values | PORTED | BleDeviceRepositoryTest: `updateBatteryLevel clamps out-of-range values` | - |
| an identical battery reading does not re-persist or advance the stamp | PORTED | BleDeviceRepositoryTest.kt `an identical battery reading does not re-persist or advance the stamp` | fixed: updateBatteryLevel early-returns on an unchanged percent, so nothing is written or published |
| updateBatteryLevel ignores an unknown device id | PORTED | BleDeviceRepositoryTest: `updateBatteryLevel ignores an unknown device id` | - |
| watches > kind and lastSyncedAt survive a storage round-trip | REMOVED | — | Watch kinds removed from the BLE registry |
| watches > a device stored before watches existed reads back as a sensor | PORTED | BleDeviceRepositoryTest: `registryJson_oldJsonWithoutWatchFieldsRoundTripsAsSensor` | - |
| watches > markSynced ignores an unknown device id | REMOVED | — | `markSynced` / watch sync stamps are gone |
| watches > an enabled watch is kept out of capability assignment | REMOVED | BleDeviceRepositoryTest: `registryJson_dropsWatchEraEntries` | WATCH rows are dropped on read |
| watches > an Edge bike computer with capabilities DOES take part | PORTED | BleDeviceRepositoryTest: `registryJson_ignoresLegacyKindFieldsOnSensors` | Edge rows load as plain sensors |
| watches > a bike computer with NO capabilities stays out | PORTED | BleDeviceRepositoryTest: `a device with no capabilities stays out of assignments` | Capability emptiness, not kind |

## test/devices/core/registry/device_classification_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/devices/core/DeviceClassificationTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a Garmin watch product name → (garmin, watch) | PORTED | `a Garmin watch family classifies as a Garmin watch` | same "vívoactive 5" fixture |
| the member service alone does NOT make an unknown name a watch | PORTED | DeviceClassificationTest: `the member service alone does NOT make an unknown name a watch` | - |
| a Garmin Edge name → (garmin, bikeComputer) | PORTED | `an Edge classifies as a Garmin bike computer, not a watch` | - |
| a prefixed Edge name → (garmin, bikeComputer) | PORTED | DeviceClassificationTest: `a prefixed Edge name → (garmin, bikeComputer)` | Now goes through `classifyDevice`, not just the name matcher |
| a member-service-only advert (no distinguishing name) → sensor | PORTED | DeviceClassificationTest: `a member-service-only advert (no distinguishing name) → sensor` | - |
| a WearOS smartwatch name → (wearos, watch) | PORTED | `a wearos-style smartwatch classifies as a wearos watch` | same "Galaxy Watch8 (89FZ)" fixture |
| a live heart-rate strap → a plain sensor | PORTED | `anything unclaimed falls through to a plain sensor` | "Polar H10" instead of "TICKR"; same identity assertion against `DeviceClassification.SENSOR` |
| the NAME decides, so a WearOS name is WearOS even with 0xFE1F | PORTED | DeviceClassificationTest: `the NAME decides, so a WearOS name is WearOS even with 0xFE1F` | - |
| an unnamed, unremarkable device → sensor | PORTED | DeviceClassificationTest: `an unnamed, unremarkable device → sensor` | - |

## test/devices/core/registry/device_integration_test.dart
Kotlin counterpart: removed — `BleDeviceKind` / `DeviceIntegration` are gone; the BLE registry is sensors only.
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| watch ownership helpers > a null-integration watch reads as Garmin (legacy) | REMOVED | — | Watch kinds removed |
| watch ownership helpers > an explicit Garmin watch is a Garmin watch | REMOVED | — | Watch kinds removed |
| watch ownership helpers > a WearOS watch is not a Garmin watch (the sync-port ownership fix) | REMOVED | — | Watch kinds removed |
| watch ownership helpers > a sensor is neither, whatever the integration | REMOVED | — | Watch kinds removed |
| watch ownership helpers > an Edge bike computer: GFDI + live-sensor, but never a watch | REMOVED | — | Watch kinds removed |
| watch ownership helpers > a watch is GFDI but never live-sensor-capable | REMOVED | — | Watch kinds removed |
| a bike computer survives a persistence round-trip | REMOVED | — | Bike-computer kind removed; Edge rows load as plain sensors |
| the integration survives a persistence round-trip | REMOVED | — | DeviceIntegration removed from the BLE registry |

## test/devices/wearos/onboard_wearos_watch_use_case_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/devices/wearos/OnboardWearOsWatchUseCaseTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| registers a (watch, wearos) device, no bond | PORTED | `registers a (watch, wearos) device, no bond` | same address/name fixture, same five assertions, same throwing bond stubs |
| a declined association still onboards the watch | PORTED | `a declined association still onboards the watch` | - |
| a thrown association is swallowed — the watch still registers | PORTED | `a thrown association is swallowed - the watch still registers` | - |
| forget drops the companion association | PORTED | `forget drops the companion association` | - |

## test/devices/wearos/wearos_device_names_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/devices/wearos/WearOsDeviceNamesTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| isSmartwatchName > matches the Galaxy Watch (the test rig) | PORTED | `matches the Galaxy Watch, the test rig` | identical fixtures |
| isSmartwatchName > matches other wrist smartwatch families | PORTED | `matches other wrist smartwatch families` | identical four names |
| isSmartwatchName > does not match live sensors | PORTED | `does not match live sensors` | identical four names |
| isSmartwatchName > is null- and blank-safe | PORTED | `is null- and blank-safe` | - |

## test/devices/garmin/wellness/fit_metrics_sleep_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/devices/garmin/wellness/FitMetricsSleepTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| health snapshot > unpacks an array field into one sample per interval | PORTED | `unpacks an array field into one sample per interval` | same hsa message 305, interval 5, samples 96/97/97/98 |
| health snapshot > SpO2 reaches Health Connect, stress does not | PORTED | `SpO2 reaches Health Connect, stress does not` | same 305/306 fixtures, same `garmin_fit_hsa_spo2_` prefix assertion |
| health snapshot > respiration is scaled by 100 | PORTED | `respiration is scaled by 100` | same 307, sint16, 1450/1520 → 14.5/15.2 |
| health snapshot > a zero interval drops the record rather than stacking samples | PORTED | `a zero interval drops the record rather than stacking samples` | - |
| health snapshot > out-of-range readings are dropped | PORTED | `out-of-range readings are dropped` | same 96/0/97 and the no-shift assertion |
| daily sleep, from the metrics file > reads awake duration as SECONDS, not the profile's minutes | PORTED | `reads awake duration as SECONDS not the profile's minutes` | same 1020 → 17 min |
| daily sleep > sleep pressure passes through raw, including negatives | PORTED | `sleep pressure passes through raw including negatives` | same -33 |
| daily sleep > reads Sleep Coach need against the usual need | PORTED | `reads Sleep Coach need against the usual need` | same 470/520 minutes |
| daily sleep > a metrics file of only sleep data is not empty | PORTED | `a metrics file of only sleep data is not empty` | - |
| daily sleep > invalid sentinels do not become readings | PORTED | `invalid sentinels do not become readings` | same 0xFF/0xFFFF/0x7FFF sentinels in the builder |
| intensity minutes > reads the running daily totals | PORTED | `reads the running daily totals` | same fields 37/38, same 12/4 → 19/4 |
| intensity minutes > reads the alternate field pair too | PORTED | `reads the alternate field pair too` | same alt fields 33/34 |
| intensity minutes > zero is a real total and is kept | PORTED | `zero is a real total and is kept` | - |
| intensity minutes > the uint16 invalid sentinel is not a total | PORTED | `the uint16 invalid sentinel is not a total` | - |
| metrics file > reads VO2 max, recovery, readiness and load from one file | PORTED | `reads VO2 max, recovery, readiness and load from one file` | same messages 229/140/369/378 and 425/1320/68/412/380 |
| metrics file > a file carrying only training load still yields metrics | PORTED | `a file carrying only training load still yields metrics` | - |
| metrics file > only VO2 max reaches Health Connect | PORTED | `only VO2 max reaches Health Connect` | same 501 → 50.1 and `garmin_fit_vo2max_<ms>` id |
| metrics file > a metrics file with no VO2 max maps to nothing | PORTED | `a metrics file with no VO2 max maps to nothing` | - |
| sleep extras > carries the watch's own score alongside the derived stages | PORTED | `carries the watch's own score alongside the derived stages` | same sleep_stats 346 fields 6/11, 74/3 |
| sleep extras > a night without sleep_stats still parses | PORTED | `a night without sleep_stats still parses` | - |
| sleep extras > naps become their own stage-less sleep sessions | PORTED | `naps become their own stage-less sleep sessions` | same nap message 412, same `garmin_fit_nap_<ms>` id |
| sleep extras > a nap that ends before it starts is dropped | PORTED | `a nap that ends before it starts is dropped` | - |

## test/devices/garmin/wellness/fit_stress_body_energy_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/devices/garmin/wellness/FitStressBodyEnergyTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| extracts stress and body energy from the stress_level message | PORTED | `extracts stress and body energy from the stress_level message` | same stress_level(227) field layout 0/1/3 and same 42/72, 51/72 samples (file_id local msg type differs: 0 in Dart, 3 in Kotlin) |
| a negative stress score is dropped, not clamped | PORTED | `a negative stress score is dropped not clamped` | same -23 then 30, body energy still 2 |
| the stress message alone makes a file non-empty | PORTED | `the stress message alone makes a file non-empty` | - |
| uses the message own time field, not the record header | PORTED | `uses the message's own time field not the record header` | - |

## test/devices/garmin/wellness/fit_wellness_import_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/devices/garmin/wellness/FitWellnessImportTest.kt (fixtures in FitTestBytes.kt)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| parseGarminSleepSession > reads the session bounds and a contiguous stage timeline | PORTED | `reads the session bounds and a contiguous stage timeline` | FitTestBytes.kt `FitW`/`fitWrap`/`fileId` are a byte-for-byte port of Dart `_W`/`_wrap`; same 5-transition ladder |
| parseGarminSleepSession > returns null when the file carries no sleep timeline | PORTED | `returns null when the file carries no sleep timeline` | - |
| fitSleepImportRecords > maps to one SleepSessionRecord with a deterministic id | PORTED | `maps to one SleepSessionRecord with a deterministic id` | same `garmin_fit_sleep_<ms>` id and same stage sequence |
| fitSleepImportRecords > drops unmeasurable spans, which have no Health Connect stage | PORTED | `drops unmeasurable spans which have no Health Connect stage` | - |
| HRV (type 68) > reads last_night_average as an RMSSD in ms | PORTED | `reads last_night_average as an RMSSD in ms` | same hrv_status_summary(370) field 1, 42.5×128 |
| HRV (type 68) > maps to one HeartRateVariabilityRmssd record | PORTED | `maps to one HeartRateVariabilityRmssd record` | same `garmin_fit_hrv_<ms>` id |
| HRV (type 68) > the invalid uint16 sentinel is not read as a reading | PORTED | `the invalid uint16 sentinel is not read as a reading` | same 0xFFFF override |
| monitoring (type 32) summary > reads resting HR and BMR, maps to two records | PORTED | `reads resting HR and BMR and maps to two records` | same 65/2265 and `garmin_fit_resting_hr_<ms>` id |
| monitoring (type 32) summary > a file with only resting HR maps to one record | PORTED | `a file with only resting HR maps to one record` | - |
| monitoring high-frequency series > HR packs hourly, respiration averages hourly, steps span the file | PORTED | `HR packs hourly, respiration averages hourly, steps span the file` | identical HR/respiration/steps fixtures |
| monitoring high-frequency series > a typed message does not lend its type to the untyped one after it | PORTED | `a typed message does not lend its type to the untyped one after it` | same hand-built 500-then-620 file, same 500 total |
| a day of counters becomes intraday records, not one flat total | PORTED | `a day of counters becomes intraday records not one flat total` | Kotlin pins the zone to Europe/Madrid where Dart uses the default local zone |
| what came before the first reading is not lost | PORTED | `what came before the first reading is not lost` | - |
| standing still writes nothing | PORTED | `standing still writes nothing` | - |
| the next sync carries on from the watermark, not from midnight | PORTED | `the next sync carries on from the watermark not from midnight` | same 500/900/1500/1700 ladder |
| the day's last movement is written, not left for a sync that never comes | PORTED | `the day's last movement is written not left for a sync that never comes` | same 100/150/250, last bucket at 09:15 |
| the open bucket is rewritten in full next sync, under the same id | PORTED | `the open bucket is rewritten in full next sync under the same id` | same 50 → 150 rewrite, 300 latest-upsert total |
| re-importing a file already behind the watermark writes nothing | PORTED | `re-importing a file already behind the watermark writes nothing` | - |
| a counter rollover is not a walk backwards, and not a full stop | PORTED | `a counter rollover is not a walk backwards and not a full stop` | same 900/0/300 → 1200 |
| a morning sync does not carry yesterday onto today | PORTED | `a morning sync does not carry yesterday onto today` | same 6100/6123/6123/6132 → 9 and 6132 |
| yesterday carries over from its watermark, not just from this run | PORTED | `yesterday carries over from its watermark not just from this run` | - |
| a day still starts from zero once the counter has rolled over | PORTED | `a day still starts from zero once the counter has rolled over` | same 4000 |
| a carry is not spent across a gap of days | PORTED | `a carry is not spent across a gap of days` | same 9000 |
| activity-type counters are summed, never subtracted | PORTED | `activity-type counters are summed never subtracted` | - |
| a total moved between activity types is not counted twice | PORTED | `a total moved between activity types is not counted twice` | - |
| types still add up when they hold different totals | PORTED | `types still add up when they hold different totals` | - |
| a counter naming no activity is not a bucket of its own | PORTED | `a counter naming no activity is not a bucket of its own` | - |
| a type absent from a sync's first readings is not the day again | PORTED | `a type absent from a sync's first readings is not the day again` | same 3400 / 520 |
| yesterday's counter restated unchanged overnight writes nothing | PORTED | `yesterday's counter restated unchanged overnight writes nothing` | same 0 |
| a genuinely reset type still counts from zero across midnight | PORTED | `a genuinely reset type still counts from zero across midnight` | same 350 |
| a watermark from before the per-type maps never re-counts the day | PORTED | `a watermark from before the per-type maps never re-counts the day` | same 195 |
| an untyped counter still counts when it is all the file has | PORTED | `an untyped counter still counts when it is all the file has` | - |
| incremental files in the same hour > two HR chunks produce two distinct records | PORTED | `two HR chunks in one hour produce two distinct records` | same 10:05/10:06 and 10:40/10:41 windows |
| incremental files in the same hour > re-importing the same chunk stays idempotent | PORTED | `re-importing the same chunk stays idempotent` | - |
| incremental files in the same hour > a whole-day file still yields one record per hour | PORTED | `a whole-day file still yields one record per hour` | same 24 records / 24 distinct ids |
| incremental files in the same hour > respiration is keyed and timed on its first reading | PORTED | `respiration is keyed and timed on its first reading` | same 14/16 → 15.0 at 10:05 |
| counter record identity > a re-sync from a lost watermark replaces rather than accumulates | PORTED | `a re-sync from a lost watermark replaces rather than accumulates` | same step-5 vs step-7 minute grids, same ≤1790 and non-overlap checks |
| counter record identity > the same minutes always produce the same id | PORTED | `the same minutes always produce the same id` | - |
| counter record identity > the day's first record keeps the legacy day-keyed id | PORTED | `the day's first record keeps the legacy day-keyed id` | same `garmin_fit_steps_2024-01-18` |
| counter record identity > the legacy day key is handed out once, not re-handed each sync | PORTED | `the legacy day key is handed out once not re-handed each sync` | same `legacyRetired` assertions |
| counter record identity > a day whose first sync only touched the open bucket still retires the legacy id, with a later bucket | PORTED | `a first sync that only touched the open bucket retires the legacy id later` | - |
| counter record identity > calories ride the same grid as steps | PORTED | `calories ride the same grid as steps` | same `active_cal`→`steps` id-rewrite check and 80 kcal |

## test/devices/garmin/garmin_notification_actions_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/devices/garmin/GarminNotificationActionsTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| mapping Android actions onto the watch > every dismissable notification gets a dismiss the app did not provide | PORTED | `every dismissable notification gets a dismiss the app did not provide` | - |
| mapping Android actions onto the watch > an ongoing notification gets no dismiss, because clearing it would fail | PORTED | `an ongoing notification gets no dismiss, because clearing it would fail` | - |
| mapping Android actions onto the watch > the app's own buttons land in the numbered custom slots, in order | PORTED | `the app's own buttons land in the numbered custom slots, in order` | - |
| mapping Android actions onto the watch > a reply action takes the reply slot, not a custom one | PORTED | `a reply action takes the reply slot, not a custom one` | - |
| mapping Android actions onto the watch > a second reply becomes a plain button rather than overwriting the first | PORTED | `a second reply becomes a plain button rather than overwriting the first` | - |
| mapping Android actions onto the watch > more buttons than there are slots are dropped, not crammed in | PORTED | `more buttons than there are slots are dropped, not crammed in` | - |
| mapping Android actions onto the watch > an action that only opens the app is not offered at all | PORTED | `an action that only opens the app is not offered at all` | - |
| mapping Android actions onto the watch > a blocked reply does not consume the reply slot, so a later usable one still gets it | PORTED | `a blocked reply does not consume the reply slot, so a later usable one still gets it` | - |
| mapping Android actions onto the watch > an index survives the round trip, so the phone never re-derives which button was meant | PORTED | `an index survives the round trip, so the phone never re-derives which button was meant` | - |
| encoding the ACTIONS attribute > no actions encodes as the four-zero-byte sentinel | PORTED | `no actions encodes as the four-zero-byte sentinel` | identical fixture [0,0,0,0] |
| encoding the ACTIONS attribute > each action is a code, an icon position, a length and a label | PORTED | `each action is a code, an icon position, a length and a label` | identical fixture [1,1,0,2,0x4F,0x6B] |
| encoding the ACTIONS attribute > dismiss carries the LEFT icon position, which is where the watch draws it | PORTED | `dismiss carries the LEFT icon position, which is where the watch draws it` | same 98/LEFT.bit |
| encoding the ACTIONS attribute > reply carries the BOTTOM icon position | PORTED | `reply carries the BOTTOM icon position` | same 95/BOTTOM.bit |
| encoding the ACTIONS attribute > the label length is BYTES, so a non-ASCII label still parses | PORTED | `the label length is BYTES, so a non-ASCII label still parses` | same "áé" → 4 |
| encoding the ACTIONS attribute > an absurdly long label is trimmed rather than wrapping the length byte | PORTED | `an absurdly long label is trimmed rather than wrapping the length byte` | same 400 → 255, size 4+255 |
| encoding the ACTIONS attribute > several actions pack one after another | PORTED | `several actions pack one after another` | - |

## test/devices/garmin/garmin_notification_forwarder_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/devices/garmin/GarminNotificationForwarderTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| coalescing > three notifications 200ms apart open ONE link, not three | PORTED | `three notifications 200ms apart open ONE link, not three` | fakeAsync → runTest/advanceTimeBy, same intervals |
| coalescing > a steady drip cannot postpone the connect past the ceiling | PORTED | `a steady drip cannot postpone the connect past the ceiling` | - |
| coalescing > a notification arriving while the link is open is sent immediately | PORTED | `a notification arriving while the link is open is sent immediately` | - |
| coalescing > a dismissal is forwarded as a withdrawal | PORTED | `a dismissal is forwarded as a withdrawal` | - |
| the link is held > the link is still open minutes after the last notification | PORTED | `the link is still open minutes after the last notification` | - |
| the link is held > a watch that walks out of range is reconnected to | PORTED | `a watch that walks out of range is reconnected to` | - |
| the link is held > a notification the watch never subscribed for survives the link dropping | PORTED | `a notification the watch never subscribed for survives the link dropping` | Kotlin FakeLink derives `subscribed` from handler.enabled instead of hardcoding true |
| the link is held > a watch that stays away is retried on a growing backoff, not in a tight loop | PORTED | `a watch that stays away is retried on a growing backoff, not in a tight loop` | same 30-minute window, same 3 < attempts < 15 bounds |
| the link is held > a notification that arrives while the watch is away is kept for when it returns | PORTED | `a notification that arrives while the watch is away is kept for when it returns` | - |
| the radio lease > the lease is taken before connecting and held with the link | PORTED | `the lease is taken before connecting and held with the link` | - |
| the radio lease > the radio is given up when a sync asks for it, and taken back after | PORTED | `the radio is given up when a sync asks for it, and taken back after` | - |
| the radio lease > a sync holding the radio defers the notification instead of interrupting it | PORTED | `a sync holding the radio defers the notification instead of interrupting it` | - |
| the radio lease > the deferred notification is sent once the sync releases the radio | PORTED | `the deferred notification is sent once the sync releases the radio` | - |
| failure > a failed connect does not leave the lease held | PORTED | `a failed connect does not leave the lease held` | - |
| failure > a held link never reports idle, so the isolate is not torn down | PORTED | `a held link never reports idle, so the forwarder is not torn down` | renamed (no isolate on JVM); same assertions |
| failure > disposing closes an open link and releases the radio | PORTED | `disposing closes an open link and releases the radio` | - |

## test/devices/garmin/garmin_notification_messages_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/devices/garmin/GarminNotificationMessagesTest.kt (byte-exact codec port; behavioural coverage also via GarminGncsHandlerTest.kt and GarminSessionNotificationsTest.kt)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| garminNotificationDate > formats as yyyyMMddTHHmmss with every field zero-padded | PORTED | GarminNotificationMessagesTest: `formats as yyyyMMddTHHmmss with every field zero-padded` | DateTime → java.time.LocalDateTime |
| garminNotificationAttributeBytes > MESSAGE_SIZE counts the body characters, not its bytes | PORTED | GarminNotificationMessagesTest: `MESSAGE_SIZE counts the body characters, not its bytes` | - |
| garminNotificationAttributeBytes > a maxLength of zero means no limit | PORTED | GarminNotificationMessagesTest: `a maxLength of zero means no limit` | - |
| garminNotificationAttributeBytes > a body longer than maxLength is cut to that many characters | PORTED | GarminNotificationMessagesTest: `a body longer than maxLength is cut to that many characters` | - |
| garminNotificationAttributeBytes > a cut that would split an emoji drops it rather than half of it | PORTED | GarminNotificationMessagesTest: `a cut that would split an emoji drops it rather than half of it` | Valid-UTF-8 check via Charsets.UTF_8 round-trip |
| garminNotificationAttributeBytes > ACTIONS is the four-zero-byte "none" sentinel, not an empty value | PORTED | GarminNotificationMessagesTest: `ACTIONS is the four-zero-byte none sentinel, not an empty value` | Attribute-bytes path now asserted directly |
| encodeGarminNotificationAttributes > writes the command byte and the notification id first | PORTED | GarminNotificationMessagesTest: `writes the command byte and the notification id first` | Exact 5-byte prefix |
| encodeGarminNotificationAttributes > each attribute is a code, a 16-bit byte length, then the value | PORTED | GarminNotificationMessagesTest: `each attribute is a code, a 16-bit byte length, then the value` | Exact [0x01,0x03,0x00,'A','d','a'] |
| encodeGarminNotificationAttributes > MESSAGE_SIZE is encoded last even when the watch asked for it first | PORTED | GarminNotificationMessagesTest: `MESSAGE_SIZE is encoded last even when the watch asked for it first` | - |
| encodeGarminNotificationAttributes > a value length is the BYTE count, not the character count | PORTED | GarminNotificationMessagesTest: `a value length is the BYTE count, not the character count` | - |
| buildNotificationUpdate > carries the update type, category and id with no text at all | PORTED | GarminNotificationMessagesTest: `carries the update type, category and id with no text at all` | Category 0x0C and category-flag 0x12 bytes asserted in the exact payload |
| buildNotificationUpdate > MODIFY and REMOVE use ordinals 1 and 2 | PORTED | GarminNotificationMessagesTest: `MODIFY and REMOVE use ordinals 1 and 2` | Literals 1 and 2, not `.ordinal` |
| buildNotificationUpdate > the phone flags byte announces actions and attachments separately | PORTED | GarminNotificationMessagesTest: `the phone flags byte announces actions and attachments separately` | Includes HAS_ATTACHMENTS 0x04 |
| buildNotificationData > declares the total size, the running CRC and the offset | PORTED | GarminNotificationMessagesTest: `declares the total size, the running CRC and the offset` | Exact [0x02,0x01,0x06,0x05,0x04,0x03,0xAA,0xBB] fixture |
| buildNotificationSubscriptionStatus > reports ENABLED as 0 and echoes the watch back | PORTED | GarminNotificationMessagesTest: `reports ENABLED as 0 and echoes the watch back` | Echoed 0x07 byte asserted |
| buildNotificationSubscriptionStatus > reports DISABLED as 1 | PORTED | GarminNotificationMessagesTest: `reports DISABLED as 1` (also GarminSessionNotificationsTest) | - |
| buildNotificationControlStatus > names NOTIFICATION_CONTROL with ACK, chunk OK and no error | PORTED | GarminNotificationMessagesTest: `names NOTIFICATION_CONTROL with ACK, chunk OK and no error` | Exact [0xAA,0x13,0x00,0x00,0x00] body |
| decoding NOTIFICATION_CONTROL > an attribute request reads the id and every requested field | PORTED | GarminNotificationMessagesTest: `an attribute request reads the id and every requested field` | Incl. APP_IDENTIFIER and MESSAGE_SIZE forms with max lengths |
| decoding NOTIFICATION_CONTROL > the requested order is preserved, because the answer reproduces it | PORTED | GarminNotificationMessagesTest: `the requested order is preserved, because the answer reproduces it` | - |
| decoding NOTIFICATION_CONTROL > ACTIONS consumes its length AND its extra byte, so the next attribute still parses | PORTED | GarminNotificationMessagesTest: `ACTIONS consumes its length AND its extra byte, so the next attribute still parses` | - |
| decoding NOTIFICATION_CONTROL > an unknown attribute stops the walk instead of mis-parsing the rest | PORTED | GarminNotificationMessagesTest: `an unknown attribute stops the walk instead of mis-parsing the rest` | - |
| decoding NOTIFICATION_CONTROL > an app-attributes request reads the NUL-terminated package name | PORTED | GarminNotificationMessagesTest: `an app-attributes request reads the NUL-terminated package name` | - |
| decoding NOTIFICATION_CONTROL > an action with no text is decoded, not treated as a short frame | PORTED | GarminNotificationMessagesTest: `an action with no text is decoded, not treated as a short frame` | - |
| decoding NOTIFICATION_CONTROL > an unknown command decodes to unhandled, not an error | PORTED | GarminNotificationMessagesTest: `an unknown command decodes to unhandled, not an error` | - |
| decoding a NOTIFICATION_DATA transfer status > OK can proceed | PORTED | GarminNotificationMessagesTest: `OK can proceed` | `transferStatus`/`canProceed` asserted directly |
| decoding a NOTIFICATION_DATA transfer status > each non-OK transfer status is named, so the upload can tell them apart | PORTED | GarminNotificationMessagesTest: `each non-OK transfer status is named, so the upload can tell them apart` | Codes 1-4 decoded from bytes |
| decoding a NOTIFICATION_DATA transfer status > a NAK cannot proceed even when the transfer status says OK | PORTED | GarminNotificationMessagesTest: `a NAK cannot proceed even when the transfer status says OK` | - |
| acknowledgement policy > NOTIFICATION_CONTROL is self-acknowledged, so no generic ACK is sent | PORTED | GarminNotificationMessagesTest: `NOTIFICATION_CONTROL is self-acknowledged, so no generic ACK is sent` | Asserted on `garminSelfAcknowledgedTypes` membership |

## test/devices/garmin/garmin_notifications_handler_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/devices/garmin/GarminGncsHandlerTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| before the watch has subscribed > nothing is announced, so a sync session sends no notification traffic | PORTED | `nothing is announced, so a sync session sends no notification traffic` | - |
| before the watch has subscribed > a notification that arrives before the subscription is announced as soon as it lands | PORTED | `a notification that arrives before the subscription is announced as soon as it lands` | - |
| before the watch has subscribed > several held notifications are all announced, oldest first | PORTED | `several held notifications are all announced, oldest first` | - |
| before the watch has subscribed > one held notification edited twice is announced once | PORTED | `one held notification edited twice is announced once` | - |
| before the watch has subscribed > a held notification that aged out of the queue is not announced | PORTED | `a held notification that aged out of the queue is not announced` | - |
| announcing > a new notification is announced as ADD | PORTED | `a new notification is announced as ADD` | - |
| announcing > a second notification with the same id is announced as MODIFY, so the watch updates instead of buzzing again | PORTED | `a second notification with the same id is announced as MODIFY, so the watch updates instead of buzzing again` | - |
| announcing > the count names how many of that category are outstanding | PORTED | `the count names how many of that category are outstanding` | - |
| announcing > dismissing a notification announces REMOVE and drops it | PORTED | `dismissing a notification announces REMOVE and drops it` | - |
| announcing > dismissing an id the queue no longer holds sends nothing | PORTED | `dismissing an id the queue no longer holds sends nothing` | - |
| announcing > the eleventh notification evicts the oldest | PORTED | `the eleventh notification evicts the oldest` | - |
| answering an attribute request > an id that has aged out of the queue sends nothing at all | PORTED | `an id that has aged out of the queue sends nothing at all` | - |
| answering an attribute request > a short body goes out as one chunk | PORTED | `a short body goes out as one chunk` | - |
| answering an attribute request > a body too long for one chunk is split at 300 bytes with a cumulative CRC | PORTED | `a body too long for one chunk is split at 300 bytes with a cumulative CRC` | same 700-byte body, same 300/300/<300 split, offsets and running-CRC check |
| answering an attribute request > the final acknowledgement is sent once the blob has drained | PORTED | `the final acknowledgement is sent once the blob has drained` | identical fixture [0xAB,0x13,0x00,0x00] |
| answering an attribute request > a second request for the same notification restarts the transfer from offset zero | PORTED | `a second request for the same notification restarts the transfer from offset zero` | - |
| answering an attribute request > an action request is not answered, because none were announced | PORTED | `an action request is not answered, because none were announced` | - |
| transfer flow control > a chunk answered with RESEND is sent again at the same offset with the same CRC | PORTED | `a chunk answered with RESEND is sent again at the same offset with the same CRC` | - |
| transfer flow control > the transfer continues normally after a honoured RESEND | PORTED | `the transfer continues normally after a honoured RESEND` | - |
| transfer flow control > a second RESEND for the same chunk abandons the transfer | PORTED | `a second RESEND for the same chunk abandons the transfer` | - |
| transfer flow control > ABORT stops the transfer without sending anything further | PORTED | `ABORT stops the transfer without sending anything further` | - |
| transfer flow control > a CRC mismatch abandons rather than retrying, because retrying would send the same bytes | PORTED | `a CRC mismatch abandons rather than retrying, because retrying would send the same bytes` | - |
| transfer flow control > an OFFSET_MISMATCH abandons, because the status names no offset to recover to | PORTED | `an OFFSET_MISMATCH abandons, because the status names no offset to recover to` | - |
| transfer flow control > a transfer status arriving with nothing in flight is ignored | PORTED | `a transfer status arriving with nothing in flight is ignored` | - |
| transfer flow control > unsubscribing mid-transfer drops it | PORTED | `unsubscribing mid-transfer drops it` | - |
| announcing actions > a notification with actions sets the NEW_ACTIONS phone flag | PORTED | `a notification with actions sets the NEW_ACTIONS phone flag` | - |
| announcing actions > a notification with no actions does not claim any | PORTED | `a notification with no actions does not claim any` | - |
| announcing actions > the ACTIONS attribute carries every offered action | PORTED | `the ACTIONS attribute carries every offered action` | same blob[8] == 3 assertion |
| acting from the wrist > a custom action resolves to the Android index it came from | PORTED | `a custom action resolves to the Android index it came from` | - |
| acting from the wrist > a reply carries the text the wearer dictated | PORTED | `a reply carries the text the wearer dictated` | - |
| acting from the wrist > dismiss resolves to the synthetic action, not one of the app's | PORTED | `dismiss resolves to the synthetic action, not one of the app's` | - |
| acting from the wrist > the legacy refuse control maps onto dismiss | PORTED | `the legacy refuse control maps onto dismiss` | same code 1 fixture |
| acting from the wrist > the legacy accept control does nothing, because nothing offers it | PORTED | `the legacy accept control does nothing, because nothing offers it` | - |
| acting from the wrist > an action code that was never offered is ignored | PORTED | `an action code that was never offered is ignored` | - |
| acting from the wrist > an action on a notification that has aged out is ignored | PORTED | `an action on a notification that has aged out is ignored` | - |
| acting from the wrist > actions are ignored entirely before the watch subscribes | PORTED | `actions are ignored entirely before the watch subscribes` | - |

## test/devices/garmin/garmin_settings_link_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/devices/garmin/GarminSettingsLinkTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| closing the link resolves an in-flight screen read at once | PORTED | `closing the link resolves an in-flight screen read at once` | stronger: asserts completion with no virtual time advanced instead of a 2s timeout |
| a request on an already-closed link answers null immediately | PORTED | `a request on an already-closed link answers null immediately` | same, asserts `isCompleted` without advancing the clock |

## test/devices/garmin/garmin_settings_model_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/devices/garmin/GarminSettingsModelTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| an alarm's own screen, as a vívoactive 5 sends it > reads each row as the control the watch declared | PORTED | `reads each row as the control the watch declared` | identical protobuf fixture builders |
| an alarm's own screen > the options come from the WATCH, never from this app | PORTED | `the options come from the WATCH never from this app` | - |
| an alarm's own screen > a switch takes its value from the STATE, not the definition | PORTED | `a switch takes its value from the STATE not the definition` | - |
| an alarm's own screen > without a state, a switch is neither a toggle NOR a button | PORTED | `without a state a switch is neither a toggle NOR a button` | - |
| an alarm's own screen > a button is the row the WATCH marked, not one we inferred | PORTED | `a button is the row the WATCH marked not one we inferred` | same field-9-empty removable state |
| an alarm's own screen > an untargeted row the watch did NOT mark is never a button | PORTED | `an untargeted row the watch did NOT mark is never a button` | - |
| rows a phone cannot act on > an unused slot is blank, and blank rows are droppable | PORTED | `an unused slot is blank and blank rows are droppable` | - |
| rows a phone cannot act on > an unhandled target keeps the type it declared | PORTED | `an unhandled target keeps the type it declared` | - |
| rows a phone cannot act on > an empty alarm slot leads nowhere | PORTED | `an empty alarm slot leads nowhere` | - |
| rows a phone cannot act on > opens-on-the-watch and hidden are inert, not guessed at | PORTED | `opens-on-the-watch and hidden are inert not guessed at` | - |
| rows a phone cannot act on > an unknown target type is inert rather than a guessed widget | PORTED | `an unknown target type is inert rather than a guessed widget` | - |
| the Clocks screen > a populated alarm is a subscreen, whichever target type it uses | PORTED | `a populated alarm is a subscreen whichever target type it uses` | - |
| telling one screen's reply from another > a definition names the screen it describes | PORTED | `a definition names the screen it describes` | - |
| telling one screen's reply from another > a state names it too | PORTED | `a state names it too` | - |
| telling one screen's reply from another > a change response names it from a field of its own | PORTED | `a change response names it from a field of its own` | byte-for-byte identical captured 28-byte reply, same 16973888 |
| telling one screen's reply from another > a reply about another screen is not this screen's answer | PORTED | `a reply about another screen is not this screen's answer` | - |
| the value behind a row, as the watch reports it > a chosen option is a position, not the summary text | PORTED | `a chosen option is a position not the summary text` | - |
| the value behind a row > a time comes back as the time, not just its rendering | PORTED | `a time comes back as the time not just its rendering` | same 40200s → 11:10 |
| a nameless row is hidden even when it carries a value | PORTED | `a nameless row is hidden even when it carries a value` | - |
| a reply that is not a definition yields no screen | PORTED | `a reply that is not a definition yields no screen` | - |

## test/devices/garmin/garmin_settings_service_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/devices/garmin/GarminSettingsServiceTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| ChangeRequest — the only write in the stack > names the screen and entry it changes | PORTED | `a change names the screen and entry it changes` | - |
| ChangeRequest > each value kind lands in its OWN field | PORTED | `each value kind lands in its OWN field` | same fields 3/4/6/8 and 25200s |
| ChangeRequest > a switch off is a present false, not an absent field | PORTED | `a switch off is a present false not an absent field` | - |
| ChangeRequest > a time outside one day is refused, not wrapped | PORTED | `a time outside one day is refused not wrapped` | ArgumentError → IllegalArgumentException, same 25h and -1s inputs |
| ChangeRequest > SUCCESS is ZERO here — the opposite of the find service | PORTED | `SUCCESS is ZERO here — the opposite of the find service` | same 0/1/absent/null cases |
| walking the tree > finds the screens an entry leads to | PORTED | `finds the screens an entry leads to` | - |
| walking the tree > follows a subscreen-WITH-OPTIONS, which is what an alarm is | PORTED | `follows a subscreen-WITH-OPTIONS which is what an alarm is` | - |
| walking the tree > an empty alarm slot points at screen zero and is skipped | PORTED | `an empty alarm slot points at screen zero and is skipped` | - |
| walking the tree > does not try to walk into what it cannot open | PORTED | `does not try to walk into what it cannot open` | - |
| walking the tree > a reply carrying a STATE is not a definition | PORTED | `a reply carrying a STATE is not a definition` | - |
| init carries the locale that translates the whole tree | PORTED | `init carries the locale that translates the whole tree` | - |
| a definition request names the screen and the language | PORTED | `a definition request names the screen and the language` | - |
| a state request carries only the screen id | PORTED | `a state request carries only the screen id` | - |
| recognises a definition reply, and does not mistake a state for one | PORTED | `recognises a definition reply and does not mistake a state for one` | - |
| a reply for another service is not a settings reply | PORTED | `a reply for another service is not a settings reply` | - |

## test/ui/charts/bar_chart_zoom_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/ui/charts/PeriodChartTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a week chart draws every day until it is pinched | PORTED | ChartPinchAxisTest: `aWeekBarChart_drawsEveryDayUntilItIsPinched` | Compose instrumentation; runs on a device, not in CI |
| tapping a bar on a ZOOMED chart selects the day under the finger | PORTED | PeriodChartTest: `a tap selects the bucket it lands on, zoomed or not` | - |

## test/ui/charts/bar_label_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/ui/charts/BarLabelTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a label that fits is drawn at full size | PORTED | BarLabelTest: `a label that fits is drawn at full size` | Kotlin measures at ONE size, so "full size" is the only size; the fits-therefore-drawn half is identical |
| a label that does not fit SHRINKS — it does not vanish | BLOCKED | - | Kotlin measures at one size and drops on overflow — there is no shrink-to-fit ladder to assert |
| the longer the number, the smaller it is drawn — but it is drawn | BLOCKED | - | Same one-size measurement: no step-down exists in Kotlin |
| the unit goes on its own line, so the number keeps the room | PORTED | BarLabelTest: `the unit goes on its own line, so the number keeps the room` | `splitBarValueLabel` also strips the grouping separator — a deliberate divergence from Dart's ['21,104','steps'] |
| a label nobody could read is still dropped | PORTED | BarLabelTest: `a label nobody could read is still dropped` | Overflow, blank and zero-width cases |

## test/ui/charts/chart_curve_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/ui/charts/ChartCurveTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a rising series never dips on the way up | PORTED | ChartCurveTest: `a rising series never dips on the way up` | samples Bézier segments instead of walking a Path; same inputs/assertion |
| a flat run stays flat | PORTED | ChartCurveTest: `a flat run stays flat` | - |
| never overshoots below the lowest sample | PORTED | ChartCurveTest: `never overshoots beyond the extreme samples` | - |
| draws a vertical riser straight rather than looping through it | PORTED | ChartCurveTest: `draws a vertical riser straight rather than looping through it` | Kotlin additionally asserts the segment is a Line |
| degenerate inputs do not throw | PORTED | ChartCurveTest: `degenerate inputs yield no segments and do not throw` | - |

## test/ui/charts/chart_decimation_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/ui/charts/ChartDecimationTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| decimateOffsets > returns the same list unchanged when already at or below target | PORTED | ChartDecimationTest: `returns the same list unchanged when already at or below target` | - |
| decimateOffsets > does not downsample when target is degenerate | PORTED | ChartDecimationTest: `does not downsample when target is degenerate` | - |
| decimateOffsets > reduces to exactly the target count | PORTED | ChartDecimationTest: `reduces to exactly the target count` | - |
| decimateOffsets > keeps the first and last point | PORTED | ChartDecimationTest: `keeps the first and last point` | - |
| decimateOffsets > preserves an isolated peak (LTTB keeps extremes) | PORTED | ChartDecimationTest: `preserves an isolated peak because LTTB keeps extremes` | - |

## test/ui/charts/chart_scrubber_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/ui/charts/ChartScrubberTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a VERTICAL drag starting on the chart still scrolls the page | PORTED | ChartScrubberGestureTest: `aVerticalDragStartingOnTheChart_stillScrollsThePage` | Compose instrumentation; runs on a device, not in CI |
| a HORIZONTAL drag reads the chart | PORTED | ChartScrubberGestureTest: `aHorizontalDragReadsTheChart` | Compose instrumentation; runs on a device, not in CI |
| it snaps to the nearest SAMPLE, never between two | PORTED | ChartScrubberTest: `snaps to the nearest target by x` + `a tie keeps the first target` | - |
| it stands down while a pinch is in progress | PORTED | ChartScrubberGestureTest: `itStandsDownWhileAPinchIsInProgress` | Compose instrumentation; runs on a device, not in CI |
| a chart with nothing to say stays inert | PORTED | ChartScrubberGestureTest: `aChartWithNothingToSayStaysInert` | Compose instrumentation; runs on a device, not in CI |

## test/ui/charts/chart_skeleton_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| the skeleton settles when motion is off | PORTED | ChartMotionTest: `theSkeletonSettlesWhenMotionIsOff` | Compose instrumentation; runs on a device, not in CI |
| the skeleton does animate when motion is on | PORTED | ChartMotionTest: `theSkeletonDoesAnimateWhenMotionIsOn` | Compose instrumentation; runs on a device, not in CI |
| the reveal is fully drawn on the first frame when motion is off | PORTED | ChartMotionTest: `theRevealIsFullyDrawnOnTheFirstFrameWhenMotionIsOff` | Compose instrumentation; runs on a device, not in CI |
| the reveal animates from nothing when motion is on | PORTED | ChartMotionTest: `theRevealAnimatesFromNothingWhenMotionIsOn` | Compose instrumentation; runs on a device, not in CI |

## test/ui/charts/chart_zoom_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/ui/charts/ChartViewportTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| ChartViewport > starts as the whole chart | PORTED | ChartViewportTest: `starts as the whole chart` | - |
| ChartViewport > zooming keeps the point between the fingers under the fingers | PORTED | ChartViewportTest: `zooming keeps the point between the fingers under the fingers` | - |
| ChartViewport > a point outside the window is NOT clamped into it | PORTED | ChartViewportTest: `a point outside the window is NOT clamped into it` | - |
| ChartViewport > panning moves the data under the finger by the distance dragged | PORTED | ChartViewportTest: `panning moves the data under the finger by the distance dragged` | - |
| ChartViewport > the window stops at the ends rather than sliding off | PORTED | ChartViewportTest: `the window stops at the ends rather than sliding off` | - |
| ChartViewport > there is a floor on how far you can zoom in | PORTED | ChartViewportTest: `there is a floor on how far you can zoom in` | - |
| ChartViewport > zooming back out never overshoots the whole chart | PORTED | ChartViewportTest: `zooming back out never overshoots the whole chart` | - |
| ChartZoom > two fingers pinching apart zooms in | PORTED | ChartZoomGestureTest: `twoFingersPinchingApart_zoomsIn` | Compose instrumentation; runs on a device, not in CI |
| ChartZoom > two fingers zoom even inside a scrolling page | PORTED | ChartZoomGestureTest: `twoFingersZoom_evenInsideAScrollingPage` | Compose instrumentation; runs on a device, not in CI |
| ChartZoom > ONE finger dragging horizontally does not zoom | PORTED | ChartZoomGestureTest: `oneFingerDraggingHorizontally_doesNotZoom` | Compose instrumentation; runs on a device, not in CI |
| ChartZoom > the page still scrolls when dragged from inside a chart | PORTED | ChartZoomGestureTest: `thePageStillScrolls_whenDraggedFromInsideAChart` | Compose instrumentation; runs on a device, not in CI |
| ChartZoom > double tap returns the whole chart | PORTED | ChartZoomGestureTest: `doubleTap_returnsTheWholeChart` | Compose instrumentation; runs on a device, not in CI |

## test/ui/charts/charts_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/ui/charts/PeriodChartTest.kt (plus ui/components/PeriodHeatmapTest.kt)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| SparklineChart renders | PORTED | ChartsRenderTest: `sparkline_renders` | Compose instrumentation; needs a device, so not run in CI |
| SparklineChart with a single value renders | PORTED | ChartsRenderTest: `sparkline_withASingleValue_renders` | Compose instrumentation; needs a device, so not run in CI |
| PeriodBarChart (week) renders bars and axis | PORTED | ChartsRenderTest: `periodBarChart_week_rendersBarsAndAxis` | Compose instrumentation; needs a device, so not run in CI |
| PeriodBarChart tap selects a day | PORTED | ChartsRenderTest: `periodBarChart_tapSelectsADay` | Compose instrumentation; needs a device, so not run in CI |
| PeriodHistoryChart month renders a calendar heatmap | PORTED | ChartsRenderTest: `periodHistoryChart_month_rendersACalendarHeatmap` | Compose instrumentation; needs a device, so not run in CI |
| PeriodHistoryChart year renders a year heatmap | PORTED | ChartsRenderTest: `periodHistoryChart_year_rendersAYearHeatmap` | Compose instrumentation; needs a device, so not run in CI |
| periodYearHeatmapCells (rolling) spans across the calendar-year edge | BLOCKED | - | Kotlin's `periodYearHeatmapCells` has no `rolling` parameter — the year heatmap always draws the calendar year of `period.start` |
| periodYearHeatmapCells (calendar) draws Jan 1 to Dec 31 | PORTED | PeriodHeatmapTest: `year heatmap includes leap day` (+ `year heatmap includes each day in the year and aggregates values by date`) | - |
| MetricBarChart builds its summary from the period title | PORTED | ChartsRenderTest: `metricBarChart_buildsItsSummaryFromThePeriodTitle` | Compose instrumentation; needs a device, so not run in CI |
| MetricLineChart (week) renders line + axis | PORTED | ChartsRenderTest: `metricLineChart_week_rendersLineAndAxis` | Compose instrumentation; needs a device, so not run in CI |
| MetricLineChart (day) renders time axis with distinct times | PORTED | ChartsRenderTest: `metricLineChart_day_rendersATimeAxisWithDistinctTimes` | Compose instrumentation; needs a device, so not run in CI |
| MetricLineChart renders nothing when there are no points | PORTED | ChartsRenderTest: `metricLineChart_rendersNothingWhenThereAreNoPoints` | Compose instrumentation; needs a device, so not run in CI |
| PeriodChartXAxis renders its labels | PORTED | ChartsRenderTest: `periodChartXAxis_rendersItsLabels` | Compose instrumentation; needs a device, so not run in CI |
| charts render in dark theme | PORTED | ChartsRenderTest: `charts_renderInDarkTheme` | Compose instrumentation; needs a device, so not run in CI |
| formatCompactAxisValue formats compactly | PORTED | PeriodChartTest: `compact y axis values abbreviate large numbers` | All four Dart cases: 0, 12, 1.5k, 2M |

## test/ui/charts/day_axis_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/ui/charts/ChartTimeAxesTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| DayAxis > places a time at its real hour, not at its share of the elapsed day | PORTED | ChartTimeAxesTest: `places a time at its real hour, not at its share of the elapsed day` | Same 12:49 / 09:29 → 0.395 regression fixture, against `axisFractionOf` over a whole day |
| DayAxis > spans the whole day, so the labels under it are true | PORTED | ChartTimeAxesTest: `the day axis spans the whole day, so the labels under it are true` | Same 0 / 0.25 / 0.5 / 0.75 quarter grid |
| DayAxis > today's series stops at now, rather than claiming the rest of the day | PORTED | ChartTimeAxesTest: `today's series stops at now, rather than claiming the rest of the day` | Against the new `dayEndFraction` seam; same 12:00 → 0.5 and 06:00 → 0.25 |
| DayAxis > a past day runs to its right edge | PORTED | ChartTimeAxesTest: `a past day runs to its right edge` | Same next-morning-04:00 clock, isToday false and endFraction 1 |
| DayAxis > clamps a time from outside the day onto it | PORTED | ChartTimeAxesTest: `clamps a time from outside the day onto it` | Same previous-day 22:00 → 0 and next-day 02:00 → 1 |
| DayAxis > honours the injected clock rather than the wall clock | PORTED | ChartTimeAxesTest: `honours the injected clock rather than the wall clock` | `isDayToday` takes the clock; also pins that midnight opens the day and the next midnight does not |
| DayAxisLabels > reads midnight to midnight | PORTED | ChartTimeAxesTest: `full viewport gives the classic five hour labels` | - |
| DayAxisLabels > starts where the plot starts, not where the card does | PORTED | ChartAxisLayoutTest: `dayAxisLabels_startWhereThePlotStarts_notWhereTheCardDoes` | Compose instrumentation; runs on a device, not in CI |
| DayAxisLabels > a painter with no y axis can opt out | PORTED | ChartAxisLayoutTest: `dayAxisLabels_aPainterWithNoYAxisCanOptOut` | Compose instrumentation; runs on a device, not in CI |

## test/ui/charts/day_chart_zoom_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/ui/charts/ChartTimeAxesTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| dayAxisLabelsFor > the whole day is the five labels it always was | PORTED | ChartTimeAxesTest: `full viewport gives the classic five hour labels` | - |
| dayAxisLabelsFor > zoomed in, the row says the hours actually on the plot | PORTED | ChartTimeAxesTest: `zoomed viewport labels the hours actually under the plot` (+ `zoomed labels round to whole minutes`) | different viewport values, identical rule |
| pinching a day chart zooms the plot AND its hours | PORTED | ChartPinchAxisTest: `pinchingADayChart_zoomsThePlotAndItsHours` | Compose instrumentation; runs on a device, not in CI |

## test/ui/charts/heatmap_cells_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/ui/components/PeriodMonthHeatmapCellsTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| periodMonthHeatmapCells > a calendar month renders the whole month, greying past the window | PORTED | PeriodMonthHeatmapCellsTest: `a calendar month draws the whole month` + `a calendar month greys the days outside the loaded window on both sides` | - |
| periodMonthHeatmapCells > a rolling window spans both months and keeps every day | PORTED | PeriodMonthHeatmapCellsTest: `a rolling window keeps the values on both sides of the month boundary` | Same 21 Jun – 20 Jul window and the same 1800 / 2200 values either side of the boundary |

## test/ui/charts/metric_day_chart_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/ui/charts/ChartTimeAxesTest.kt, ChartRangeTest.kt, BucketedSeriesTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| DaySeriesShape > raw plots the readings and invents nothing | PORTED | ChartTimeAxesTest: `raw plots the readings and invents nothing` | Same 06:00/18:00 → 0.25/0.75 two-point fixture; no midnight anchor, no trailing hold |
| DaySeriesShape > cumulative anchors at midnight and holds the total to the end | PORTED | ChartTimeAxesTest: `cumulative shape anchors at zero and plateaus out to the end fraction` | - |
| DaySeriesShape > cumulative on today stops at now, not at the right edge | PORTED | ChartTimeAxesTest: `cumulative on today stops at now, not at the right edge` | endFraction now derived from `now` through `dayEndFraction`; same 400 held to 0.5 |
| DaySeriesShape > every shape survives an empty day | PORTED | ChartTimeAxesTest: `every shape survives an empty day` | Both shapes, not just the cumulative one |
| ChartRange.padded > clears the data without dropping below the floor | PORTED | ChartRangeTest: `the floor stops the padding from dipping below it` (+ `pads by the given fraction of the span`) | Kotlin pins exact values rather than inequalities |
| ChartRange.padded > a flat series still gets an axis to breathe in | PORTED | ChartRangeTest: `a flat series pads against the value's own magnitude` (+ `a flat series below one pads against a unit basis`) | - |
| ChartRange.padded > an empty series does not divide by nothing | PORTED | ChartRangeTest: `an empty series falls back to a unit range` | - |
| MetricDayChart > an empty day says so, and draws no plot | PORTED | IntradayActivityChartCardTest: `anEmptyDaySaysSoAndDrawsNoPlot` | Compose instrumentation; runs on a device, not in CI |
| MetricDayChart > a day with readings draws the plot and the hour row | PORTED | IntradayActivityChartCardTest: `aDayWithReadingsDrawsThePlotAndTheHourRow` | Compose instrumentation; runs on a device, not in CI |
| MetricDayChart > sorts the samples it is handed | BLOCKED | - | Kotlin's `rawDayPlotPoints` plots in caller order; only the bucketed branch sorts, so the guarantee does not exist to assert |
| MetricDayChart > the header and footer slots replace the defaults | N/A-WIDGET | - | slot composition |
| MetricDayChart > off draws the raw readings and no band | DIVERGED | ChartTimeAxesTest: `raw plots the readings and invents nothing`; BucketedSeriesTest: `non-positive bucket width yields no buckets` | The point list is pinned; the band-is-empty half lives in the composable's non-bucketed branch |
| MetricDayChart > aggregated buckets the readings into an average line with a band | PORTED | BucketedSeriesTest: `computes average, min and max per bucket` + `splits samples into separate buckets and orders them by time` | - |

## test/ui/charts/metric_line_chart_zoom_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| pinching a day MetricLineChart zooms the line and its hours | PORTED | ChartPinchAxisTest: `pinchingADayChart_zoomsThePlotAndItsHours` | Compose instrumentation; runs on a device, not in CI |
| pinching a year MetricLineChart zooms it too | PORTED | ChartPinchAxisTest: `pinchingAYearLineChart_zoomsItsDateRowToo` | Compose instrumentation; runs on a device, not in CI |
| a period chart still pinches when one finger moves before the other lands (real-device timing) | PORTED | ChartZoomGestureTest: `aPinchStillLands_whenOneFingerMovesBeforeTheOther` | Compose instrumentation; runs on a device, not in CI |
| switching the year resets a zoom rather than carrying it over | PORTED | ChartZoomGestureTest: `changingTheChartsKey_resetsTheZoomRatherThanCarryingItOver` | Compose instrumentation; runs on a device, not in CI |

## test/ui/charts/session_axis_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/ui/charts/ChartTimeAxesTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| SessionAxis > places a sample at its elapsed position in the session | PORTED | ChartTimeAxesTest: `places a sample at its elapsed position in the session` | Same 0 / 0.25 / 0.5 / 1 quarter grid |
| SessionAxis > spans the recorded session, not the samples that exist | PORTED | ChartTimeAxesTest: `spans the recorded session, not the samples that exist` | Same twenty-minutes-into-an-hour → 1/3 regression |
| SessionAxis > clamps a sample from outside the session onto it | PORTED | ChartTimeAxesTest: `clamps a sample from outside the session onto it` | Asserted on SessionAxis itself |
| SessionAxis > a zero-length session does not divide by zero | PORTED | ChartTimeAxesTest: `a zero length session still has a positive axis` | - |
| SessionAxis > labels the quarters in elapsed time | PORTED | ChartTimeAxesTest: `elapsed labels at full zoom span the whole session` | - |
| SessionAxis with pauses > the axis is moving time, not wall-clock | PORTED | ChartTimeAxesTest: `duration is the moving time` | - |
| SessionAxis with pauses > the fixes either side of a pause end up next to each other | PORTED | ChartTimeAxesTest: `an instant inside a pause resolves to the moment the pause began` | - |
| SessionAxis with pauses > what was recorded DURING a pause sits where the pause began | PORTED | ChartTimeAxesTest: `an instant inside a pause resolves to the moment the pause began` | - |
| SessionAxis with pauses > the scrubber and the labels agree, both in moving time | PORTED | ChartTimeAxesTest: `the scrubber and the labels agree, both in moving time` | Paused axis: elapsedAt(0.5) == 15 min and the same 0:00/7:30/15:00/22:30/30:00 row |
| SessionAxis with pauses > overlapping pauses are counted once | PORTED | ChartTimeAxesTest: `overlapping pauses are merged so shared time is not subtracted twice` | - |
| SessionAxis with pauses > a pause reaching outside the session is clipped to it | PORTED | ChartTimeAxesTest: `pauses are clipped to the session` | - |
| SessionAxis with pauses > an entirely paused session does not divide by zero | PORTED | ChartTimeAxesTest: `an entirely paused session does not divide by zero` | Same durationMs 1 and mid-session fraction 0 |
| SessionAxisLabels starts where the plot starts | PORTED | ChartAxisLayoutTest: `sessionAxisLabels_startWhereThePlotStarts` | Compose instrumentation; runs on a device, not in CI |

## test/ui/charts/session_chart_zoom_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/ui/charts/ChartTimeAxesTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| elapsedLabelsFor > the whole session is the five labels it always was | PORTED | ChartTimeAxesTest: `elapsed labels at full zoom span the whole session` | - |
| elapsedLabelsFor > zoomed in, the row says the minutes actually on the plot | PORTED | ChartTimeAxesTest: `elapsed labels under zoom describe the visible slice` | 0.5..1 slice vs 0.75..1; same rule |
| pinching a session chart zooms the trace AND its elapsed row | PORTED | ChartPinchAxisTest: `pinchingASessionChart_zoomsTheTraceAndItsElapsedRow` | Compose instrumentation; runs on a device, not in CI |

## test/ui/components/health_connect_gate_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/healthconnect/HealthConnectFeatureTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| shows the access gate when Health Connect is unavailable | BLOCKED | HealthConnectFeatureTest: `unavailable Health Connect bypasses the access gate and surfaces availability on the state` | Kotlin deliberately routes unavailability out of band (state.availability + the dashboard promo) instead of a blocking gate; the Kotlin test pins that routing |
| shows the child when available and permitted | PORTED | HealthConnectFeatureTest: `shows the child when available and permitted` | Pass-through: no gate mode, no prompt, nothing missing |
| shows the permission gate when a required permission is missing | BLOCKED | HealthConnectFeatureTest: `a missing required permission raises the contextual prompt over a still-visible child` | Kotlin raises a contextual prompt with the child still shown rather than a blocking gate; the Kotlin test pins that divergence |
| shows the sync-paused gate when sync is disabled | PORTED | HealthConnectFeatureTest: `buildStateShowsAccessGateWhenSyncPaused` | - |
| shows a recoverable error (not a permanent spinner) when a read throws | N/A-FRAMEWORK | - | Riverpod AsyncValue error branch |

## test/ui/components/metric_card_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/androidTest/kotlin/tech/mmarca/openvitals/ui/components/MaterialUxComponentsTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| MetricCard shows title, value, unit, subtitle and source | PORTED | MetricCardTest: `metricCard_showsTitleValueUnitSubtitleAndSource` | Compose instrumentation; needs a device, so not run in CI |
| MetricCard onTap fires | PORTED | MetricCardTest: `metricCard_onClickFires` | Compose instrumentation; needs a device, so not run in CI |
| MetricCardPlaceholder shows its message | PORTED | MetricCardTest: `metricCardPlaceholder_showsItsMessage` | Compose instrumentation; needs a device, so not run in CI |
| TimeRangeSelector renders all ranges and reports selection | PORTED | MaterialUxComponentsTest: `timeRangeSelector_reportsSegmentedSelection` | androidTest; now asserts every range renders, not just the tapped one |
| SectionHeader renders its text | PORTED | MetricCardTest: `sectionHeader_rendersItsText` | Compose instrumentation; needs a device, so not run in CI |

## test/ui/components/metric_detail_scaffold_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/androidTest/kotlin/tech/mmarca/openvitals/ui/components/MetricDetailScaffoldTest.kt (plus core/period/PeriodNavigatorTest.kt, PeriodSelectionDriverSelectDayTest.kt, PeriodLoadQueryTest.kt)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| renders header, range selector and content for the period | PORTED | MetricDetailScaffoldHeaderTest: `rendersTheHeaderSlotTheRangeLabelsAndTheContent` | Compose instrumentation; runs on a device, not in CI |
| selecting a range drives the navigator + content | PORTED | MetricDetailScaffoldHeaderTest: `selectingARangeReportsItAndRetitlesTheNavigator`, `aDayRangeAnchoredOnTodayNamesItToday` | Compose instrumentation; runs on a device, not in CI |
| provides a day opener that drills into that day's Day view | PORTED | PeriodSelectionDriverSelectDayTest: `selectDay switches to the day range anchored on the tapped date` (+ `selectDay pins a past day so resuming does not bounce back to today`) | Kotlin tests the driver directly rather than a scaffold-provided opener |
| tapping next past today is a no-op (forward-capped) | PORTED | PeriodNavigatorTest: `periodFor end is never after today`; PeriodLoadQueryTest: `selection driver persists range and clamps next period` | The cap is the whole behaviour; the greyed-out Next button is Compose-only |
| rolling dates retitle the week/month/year periods | PORTED | PeriodLoadQueryTest (rolling windows) + PeriodTitleTest: `rollingPeriodTitlesUseFixedDayWindowLabels` | "Last 7/30/365 days" pinned at the title layer, plus the dated-span fallback once the window stops ending today |
| changing the week mode reloads the selection and retitles | PORTED | PeriodSelectionDriverWeekModeTest: `changing the week mode reloads the selection and retitles` (+ `flipping the week mode back re-derives the calendar week for the same anchor`) | - |
| renders the error block from a ScreenError | PORTED | MetricDetailScaffoldTest: `metricDetailScaffold_displaysErrorMessage` | androidTest |
| reserves the system navigation bar below the last item | N/A-WIDGET | - | edge-to-edge inset padding |
| reserves nothing extra when the bar is a gesture sliver | N/A-WIDGET | - | - |

## test/ui/components/metric_detail_scaffold_refresh_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a signal for the screen own domain refreshes it once | N/A-FRAMEWORK | - | Kotlin has no RefreshCoordinator/DataDomain mechanism; screens reload via their own VM lifecycle |
| a signal for an unrelated domain does not refresh the screen | N/A-FRAMEWORK | - | no DataDomain type exists in Kotlin main to match against |
| a derived domain refreshes the screen that reads it | N/A-FRAMEWORK | - | no hydration→nutrition refresh derivation exists in Kotlin main |
| an app-open signal refreshes the screen | N/A-FRAMEWORK | - | no app-open refresh signal exists in Kotlin main |
| a screen with no declared domains never refreshes | N/A-FRAMEWORK | - | no refresh-domain declaration exists in Kotlin main |
| a signal arriving while a detail route is pushed on top defers the refresh until it is popped | N/A-FRAMEWORK | - | RouteObserver visibility plumbing |
| several signals while covered collapse into one refresh on pop | N/A-FRAMEWORK | - | nearest analogue is DashboardLoadCoalescerTest `concurrent callers share one dashboard load`, a different mechanism |
| a plain back navigation with nothing changed does not refresh | N/A-FRAMEWORK | - | - |
| an app-open signal refreshes only the visible screen, not every mounted one | N/A-FRAMEWORK | - | - |

## test/ui/components/permission_callout_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| PermissionCallout shows title, body and grant action | PORTED | PermissionCalloutTest: `showsTitleBodyAndGrantAction_withNoDismissUntilOneIsGiven` | Compose instrumentation; needs a device, so not run in CI |
| PermissionCallout shows dismiss when provided | PORTED | PermissionCalloutTest: `showsDismissWhenProvided_andHonoursACustomActionLabel` | Compose instrumentation; needs a device, so not run in CI |

## test/ui/components/swipe_to_delete_entry_row_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| end-to-start swipe deletes | PORTED | SwipeToDeleteEntryRowTest: `endToStartSwipe_deletes` | Compose instrumentation; needs a device, so not run in CI |
| start-to-end swipe does nothing (Kotlin disables it) | PORTED | SwipeToDeleteEntryRowTest: `startToEndSwipe_doesNothing` | Compose instrumentation; needs a device, so not run in CI |

## test/ui/text_scaling_sweep_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| dashboard screen survives 2.0 text scale | PORTED | TextScalingSweepTest: `dashboardScreenSurvivesTheLargestFontScale` | Compose instrumentation; runs on a device, not in CI |
| sleep screen survives 2.0 text scale | PORTED | TextScalingSweepTest: `sleepScreenSurvivesTheLargestFontScale` | Compose instrumentation; runs on a device, not in CI |
| hydration screen survives 2.0 text scale | PORTED | TextScalingSweepTest: `hydrationScreenSurvivesTheLargestFontScale` | Compose instrumentation; runs on a device, not in CI |
| activities screen survives 2.0 text scale | PORTED | TextScalingSweepTest: `activitiesScreenSurvivesTheLargestFontScale` | Compose instrumentation; runs on a device, not in CI |
| body screen survives 2.0 text scale | PORTED | TextScalingSweepTest: `bodyScreenSurvivesTheLargestFontScale` | Compose instrumentation; runs on a device, not in CI |
| mindfulness screen survives 2.0 text scale | PORTED | TextScalingSweepTest: `mindfulnessScreenSurvivesTheLargestFontScale` | Compose instrumentation; runs on a device, not in CI |
| heart vitals overview screen survives 2.0 text scale | PORTED | TextScalingSweepTest: `heartVitalsOverviewScreenSurvivesTheLargestFontScale` | Compose instrumentation; runs on a device, not in CI |
| settings screen survives 2.0 text scale | PORTED | TextScalingSweepTest: `settingsScreenSurvivesTheLargestFontScale` | Compose instrumentation; runs on a device, not in CI |

## test/integration/activity_pipeline_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/healthconnect/FixtureReaderTest.kt (plus SwallowingRecordTest.kt, domain/insights/ActivitySplitsTest.kt, domain/model/ExerciseTypeTraitsTest.kt)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a workout gets the heart rate that lived inside a 17-hour record | PORTED | FixtureReaderTest: `but the real reader finds it anyway` (+ `Health Connect hides that workout's heart rate from a windowed read`, SwallowingRecordTest: `a workout buried inside a 17 hour record still has a heart rate`) | Same byte-identical golden.json; Kotlin additionally bounds every returned sample to the workout window |
| a GPS session gets REAL splits, not evenly-estimated ones | DIVERGED | FixtureReaderTest: `speed samples survive the same trap, which is what the splits ride on` | Fixture speed samples are only proven to reach the reader; no Kotlin test feeds them into `computeActivitySplits` to assert `source != ESTIMATED` |
| a strength session gets NO splits, however much GPS drift it picked up | PORTED | ActivitySplitsTest: `a strength session with GPS drift is not cut into laps` + `a run with the same distance IS cut, so the gate is on the KIND`; ExerciseTypeTraitsTest: `a strength session does not travel, whatever GPS drift says` | Kotlin asserts unconditionally on synthetic sessions instead of the fixture's real session (Flutter's assert is guarded by an `if`) |
| sessions keep the provenance that decides dedup and the manual count | PORTED | FixtureReaderTest: `every record keeps the provenance the Pigeon messages kept dropping` + `sessions come from more than one writer, so dedup has something to do` | Kotlin also asserts zone offset and writer, over the same fixture window |

## test/integration/degraded_device_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/healthconnect/HealthConnectAvailabilityServiceTest.kt (partial; permission degradation spread across data/repository/*Test.kt)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a missing Health Connect is reported as unavailable, not as an error | DIVERGED | HealthConnectAvailabilityServiceTest: `availability is not supported in unsupported profiles` | NOT_SUPPORTED is only reached via the work-profile path; no case for `SDK_UNAVAILABLE` (status 1) |
| an out-of-date provider is distinguishable from a missing one | PORTED | HealthConnectAvailabilityServiceTest: `availability needs provider update when Health Connect reports update required` | - |
| a read without its permission returns EMPTY, and does not throw | DIVERGED | VitalsRepositoryTest: `missing permission skips the daily and latest reads for that metric`; MindfulnessRepositoryTest: `loadMindfulnessSessions skips Health Connect when feature permissions are unavailable`; CycleRepositoryTest: `loadCycleData skips reads when no cycle permissions are granted` | Asserted per-repository against a mocked HealthConnectManager, never through the real reader with a heart read |
| holding ONLY the sleep permission still loads sleep, and nothing else | DIVERGED | SleepRepositoryTest: `loadSleepPeriod includes Health Connect aggregate sleep durations` (granted set = sleep only) | No assertion that a missing sibling category leaves the screen/state error-free |
| the permission set the app asks for is the one the device supports | PORTED | HealthConnectPermissionServiceTest: `the permission set the app asks for is the one the device supports` | Same assertions (non-empty, >10, read sleep, read exercise) against the resolved taxonomy; the runtime grant diff needs a device |
| sync paused degrades reads to empty rather than failing | DIVERGED | HealthConnectFeatureTest: `buildStateShowsAccessGateWhenSyncPaused` | Asserts the UX gate mode only; nothing exercises a read with `syncGate.isEnabled == false` |

## test/integration/repositories_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/healthconnect/FixtureReaderTest.kt (fixture-backed half only; rest is mocked repository tests)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| health > availability resolves through the real repository | DIVERGED | HealthConnectAvailabilityServiceTest: `availability remains available on Android 14 even when Play Store is missing` | Service-level with mocked providers, not the repository resolving against a seeded client |
| health > the granted set is the app's permission taxonomy, not a stub | PORTED | HealthConnectPermissionServiceTest: `the permission set the app asks for is the one the device supports` | Same size and membership assertions on the app's own taxonomy |
| sleep > every night in the fixture comes back, with its stages | PORTED | FixtureReaderTest: `every night in the fixture comes back, with its stages` | Real SleepHealthReader over the seeded fixture; same not-empty and stages-survive assertions |
| sleep > two writers on one night are merged into one | PORTED | FixtureReaderTest: `two writers on one night are merged into one` | The fixture's own two-writer night, read back through SleepHealthReader and collapsed to one session |
| heart > the day of the swallowing record has heart rate | PORTED | FixtureReaderTest: `but the real reader finds it anyway` | Window is the swallowed workout rather than the whole local day |
| heart > daily summaries carry min, max and average — not just average | DIVERGED | HeartRepositoryTest: `WEEK average heart rate uses daily aggregate summaries without raw day samples`; HeartVitalsRangeSummaryTest: `resting heart rate summary reports the real average and range` | Routing verified against an empty mock; min ≤ max asserted only on synthetic presentation data |
| activity > every session in the fixture survives the trip, with its writer | PORTED | FixtureReaderTest: `sessions come from more than one writer, so dedup has something to do` | Now asserts the count equals the fixture's own session count as well as the writer multiplicity |
| activity > the GPS session keeps its route points | PORTED | FixtureReaderTest: `the GPS session keeps its route points` | HcFixture now rebuilds the 954-point route onto the record; the session is matched on its boundary because the fake re-stamps ids |
| activity > speed samples reach the repository, which is what splits ride on | PORTED | FixtureReaderTest: `speed samples survive the same trap, which is what the splits ride on` | Same fixture; Kotlin also bounds samples to the window |
| hydration > the fixture's hydration entries come back | PORTED | FixtureReaderTest: `the fixture's hydration entries come back` | HcFixture gained a hydration loader; all 5 entries and their total volume come back through HydrationHealthReader |

## test/integration/use_cases_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/domain/usecase/ (only LoadSleepPeriodUseCaseTest.kt, LoadHeartPeriodUseCaseTest.kt, LoadDashboardDayUseCaseTest.kt exist; the other domains have no use-case layer in Kotlin — coverage is via repository/VM tests over mocks)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| sleep > day returns over real data | DIVERGED | LoadSleepPeriodUseCaseTest: `loads sleep period and daily hrv in parallel windows` | Mocked repositories, one query, no fixture corpus |
| sleep > week returns over real data | DIVERGED | SleepViewModelTest: `initial range is WEEK` | Range only reached via period math, never a load asserted over data |
| sleep > month returns over real data | DIVERGED | SleepViewModelTest: `previousPeriod MONTH moves back one month` | Period math only |
| sleep > year returns over real data | DIVERGED | SleepViewModelTest: `previousPeriod YEAR moves back one year` | Period math only |
| sleep > an EMPTY day returns empty-but-VALID, never null and never a throw | DIVERGED | SleepViewModelTest: `initial sessions list is empty when repo returns nothing` | VM-level, and no assertion that the empty case is error-free |
| sleep > a forced refresh returns the same shape as a normal load | DIVERGED | LoadSleepPeriodUseCaseTest: `force refresh passes refresh mode to sleep repository` | Verifies the flag is forwarded, not that the answer's shape is unchanged |
| heart > day returns over real data | DIVERGED | HeartRepositoryTest: `DAY average heart rate uses raw full samples for selected day graph`; LoadHeartPeriodUseCaseTest: `combined request merges heart and vitals` | Mocked HealthConnectManager |
| heart > week returns over real data | DIVERGED | HeartRepositoryTest: `WEEK average heart rate uses daily aggregate summaries without raw day samples` | Mocked, empty result |
| heart > month returns over real data | DIVERGED | HeartViewModelTest (MONTH via selectRange) | No load assertions for MONTH |
| heart > year returns over real data | DIVERGED | FixtureRangeLoadTest: `the ranged reads answer inside their window, over real data, and never shrink` | Now loaded over the real corpus at every range, but at the reader layer rather than the use case — see that class's doc for why |
| heart > an EMPTY day returns empty-but-VALID, never null and never a throw | DIVERGED | HeartViewModelTest: `DAY range leaves dayRestingBpm null when repo returns null` | Per-field null tolerance, not a whole-result validity check |
| heart > a forced refresh returns the same shape as a normal load | DIVERGED | LoadHeartPeriodUseCaseTest: `force refresh passes refresh mode to repositories` | Flag forwarding only |
| activities > day returns over real data | DIVERGED | ActivityRepositoryTest: `DAY activity metric progress uses raw full data for selected day graph` | Mocked |
| activities > week returns over real data | DIVERGED | ActivitiesViewModelTest: `last seven days week mode loads and displays rolling seven day window` | Mocked |
| activities > month returns over real data | DIVERGED | ActivityViewModelTest: `previousPeriod MONTH moves back one month` | Period math only |
| activities > year returns over real data | DIVERGED | ActivityViewModelTest: `previousPeriod YEAR moves back one year` | Period math only |
| activities > an EMPTY day returns empty-but-VALID, never null and never a throw | DIVERGED | ActivityViewModelTest: `initial load clears loading and sets empty lists` | VM-level |
| activities > a forced refresh returns the same shape as a normal load | MISSING | - | No force/refresh-mode path on the activities load |
| hydration > day returns over real data | DIVERGED | HydrationRepositoryTest: `DAY hydration uses raw full entries for selected day total` | Mocked |
| hydration > week returns over real data | DIVERGED | HydrationViewModelTest: `previousPeriod WEEK moves back one week` / `nextPeriod WEEK advances from a past week` | Period math, load not asserted per range |
| hydration > month returns over real data | DIVERGED | HydrationViewModelTest (MONTH via selectRange) | Range switching only |
| hydration > year returns over real data | DIVERGED | FixtureRangeLoadTest: `the ranged reads answer inside their window, over real data, and never shrink` | Now loaded over the real corpus at every range, at the reader layer |
| hydration > an EMPTY day returns empty-but-VALID, never null and never a throw | DIVERGED | HydrationViewModelTest: `initial load clears loading and sets empty list` | VM-level |
| hydration > a forced refresh returns the same shape as a normal load | DIVERGED | HydrationViewModelTest: `deleteHydrationEntry removes entry and reloads period data` (coVerify RefreshMode.FORCE) | Verifies FORCE is passed, not shape equality |
| body > day returns over real data | MISSING | - | BodyRepositoryTest covers mutations only; no DAY load |
| body > week returns over real data | DIVERGED | BodyViewModelTest: `previousPeriod WEEK moves back one week` | Period math only |
| body > month returns over real data | DIVERGED | BodyViewModelTest: `initial range is MONTH` / `load success populates weight entries` | Mocked repo, no corpus |
| body > year returns over real data | DIVERGED | BodyViewModelTest: `previousPeriod YEAR moves back one year` | Period math only |
| body > an EMPTY day returns empty-but-VALID, never null and never a throw | DIVERGED | BodyViewModelTest: `initial state has empty weight entries and all nulls` + `load uses latest body values when selected period entries are empty` | VM-level |
| body > a forced refresh returns the same shape as a normal load | DIVERGED | BodyViewModelTest (coVerify loadBodyPeriod(any(), ALL, RefreshMode.FORCE)) | Flag forwarding only |
| nutrition > day returns over real data | DIVERGED | NutritionRepositoryTest: `DAY nutrition uses raw full entries for selected day metrics` | Mocked |
| nutrition > week returns over real data | DIVERGED | NutritionViewModelTest: `initial range is WEEK` / `nextPeriod WEEK advances from a past week` | Period math |
| nutrition > month returns over real data | DIVERGED | NutritionViewModelTest (MONTH via selectRange) | Range switching only |
| nutrition > year returns over real data | DIVERGED | NutritionViewModelTest: `year range loads raw meal entries` | Mocked, verifies read routing not result validity |
| nutrition > an EMPTY day returns empty-but-VALID, never null and never a throw | DIVERGED | NutritionViewModelTest: `initial load clears loading and sets empty lists` | VM-level |
| nutrition > a forced refresh returns the same shape as a normal load | DIVERGED | NutritionViewModelTest: `deleting an entry removes it optimistically and force-reloads` | Flag forwarding only |
| cycle > day returns over real data | BLOCKED | - | Kotlin's cycle screen is month-anchored: there is no DAY load to exercise |
| cycle > week returns over real data | BLOCKED | - | Same month-anchored load — no WEEK range exists on the cycle stack |
| cycle > month returns over real data | DIVERGED | CycleViewModelTest: `initial load requests the current month period` + CycleRepositoryTest: `loadCycleData combines all granted cycle data` | Mocked |
| cycle > year returns over real data | MISSING | - | No YEAR case |
| cycle > an EMPTY day returns empty-but-VALID, never null and never a throw | DIVERGED | CycleViewModelTest: `initial load clears loading and sets empty data` | VM-level, month range |
| cycle > a forced refresh returns the same shape as a normal load | DIVERGED | CycleViewModelTest (coVerify loadCyclePeriod(any(), RefreshMode.FORCE)) | Flag forwarding only |
| mindfulness > day returns over real data | MISSING | - | No per-range load test |
| mindfulness > week returns over real data | DIVERGED | MindfulnessViewModelTest: `load success populates sessions and derived total minutes` | Default WEEK range, mocked |
| mindfulness > month returns over real data | DIVERGED | MindfulnessViewModelTest: `selectRange updates selectedRange and reloads` (MONTH) | Range switching only |
| mindfulness > year returns over real data | MISSING | - | No YEAR case |
| mindfulness > an EMPTY day returns empty-but-VALID, never null and never a throw | DIVERGED | MindfulnessViewModelTest: `initial load clears loading and sets empty list` | VM-level |
| mindfulness > a forced refresh returns the same shape as a normal load | DIVERGED | MindfulnessViewModelTest (coVerify loadMindfulnessPeriod(any(), RefreshMode.FORCE)) | Flag forwarding only |
| caffeine > day returns over real data | DIVERGED | CaffeineRepositoryTest: `loadCaffeinePeriod filters caffeine entries and uses lookback` | Mocked, window-based rather than per-TimeRange |
| caffeine > week returns over real data | DIVERGED | CaffeineViewModelTest: `analytics range selection reloads matching caffeine window` | Window plumbing only |
| caffeine > month returns over real data | DIVERGED | CaffeineViewModelTest: `analytics range selection reloads matching caffeine window` | Same test covers all analytics windows loosely |
| caffeine > year returns over real data | MISSING | - | No YEAR analytics window case |
| caffeine > an EMPTY day returns empty-but-VALID, never null and never a throw | DIVERGED | CaffeineRepositoryTest: `loadCaffeinePeriod returns empty list when Health Connect has no caffeine` | Repository-level, not the use case |
| caffeine > a forced refresh returns the same shape as a normal load | DIVERGED | CaffeineViewModelTest: `refresh reloads with force mode` | Flag forwarding only |
| calories > day returns over real data | DIVERGED | CaloriesViewModelTest: `initial load combines total active and BMR data` | Mocked |
| calories > week returns over real data | MISSING | - | No WEEK load case |
| calories > month returns over real data | DIVERGED | CaloriesViewModelTest: `selectRange saves range and reloads` (MONTH) | Range switching only |
| calories > year returns over real data | MISSING | - | No YEAR case |
| calories > an EMPTY day returns empty-but-VALID, never null and never a throw | DIVERGED | CaloriesViewModelTest: `latest BMR is used when selected period has no BMR readings` | Partial-empty only |
| calories > a forced refresh returns the same shape as a normal load | MISSING | - | No refresh-mode path tested for calories |

## test/integration/view_models_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/features/ (per-VM tests; all mocked repositories, none over the fixture corpus) plus /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/core/performance/LoadCoordinatorTest.kt for the shared newest-wins guard
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| sleep > loads a day of real data without erroring | DIVERGED | SleepViewModelTest: `initial load clears loading` + `load success populates sessions` | Hand-built sessions from a mocked repo, not a real day of the corpus |
| sleep > an EMPTY day is empty-but-valid, not an error | DIVERGED | SleepViewModelTest: `initial sessions list is empty when repo returns nothing` | Asserts emptiness but not `error == null` |
| sleep > a stale load does not clobber a newer one | PORTED | LoadCoordinatorTest: `a stale load does not clobber a newer one` | Kotlin's view models share ONE LoadCoordinator, so the guard Dart copy-pastes per screen is pinned once (+ `a superseded load is cancelled, not merely ignored`) |
| activities > loads a day of real data without erroring | DIVERGED | ActivitiesViewModelTest: `last seven days week mode loads and displays rolling seven day window`; ActivityViewModelTest: `load success populates data` | Mocked repo |
| activities > an EMPTY day is empty-but-valid, not an error | DIVERGED | ActivityViewModelTest: `initial load clears loading and sets empty lists`; ActivitiesViewModelTest: `monday to sunday week mode displays all seven days including empty future days` | No error-is-null assertion on the empty path |
| activities > a stale load does not clobber a newer one | PORTED | LoadCoordinatorTest: `a stale load does not clobber a newer one` | Kotlin's view models share ONE LoadCoordinator, so the guard Dart copy-pastes per screen is pinned once (+ `a superseded load is cancelled, not merely ignored`) |
| hydration > loads a day of real data without erroring | DIVERGED | HydrationViewModelTest: `load success populates hydration and derived totals` | Mocked repo |
| hydration > an EMPTY day is empty-but-valid, not an error | DIVERGED | HydrationViewModelTest: `initial load clears loading and sets empty list` | No error-is-null assertion |
| hydration > a stale load does not clobber a newer one | PORTED | LoadCoordinatorTest: `a stale load does not clobber a newer one` | Kotlin's view models share ONE LoadCoordinator, so the guard Dart copy-pastes per screen is pinned once (+ `a superseded load is cancelled, not merely ignored`) |
| heartVitalsOverview > loads a day of real data without erroring | DIVERGED | HeartViewModelTest: `load success populates selected vitals and latest values` + `initial load clears loading` | Kotlin has no HeartVitalsOverview VM; covered by HeartViewModel over mocks |
| heartVitalsOverview > an EMPTY day is empty-but-valid, not an error | DIVERGED | HeartViewModelTest: `DAY range produces empty dailyRestingHR and dailyHrv`; HeartVitalsRangeSummaryTest: `resting heart rate summary is null for no days, not an invented 40 to 80 range` | Field-level emptiness, not a state-validity check |
| heartVitalsOverview > a stale load does not clobber a newer one | PORTED | LoadCoordinatorTest: `a stale load does not clobber a newer one` | Kotlin's view models share ONE LoadCoordinator, so the guard Dart copy-pastes per screen is pinned once (+ `a superseded load is cancelled, not merely ignored`) |

## test/fixtures/fixture_scrub_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/healthconnect/FixtureScrubTest.kt (same byte-identical golden.json, off the test classpath)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| no real writer, vendor or person appears anywhere in the fixture | PORTED | FixtureScrubTest: `no real writer, vendor or person appears anywhere in the fixture` | Identical 19-needle denylist over the same byte-identical golden.json |
| every writer is either an example alias or OpenVitals itself | PORTED | FixtureScrubTest: `every writer is either an example alias or OpenVitals itself` | Same fail-closed allow rule (com.example.*, tech.mmarca.openvitals*, android, com.android.shell) |
| no coordinate is anywhere near the real route | PORTED | FixtureScrubTest: `no coordinate is anywhere near the real route` | Same 55..57 / 2..4 synthetic bbox |
| no free text survived | PORTED | FixtureScrubTest: `no free text survived` | Same four canned title/notes replacements |

## test/fixtures/fixture_shape_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/healthconnect/FixtureShapeTest.kt (the raw-JSON guards) and /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/healthconnect/FixtureReaderTest.kt (the reader-level halves)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a heart-rate record long enough to swallow a workout still exists | PORTED | FixtureReaderTest: `the corpus loads and the swallowing record is still in it` | Same >12 h threshold plus >500 samples and containment of the workout |
| the swallowed workout is invisible to a naive windowed read | PORTED | FixtureReaderTest: `Health Connect hides that workout's heart rate from a windowed read` | Asserted through the real FakeHealthConnectClient rather than on the raw JSON — stronger |
| more than one app wrote sleep on the same night | PORTED | FixtureShapeTest: `more than one app wrote sleep on the same night` | Sleep grouped per UTC night, same "some night has two writers" assertion |
| a GPS route with enough points to compute splits from | PORTED | FixtureShapeTest: `a GPS route with enough points to compute splits from` | Same >500-point threshold |
| the sibling records that a session does NOT carry are present | PORTED | FixtureShapeTest: `the sibling records that a session does NOT carry are present` | Same steps/distance/activeCalories/basalMetabolicRate guards |
| speed is a SERIES record, so splits hit the same bug as heart rate | PORTED | FixtureShapeTest: `speed is a SERIES record, so splits hit the same bug as heart rate` | Raw sample count >10 now asserted, alongside FixtureReaderTest's reader-level case |
| the synthetic records are exactly the two we could not derive | PORTED | FixtureShapeTest: `the synthetic records are exactly the two we could not derive` | Same {power, cyclingCadence} set and the non-empty power guard |
| records carry the provenance the port kept losing | PORTED | FixtureReaderTest: `every record keeps the provenance the Pigeon messages kept dropping` | Same three fields (recordingMethod, lastModifiedTime, zone offset), asserted on reader output rather than on the JSON |

## test/goldens/charts/achievements_cards_golden_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| the summary card, part way through the catalogue | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| the summary card before the first load lands | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| a badge mid-progress — locked, and honest about it | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| a badge barely started | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| a badge earned — the whole card changes, not just the bar | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| a badge earned more than once | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |

## test/goldens/charts/activity_splits_golden_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| splits cut from the route | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| laps the device recorded itself | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| estimated splits — the numbers, and no bar | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |

## test/goldens/charts/axis_rows_golden_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| the y-axis label column, beside its plot | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| the hour row under a plot that HAS a y axis | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| the hour row under a plot that has NO y axis | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| the elapsed row, inset and not | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| the date strip — a week keeps every label | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| the date strip — a month drops all but every fifth | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| the date strip — a year keeps the twelve month names | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |

## test/goldens/charts/body_energy_timeline_golden_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a day up to the golden clock | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| early morning — few enough points that the line grows dots | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |

## test/goldens/charts/caffeine_curve_golden_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a day of drinking — sawtooth, threshold, markers | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| a single dose — one rise, one long decay | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| a day with nothing in it | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |

## test/goldens/charts/distribution_bars_golden_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| caffeine by source | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| caffeine by category — long labels against the value | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| caffeine with nothing logged | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| caffeine by time of day | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| hydration drink breakdown | N/A-WIDGET | - | hydration_week_detail_current.png covers the hydration detail surface, not this component in isolation |
| hydration where one drink is nearly everything | N/A-WIDGET | - | - |

## test/goldens/charts/metric_day_chart_golden_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| cumulative, a day that is over | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| cumulative, today — the rest of the day has not happened | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| raw readings, plotted where they were taken | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| a day with nothing in it | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |

## test/goldens/charts/metric_line_chart_golden_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| one series, no legend | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| two series and the legend that comes with them | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| two series with a day selected | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |

## test/goldens/charts/metric_session_chart_golden_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a recorded trace, dense enough that the dots come off | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| a trace stepped per split, sparse enough to show its points | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| cadence, the other card that shares this scaffold | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |

## test/goldens/charts/period_bar_chart_golden_test.dart
Kotlin counterpart: none (closest surface: app/src/androidTest/assets/goldens/hydration_week_detail_current.png via OpenVitalsVisualRegressionTest.hydrationWeekDetail_matchesCurrentBaseline)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a week — seven slots, and a number on every bar | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| a week with a day selected | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| a month — 31 slots, too narrow for a label to survive | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| a week at 1.5× text — the labels step down instead of vanishing | N/A-WIDGET | - | no androidTest golden equivalent; androidTest pins fontScale = 1f |

## test/goldens/charts/period_chart_golden_test.dart
Kotlin counterpart: none (closest surface: hydration_week_detail_current.png)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| week — bars | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| week — bars, with the selected day highlighted | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| month — the calendar heatmap | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| year — the dot heatmap, twelve averaged months | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |

## test/goldens/charts/sleep_cards_golden_test.dart
Kotlin counterpart: none (SleepScreenWeekTest.kt is semantics-only, no golden)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a night broken down by stage | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| a device that only says "asleep" | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |

## test/goldens/charts/sleep_schedule_chart_golden_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a week of nights | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| a partly-staged night reads at its full duration | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| a week with the average bedtime and wake-up marked | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |

## test/goldens/charts/sleep_stage_chart_golden_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a night, with the lane totals the detail screen shows | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| the same night on the day card, labels without totals | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| a device that only says "asleep" | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |

## test/goldens/charts/sparkline_golden_test.dart
Kotlin counterpart: none (dashboard_current.png renders the full DashboardContent, which includes sparkline widgets)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a week of buckets | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| one point — a flat run across the whole width | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| one point with singlePointLine off — the dot on its own | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |

## test/goldens/charts/summary_ring_golden_test.dart
Kotlin counterpart: none (dashboard_current.png includes ring widgets on the dashboard surface)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| nothing yet — the track, and no fill | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| part way round | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| past the goal — the arc closes, the number keeps going | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| a value long enough to fight the ring for room | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |

## Summary

| Status | Count |
|---|---|
| PORTED | 534 |
| DIVERGED | 63 |
| MISSING | 11 |
| BLOCKED | 8 |
| N/A-WIDGET | 118 |
| N/A-FRAMEWORK | 10 |
| Total cases | 744 |

### Blocked on a behavior decision

Porting these would mean changing production code, which this wave does not do.

- test/ui/charts/bar_label_test.dart — a label that does not fit SHRINKS — it does not vanish — Kotlin measures at one size and drops on overflow — there is no shrink-to-fit ladder to assert
- test/ui/charts/bar_label_test.dart — the longer the number, the smaller it is drawn — but it is drawn — Same one-size measurement: no step-down exists in Kotlin
- test/ui/charts/charts_test.dart — periodYearHeatmapCells (rolling) spans across the calendar-year edge — Kotlin's `periodYearHeatmapCells` has no `rolling` parameter — the year heatmap always draws the calendar year of `period.start`
- test/ui/charts/metric_day_chart_test.dart — MetricDayChart > sorts the samples it is handed — Kotlin's `rawDayPlotPoints` plots in caller order; only the bucketed branch sorts, so the guarantee does not exist to assert
- test/ui/components/health_connect_gate_test.dart — shows the access gate when Health Connect is unavailable — Kotlin deliberately routes unavailability out of band (state.availability + the dashboard promo) instead of a blocking gate; the Kotlin test pins that routing
- test/ui/components/health_connect_gate_test.dart — shows the permission gate when a required permission is missing — Kotlin raises a contextual prompt with the child still shown rather than a blocking gate; the Kotlin test pins that divergence
- test/integration/use_cases_test.dart — cycle > day returns over real data — Kotlin's cycle screen is month-anchored: there is no DAY load to exercise
- test/integration/use_cases_test.dart — cycle > week returns over real data — Same month-anchored load — no WEEK range exists on the cycle stack

### Portable gaps

Every remaining MISSING or DIVERGED row. All of them now sit in `features/**`, `data/repository/**` or the repository-level integration surfaces, i.e. outside this wave's file ownership (`test/**/devices/**`, `**/ui/**`, `**/sensors/**`); the devices, charts and fixture areas are closed.

- [DIVERGED] test/ui/charts/metric_day_chart_test.dart — MetricDayChart > off draws the raw readings and no band
- [DIVERGED] test/integration/activity_pipeline_test.dart — a GPS session gets REAL splits, not evenly-estimated ones
- [DIVERGED] test/integration/degraded_device_test.dart — a missing Health Connect is reported as unavailable, not as an error
- [DIVERGED] test/integration/degraded_device_test.dart — a read without its permission returns EMPTY, and does not throw
- [DIVERGED] test/integration/degraded_device_test.dart — holding ONLY the sleep permission still loads sleep, and nothing else
- [DIVERGED] test/integration/degraded_device_test.dart — sync paused degrades reads to empty rather than failing
- [DIVERGED] test/integration/repositories_test.dart — health > availability resolves through the real repository
- [DIVERGED] test/integration/repositories_test.dart — heart > daily summaries carry min, max and average — not just average
- [DIVERGED] test/integration/use_cases_test.dart — sleep > day returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — sleep > week returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — sleep > month returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — sleep > year returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — sleep > an EMPTY day returns empty-but-VALID, never null and never a throw
- [DIVERGED] test/integration/use_cases_test.dart — sleep > a forced refresh returns the same shape as a normal load
- [DIVERGED] test/integration/use_cases_test.dart — heart > day returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — heart > week returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — heart > month returns over real data
- [MISSING] test/integration/use_cases_test.dart — heart > year returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — heart > an EMPTY day returns empty-but-VALID, never null and never a throw
- [DIVERGED] test/integration/use_cases_test.dart — heart > a forced refresh returns the same shape as a normal load
- [DIVERGED] test/integration/use_cases_test.dart — activities > day returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — activities > week returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — activities > month returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — activities > year returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — activities > an EMPTY day returns empty-but-VALID, never null and never a throw
- [MISSING] test/integration/use_cases_test.dart — activities > a forced refresh returns the same shape as a normal load
- [DIVERGED] test/integration/use_cases_test.dart — hydration > day returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — hydration > week returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — hydration > month returns over real data
- [MISSING] test/integration/use_cases_test.dart — hydration > year returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — hydration > an EMPTY day returns empty-but-VALID, never null and never a throw
- [DIVERGED] test/integration/use_cases_test.dart — hydration > a forced refresh returns the same shape as a normal load
- [MISSING] test/integration/use_cases_test.dart — body > day returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — body > week returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — body > month returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — body > year returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — body > an EMPTY day returns empty-but-VALID, never null and never a throw
- [DIVERGED] test/integration/use_cases_test.dart — body > a forced refresh returns the same shape as a normal load
- [DIVERGED] test/integration/use_cases_test.dart — nutrition > day returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — nutrition > week returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — nutrition > month returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — nutrition > year returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — nutrition > an EMPTY day returns empty-but-VALID, never null and never a throw
- [DIVERGED] test/integration/use_cases_test.dart — nutrition > a forced refresh returns the same shape as a normal load
- [DIVERGED] test/integration/use_cases_test.dart — cycle > month returns over real data
- [MISSING] test/integration/use_cases_test.dart — cycle > year returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — cycle > an EMPTY day returns empty-but-VALID, never null and never a throw
- [DIVERGED] test/integration/use_cases_test.dart — cycle > a forced refresh returns the same shape as a normal load
- [MISSING] test/integration/use_cases_test.dart — mindfulness > day returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — mindfulness > week returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — mindfulness > month returns over real data
- [MISSING] test/integration/use_cases_test.dart — mindfulness > year returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — mindfulness > an EMPTY day returns empty-but-VALID, never null and never a throw
- [DIVERGED] test/integration/use_cases_test.dart — mindfulness > a forced refresh returns the same shape as a normal load
- [DIVERGED] test/integration/use_cases_test.dart — caffeine > day returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — caffeine > week returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — caffeine > month returns over real data
- [MISSING] test/integration/use_cases_test.dart — caffeine > year returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — caffeine > an EMPTY day returns empty-but-VALID, never null and never a throw
- [DIVERGED] test/integration/use_cases_test.dart — caffeine > a forced refresh returns the same shape as a normal load
- [DIVERGED] test/integration/use_cases_test.dart — calories > day returns over real data
- [MISSING] test/integration/use_cases_test.dart — calories > week returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — calories > month returns over real data
- [MISSING] test/integration/use_cases_test.dart — calories > year returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — calories > an EMPTY day returns empty-but-VALID, never null and never a throw
- [MISSING] test/integration/use_cases_test.dart — calories > a forced refresh returns the same shape as a normal load
- [DIVERGED] test/integration/view_models_test.dart — sleep > loads a day of real data without erroring
- [DIVERGED] test/integration/view_models_test.dart — sleep > an EMPTY day is empty-but-valid, not an error
- [DIVERGED] test/integration/view_models_test.dart — activities > loads a day of real data without erroring
- [DIVERGED] test/integration/view_models_test.dart — activities > an EMPTY day is empty-but-valid, not an error
- [DIVERGED] test/integration/view_models_test.dart — hydration > loads a day of real data without erroring
- [DIVERGED] test/integration/view_models_test.dart — hydration > an EMPTY day is empty-but-valid, not an error
- [DIVERGED] test/integration/view_models_test.dart — heartVitalsOverview > loads a day of real data without erroring
- [DIVERGED] test/integration/view_models_test.dart — heartVitalsOverview > an EMPTY day is empty-but-valid, not an error
