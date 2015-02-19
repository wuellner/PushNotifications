# PushNotification plugin changelog

### 1.2.0 (TODO)

Breaking changes (Android):
- The `coldstart` property now means that the message was received when the app was not running, not that it was launched via the notification.

Bugfixes (Android):
- Correctly kickstart the application when it is not running and a push notification in the launcher is tapped.
- Do not display push notifications in status bar/launcher if the app is running.
- Clear push notifications from status bar when the app is opened, not when it again returns to the background.

### 1.1.2 (2015-02-11)

Features:
- (iOS) Custom payload content is now available under the `custom` key in the notification JSON object.

### 1.1.1 (2015-01-15)

Features:
- Messages now have `uuid` and `timestamp` fields on Android.

### 1.1.0 (2015-01-09)

Features:
- Messages now have `uuid` and `timestamp` fields on iOS.

Changes:
- Badges are no longer reset to 0 automatically when the application is opened on iOS. Instead, the `setApplicationBadgeNumber` method must be used.

Bugfixes:
- Messages with identical content are no longer discarded on iOS.
- Fixed a bug on iOS where the plugin would be left in an inconsistent state when registering, turning notifications off, unregistering and registering again.

Known issues:
- If user tries to register for push notifications on iOS with all alert types set to `false`, the registration fails with the message "User has not allowed push notifications", even though they are enabled.

### 1.0.0

Initial release.
