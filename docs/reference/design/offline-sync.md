# Design Guidelines for Offline &amp; Sync

> **Status:** External Open Health Stack design reference. This is background material, not OpenVitals implementation policy.

<br />

## Introduction

![](https://developers.google.com/static/open-health-stack/design/images/sync/intro-offline-sync.png)

Apps that work offline give healthcare workers access to the tools they need to provide quality care, even when they are not connected to the internet. Offline apps are useful for healthcare workers who work in community environments where there is unreliable connectivity or at a healthcare facility without internet access. Offline apps can also help reduce data costs.

If healthcare workers can't use an app offline, they may not be able to complete important tasks. This can result in healthcare workers not using the app and loss of data. With the FHIR Engine library and these design guidelines we aim to improve the user experience of offline capable health apps so they're reliable and easy to use.

## Key offline principles

Consider these principles when building offline health apps:

1. Community healthcare worker apps should be designed for an offline-first experience.
2. Ensure users can complete their primary workflow offline.
3. Inform users of how frequently they need to go online to sync their device.

## Initial sync

Your app may require an initial sync to download tasks or a patient list, before a healthcare worker can begin using the app. If so, make initial sync a distinct step as part of healthcare workers getting started using the app. Provide guidance on where and when to do it, and an estimate for how much time it will take. Explain if the app needs to be open or whether healthcare workers can do other things on their device while it syncs.
![Sync my tasks. This can take up to one hour depending on your internet connection. Three steps. 1. connect to Wi-Fi or data. 2. Leave the device in a location with good internet reception. 3. You can do other things on the device while it syncs. Button: sync my tasks.](https://developers.google.com/static/open-health-stack/design/images/sync/initial-sync-do.png) Yes
Provide clear expectations of how long it will take. ![Blank my tasks screen without instructions. Syncing bar and a loading spinning wheel.](https://developers.google.com/static/open-health-stack/design/images/sync/initial-sync-dont.png) No
Don't start initial sync without providing any information about what to expect.

## Status bar

The status bar indicates if the device is offline or syncing. The status bar appears when:

1. The device is offline due to no internet connectivity
2. Data is actively syncing
3. Sync has failed
4. Sync is completed

Only show the status bar when relevant - on pages or around components that will change when data is done syncing. For example, the status is important when looking at a task list, searching a patient list or loading a patient card so the healthcare worker can recognize if the latest info has synced or not.
![Grey status bar with offline written.](https://developers.google.com/static/open-health-stack/design/images/sync/status-offline-do.png) Yes
Show offline status when relevant, for instance when loading the patient list. ![Red status bar, with error icon and offline written.](https://developers.google.com/static/open-health-stack/design/images/sync/status-offline-dont.png) No
Avoid making the connectivity status bar look like an error state. ![Light blue status bar with syncing icon and syncing written.](https://developers.google.com/static/open-health-stack/design/images/sync/status-syncing.png) Yes
When connectivity is established show that the app is syncing in the status bar. ![Light gray status bar with checkmark icon and sync complete written.](https://developers.google.com/static/open-health-stack/design/images/sync/status-sync-done.png) Yes
Show confirmation of completing sync by changing the icon to a checkmark and changing the color and text in the status bar. This helps users know that information has been completed. ![Light yellow status bar with syncing failed icon and sync failed written.](https://developers.google.com/static/open-health-stack/design/images/sync/status-sync-failed.png) Yes
If sync didn't complete, then show that it failed to complete sync. If the cause of sync failing is that the app is offline, change the status to offline. Important for people to know what is happening.

## Sync patterns

Sync works in the background to upload and download data to and from the server. The sync behavior shouldn't be distracting to the user.

Sync intervals should be set based on thresholds that are relevant to the healthcare setting the app is used in. Example: sync every 12 hours in a community setting or every 15 minutes in a healthcare facility. Having the right automatic sync intervals minimize the need for manual sync.
![Snack bar message: You're offline. Updates will be uploaded when the device reconnects.](https://developers.google.com/static/open-health-stack/design/images/sync/sync-pattern-reassure.png) Yes
Reassure users that even though the app is offline that they can still complete their tasks and that changes will be uploaded when connectivity resumes. ![Light gray status bar message: Retrying again in 3 minutes 22 seconds.](https://developers.google.com/static/open-health-stack/design/images/sync/sync-pattern-distract.png) No
Avoid going into detail of when sync is going to retry connecting to the internet. ![Patient card syncing.](https://developers.google.com/static/open-health-stack/design/images/sync/sync-pattern-prioritize.png) Yes
Prioritize what data is synced so that healthcare workers can complete their workflow. Example: in a facility where patients are handed off to another healthcare worker, make sure to prioritize syncing the patient card that was just completed. ![Patient list with 340 patients syncing.](https://developers.google.com/static/open-health-stack/design/images/sync/sync-pattern-irrelevant.png) No
Avoid syncing irrelevant info first such as the entire patient list, or old visit history that's unrelated to today's tasks.

## Sync progress indicator

A sync progress indicator appears when the content is syncing from the server. The progress indicator should visually show that sync is working.

Only add a progress indicator on key screens, such as the patient list or patient card. Provide an estimate for how long sync will take by showing what percentage has been downloaded.
![Progress bar showing 35% downloaded.](https://developers.google.com/static/open-health-stack/design/images/sync/sync-progress-bar.png) Yes
Loading bar that clearly shows that progress is happening. ![Loading wheel that is spinning.](https://developers.google.com/static/open-health-stack/design/images/sync/sync-progress-wheel.png) No
Avoid a spinning loading wheel. It's unclear if it is stalled or making progress.

## Sync timestamps

Sync timestamps inform healthcare workers of when the information was last updated. Timestamps help healthcare workers:

1. Know if they are looking at the most up to date information.
2. Understand if the app is syncing and updating as expected.
3. Provide continuity of care by picking up where the previous healthcare worker left off.

Use time stamps sparingly and only display when critical, such as on task list or patient card.
![Patient card was last updated today at 12:31pm.](https://developers.google.com/static/open-health-stack/design/images/sync/sync-time-relevant.png) Yes
Show when information was last updated (and show it in context), to help people know if it's been too long since they've synced. ![The sync timestamp May 23, 2022, 12:31, is shown on each row of content on the patient card. For example for mobile number, ID number and address.](https://developers.google.com/static/open-health-stack/design/images/sync/sync-time-all.png) No
Don't show timestamps for every piece of data, such as when the phone number was updated. Avoid a long list of what data was synced when. Showing too much time and date precision if it's been beyond 24 hours.

## Sync reminders

Sync reminders are displayed when the device has been offline for too long or the user needs to take action to sync the device.

Use reminders to communicate to users the need to sync the app and how to do it.
![Dialog. Significant changes since last sync. Connect to data or Wi-Fi to complete the sync. Button: Later, Button: Sync now.](https://developers.google.com/static/open-health-stack/design/images/sync/sync-reminder-friendly.png) Yes
Remind people at a chosen interval to sync, when appropriate. Use a friendly tone when communicating the need to sync. ![Dialog. Sync immediately. You are 12 hours overdue. Connect to data or Wi-Fi to complete the sync. Button: Later, Button: Sync now.](https://developers.google.com/static/open-health-stack/design/images/sync/sync-reminder-rude.png) No
Avoid alarmist communication or making people feel bad that they haven't synced. ![Dialog. 24 hours since last sync. Please sync again. Connect to data or Wi-Fi to complete the sync. Button: Later, Button: Sync now.](https://developers.google.com/static/open-health-stack/design/images/sync/sync-reminder-occasional.png) Yes
Remind people occasionally to sync their device, when it has gone beyond the threshold set for the type of healthcare setting (facility vs community). ![Dialog. 1 hour since last sync. Please sync again. Connect to data or Wi-Fi to complete the sync. Button: Later, Button: Sync now.](https://developers.google.com/static/open-health-stack/design/images/sync/sync-reminder-frequent.png) No
Too many reminders can be annoying and can frustrate users. Only provide error messages when action is urgent.

## Manual sync

Manual sync overrides the default sync settings and allows users to sync now. This could be done through the manual sync page or directly on the patient card. The sync page shows when the last sync happened and when the next sync is scheduled for. Ideally the automatic sync intervals minimize the need for manual sync.

Manual sync can be useful for healthcare workers who are out in the community all day and want to sync when they are back home at night with better connectivity.
![History showing last sync timestamp and next sync timestamp.](https://developers.google.com/static/open-health-stack/design/images/sync/manual-history.png) Yes
On the manual sync page, show when the last sync happened, and when the next sync is scheduled for. Include a button to "sync now". ![No sync history shown, only a sync now button.](https://developers.google.com/static/open-health-stack/design/images/sync/manual-history-none.png) No
When there's no sync history, it's hard for healthcare workers to troubleshoot and to know what to expect. ![Sync now button displayed on patient card.](https://developers.google.com/static/open-health-stack/design/images/sync/manual-patient.png) Yes
When healthcare workers are handing off patients between each other, provide a way for them to immediately sync the patient record, by displaying a sync now button on the patient card. Alternatively, this can also be achieved with an event based sync. ![Sync now button displayed next to mobile number on patient card.](https://developers.google.com/static/open-health-stack/design/images/sync/manual-everything.png) No
Avoid enabling users to select specific data to sync. It is too complex and too granular.

## Error messages \& troubleshooting

Error messages appear when a function fails to complete, such as there are no patient's in the patient list.

Show the error message on the relevant screen. Help people troubleshoot by providing a clear description of what's not working and why. Then give instructions for how to solve the problem. If the first solution didn't work, provide a second set of instructions of what to try. Always provide additional ways for people to get help, through messaging or a phone call.
![Dialog. No tasks available. Sync the task list. Check if you're connected to data or Wi-Fi. Go to settings > Network & internet. Come back to the app and tap Sync now. Button: later, Button: sync now.](https://developers.google.com/static/open-health-stack/design/images/sync/sync-error-actionable.png) Yes
Use an error message that describes what is wrong and steps for how to fix it. Include directions on how to navigate system settings. ![Dialog. Error. Something went wrong. Button: try again.](https://developers.google.com/static/open-health-stack/design/images/sync/sync-error-unhelpful.png) No
Avoid generic error messages that don't explain what's wrong or provide suggestions for how to fix it.
