package pm.gnosis.heimdall.utils

import android.content.Context
import android.text.format.DateUtils
import pm.gnosis.heimdall.R

object DateTimeUtils {
    const val SECOND_IN_MS: Long = 1000
    const val MINUTE_IN_MS = 60 * SECOND_IN_MS
    const val HOUR_IN_MS = 60 * MINUTE_IN_MS
    const val DAY_IN_MS = 24 * HOUR_IN_MS
    const val WEEK_IN_MS = 7 * DAY_IN_MS
    const val YEAR_IN_MS = 52 * WEEK_IN_MS

    /**
     * Generates a formatted time string that is as short as possible

     * @param context       Context to retrieve localisations
     * *
     * @param referenceTime long representing the time for which the result is generated
     * *
     * @return String that contains the short localised time representation
     */
    fun getShortTimeString(context: Context, referenceTime: Long): String {
        val timeDistance = System.currentTimeMillis() - referenceTime
        if (timeDistance < MINUTE_IN_MS) {
            return context.getString(R.string.date_tools_ds, timeDistance / SECOND_IN_MS)
        }
        if (timeDistance < HOUR_IN_MS) {
            return context.getString(R.string.date_tools_dm, timeDistance / MINUTE_IN_MS)
        }
        if (timeDistance < DAY_IN_MS) {
            return context.getString(R.string.date_tools_dh, timeDistance / HOUR_IN_MS)
        }
        if (timeDistance < WEEK_IN_MS) {
            return context.getString(R.string.date_tools_dd, timeDistance / DAY_IN_MS)
        }
        if (timeDistance < YEAR_IN_MS) {
            return context.getString(R.string.date_tools_dw, timeDistance / WEEK_IN_MS)
        }
        return context.getString(R.string.date_tools_dy, timeDistance / YEAR_IN_MS)
    }

    /**
     * Generates a long time string that contains the date if the reference date is longer than one
     * day ago.

     * @param context       Context to retrieve localisations
     * *
     * @param timeProvider  TimeProvider to get current time
     * *
     * @param referenceTime long representing the time for which the result is generated
     * *
     * @return String that contains the long localised time representation
     */
    fun getLongTimeString(context: Context, referenceTime: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - referenceTime
        if (diff < DateUtils.MINUTE_IN_MILLIS) {
            return context.getString(R.string.just_a_moment_ago)
        }
        var relativeTimeSpanString = DateUtils.getRelativeTimeSpanString(referenceTime, now, DateUtils.SECOND_IN_MILLIS).toString()
        if (diff > DateUtils.DAY_IN_MILLIS) {
            relativeTimeSpanString += ", " + DateUtils.formatDateTime(context, referenceTime, DateUtils.FORMAT_SHOW_TIME)
        }
        return relativeTimeSpanString
    }
}

fun Context.formatAsLongDate(referenceTime: Long): String = DateTimeUtils.getLongTimeString(this, referenceTime)
