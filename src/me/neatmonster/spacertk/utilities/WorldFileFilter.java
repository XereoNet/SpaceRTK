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

import java.io.File;
import java.io.Serializable;

import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.NameFileFilter;

@SuppressWarnings("serial")
public class WorldFileFilter extends AbstractFileFilter implements Serializable {
    public static final IOFileFilter INSTANCE = new WorldFileFilter();
    public static final IOFileFilter WORLD    = new WorldFileFilter();

    protected WorldFileFilter() {}

    @Override
    public boolean accept(final File file) {
        if (file.isDirectory())
            if (file.list(new NameFileFilter("level.dat")).length > 0)
                return true;
        return false;
    }
}
