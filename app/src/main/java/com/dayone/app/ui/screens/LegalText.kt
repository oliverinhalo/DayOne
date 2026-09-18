package com.dayone.app.ui.screens

/**
 * Terms and privacy policy, held in the app rather than fetched.
 *
 * DayOne has no `INTERNET` permission, so a policy hosted on a web page would be
 * unreachable from inside the app. Keeping the text here means it is always readable
 * offline; [TERMS_VERSION] is bumped whenever the wording changes materially, which
 * re-prompts anyone who accepted an older version.
 */
object Legal {

    const val TERMS_VERSION = 1

    const val SUMMARY = "DayOne runs entirely on this phone. Your photos never leave it: " +
        "there are no accounts, no servers, no analytics, and the app has no permission " +
        "to use the internet at all."

    val TERMS = """
        Terms of Use

        Last updated: September 2026

        1. What DayOne is
        DayOne is an offline camera and journal app for taking one photo a day and turning
        those photos into a timelapse. It is provided as-is, for personal use.

        2. Your content is yours
        Every photo, note and video you make with DayOne belongs to you. The app claims no
        rights over any of it. Because the app is entirely offline, the developer has no
        copy of and no access to anything you create.

        3. Your responsibilities
        You are responsible for what you photograph and for having the consent of anyone
        else who appears in your photos. Do not use DayOne to record people without their
        knowledge, or in places where photography is not permitted.

        4. Backups
        Photos are stored in the app's own folder on this device. Uninstalling the app,
        clearing its storage, or losing the device will remove them. DayOne provides a
        backup export and can copy photos to your gallery; keeping usable backups is your
        responsibility.

        5. No warranty
        DayOne is provided without warranty of any kind. To the fullest extent allowed by
        law, the developer is not liable for lost photos, missed reminders, or any damage
        arising from use of the app. Reminders depend on Android's alarm and battery
        settings and cannot be guaranteed to fire at an exact moment.

        6. Age
        DayOne is not directed at children under 13, and should not be used by them without
        a parent or guardian.

        7. Changes
        These terms may change in a future version of the app. Continuing to use DayOne
        after an update means you accept the terms shipped with it.
    """.trimIndent()

    val PRIVACY = """
        Privacy Policy

        Last updated: September 2026

        Short version: DayOne collects nothing, sends nothing, and cannot reach the
        internet. There is no account to create and no data to delete from a server,
        because there is no server.

        What the app stores, and where
        - Photos you take are written to this app's private folder on your device.
        - Dates, notes, favourites, streaks, project names and settings are stored in a
          local database and preferences file on this device.
        - Videos you export, photos you choose to copy to your gallery, and backup files
          you create are written to the folders you would expect (Movies, Pictures,
          Documents) on this device.

        What the app sends: nothing
        DayOne does not request the INTERNET permission. Android therefore prevents it
        from opening any network connection, whatever its code or any bundled library
        tries to do. There is no analytics, no crash reporting, no advertising and no
        tracking of any kind.

        Face detection
        To keep your face in the same place from day to day, photos are analysed on-device
        by Google's ML Kit face detection using a model bundled inside the app. The image
        never leaves the device and no face data is stored beyond the coordinates of the
        crop, which stay in the local database.

        Permissions and why they are needed
        - Camera: to take your daily photo. Used only while the capture screen is open.
        - Notifications: to remind you to take the day's photo.
        - Alarms and reminders: to schedule those reminders at the time you choose.
        - Run at startup: to restore your reminders after the phone reboots.
        - Storage (Android 9 and older only): to save exported videos and gallery copies
          to shared folders. Newer versions of Android need no storage permission for this.

        Sharing
        Nothing is shared automatically. When you tap Share, Android's own share sheet
        hands the file to whichever app you pick; that app's own privacy policy then
        applies.

        Children
        DayOne is not directed at children under 13 and collects no personal data from
        anyone.

        Deleting your data
        Deleting a project removes its photos from the device. Uninstalling the app removes
        the database, settings and all photos in the app's folder. Files you exported to
        your gallery, Movies or Documents stay until you delete them yourself.

        Contact
        Questions about this policy can be raised on the project's GitHub repository.
    """.trimIndent()
}
