/*
 * This file is part of SpaceRTK (http://spacebukkit.xereo.net/).
 *
 * SpaceRTK is free software: you can redistribute it and/or modify it under the terms of the
 * Attribution-NonCommercial-ShareAlike Unported (CC BY-NC-SA) license as published by the Creative Common organization,
 * either version 3.0 of the license, or (at your option) any later version.
 *
 * SpaceRTK is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the Attribution-NonCommercial-ShareAlike
 * Unported (CC BY-NC-SA) license for more details.
 *
 * You should have received a copy of the Attribution-NonCommercial-ShareAlike Unported (CC BY-NC-SA) license along with
 * this program. If not, see <http://creativecommons.org/licenses/by-nc-sa/3.0/>.
 */
package me.neatmonster.spacertk.utilities;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Formats the log output
 */
public class Format extends Formatter {
    SimpleDateFormat dformat;

    /**
     * Creates a new FOrmatter
     */
    public Format() {
        dformat = new SimpleDateFormat("HH:mm:ss ");
    }

    @Override
    public String format(final LogRecord record) {
        final StringBuffer buf = new StringBuffer();
        buf.append(dformat.format(new Date(record.getMillis()))).append("[").append(record.getLevel().getName())
                .append("] ").append(formatMessage(record)).append('\n');
        if (record.getThrown() != null)
            buf.append('\t').append(record.getThrown().toString()).append('\n');
        return buf.toString();
    }
}
