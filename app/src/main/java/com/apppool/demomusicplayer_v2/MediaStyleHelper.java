package com.apppool.demomusicplayer_v2;

//import android.app.Notification;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.NotificationCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

//Helper APIs for constructing MediaStyle notifications
public class MediaStyleHelper {
    /**
     * Build a notification using the information from the given media session. Makes heavy use
     * of {@link MediaMetadataCompat#getDescription()} to extract the appropriate information.
     * @param context Context used to construct the notification.
     * @param mediaSessionCompat Media session to get information.
     * @return A pre-built notification with information from the given media session.
     */

    public static NotificationCompat.Builder from (Context context, MediaSessionCompat mediaSessionCompat, Class DActivity)
    {

        // Code For Notification Click Event to Open Activity.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        // Adds the back stack
        stackBuilder.addParentStack(DActivity);
        // Adds the Intent to the top of the stack
        stackBuilder.addNextIntent(new Intent(context, DActivity));

        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,
                PendingIntent.FLAG_ONE_SHOT);// .FLAG_UPDATE_CURRENT);

        MediaControllerCompat mediaController = mediaSessionCompat.getController();
        MediaMetadataCompat mediaMetadata = mediaController.getMetadata();
        MediaDescriptionCompat mediaDescription = mediaMetadata.getDescription();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setContentTitle(mediaDescription.getTitle())
        .setContentText(mediaDescription.getSubtitle())
        .setSubText(mediaDescription.getDescription())
        .setLargeIcon(mediaDescription.getIconBitmap())
        .setContentIntent(resultPendingIntent)
                .setPriority(Notification.PRIORITY_HIGH)
        .setDeleteIntent(
                MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        return builder;
    }
}
